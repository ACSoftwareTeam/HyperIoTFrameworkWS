package it.acsoftware.hyperiot.company.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.company.model.Company;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for Company
 *
 */
public class CompanyActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Bundle activator for Company class
	 */
	public CompanyActionsBundleActivator() {
		super(Company.class);
	}

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(Company.class.getName(),
				Company.class.getName());
		//TO DO: add more actions to actionList here...
		return actionList;
	}

}
