package it.acsoftware.hyperiot.jobscheduler.service;

import it.acsoftware.hyperiot.base.api.HyperIoTLeadershipRegistrar;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTJob;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hproject.algorithm.api.HProjectAlgorithmSystemApi;
import it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm;
import it.acsoftware.hyperiot.jobscheduler.api.JobSchedulerSystemApi;
import it.acsoftware.hyperiot.jobscheduler.job.HyperIoTQuartzSparkJob;
import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorSystemApi;
import org.apache.curator.framework.recipes.leader.LeaderLatchListener;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.text.ParseException;
import java.util.Collection;
import java.util.logging.Level;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

/**
 * @author Aristide Cittadino Implementation class of the JobSchedulerSystemApi
 * interface. This  class is used to implements all additional
 * methods to interact with the persistence layer.
 */
@Component(service = JobSchedulerSystemApi.class, immediate = true)
public final class JobSchedulerSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements JobSchedulerSystemApi {

    private Scheduler scheduler;
    private ZookeeperConnectorSystemApi zookeeperConnectorSystemApi;
    private HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi;
    private HyperIoTLeadershipRegistrar jobSchedulerLeadershipRegistrar;

    @Activate
    public void onActivate(BundleContext context) {
        Collection<HProjectAlgorithm> hProjectAlgorithms =
                hProjectAlgorithmSystemApi.findAll(null, null);
        try {
            // create the scheduler
            log.info("Create scheduler");
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            if (zookeeperConnectorSystemApi.isLeader(jobSchedulerLeadershipRegistrar.getLeadershipPath())) {
                log.info("Scheduler is on zk leader, start");
                scheduler.start();
            }
            for (HProjectAlgorithm hProjectAlgorithm : hProjectAlgorithms)
                addJob(hProjectAlgorithm);
            addLeaderLatchListener();
        } catch (SchedulerException e) {
            log.severe(e.getMessage());
        }
    }

    @Deactivate
    public void onDeactivate() throws SchedulerException {
        scheduler.shutdown();
    }

    @Override
    public void addJob(HyperIoTJob job) {
        HProjectAlgorithm hProjectAlgorithm = (HProjectAlgorithm) job;
        JobKey jobKey = getJobKey(hProjectAlgorithm);
        log.fine("Adding job " + jobKey + " to scheduler");
        try {
            if (!scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = getJobDetail(hProjectAlgorithm, jobKey);
                // Add the the job to the store of scheduler
                scheduler.addJob(jobDetail, false);
                schedule(hProjectAlgorithm, jobKey);
            }
            else
                log.warning("Job " + jobKey + " already exists, it has not been added");
        } catch(ParseException | SchedulerException e){
            String SCHEDULING_ERROR = "Could not schedule job of algorithm {0} on project {1} with cron expression {2}";
            log.log(Level.SEVERE, SCHEDULING_ERROR, new String[]{hProjectAlgorithm.getAlgorithm().getName(),
                    hProjectAlgorithm.getProject().getName(), hProjectAlgorithm.getCronExpression()});
            log.warning(e.getMessage());
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
                    log.severe("Scheduler has not been started!");
                    log.severe(e.getMessage());
                }
            }

            @Override
            public void notLeader() {
                log.info("This node is not a zk leader anymore, standby scheduler");
                try {
                    scheduler.standby();
                } catch (SchedulerException e) {
                    log.severe("Scheduler has not been paused!");
                    log.severe(e.getMessage());
                }
            }

        }, leadershipPath);
    }

    @Override
    public void deleteJob(HyperIoTJob job) {
        HProjectAlgorithm hProjectAlgorithm = (HProjectAlgorithm) job;
        JobKey jobKey = getJobKey(hProjectAlgorithm);
        log.fine("Removing job " + jobKey + " from scheduler");
        try {
            if(!scheduler.checkExists(jobKey))
                log.warning("Job " + jobKey + " does not exist");
            else
                unschedule(hProjectAlgorithm, jobKey);
        } catch (SchedulerException e) {
            log.severe("Job" + jobKey + " has not been removed!");
            log.severe(e.getMessage());
        }
    }

    /**
     * It returns detail of job
     * @param hProjectAlgorithm Job which obtain details from
     * @param jobKey Id of job
     * @return JobDetail
     */
    private JobDetail getJobDetail(HProjectAlgorithm hProjectAlgorithm, JobKey jobKey) {
        return newJob(HyperIoTQuartzSparkJob.class)
                .withIdentity(jobKey)
                .usingJobData("algorithmId", hProjectAlgorithm.getAlgorithm().getId())
                .usingJobData("projectId", hProjectAlgorithm.getProject().getId())
                .usingJobData("cronExpression", hProjectAlgorithm.getCronExpression())
                .usingJobData("config", hProjectAlgorithm.getConfig())
                .storeDurably() // Define a durable job instance (durable jobs can exist without triggers)
                .build();
    }

    /**
     * Get key of job, i.e. an HProjectAlgorithm entity. Each key is a pair, i.e. name and group.
     * Name is id of HProjectAlgorithm
     * Group is a string with the following format: "<algorithmId,projectId>"
     * @param hProjectAlgorithm Job which derive key from
     * @return Key of job
     */
    private JobKey getJobKey(HProjectAlgorithm hProjectAlgorithm) {
        String group = "<" +
                hProjectAlgorithm.getAlgorithm().getId() +
                "," +
                hProjectAlgorithm.getProject().getId() +
                ">";
        return new JobKey(String.valueOf(hProjectAlgorithm.getId()), group);
    }

    /**
     * This method tells to quartz scheduler to schedule job
     * @param hProjectAlgorithm Job to be scheduled
     * @param jobKey Id of job
     * @throws ParseException ParseException
     * @throws SchedulerException SchedulerException
     */
    private void schedule(HProjectAlgorithm hProjectAlgorithm, JobKey jobKey)
            throws ParseException, SchedulerException {
        log.fine("Scheduling job " + jobKey);
        // Validate cron expression
        CronExpression.validateExpression(hProjectAlgorithm.getCronExpression());
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
                .withSchedule(cronSchedule(hProjectAlgorithm.getCronExpression()))
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
     * @param hProjectAlgorithm Job to be removed
     * @param jobKey Id of job
     */
    private void unschedule(HProjectAlgorithm hProjectAlgorithm, JobKey jobKey) throws SchedulerException {
        log.fine("Unscheduling job " + hProjectAlgorithm.getId());
        if (scheduler.checkExists(jobKey)) {
            scheduler.deleteJob(jobKey);
            log.fine("Job " + hProjectAlgorithm.getId() + " has been unscheduled successfully");
        }
        else
            log.fine("Job " + hProjectAlgorithm.getId() + " has not been scheduled yet");
    }

    @Override
    public void updateJob(HyperIoTJob job) {
        HProjectAlgorithm hProjectAlgorithm = (HProjectAlgorithm) job;
        log.fine("Updating job " + hProjectAlgorithm.getId() + " to scheduler");
        JobKey jobKey = getJobKey(hProjectAlgorithm);
        try {
            if (scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = getJobDetail(hProjectAlgorithm, jobKey);
                scheduler.addJob(jobDetail, true);
                schedule(hProjectAlgorithm, jobKey);
            }
            else
                log.warning("Job does not exists, it has been neither updated nor scheduled");
        } catch (ParseException | SchedulerException e) {
            log.severe("Job" + jobKey + " has not been updated!");
            log.severe(e.getMessage());
        }
    }

    @Reference
    public void setHProjectAlgorithmSystemApi(HProjectAlgorithmSystemApi hProjectAlgorithmSystemApi) {
        this.hProjectAlgorithmSystemApi = hProjectAlgorithmSystemApi;
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
