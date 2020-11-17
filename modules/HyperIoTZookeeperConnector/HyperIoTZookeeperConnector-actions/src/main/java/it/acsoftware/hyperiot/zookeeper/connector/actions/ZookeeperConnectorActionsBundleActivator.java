package it.acsoftware.hyperiot.zookeeper.connector.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;

import it.acsoftware.hyperiot.zookeeper.connector.model.ZookeeperConnector;

/**
 *
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for ZookeeperConnector
 *
 */
public class ZookeeperConnectorActionsBundleActivator extends HyperIoTPermissionActivator {

	/**
	 * Bundle activator for ZookeeperConnector class
	 */
	public ZookeeperConnectorActionsBundleActivator() {
		super(ZookeeperConnector.class);
	}

	/**
	 * Return a list actions that have to be registerd as OSGi components
	 */
	@Override
	public HyperIoTActionList getActions() {
		HyperIoTActionList actionList = HyperIoTActionFactory.createEmptyActionList();
		actionList.addAction(HyperIoTActionFactory.createAction(ZookeeperConnector.class.getName(),
				ZookeeperConnector.class.getName(), ZookeeperConnectorAction.CHECK_LEADERSHIP));
		return actionList;
	}

}
