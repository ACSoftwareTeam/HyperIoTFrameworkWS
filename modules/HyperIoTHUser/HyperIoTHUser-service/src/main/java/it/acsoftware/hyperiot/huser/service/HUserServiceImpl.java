package it.acsoftware.hyperiot.huser.service;

import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAuthenticationProvider;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTAuthenticable;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.exception.HyperIoTWrongUserPasswordResetCode;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.huser.api.HUserApi;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.mail.api.MailSystemApi;
import it.acsoftware.hyperiot.mail.util.MailConstants;
import it.acsoftware.hyperiot.mail.util.MailUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

import static it.acsoftware.hyperiot.base.util.HyperIoTConstants.OSGI_AUTH_PROVIDER_RESOURCE;

/**
 * @author Aristide Cittadino Implementation class of the HUserApi interface. It
 * is used to implement all additional methods in order to interact with
 * the system layer.
 */
@Component(service = {HUserApi.class, HyperIoTAuthenticationProvider.class}, immediate = true, property = {
    OSGI_AUTH_PROVIDER_RESOURCE + "=it.acsoftware.hyperiot.huser.model.HUser"
})
public final class HUserServiceImpl extends HyperIoTBaseEntityServiceImpl<HUser> implements HUserApi, HyperIoTAuthenticationProvider {

    /**
     * Injecting the HUserSystemApi to use methods defined in HUserApi interface
     */
    private HUserSystemApi systemService;
    private MailSystemApi mailService;

    /**
     * Constructor for a HUserServiceImpl
     */
    public HUserServiceImpl() {
        super(HUser.class);
    }

    /**
     * @return The current HUserSystemService
     */
    protected HUserSystemApi getSystemService() {
        getLog().log(Level.FINEST, "invoking getSystemService, returning: {0}", this.systemService);
        return systemService;
    }

    /**
     * @param hUserSystemService Injecting via OSGi DS current HUserSystemService
     */
    @Reference
    protected void setSystemService(HUserSystemApi hUserSystemService) {
        getLog().log(Level.FINEST, "invoking setSystemService, setting: {0}", hUserSystemService);
        this.systemService = hUserSystemService;
    }

    /**
     * @return HyperIoT Mail Service
     */
    public MailSystemApi getMailService() {
        return mailService;
    }

    /**
     * @param mailService
     */
    @Reference
    public void setMailService(MailSystemApi mailService) {
        this.mailService = mailService;
    }

    /**
     * Find an existing user by username
     *
     * @param username parameter required to find a user
     * @return the user researched
     */
    @Override
    public HUser findUserByUsername(String username) {
        return systemService.findUserByUsername(username);
    }

    /**
     * This method allows a user registration that will be accessible by
     * unregistered users
     *
     * @param u   parameter required to register a user
     * @param ctx user context of HyperIoT platform
     */
    @Override
    public void registerUser(HUser u, HyperIoTContext ctx) {
        getLog().log(Level.FINE, "Invoking registerUser User {0} Context: {1}", new Object[]{u, ctx});
        this.systemService.registerUser(u, ctx);
        // if ok sending mail
        List<String> recipients = new ArrayList<>();
        recipients.add(u.getEmail());
        HashMap<String, Object> params = new HashMap<>();
        params.put("username", u.getUsername());
        params.put("activateAccountUrl", HyperIoTUtil.getActivateAccountUrl() + "/" + u.getEmail() + "/" + u.getActivateCode());
        params.put("activationCode", u.getActivateCode());
        try {
            String mailBody = mailService.generateTextFromTemplate(MailConstants.MAIL_TEMPLATE_REGISTRATION, params);
            this.mailService.sendMail(MailUtil.getUsername(), recipients, null, null, "HyperIoT Account Activation!",
                mailBody, null);
        } catch (Exception e) {
            getLog().log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Updates user account only if the user is changing his personal info
     */
    @Override
    public HUser updateAccountInfo(HyperIoTContext context, HUser user) {
        HUser loggedUser = this.systemService.findUserByUsername(context.getLoggedUsername());
        if (loggedUser == null)
            throw new HyperIoTUnauthorizedException();
        if (loggedUser.getId() == user.getId() || HyperIoTSecurityUtil.checkPermission(context, user,
            this.getAction(user.getResourceName(), HyperIoTCrudAction.UPDATE))) {
            loggedUser.setName(user.getName());
            loggedUser.setLastname(user.getLastname());
            loggedUser.setEmail(user.getEmail());
            loggedUser.setUsername(user.getUsername());
            this.systemService.update(loggedUser, context);
            return loggedUser;
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     *
     */
    @Override
    public void activateUser(String email, String activationCode) {
        this.systemService.activateUser(email, activationCode);
    }

    /**
     *
     */
    public void resetPassword(String email, String resetCode, String password, String passwordConfirm) {
        HUser u = this.systemService.findUserByEmail(email);
        if (u != null) {
            if (u.getPasswordResetCode() == null) {
                throw new HyperIoTWrongUserPasswordResetCode();
            }
            if (u.getPasswordResetCode().equals(resetCode)) {
                this.systemService.changePassword(u, password, passwordConfirm);
                return;
            }
            throw new HyperIoTWrongUserPasswordResetCode();
        } else {
            throw new HyperIoTEntityNotFound();
        }
    }

    /**
     *
     */
    public void passwordResetRequest(String email) {
        HUser u = this.systemService.findUserByEmail(email);
        if (u != null) {
            u.setPasswordResetCode(UUID.randomUUID().toString());
            this.systemService.update(u, null);
            List<String> recipients = new ArrayList<>();
            recipients.add(u.getEmail());
            HashMap<String, Object> params = new HashMap<>();
            params.put("username", u.getUsername());
            params.put("changePwdUrl", HyperIoTUtil.getPasswordResetUrl() + "/" + email + "/" + u.getPasswordResetCode());
            params.put("resetPwdCode", u.getPasswordResetCode());
            try {
                String mailBody = mailService.generateTextFromTemplate(MailConstants.MAIL_TEMPLATE_PWD_RESET, params);
                this.mailService.sendMail(MailUtil.getUsername(), recipients, null, null, "Reset Password", mailBody,
                    null);
            } catch (Exception e) {
                getLog().log(Level.SEVERE, e.getMessage(), e);
            }
        } else {
            throw new HyperIoTEntityNotFound();
        }
    }

    @Override
    public HUser changePassword(HyperIoTContext context, long userId, String oldPassword, String newPassword,
                                String passwordConfirm) {
        HUser u;
        try {
            u = this.systemService.find(userId, context);
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
        HUser loggedUser = this.systemService.findUserByUsername(context.getLoggedUsername());
        if (loggedUser == null)
            throw new HyperIoTUnauthorizedException();
        if (oldPassword != null && newPassword != null && passwordConfirm != null) {
            if (HyperIoTUtil.getPasswordHash(oldPassword).equals(u.getPassword())) {
                if (loggedUser.getId() == userId || HyperIoTSecurityUtil.checkPermission(context, u,
                    this.getAction(u.getResourceName(), HyperIoTCrudAction.UPDATE))) {
                    return this.systemService.changePassword(u, newPassword, passwordConfirm);
                } else {
                    throw new HyperIoTUnauthorizedException();
                }
            } else {
                throw new HyperIoTRuntimeException("it.ascoftware.hyperiot.error.password.not.match");
            }
        } else {
            throw new HyperIoTRuntimeException("it.ascoftware.hyperiot.error.password.not.null");
        }
    }


    @Override
    public HyperIoTAuthenticable findByUsername(String username) {
        return this.systemService.findUserByUsername(username);
    }

    @Override
    public HyperIoTAuthenticable login(String username, String password) {
        return this.systemService.login(username, password);
    }

    @Override
    public String[] validIssuers() {
        //returning the generic interface, so it matches the rule inside rest filters
        return new String[]{HyperIoTUser.class.getName()};
    }

    /**
     * @param hyperIoTAuthenticable
     * @return
     */
    @Override
    public boolean screeNameAlreadyExists(HyperIoTAuthenticable hyperIoTAuthenticable) {
        try {
            HashMap<String, Object> params = new HashMap<>();
            params.put("username", hyperIoTAuthenticable.getScreenName());
            HUser huser = this.systemService.queryForSingleResult("from HUser u where lower(u.username) = lower(:username)", params);
            //if the authenticable is the current device is ok
            if (hyperIoTAuthenticable instanceof HUser) {
                HUser huserAuthenticable = (HUser) hyperIoTAuthenticable;
                return !huserAuthenticable.equals(huser);
            }
            return huser != null;
        } catch (NoResultException e) {
            getLog().log(Level.FINE, "No devices with device name: {0}", hyperIoTAuthenticable.getScreenName());
        }
        return false;
    }
}
