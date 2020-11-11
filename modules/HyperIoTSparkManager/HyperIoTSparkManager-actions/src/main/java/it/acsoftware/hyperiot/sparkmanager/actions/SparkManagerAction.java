package it.acsoftware.hyperiot.sparkmanager.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate SparkManager Actions
 *
 */
public enum SparkManagerAction implements HyperIoTActionName {

	GET_JOB_STATUS("get_job_status"),
	KILL_JOB("kill_job"),
	SUBMIT_JOB("submit_job");

	private String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the SparkManager  action
	 */
	SparkManagerAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of SparkManager action
	 */
	public String getName() {
		return name;
	}

}
