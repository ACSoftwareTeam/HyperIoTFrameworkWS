package it.acsoftware.hyperiot.asset.category.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.asset.category.model.AssetCategory;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for AssetCategory
 *
 */
public class AssetCategoryActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Bundle activator for AssetCategory class
	 */
	public AssetCategoryActionsBundleActivator() {
		super(AssetCategory.class);
	}

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(AssetCategory.class.getName(),
				AssetCategory.class.getName());
		//TO DO: add more actions to actionList here...
		return actionList;
	}

}
