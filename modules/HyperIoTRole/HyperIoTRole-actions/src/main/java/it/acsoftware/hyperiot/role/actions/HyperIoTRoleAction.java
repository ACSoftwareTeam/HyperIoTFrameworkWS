package it.acsoftware.hyperiot.role.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate Role Actions
 *
 */
public enum HyperIoTRoleAction implements HyperIoTActionName {

	// TO DO: add enumerations here
	ASSIGN_MEMBERS("assign_members"), REMOVE_MEMBERS("unassign"), API_ACCESS("api_access"), PERMISSION("permission");

	/**
	 * String name for Role Action
	 */
	private String name;

	/**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the role action
	 */
	private HyperIoTRoleAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of role action
	 */
	public String getName() {
		return name;
	}

}
