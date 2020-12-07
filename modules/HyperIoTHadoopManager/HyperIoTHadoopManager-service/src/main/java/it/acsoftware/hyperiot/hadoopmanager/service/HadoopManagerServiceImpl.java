package it.acsoftware.hyperiot.hadoopmanager.service;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.hadoopmanager.actions.HadoopManagerAction;
import it.acsoftware.hyperiot.hadoopmanager.model.HadoopManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerApi;

import  it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;

/**
 *
 * @author Aristide Cittadino Implementation class of HadoopManagerApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = HadoopManagerApi.class, immediate = true)
public final class HadoopManagerServiceImpl extends HyperIoTBaseServiceImpl implements HadoopManagerApi {
	/**
	 * Injecting the HadoopManagerSystemApi
	 */
	private HadoopManagerSystemApi systemService;

	/**
	 *
	 * @return The current HadoopManagerSystemApi
	 */
	protected HadoopManagerSystemApi getSystemService() {
        getLog().log(Level.FINEST, "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 *
	 * @param hadoopManagerSystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(HadoopManagerSystemApi hadoopManagerSystemService) {
        getLog().log(Level.FINEST, "invoking setSystemService, setting: {}" , systemService);
		this.systemService = hadoopManagerSystemService ;
	}

	@Override
	public void copyFile(HyperIoTContext context, File file, String path, boolean deleteSource)
			throws IOException  {
		if (HyperIoTSecurityUtil.checkPermission(context, HadoopManager.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HadoopManager.class.getName(), HadoopManagerAction.COPY_FILE))) {
			systemService.copyFile(file, path, deleteSource);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public void deleteFile(HyperIoTContext context, String path) throws IOException {
		if (HyperIoTSecurityUtil.checkPermission(context, HadoopManager.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(HadoopManager.class.getName(), HadoopManagerAction.DELETE_FILE))) {
			systemService.deleteFile(path);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

}
