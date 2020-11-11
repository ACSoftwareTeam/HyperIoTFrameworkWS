package it.acsoftware.hyperiot.base.api;

import java.util.logging.Level;
import java.util.logging.Logger;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTProtectedEntity;

/**
 * @author Aristide Cittadino Generic Interface Component for
 * HyperIoTPermissionManager. This interface define all methods able to
 * check if a user has permissions for each actions of the HyperIoT
 * platform.
 */
public interface HyperIoTPermissionManager {

    /**
     * Checks if an existing user has permissions for action of HyperIoTAction.
     * Moreover every user, if protected, is set as a base entity of the HyperIoT
     * platform.
     *
     * @param username parameter that indicates the username of entity
     * @param entity   parameter that indicates the resource on which the action should be performed
     * @param action   interaction of the entity with HyperIoT platform
     */
    boolean checkPermission(String username, HyperIoTResource entity, HyperIoTAction action);

    /**
     * Checks if an existing user has permissions for action of HyperIoTAction.
     *
     * @param username parameter that indicates the username of entity
     * @param resource parameter that indicates the resource on which the action should be performed
     * @param action   interaction of the user with HyperIoT platform
     */
    boolean checkPermission(String username, Class<? extends HyperIoTResource> resource,
                            HyperIoTAction action);

    /**
     * Checks if an existing user has permissions for action of HyperIoTAction.
     *
     * @param username     parameter that indicates the username of entity
     * @param resourceName parameter that indicates the resource name of action
     * @param action       interaction of the user with HyperIoT platform
     */
    boolean checkPermission(String username, String resourceName, HyperIoTAction action);

    /**
     * @param username     parameter that indicates the username of entity
     * @param resourceName parameter that indicates the resource name of action
     * @param action       interaction of the user with HyperIoT platform
     * @param entities     List of entities User must own in order to perform the action
     * @return
     */
    boolean checkPermissionAndOwnership(String username, String resourceName, HyperIoTAction action, HyperIoTResource... entities);

    /**
     * @param username parameter that indicates the username of entity
     * @param resource parameter that indicates the resource on which the action should be performed
     * @param action   interaction of the user with HyperIoT platform
     * @param entities List of other entities User must own in order to perform the action
     * @return
     */
    boolean checkPermissionAndOwnership(String username, HyperIoTResource resource, HyperIoTAction action, HyperIoTResource... entities);

    /**
     * Checks wether resource is owned by the user
     *
     * @param user     User that should own the resource
     * @param resource Object that should be owned by the user
     * @return true if the user owns the resource
     */
    public boolean checkUserOwnsResource(HyperIoTUser user, Object resource);

    /**
     * Return the protected entity of HyperIoT platform
     *
     * @param entity parameter that indicates the protected entity of HyperIoT
     *               platform
     * @return protected entity
     */
    static boolean isProtectedEntity(Object entity) {
        Logger.getLogger("it.acsoftware.hyperiot").log(Level.FINE,
                "invoking Permission Manager getProtectedEntity "
                        + entity.getClass().getSimpleName());
        if (entity instanceof HyperIoTProtectedEntity)
            return true;

        if (entity instanceof HyperIoTProtectedResource)
            return true;

        return false;
    }

    /**
     * Return the protected resource name of entity of HyperIoT platform
     *
     * @param resourceName parameter that indicates the protected resource name of
     *                     entity of HyperIoT platform
     * @return protected resource name of entity
     */
    static boolean isProtectedEntity(String resourceName) {
        Logger.getLogger("it.acsoftware.hyperiot").log(Level.FINE,
                "invoking Permission getProtectedEntity " + resourceName);
        try {
            boolean isAssignable = HyperIoTProtectedEntity.class.isAssignableFrom(Class.forName(resourceName));
            isAssignable = isAssignable || HyperIoTProtectedResource.class.isAssignableFrom(Class.forName(resourceName));
            return isAssignable;
        } catch (ClassNotFoundException e) {
            Logger.getLogger("it.acsoftware.hyperiot").log(Level.WARNING, e.getMessage());
        }
        // return the most restrictive condition
        return true;
    }
}
