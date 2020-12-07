package it.acsoftware.hyperiot.authentication.service;

import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.authentication.api.AuthenticationSystemApi;
import it.acsoftware.hyperiot.authentication.model.JWTLoginResponse;
import it.acsoftware.hyperiot.authentication.model.JWTProfile;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.model.Role;

/**
 * @author Aristide Cittadino Implementation class of the interace
 * AuthenticationApi for user login. Service classes differ from System
 * Service classes because the first checks permissions the latter not.
 * It's always up to the user to check the permission in the service
 * class.
 */

@Component(service = AuthenticationApi.class, immediate = true)
public final class AuthenticationServiceImpl extends HyperIoTBaseServiceImpl
        implements AuthenticationApi {
    /**
     * Authentication System Service
     */
    private AuthenticationSystemApi systemService;
    private RoleSystemApi roleSystemService;
    private PermissionSystemApi permissionSystemService;

    @Override
    public HyperIoTAuthenticable login(String username, String password) {
        return systemService.login(username, password);
    }

    /**
     * return the current AuthenticationSystemService
     */
    public AuthenticationSystemApi getSystemService() {
        getLog().log(Level.FINEST, "invoking getSystemService, returning: {0}" , this.systemService);
        return systemService;
    }

    /**
     * @param authenticationSystemService injecting authenticationSystemService via
     *                                    OSGi DS
     */
    @Reference
    protected void setSystemService(AuthenticationSystemApi authenticationSystemService) {
        getLog().log(Level.FINEST, "invoking setSystemService, setting: {0}" , systemService);
        this.systemService = authenticationSystemService;
    }

    /**
     * @return
     */
    public RoleSystemApi getRoleSystemService() {
        return roleSystemService;
    }

    /**
     * @param roleSystemService
     */
    @Reference
    public void setRoleSystemService(RoleSystemApi roleSystemService) {
        this.roleSystemService = roleSystemService;
    }

    /**
     * @return
     */
    public PermissionSystemApi getPermissionSystemService() {
        return permissionSystemService;
    }

    /**
     * @param permissionSystemService
     */
    @Reference
    public void setPermissionSystemService(PermissionSystemApi permissionSystemService) {
        this.permissionSystemService = permissionSystemService;
    }

    /**
     * @param user user from which token must be generated
     * @return JWT Token with profile information
     */
    @Override
    public JWTLoginResponse generateToken(HyperIoTAuthenticable user) {
        String token = this.systemService.generateToken(user);
        JWTLoginResponse jwtResponse = new JWTLoginResponse(token, user);
        //retrieving roles and permissions
        Collection<Role> roles = this.roleSystemService.getUserRoles(user.getId());
        for (Role r : roles) {
            Collection<Permission> permissions = this.permissionSystemService.findByRole(r);
            for (Permission p : permissions) {
                if (!jwtResponse.getProfile().containsKey(p.getEntityResourceName()))
                    jwtResponse.getProfile().put(p.getEntityResourceName(), new JWTProfile());
                List<String> actions = HyperIoTActionsUtil.getHyperIoTActions(p.getEntityResourceName())
                        .stream()
                        .filter(action -> (p.getActionIds() & action.getActionId()) == action.getActionId())
                        .map(HyperIoTAction::getActionName).collect(Collectors.toList());
                jwtResponse.getProfile().get(p.getEntityResourceName()).addPermissionInfo(actions);
            }
        }
        return jwtResponse;
    }

}
