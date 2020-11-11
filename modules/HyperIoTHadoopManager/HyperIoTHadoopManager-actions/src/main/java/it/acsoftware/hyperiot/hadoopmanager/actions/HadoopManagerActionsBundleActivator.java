package it.acsoftware.hyperiot.hadoopmanager.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.hadoopmanager.model.HadoopManager;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for HadoopManager
 *
 */
public class HadoopManagerActionsBundleActivator extends HyperIoTPermissionActivator<HadoopManager> {

	/**
	 * Bundle activator for HadoopManager class
	 */
	public HadoopManagerActionsBundleActivator() {
		super(HadoopManager.class);
	}
	
	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		HyperIoTActionList actionList = HyperIoTActionFactory.createEmptyActionList();
		actionList.addAction(HyperIoTActionFactory.createAction(HadoopManager.class.getName(),
				HadoopManager.class.getName(), HadoopManagerAction.COPY_FILE));
		actionList.addAction(HyperIoTActionFactory.createAction(HadoopManager.class.getName(),
				HadoopManager.class.getName(), HadoopManagerAction.DELETE_FILE));
		return actionList;
	}

}
