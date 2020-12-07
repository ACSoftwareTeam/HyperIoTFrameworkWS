package it.acsoftware.hyperiot.role.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.role.actions.HyperIoTRoleAction;
import it.acsoftware.hyperiot.role.api.RoleApi;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.model.Role;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of RoleApi interface. It is
 * used to implement all additional methods in order to interact with
 * the system layer.
 */
@Component(service = RoleApi.class, immediate = true)
public final class RoleServiceImpl extends HyperIoTBaseEntityServiceImpl<Role> implements RoleApi {
    private static final String resourceName = Role.class.getName();
    /**
     * Injecting the RoleSystemService to use methods in RoleSystemApi interface
     */
    private RoleSystemApi systemService;
    /**
     * Injecting the HUserSystemApi to use methods in HUserSystemApi interface
     */
    private HUserSystemApi userSystemService;

    /**
     * Constructor for a RoleServiceImpl
     */
    public RoleServiceImpl() {
        super(Role.class);
    }

    /**
     * @return The current RoleSystemService
     */
    public RoleSystemApi getSystemService() {
        getLog().log(Level.FINEST, "invoking getSystemService, returning: {0}", this.systemService);
        return systemService;
    }

    /**
     * @param roleSystemService Injecting via OSGi DS current RoleSystemService
     */
    @Reference
    protected void setSystemService(RoleSystemApi roleSystemService) {
        getLog().log(Level.FINEST, "invoking setSystemService, setting: {0}", systemService);
        this.systemService = roleSystemService;
    }

    /**
     * @param hUserSystemService Injecting via OSGi DS current HUserSystemService
     */
    @Reference
    protected void setUserSystemService(HUserSystemApi hUserSystemService) {
        getLog().log(Level.FINE, "invoking setUserSystemService, setting: {0}", hUserSystemService);
        this.userSystemService = hUserSystemService;
    }

    /**
     * Collection of user roles obtained via query
     *
     * @param userId parameter required to find all user roles
     * @return collection of user roles
     */
    public Collection<Role> getUserRoles(long userId, HyperIoTContext ctx) {
        getLog().log(Level.FINE, "invoking getUserRoles, by: " + userId);
        if (HyperIoTSecurityUtil.checkPermission(ctx, resourceName,
            HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTCrudAction.FIND))) {
            HUser huser;
            try {
                huser = this.userSystemService.find(userId, ctx);
            } catch (NoResultException e) {
                getLog().log(Level.FINE, "invoking getUserRoles, Entity not found! ");
                throw new HyperIoTEntityNotFound();
            }
            HashMap<String, Object> params = new HashMap<>();
            params.put("userId", huser.getId());
            return this.getSystemService()
                .queryForResultList("select r from Role r inner join r.users ur where ur.id=:userId", params);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Save a user role
     *
     * @param userId parameter required to find an existing user
     * @param roleId parameter required to save a user role
     * @param ctx    user context of HyperIoT platform
     * @return the user's role saved
     */
    public Role saveUserRole(long userId, long roleId, HyperIoTContext ctx) {
        getLog().log(Level.FINE, "invoking saveUserRole, save role: {0}  from user: {1}", new Object[]{roleId, userId});
        if (HyperIoTSecurityUtil.checkPermission(ctx, resourceName,
            HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTRoleAction.ASSIGN_MEMBERS))) {
            HUser u;
            try {
                u = this.userSystemService.find(userId, ctx);
            } catch (NoResultException e) {
                getLog().log(Level.FINE, "invoking saveUserRole, HUser not found! ");
                throw new HyperIoTEntityNotFound();
            }
            Role r;
            try {
                r = this.systemService.find(roleId, ctx);
            } catch (NoResultException e) {
                getLog().log(Level.FINE, "invoking saveUserRole, Role not found! ");
                throw new HyperIoTEntityNotFound();
            }
            Collection<Role> roles = u.getRoles();
            if (roles.contains(r) == true) {
                getLog().log(Level.FINE, "invoking saveUserRole, Entity is duplicated ");
                String[] message = {"Entity is duplicated"};
                throw new HyperIoTDuplicateEntityException(message);
            }
            u.addRole(r);
            userSystemService.update(u, ctx);
            return r;
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Remove a user role
     *
     * @param userId parameter required to find an existing role
     * @param roleId parameter required to delete a user role
     * @param ctx    user context of HyperIoT platform
     * @return the user's role deleted
     */
    public Role removeUserRole(long userId, long roleId, HyperIoTContext ctx) {
        getLog().log(Level.FINE, "invoking removeUserRole, remove role: {0} from user: {1}", new Object[]{roleId, userId});
        if (HyperIoTSecurityUtil.checkPermission(ctx, resourceName,
            HyperIoTActionsUtil.getHyperIoTAction(resourceName, HyperIoTRoleAction.REMOVE_MEMBERS))) {
            HUser u;
            try {
                u = this.userSystemService.find(userId, ctx);
            } catch (NoResultException e) {
                getLog().log(Level.FINE, "invoking removeUserRole, HUser not found! ");
                throw new HyperIoTEntityNotFound();
            }
            Role r;
            try {
                r = this.systemService.find(roleId, ctx);
            } catch (NoResultException e) {
                getLog().log(Level.FINE, "invoking removeUserRole, Role not found! ");
                throw new HyperIoTEntityNotFound();
            }
            Collection<Role> roles = u.getRoles();
            if (roles.removeIf(role -> role.getId() == roleId) == false) {
                throw new HyperIoTEntityNotFound();
            }
            u.removeRole(r);
            userSystemService.update(u, ctx);
            return r;
        }
        throw new HyperIoTUnauthorizedException();
    }

}
