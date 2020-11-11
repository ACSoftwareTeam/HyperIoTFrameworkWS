/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * Written by Gene <generoso.martello@acsoftware.it>, March 2019
 *
 */
package it.acsoftware.hyperiot.stormmanager.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerApi;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of StormManagerApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = StormManagerApi.class, immediate = true)
public final class StormManagerServiceImpl extends HyperIoTBaseServiceImpl implements StormManagerApi {

    //Topologies permissions are managed by HProject action list
    private static final String resourceName = HProject.class.getName();

    /**
     * Injecting the StormManagerSystemApi
     */
    private StormManagerSystemApi systemService;

    /**
     * Injecting HProjectSystemApii
     */
    private HProjectSystemApi hProjectSystemApi;

    /**
     * @return The current StormManagerSystemApi
     */
    protected StormManagerSystemApi getSystemService() {
        log.log(Level.FINEST, "invoking getSystemService, returning: {0}", this.systemService);
        return systemService;
    }

    /**
     * @param stormManagerSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(StormManagerSystemApi stormManagerSystemService) {
        log.log(Level.FINEST, "invoking setSystemService, setting: {0}", systemService);
        this.systemService = stormManagerSystemService;
    }

    /**
     * @param hProjectSystemApi
     */
    @Reference
    public void sethProjectSystemApi(HProjectSystemApi hProjectSystemApi) {
        this.hProjectSystemApi = hProjectSystemApi;
    }

    @Override
    public TopologyInfo getTopologyStatus(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, null, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        if (HyperIoTSecurityUtil.checkPermission(context, project, HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTHProjectAction.GET_TOPOLOGY))) {
            return systemService.getTopologyStatus(projectId);
        }
        throw new HyperIoTUnauthorizedException();
    }

    @Override
    public void activateTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, null, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        if (HyperIoTSecurityUtil.checkPermission(context, project, HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTHProjectAction.ACTIVATE_TOPOLOGY))) {
            systemService.activateTopology(systemService.getTopologyName(projectId));
        } else throw new HyperIoTUnauthorizedException();
    }

    @Override
    public void deactivateTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, null, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        if (HyperIoTSecurityUtil.checkPermission(context, project, HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTHProjectAction.DEACTIVATE_TOPOLOGY))) {
            systemService.deactivateTopology(systemService.getTopologyName(projectId));
        } else throw new HyperIoTUnauthorizedException();
    }

    @Override
    public void killTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = this.hProjectSystemApi.find(projectId, null, context);
        if (project == null)
            throw new HyperIoTEntityNotFound();
        if (HyperIoTSecurityUtil.checkPermission(context, project, HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTHProjectAction.KILL_TOPOLOGY))) {
            systemService.killTopology(systemService.getTopologyName(projectId));
        } else throw new HyperIoTUnauthorizedException();
    }

    @Override
    public void submitProjectTopology(HyperIoTContext context, long projectId) throws IOException, HyperIoTUnauthorizedException {
        HProject project = hProjectSystemApi.find(projectId, null, context);
        if (HyperIoTSecurityUtil.checkPermission(context, project, HyperIoTActionsUtil.getHyperIoTAction(HProject.class.getName(), HyperIoTHProjectAction.ADD_TOPOLOGY))) {
            this.systemService.submitProjectTopology(projectId);
        } else throw new HyperIoTUnauthorizedException();
    }

}
