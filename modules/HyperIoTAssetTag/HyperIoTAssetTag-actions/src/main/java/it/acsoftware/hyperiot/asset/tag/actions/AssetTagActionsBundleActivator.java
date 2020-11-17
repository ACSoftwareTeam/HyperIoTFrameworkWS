package it.acsoftware.hyperiot.asset.tag.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for AssetTag
 *
 */
public class AssetTagActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Bundle activator for AssetTag class
	 */
	public AssetTagActionsBundleActivator() {
		super(AssetTag.class);
	}

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(AssetTag.class.getName(),
				AssetTag.class.getName());
		//TO DO: add more actions to actionList here...
		return actionList;
	}

}
