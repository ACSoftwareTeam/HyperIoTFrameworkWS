package it.acsoftware.hyperiot.jobscheduler.api;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTJob;

/**
 * 
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface JobSchedulerSystemApi extends HyperIoTBaseSystemApi {

    /**
     * This method adds job to be scheduled
     * @param job Job to be scheduled
     */
    void addJob(HyperIoTJob job);

    /**
     * This method removes job from being scheduled
     * @param job Job to be removed
     */
    void deleteJob(HyperIoTJob job);

    /**
     * This method update scheduling of given job
     * @param job Job to be scheduled
     */
    void updateJob(HyperIoTJob job);

}