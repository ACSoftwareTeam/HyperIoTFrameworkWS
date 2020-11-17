package it.acsoftware.hyperiot.role.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;
import it.acsoftware.hyperiot.role.model.Role;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 *         register actions for Role
 *
 */
public class RoleActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Bundle activator for Role class
	 */
	public RoleActionsBundleActivator() {
		super(Role.class);
	}

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(Role.class.getName(),
				Role.class.getName());
		log.info("Registering " + HyperIoTRoleAction.ASSIGN_MEMBERS.getName() + " action");
		actionList.addAction(HyperIoTActionFactory.createAction(Role.class.getName(), Role.class.getName(),
				HyperIoTRoleAction.ASSIGN_MEMBERS));
		log.info("Registering " + HyperIoTRoleAction.REMOVE_MEMBERS.getName() + " action");
		actionList.addAction(HyperIoTActionFactory.createAction(Role.class.getName(), Role.class.getName(),
				HyperIoTRoleAction.REMOVE_MEMBERS));
		log.info("Registering " + HyperIoTRoleAction.API_ACCESS.getName() + " action");
		actionList.addAction(
				HyperIoTActionFactory.createAction(Role.class.getName(), Role.class.getName(), HyperIoTRoleAction.API_ACCESS));
		log.info("Registering " + HyperIoTRoleAction.PERMISSION.getName() + " action");
		actionList.addAction(
				HyperIoTActionFactory.createAction(Role.class.getName(), Role.class.getName(), HyperIoTRoleAction.PERMISSION));
		return actionList;
	}

}
