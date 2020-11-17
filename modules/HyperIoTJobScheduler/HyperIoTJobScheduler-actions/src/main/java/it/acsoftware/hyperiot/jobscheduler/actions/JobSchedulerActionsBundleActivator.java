package it.acsoftware.hyperiot.jobscheduler.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.jobscheduler.model.JobScheduler;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for JobScheduler
 *
 */
public class JobSchedulerActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Bundle activator for JobScheduler class
	 */
	public JobSchedulerActionsBundleActivator() {
		super(JobScheduler.class);
	}

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(JobScheduler.class.getName(),
				JobScheduler.class.getName());
		//TO DO: add more actions to actionList here...
		return actionList;
	}

}
