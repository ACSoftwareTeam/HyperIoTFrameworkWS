package it.acsoftware.hyperiot.hbase.connector.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnector;

/**
 * 
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for HBaseConnector
 *
 */
public class HBaseConnectorActionsBundleActivator extends HyperIoTPermissionActivator<HBaseConnector> {

	/**
	 * Bundle activator for HBaseConnector class
	 */
	public HBaseConnectorActionsBundleActivator() {
		super(HBaseConnector.class);
	}
	
	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		// creates base Actions save,update,remove,find,findAll for the specified entity
		log.info("Registering base CRUD actions...");
		HyperIoTActionList actionList = HyperIoTActionFactory.createEmptyActionList();
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.CHECK_CONNECTION));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.CREATE_TABLE));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.DELETE_DATA));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.DISABLE_TABLE));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.DROP_TABLE));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.ENABLE_TABLE));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.INSERT_DATA));
		actionList.addAction(HyperIoTActionFactory.createAction(HBaseConnector.class.getName(),
				HBaseConnector.class.getName(), HBaseConnectorAction.READ_DATA));
		return actionList;
	}

}
