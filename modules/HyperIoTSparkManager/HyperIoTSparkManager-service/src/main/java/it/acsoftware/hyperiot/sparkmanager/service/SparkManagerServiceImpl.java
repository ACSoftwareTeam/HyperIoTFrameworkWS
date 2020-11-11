package it.acsoftware.hyperiot.sparkmanager.service;

import java.util.logging.Level;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.sparkmanager.actions.SparkManagerAction;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiResponse;
import it.acsoftware.hyperiot.sparkmanager.model.SparkManager;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiSubmissionRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerSystemApi;
import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerApi;

import  it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;


/**
 * 
 * @author Aristide Cittadino Implementation class of SparkManagerApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = SparkManagerApi.class, immediate = true)
public final class SparkManagerServiceImpl extends  HyperIoTBaseServiceImpl  implements SparkManagerApi {
	/**
	 * Injecting the SparkManagerSystemApi
	 */
	private SparkManagerSystemApi systemService;
	
	/**
	 * 
	 * @return The current SparkManagerSystemApi
	 */
	protected SparkManagerSystemApi getSystemService() {
		log.log(Level.FINEST, "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 * 
	 * @param sparkManagerSystemService Injecting via OSGi DS current systemService 
	 */
	@Reference
	protected void setSystemService(SparkManagerSystemApi sparkManagerSystemService) {
		log.log(Level.FINEST, "invoking setSystemService, setting: {}" , systemService);
		this.systemService = sparkManagerSystemService ;
	}

	@Override
	public SparkRestApiResponse getStatus(HyperIoTContext context, String driverId) {
		if (HyperIoTSecurityUtil.checkPermission(context, SparkManager.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(SparkManager.class.getName(), SparkManagerAction.GET_JOB_STATUS))) {
			return systemService.getStatus(driverId);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public SparkRestApiResponse kill(HyperIoTContext context, String driverId) {
		if (HyperIoTSecurityUtil.checkPermission(context, SparkManager.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(SparkManager.class.getName(), SparkManagerAction.KILL_JOB))) {
			return systemService.kill(driverId);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public SparkRestApiResponse submitJob(HyperIoTContext context, SparkRestApiSubmissionRequest data) {
		if (HyperIoTSecurityUtil.checkPermission(context, SparkManager.class.getName(), HyperIoTActionsUtil
				.getHyperIoTAction(SparkManager.class.getName(), SparkManagerAction.SUBMIT_JOB))) {
			return systemService.submitJob(data);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

}
