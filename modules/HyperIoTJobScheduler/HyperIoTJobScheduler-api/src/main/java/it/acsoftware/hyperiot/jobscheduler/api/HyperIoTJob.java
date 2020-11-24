package it.acsoftware.hyperiot.jobscheduler.api;

import org.quartz.JobDetail;
import org.quartz.JobKey;

import java.util.Map;

/**
 * This interface put the attention on mandatory information required by Quartz scheduler
 */
public interface HyperIoTJob {

    /**
     * It returns implementation classname of org.quartz.Job interface
     * @return implementation classname of org.quartz.Job interface
     */
    String getClassName();

    /**
     * It returns job cron expression
     * @return job cron expression
     */
    String getCronExpression();

    /**
     * It returns detail of job, i.e. an instance of org.quartz.JobDetail class
     * @return Detail of job
     */
    JobDetail getJobDetail();

    /**
     * It returns identification of Job, i.e. an instance of org.quartz.JobKey class
     * @return Key of job
     */
    JobKey getJobKey();
    Map<String, Object> getJobParams();

}
