package it.acsoftware.hyperiot.permission.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;
import it.acsoftware.hyperiot.permission.model.Permission;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 *         register actions for Permission
 *
 */
public class PermissionActionsBundleActivator extends HyperIoTPermissionActivator<Permission> {

	/**
	 * Bundle activator for Permission class
	 */
	public PermissionActionsBundleActivator() {
		super(Permission.class);
	}

	/**
	 * Return a list actions that have to be registered as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		log.info("Registering base CRUD actions...");
		// creates base Actions save,update,remove,find,findAll for the specified entity
		HyperIoTActionList actionList = HyperIoTActionFactory
				.createBaseCrudActionList(Permission.class.getName(), Permission.class.getName());
		log.info("Registering " + HyperIoTPermissionAction.PERMISSION.getName() + " action");
		actionList.addAction(HyperIoTActionFactory.createAction(Permission.class.getName(),
				Permission.class.getName(), HyperIoTPermissionAction.PERMISSION));
		actionList.addAction(HyperIoTActionFactory.createAction(Permission.class.getName(),
				Permission.class.getName(), HyperIoTPermissionAction.LIST_ACTIONS));
		return actionList;
	}

}
