package it.acsoftware.hyperiot.mail.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.mail.model.MailTemplate;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for Mail
 *
 */
public class MailActionsBundleActivator extends HyperIoTPermissionActivator<MailTemplate> {

	/**
	 * Bundle activator for Mail class
	 */
	public MailActionsBundleActivator() {
		super(MailTemplate.class);
	}
	
	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createBaseCrudActionList(MailTemplate.class.getName(),
				MailTemplate.class.getName());
		//TO DO: add more actions to actionList here...
		return actionList;
	}

}
