package it.acsoftware.hyperiot.hbase.connector.service;

import com.google.protobuf.ServiceException;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.hbase.connector.actions.HBaseConnectorAction;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hbase.connector.model.*;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;


/**
 *
 * @author Aristide Cittadino Implementation class of HBaseConnectorApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = HBaseConnectorApi.class, immediate = true)
public final class HBaseConnectorServiceImpl extends  HyperIoTBaseServiceImpl implements HBaseConnectorApi {
	/**
	 * Injecting the HBaseConnectorSystemApi
	 */
	private HBaseConnectorSystemApi systemService;

	/**
	 *
	 * @return The current HBaseConnectorSystemApi
	 */
	protected HBaseConnectorSystemApi getSystemService() {
        getLog().log(Level.FINEST, "invoking getSystemService, returning: " + this.systemService);
		return systemService;
	}

	/**
	 *
	 * @param hBaseConnectorSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(HBaseConnectorSystemApi hBaseConnectorSystemService) {
        getLog().log(Level.FINEST, "invoking setSystemService, setting: " + systemService);
		this.systemService = hBaseConnectorSystemService ;
	}

	@Override
	public void checkConnection(HyperIoTContext context) throws IOException, HyperIoTUnauthorizedException, ServiceException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.CHECK_CONNECTION))) {
			systemService.checkConnection();
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public void createTable(HyperIoTContext context, String tableName, List<String> columnFamilies) throws IOException, HyperIoTUnauthorizedException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.CREATE_TABLE))) {
			systemService.createTable(tableName, columnFamilies);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public void deleteData(HyperIoTContext context, String tableName, String rowKey)
			throws IOException, HyperIoTUnauthorizedException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.DELETE_DATA))) {
			systemService.deleteData(tableName, rowKey);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
    public void disableTable(HyperIoTContext context, String tableName) throws IOException, HyperIoTUnauthorizedException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.DISABLE_TABLE))) {
			systemService.disableTable(tableName);
		}
		else {
			throw new HyperIoTUnauthorizedException();
		}
    }

    @Override
    public void dropTable(HyperIoTContext context, String tableName) throws IOException, HyperIoTUnauthorizedException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.DROP_TABLE))) {
			systemService.dropTable(tableName);
		}
		else {
			throw new HyperIoTUnauthorizedException();
		}
    }

    @Override
    public void enableTable(HyperIoTContext context, String tableName) throws IOException, HyperIoTUnauthorizedException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.ENABLE_TABLE))) {
			systemService.enableTable(tableName);
		}
		else {
			throw new HyperIoTUnauthorizedException();
		}
    }

	@Override
	public void insertData(HyperIoTContext context, String tableName, String rowKey, String columnFamily, String column, String cellValue)
			throws IOException, HyperIoTUnauthorizedException {
		if (HyperIoTSecurityUtil.checkPermission(context, HBaseConnector.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HBaseConnector.class.getName(), HBaseConnectorAction.INSERT_DATA))) {
			systemService.insertData(tableName, rowKey, columnFamily, column, cellValue);
		}
		else {
			throw new HyperIoTUnauthorizedException();
		}
	}

}
