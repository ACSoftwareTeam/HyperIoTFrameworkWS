package it.acsoftware.hyperiot.jobscheduler.job;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class HyperIoTQuartzSparkJob implements Job {

    private static final Logger LOGGER = LoggerFactory.getLogger(HyperIoTQuartzSparkJob.class);
    private long algorithmId;
    private String config;
    private long projectId;
    private String cronExpression;

    public HyperIoTQuartzSparkJob() { }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        LOGGER.info("HyperIoTQuartzSparkJob has been fired!");
        /*
        TODO
            You have all properties declared above (Quartz invokes setter methods on them).
            Decide weather retrieve json object containing HBase configuration here or on SparkManager
            Inject SparkManagerSystemApi and submit job
            PS: check if job is ready to accept algorithmId and projectId outside of json config
         */
    }

    public void setAlgorithmId(long algorithmId) {
        this.algorithmId = algorithmId;
    }

    public void setConfig(String config) {
        this.config = config;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

}
