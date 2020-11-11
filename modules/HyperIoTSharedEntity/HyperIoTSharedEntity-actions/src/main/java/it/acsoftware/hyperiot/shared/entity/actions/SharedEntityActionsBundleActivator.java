package it.acsoftware.hyperiot.shared.entity.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for SharedEntity
 *
 */
public class SharedEntityActionsBundleActivator extends HyperIoTPermissionActivator<SharedEntity> {

	/**
	 * Bundle activator for SharedEntity class
	 */
	public SharedEntityActionsBundleActivator() {
		super(SharedEntity.class);
	}
	
	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(SharedEntity.class.getName(),
				SharedEntity.class.getName());
		return actionList;
	}

}
