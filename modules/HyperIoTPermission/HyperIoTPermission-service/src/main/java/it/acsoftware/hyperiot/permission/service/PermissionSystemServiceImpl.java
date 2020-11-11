package it.acsoftware.hyperiot.permission.service;

import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTResource;
import it.acsoftware.hyperiot.base.api.HyperIoTRole;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionRepository;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the PermissionSystemApi
 * interface. It is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = PermissionSystemApi.class, immediate = true)
public class PermissionSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<Permission>
        implements PermissionSystemApi {

    /**
     * Injecting the PermissionRepository to interact with persistence layer
     */
    private PermissionRepository repository;

    /**
     * Constructor for a PermissionSystemServiceImpl
     */
    public PermissionSystemServiceImpl() {
        super(Permission.class);
    }

    /**
     * Return the current repository
     */
    public PermissionRepository getRepository() {
        log.log(Level.FINEST, "invoking getRepository, returning: {0}", this.repository);
        return repository;
    }

    /**
     * @param repository The current value to interact with persistence layer
     */
    @Reference
    protected void setRepository(PermissionRepository repository) {
        log.log(Level.FINEST, "invoking setRepository, setting: {0}", repository);
        this.repository = repository;
    }

    /**
     * Find a permission by a specific role and resource
     *
     * @param role     parameter required to find role by roleId
     * @param resource parameter required to find a resource
     * @return Permission if found
     */
    public Permission findByRoleAndResource(HyperIoTRole role, HyperIoTResource resource) {
        log.log(Level.FINE, "invoking findByRoleAndResource role: {0} Resource: {1}"
                , new Object[]{role.getName(), resource.getResourceName()});
        try {
            return repository.findByRoleAndResource(role, resource);
        } catch (NoResultException e) {
            log.log(Level.FINE, "No result searching for permission for role: {0} Resource: {1}"
                    , new Object[]{role.getName(), resource.getResourceName()});
            return null;
        }
    }

    /**
     * Find a permission by a specific role and resource name
     *
     * @param role         parameter required to find role by roleId
     * @param resourceName parameter required to find a resource name
     * @return Permission if found
     */
    public Permission findByRoleAndResourceName(HyperIoTRole role, String resourceName) {
        log.log(Level.FINE, "invoking findByRoleAndResourceName role: {0} Resource: {1}"
                , new Object[]{role.getName(), resourceName});
        try {
            return repository.findByRoleAndResourceName(role, resourceName);
        } catch (NoResultException e) {
            log.log(Level.FINE, "No result searching for permission for role " + role.getName()
                    + " Resource: " + resourceName);
            return null;
        }
    }

    /**
     * Find a permission by a specific role, resource name and resource id
     *
     * @param role         parameter required to find role by roleId
     * @param resourceName parameter required to find a resource name
     * @param id           parameter required to find a resource id
     * @return Permission if found
     */
    public Permission findByRoleAndResourceNameAndResourceId(HyperIoTRole role, String resourceName,
                                                             long id) {
        log.log(Level.FINE, "invoking findByRoleAndResourceNameAndResourceId role: {0} Resource: {1}"
                , new Object[]{role.getName(), resourceName});
        try {
            return repository.findByRoleAndResourceNameAndResourceId(role, resourceName, id);
        } catch (NoResultException e) {
            log.log(Level.FINE, "No result searching for permission for role " + role.getName()
                    + " Resource: " + resourceName + " with id: " + id);
            return null;
        }
    }

    /**
     * Find a permission by a specific role, resource name and resource id
     *
     * @param role parameter required to find role by roleId
     * @return Permission if found
     */
    @Override
    public Collection<Permission> findByRole(HyperIoTRole role) {
        log.log(Level.FINE, "invoking findByRoleAndResourceName role: {0}", role.getName());
        try {
            return repository.findByRole(role);
        } catch (NoResultException e) {
            log.log(Level.FINE, "No result searching for permission for role {0}", role.getName());
            return null;
        }
    }

    /**
     * @param roleName
     * @param actions  List actions to add as permissions
     */
    @Override
    public void checkOrCreateRoleWithPermissions(String roleName, List<HyperIoTAction> actions) {
        this.repository.checkOrCreateRoleWithPermissions(roleName, actions);
    }
}
