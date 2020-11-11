package it.acsoftware.hyperiot.permission.service;

import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the PermissionApi. It is
 * used to implement all additional methods in order to interact with
 * the system layer.
 */
@Component(service = PermissionApi.class, immediate = true)
public class PermissionServiceImpl extends HyperIoTBaseEntityServiceImpl<Permission> implements PermissionApi {

    /**
     * Injecting the PermissionSystemService to use methods in PermissionSystemApi
     * interface
     */
    private PermissionSystemApi systemService;

    /**
     * Constructor for a PermissionServiceImpl
     */
    public PermissionServiceImpl() {
        super(Permission.class);
    }

    /**
     * @return The current PermissionSystemService
     */
    public PermissionSystemApi getSystemService() {
        log.log(Level.FINEST, "invoking getSystemService, returning: {0}" , this.systemService);
        return systemService;
    }

    /**
     * @param systemService Injecting via OSGi DS current PermissionSystemService
     */
    @Reference
    protected void setSystemService(PermissionSystemApi systemService) {
        log.log(Level.FINEST, "invoking setSystemService, setting: {0}" , systemService);
        this.systemService = systemService;
    }

    /**
     * This method finds a list of all available permissions for HyperIoT platform
     */
    @Override
    public HashMap<String, List<HyperIoTAction>> getAvailablePermissions() {
        log.log(Level.FINE, "invoking getAvailablePermissions ");
        return null;
    }

}
