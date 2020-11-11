package it.acsoftware.hyperiot.sparkmanager.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.sparkmanager.model.SparkManager;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for SparkManager
 *
 */
public class SparkManagerActionsBundleActivator extends HyperIoTPermissionActivator<SparkManager> {

	/**
	 * Bundle activator for SparkManager class
	 */
	public SparkManagerActionsBundleActivator() {
		super(SparkManager.class);
	}
	
	/**
	 * Return a list actions that have to be registered as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		HyperIoTActionList actionList = HyperIoTActionFactory.createEmptyActionList();
		actionList.addAction(HyperIoTActionFactory.createAction(SparkManager.class.getName(),
				SparkManager.class.getName(), SparkManagerAction.GET_JOB_STATUS));
		actionList.addAction(HyperIoTActionFactory.createAction(SparkManager.class.getName(),
				SparkManager.class.getName(), SparkManagerAction.KILL_JOB));
		actionList.addAction(HyperIoTActionFactory.createAction(SparkManager.class.getName(),
				SparkManager.class.getName(), SparkManagerAction.SUBMIT_JOB));
		return actionList;
	}

}
