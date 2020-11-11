package it.acsoftware.hyperiot.hbase.connector.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;

/**
 * 
 * @author Aristide Cittadino Model class that enumerate HBaseConnector Actions
 *
 */
public enum HBaseConnectorAction implements HyperIoTActionName {
	
	//TO DO: add enumerations here
	CHECK_CONNECTION("check_connection"),
	CREATE_TABLE("create_table"),
	DELETE_DATA("delete_data"),
	DISABLE_TABLE("disable_data"),
	DROP_TABLE("drop_table"),
	ENABLE_TABLE("enable_table"),
	INSERT_DATA("insert_data"),
	READ_DATA("read_data");

	private final String name;

     /**
	 * Role Action with the specified name.
	 * 
	 * @param name parameter that represent the HBaseConnector  action
	 */
	HBaseConnectorAction(String name) {
		this.name = name;
	}

	/**
	 * Gets the name of HBaseConnector action
	 */
	public String getName() {
		return name;
	}

}
