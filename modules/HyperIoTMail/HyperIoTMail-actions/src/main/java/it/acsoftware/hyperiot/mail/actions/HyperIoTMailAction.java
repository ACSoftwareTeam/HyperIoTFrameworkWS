package it.acsoftware.hyperiot.mail.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate Mail Actions
 *
 */
public enum HyperIoTMailAction implements HyperIoTActionName {
	
	//TO DO: add enumerations here
	ACTION_ENUM("action_enum");

	private String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the Mail  action
	 */
	private HyperIoTMailAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of Mail action
	 */
	public String getName() {
		return name;
	}

}
