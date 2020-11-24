package it.acsoftware.hyperiot.jobscheduler.service;

import it.acsoftware.hyperiot.base.api.HyperIoTLeadershipRegistrar;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.jobscheduler.api.HyperIoTJob;
import it.acsoftware.hyperiot.jobscheduler.api.JobSchedulerSystemApi;
import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorSystemApi;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Aristide Cittadino Implementation class of the JobSchedulerSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = JobSchedulerSystemApi.class, immediate = true)
public final class JobSchedulerSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements JobSchedulerSystemApi {

    private static final Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    private Scheduler scheduler;
    private ZookeeperConnectorSystemApi zookeeperConnectorSystemApi;
    private HyperIoTLeadershipRegistrar jobSchedulerLeadershipRegistrar;
    private static Properties quartzProps;

    @Activate
    public void onActivate(BundleContext context) {
        try {
            // create the scheduler
            log.info("Get scheduler");
            StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(getQuartzProperties(context));
            scheduler = stdSchedulerFactory.getScheduler();
            if (zookeeperConnectorSystemApi.isLeader(jobSchedulerLeadershipRegistrar.getLeadershipPath())) {
                log.info("Scheduler is on zk leader, start");
                scheduler.start();
            }
            addLeaderLatchListener();
        } catch (SchedulerException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Deactivate
    public void onDeactivate() {
        try {
            if (scheduler != null)
                scheduler.shutdown();
        } catch (SchedulerException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @Override
    public void addJob(HyperIoTJob job) throws HyperIoTRuntimeException {
        JobDetail jobDetail = job.getJobDetail();
        if (jobDetail == null) {
            String errorMsg = "Could not add job: jobDetail was null";
            log.severe(errorMsg);
            throw new HyperIoTRuntimeException(errorMsg);
        }
        JobKey jobKey = jobDetail.getKey();
        log.log(Level.INFO, "Adding job {0} to scheduler", jobKey);
        try {
            if (!scheduler.checkExists(jobKey)) {
                // Add the the job to the store of scheduler
                scheduler.addJob(jobDetail, false);
                schedule(job);
            }
            else
                log.log(Level.WARNING, "Job {0} already exists, it has not been added", jobKey);
        } catch(ParseException | SchedulerException e){
            log.log(Level.SEVERE, "Could not schedule job {0}: {1}", new Object[] {jobKey, e.getMessage()});
            throw new HyperIoTRuntimeException(e);
        }
    }

    /**
     * This method adds a LeaderLatchListener
     */
    private void addLeaderLatchListener() {
        String leadershipPath = jobSchedulerLeadershipRegistrar.getLeadershipPath();
        zookeeperConnectorSystemApi.addListener(new LeaderLatchListener() {

            @Override
            public void isLeader() {
                log.info("This node has became a zk leader, start scheduler");
                try {
                    scheduler.start();
                } catch (SchedulerException e) {
                    log.log(Level.SEVERE, "Scheduler has not been started: {0}", e.getMessage());
                }
            }

            @Override
            public void notLeader() {
                log.info("This node is not a zk leader anymore, standby scheduler");
                try {
                    scheduler.standby();
                } catch (SchedulerException e) {
                    log.log(Level.SEVERE, "Scheduler has not been paused: {0}", e.getMessage());
                }
            }

        }, leadershipPath);
    }

    @Override
    public void deleteJob(HyperIoTJob job) throws HyperIoTRuntimeException {
        JobKey jobKey = job.getJobKey();
        if (jobKey == null) {
            String errorMsg = "Could not delete job: jobKey was null";
            log.severe(errorMsg);
            throw new HyperIoTRuntimeException(errorMsg);
        }
        log.log(Level.INFO, "Removing job {0} from scheduler", jobKey);
        try {
            if(!scheduler.checkExists(jobKey))
                log.log(Level.WARNING, "Job {0} does not exist", jobKey);
            else
                unschedule(job);
        } catch (SchedulerException e) {
            log.log(Level.SEVERE, "Job" + jobKey + " has not been removed: {1}",
                    new Object[] {jobKey, e.getMessage()});
            throw new HyperIoTRuntimeException(e);
        }
    }

    private static Properties getQuartzProperties(BundleContext context) {
        if (quartzProps == null) {
            ServiceReference<?> configurationAdminReference = context
                    .getServiceReference(ConfigurationAdmin.class.getName());

            if (configurationAdminReference != null) {
                ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                        .getService(configurationAdminReference);
                try {
                    Configuration configuration = confAdmin
                            .getConfiguration("it.acsoftware.hyperiot.scheduler");
                    if (configuration != null && configuration.getProperties() != null) {
                        Dictionary<String, Object> dict = configuration.getProperties();
                        List<String> keys = Collections.list(dict.keys());
                        Map<String, Object> dictCopy = keys.stream()
                                .collect(Collectors.toMap(Function.identity(), dict::get));
                        quartzProps = new Properties();
                        quartzProps.putAll(dictCopy);
                        log.log(Level.FINER, "Loaded properties For HyperIoT: {0}", quartzProps);
                        return quartzProps;
                    }
                } catch (IOException e) {
                    log.log(Level.SEVERE,
                            "Impossible to find it.acsoftware.hyperiot.scheduler.cfg, please create it!", e);
                    return null;
                }
            }
            log.log(Level.SEVERE,
                    "Impossible to find it.acsoftware.hyperiot.scheduler.cfg, please create it!");
            return null;
        }
        return quartzProps;
    }

    /**
     * This method tells to quartz scheduler to schedule job
     * @param job Job to be scheduled
     * @throws ParseException ParseException
     * @throws SchedulerException SchedulerException
     */
    private void schedule(HyperIoTJob job)
            throws ParseException, SchedulerException {
        JobKey jobKey = job.getJobKey();
        log.log(Level.INFO, "Scheduling job {0}", jobKey);
        String cronExpression = job.getCronExpression();
        // Validate cron expression
        CronExpression.validateExpression(cronExpression);
        // Create trigger of job
        // If job has to be updated, its trigger exists: update it too
        TriggerKey triggerKey = new TriggerKey(jobKey.getName(), jobKey.getGroup());
        TriggerBuilder triggerBuilder;
        Trigger oldTrigger = null;
        if (scheduler.checkExists(triggerKey)) {
            oldTrigger = scheduler.getTrigger(triggerKey);
            triggerBuilder = oldTrigger.getTriggerBuilder();
        }
        else {
            triggerBuilder = newTrigger().withIdentity(triggerKey);
        }
        Trigger trigger = triggerBuilder
                .withSchedule(cronSchedule(cronExpression))
                .forJob(jobKey)
                .build();
        // Tell quartz to schedule the job using trigger set above
        if (oldTrigger == null)
            scheduler.scheduleJob(trigger);
        else
            scheduler.rescheduleJob(triggerKey, trigger);
    }

    /**
     * This method tells to quartz scheduler to remove job
     * @param job Job to be removed
     */
    private void unschedule(HyperIoTJob job) throws SchedulerException {
        JobKey jobKey = job.getJobKey();
        log.log(Level.INFO, "Unscheduling job {0}", jobKey);
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            log.log(Level.INFO, "Job {0} has been unscheduled successfully", jobKey);
        }
        else
            log.log(Level.INFO, "Job {0} has not been scheduled yet", jobKey);
    }

    @Override
    public void updateJob(HyperIoTJob job) throws HyperIoTRuntimeException {
        JobDetail jobDetail = job.getJobDetail();
        if (jobDetail == null) {
            String errorMsg = "Could not update job: jobDetail was null";
            log.severe(errorMsg);
            throw new HyperIoTRuntimeException(errorMsg);
        }
        JobKey jobKey = job.getJobKey();
        log.log(Level.INFO, "Updating job {0} to scheduler", jobKey);
        try {
            if (scheduler.checkExists(jobKey)) {
                scheduler.addJob(jobDetail, true);
                schedule(job);
            }
            else
                log.warning("Job does not exists, it has been neither updated nor scheduled");
        } catch (ParseException | SchedulerException e) {
            log.log(Level.SEVERE, "Job {} has not been updated: ", new Object[] {jobKey, e.getMessage()});
            throw new HyperIoTRuntimeException(e);
        }
    }

    @Reference
    public void setJobSchedulerLeadershipRegistrar(HyperIoTLeadershipRegistrar jobSchedulerLeadershipRegistrar) {
        this.jobSchedulerLeadershipRegistrar = jobSchedulerLeadershipRegistrar;
    }

    @Reference
    public void setZookeeperConnectorSystemApi(ZookeeperConnectorSystemApi zookeeperConnectorSystemApi) {
        this.zookeeperConnectorSystemApi = zookeeperConnectorSystemApi;
    }

}
