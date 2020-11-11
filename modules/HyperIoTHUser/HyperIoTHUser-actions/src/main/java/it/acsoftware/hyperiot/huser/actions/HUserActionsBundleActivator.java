package it.acsoftware.hyperiot.huser.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;
import it.acsoftware.hyperiot.huser.model.HUser;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 *         register action for HUser
 *
 */
public class HUserActionsBundleActivator extends HyperIoTPermissionActivator<HUser> {

	/**
	 * Bundle activator for HUser class
	 */
	public HUserActionsBundleActivator() {
		super(HUser.class);
	}

	/**
	 * Return a list actions that have to be registed as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		log.info("Registering base CRUD actions...");
		// creates base Actions save,update,remove,find,findAll for the specified entity
		HyperIoTActionList actionList = HyperIoTActionFactory
				.createBaseCrudActionList(HUser.class.getName(), HUser.class.getName());
		actionList.addAction(HyperIoTActionFactory.createAction(HUser.class.getName(),
				HUser.class.getName(), HyperIoTHUserAction.IMPERSONATE));
		return actionList;
	}

}
