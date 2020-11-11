/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * Written by Gene <generoso.martello@acsoftware.it>, March 2019
 *
 */
package it.acsoftware.hyperiot.stormmanager.actions;

import it.acsoftware.hyperiot.base.action.HyperIoTActionList;
import it.acsoftware.hyperiot.base.action.HyperIoTPermissionActivator;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionFactory;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.stormmanager.model.StormManager;

/**
 * @author Aristide Cittadino Model class that define a bundle activator and
 * register actions for StormManager which is associated with HProject
 */
public class StormManagerActionsBundleActivator extends HyperIoTPermissionActivator<HProject> {

    /**
     * Bundle activator for StormManager class
     */
    public StormManagerActionsBundleActivator() {
        super(HProject.class);
    }

    /**
     * Return a list actions that have to be registered as OSGi components
     */
    @Override
    public HyperIoTActionList getActions() {
        // creates base Actions save,update,remove,find,findAll for the specified entity
        log.info("Registering base actions...");
        HyperIoTActionList actionList = HyperIoTActionFactory.createEmptyActionList();
        // TODO: add more actions to actionList here...
        actionList.addAction(HyperIoTActionFactory.createAction(StormManager.class.getName(),
                StormManager.class.getName(), HyperIoTStormManagerAction.UPLOAD_TOPOLOGY_JAR));
        return actionList;
    }

}
