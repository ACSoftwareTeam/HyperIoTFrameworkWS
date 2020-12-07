package it.acsoftware.hyperiot.zookeeper.connector.service;

import java.util.logging.Level;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.zookeeper.connector.actions.ZookeeperConnectorAction;
import it.acsoftware.hyperiot.zookeeper.connector.model.ZookeeperConnector;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorSystemApi;
import it.acsoftware.hyperiot.zookeeper.connector.api.ZookeeperConnectorApi;

import  it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;

/**
 *
 * @author Aristide Cittadino Implementation class of ZookeeperConnectorApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = ZookeeperConnectorApi.class, immediate = true)
public final class ZookeeperConnectorServiceImpl extends  HyperIoTBaseServiceImpl  implements ZookeeperConnectorApi {
	/**
	 * Injecting the ZookeeperConnectorSystemApi
	 */
	private ZookeeperConnectorSystemApi systemService;

	/**
	 *
	 * @return The current ZookeeperConnectorSystemApi
	 */
	protected ZookeeperConnectorSystemApi getSystemService() {
		getLog().log(Level.FINEST, "invoking getSystemService, returning: {0}" , this.systemService);
		return systemService;
	}

	/**
	 *
	 * @param zookeeperConnectorSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(ZookeeperConnectorSystemApi zookeeperConnectorSystemService) {
		getLog().log(Level.FINEST, "invoking setSystemService, setting: {0}" , systemService);
		this.systemService = zookeeperConnectorSystemService ;
	}

	public boolean isLeader(HyperIoTContext hyperIoTContext, String mutexPath) {
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(ZookeeperConnector.class.getName(), ZookeeperConnectorAction.CHECK_LEADERSHIP);
		if (HyperIoTSecurityUtil.checkPermission(hyperIoTContext, ZookeeperConnector.class.getName(), action)) {
			return systemService.isLeader(mutexPath);
		}
		throw new HyperIoTUnauthorizedException();
	}

}
