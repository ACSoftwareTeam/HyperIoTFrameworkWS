package it.acsoftware.hyperiot.permission.repository;

import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTResource;
import it.acsoftware.hyperiot.base.api.HyperIoTRole;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.permission.api.PermissionRepository;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.api.RoleRepository;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the PermissionRepository.
 * It is used to interact with the persistence layer.
 */
@Component(service = PermissionRepository.class, immediate = true)
public class PermissionRepositoryImpl extends HyperIoTBaseRepositoryImpl<Permission>
        implements PermissionRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Injecting role repository
     */
    private RoleRepository roleRepository;

    /**
     * Constructor for a PermissionRepositoryImpl
     */
    public PermissionRepositoryImpl() {
        super(Permission.class);
    }

    /**
     * @return The current jpa related to database operations
     */
    @Override
    protected JpaTemplate getJpa() {
        log.log(Level.FINEST, "invoking getJpa, returning: {0}", jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Reference(target = "(osgi.unit.name=hyperiot-permission-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        log.log(Level.FINEST, "invoking setJpa, setting: {0}", jpa);
        this.jpa = jpa;
    }

    /**
     * @return Current role repository
     */
    public RoleRepository getRoleRepository() {
        return roleRepository;
    }

    /**
     * Injecting role repository
     *
     * @param roleRepository
     */
    @Reference
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Find a permission by a specific role and resource
     *
     * @param role     parameter required to find role by roleId
     * @param resource parameter required to find a resource
     * @return Permission if found
     */
    @Override
    public Permission findByRoleAndResource(HyperIoTRole role, HyperIoTResource resource) {
        log.log(Level.FINEST, "invoking findByRoleAndResource Role: {0} Resource: {1}", new Object[]{role, resource.getResourceName()});
        return this.findByRoleAndResourceName(role, resource.getResourceName());
    }

    /**
     * Find a permission by a specific role and resource name via query
     *
     * @param role               parameter required to find role by roleId
     * @param entityResourceName parameter required to find a resource name
     * @return Permission if found
     */
    @Override
    public Permission findByRoleAndResourceName(HyperIoTRole role, String entityResourceName) {
        log.log(Level.FINEST, "invoking findByRoleAndResourceName Role: {0} Resource: {1}", new Object[]{role, entityResourceName});
        return jpa.txExpr(TransactionType.Required, entityManager -> {
            Permission p = null;
            try {
                p = entityManager.createQuery(
                        "from Permission p where p.role.id = :roleId and p.entityResourceName = :entityResourceName and p.resourceId = 0",
                        Permission.class).setParameter("roleId", role.getId())
                        .setParameter("entityResourceName", entityResourceName).getSingleResult();
            } catch (NoResultException e) {
                log.log(Level.FINE, e.getMessage(), e);
            }
            return p;
        });
    }

    /**
     * Find permissions by a specific role
     *
     * @param role parameter required to find role by roleId
     * @return Permissions if found
     */
    @Override
    public Collection<Permission> findByRole(HyperIoTRole role) {
        log.log(Level.FINEST, "invoking findByRoleAndResourceName Role: {0}", role.getName());
        return jpa.txExpr(TransactionType.Required, entityManager -> {
            return entityManager
                    .createQuery("from Permission p where p.role.id = :roleId",
                            Permission.class)
                    .setParameter("roleId", role.getId()).getResultList();
        });
    }

    /**
     * Find a permission by a specific role, resource name and resource id via query
     *
     * @param role               parameter required to find role by roleId
     * @param entityResourceName parameter required to find a resource name
     * @param id                 parameter required to find a resource id
     * @return Permission if found
     */
    @Override
    public Permission findByRoleAndResourceNameAndResourceId(HyperIoTRole role,
                                                             String entityResourceName, long id) {
        log.log(Level.FINEST, "invoking findByRoleAndResourceNameAndResourceId Role: {0}", new Object[]{role, entityResourceName, id});
        return jpa.txExpr(TransactionType.Required, entityManager -> {
            return entityManager.createQuery(
                    "from Permission p where p.role.id = :roleId and p.entityResourceName = :entityResourceName and p.resourceId = :id",
                    Permission.class).setParameter("roleId", role.getId())
                    .setParameter("entityResourceName", entityResourceName).setParameter("id", id)
                    .getSingleResult();
        });
    }

    /**
     * Checks if  default "RegisteredUser" role exists, and, if not, creates it.
     */
    public void checkOrCreateRoleWithPermissions(String roleName, List<HyperIoTAction> actions) {
        jpa.tx(TransactionType.Required, entityManager -> {
            Role r = null;
            try {
                r = this.roleRepository.findByName(roleName);
            } catch (NoResultException e) {
                log.log(Level.FINE, "No role found with name: {0}", roleName);
            }

            HashMap<String, Integer> actionsIds = new HashMap<>();
            HashMap<String, Permission> existingPermissions = new HashMap<>();

            if (r == null) {
                r = new Role();
                r.setName(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER);
                r.setDescription("Role associated with the registered user");
                entityManager.persist(r);
            }


            //calculating pairs resourceName - actionsIds
            for (int i = 0; i < actions.size(); i++) {
                HyperIoTAction action = actions.get(i);
                //Checks if permission already exists for that resource
                Permission p = this.findByRoleAndResourceName(r, action.getResourceName());
                if (p == null)
                    log.log(Level.FINE, "No permission found for resource: {0} and role {1}", new Object[]{action.getResourceName(), r.getName()});
                else
                    existingPermissions.put(action.getResourceName(), p);

                // create pair <resourceName, actionId> if resourceName does not exist, sum actionId otherwise
                actionsIds.merge(action.getResourceName(), action.getActionId(), Integer::sum);
            }

            // Save only modified permissions
            Iterator<String> it = actionsIds.keySet().iterator();
            while (it.hasNext()) {
                String resourceName = it.next();
                int actionIds = actionsIds.get(resourceName);
                Permission p = null;
                boolean mustUpdate = false;
                boolean isUnchanged = false;

                if (!existingPermissions.containsKey(resourceName)) {
                    // permission is new
                    p = new Permission();
                }
                else if (existingPermissions.get(resourceName).getActionIds() != actionIds) {
                    // permission has been modified (i.e. actions have been added or removed)
                    p = existingPermissions.get(resourceName);
                    mustUpdate = true;
                } else {
                    // permission has not been changed
                    isUnchanged = true;
                }

                if (!isUnchanged) {
                    // save or update
                    p.setEntityResourceName(resourceName);
                    p.setRole(r);
                    p.setActionIds(actionIds);
                    p.setName(resourceName + " " + r.getName() + " Permissions");
                    p.setResourceId(0L);
                    try {
                        if (!mustUpdate)
                            entityManager.persist(p);
                        else
                            entityManager.merge(p);
                    } catch (Exception e) {
                        log.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        });
    }

}
