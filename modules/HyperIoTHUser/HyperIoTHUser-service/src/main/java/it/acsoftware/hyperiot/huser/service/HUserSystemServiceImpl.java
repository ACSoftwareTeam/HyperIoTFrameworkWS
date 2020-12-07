package it.acsoftware.hyperiot.huser.service;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUserNotActivated;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.huser.api.HUserRepository;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the HUserSystemApi
 * interface. This model class is used to implement all additional
 * methods to interact with the persistence layer.
 */
@Component(service = HUserSystemApi.class, immediate = true)
public final class HUserSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<HUser>
        implements HUserSystemApi {


    /**
     * Injecting the HUserRepository to interact with persistence layer
     */
    private HUserRepository repository;

    /**
     * Constructor for a HUserSystemServiceImpl
     */
    public HUserSystemServiceImpl() {
        super(HUser.class);
    }

    /**
     * Return the current repository
     */
    protected HUserRepository getRepository() {
        getLog().log(Level.FINEST, "invoking getRepository, returning: {0}", this.repository);
        return repository;
    }

    /**
     * @param hUserRepository The current value to interact with persistence layer
     */
    @Reference
    protected void setRepository(HUserRepository hUserRepository) {
        getLog().log(Level.FINEST, "invoking setRepository, setting: {0}", hUserRepository);
        this.repository = hUserRepository;
    }

    /**
     * This method allows to find an existing user by username
     *
     * @param username parameter required to find a user
     * @return the user researched
     */
    public HUser findUserByUsername(String username) {
        return this.repository.findByUsername(username);
    }

    /**
     * @param username
     * @param password
     * @return
     */
    @Override
    public HUser login(String username, String password) {
        HUser user = null;
        try {
            user = this.findUserByUsername(username);
            //try to find by mail
        } catch (NoResultException e) {
            getLog().log(Level.FINE, "trying to login with email");
        }
        try {
            if (user == null) {
                user = this.findUserByEmail(username);
            }
            if (user != null) {
                if (!user.isActive())
                    throw new HyperIoTUserNotActivated();
                if (user.getPassword().equals(HyperIoTUtil.getPasswordHash(password)))
                    return user;
            }
        } catch (NoResultException e) {
            getLog().log(Level.FINE, "No User found with specified username: {0}", username);
        }
        return null;
    }

    /**
     * This method allows to find an existing user by email
     *
     * @param email parameter required to find a user
     * @return the user researched
     */
    public HUser findUserByEmail(String email) {
        return this.repository.findByEmail(email);
    }

    /**
     * This method allows a user registration that will be accessible by
     * unregistered users
     *
     * @param u   parameter required to register a user
     * @param ctx user context of HyperIoT platform
     */
    public void registerUser(HUser u, HyperIoTContext ctx) {
        getLog().log(Level.FINE, "invoking registerUser, User: {0}", new Object[]{u, ctx});
        this.save(u, ctx);
    }

    /**
     * Forcing Password Hash
     */
    @Override
    public HUser save(HUser u, HyperIoTContext ctx) {
        super.save(u, ctx);
        // forcing hash password on new users
        String password = u.getPassword();
        String passwordMD5 = HyperIoTUtil.getPasswordHash(password);
        u.setPassword(passwordMD5);
        u.setPasswordConfirm(passwordMD5);
        repository.update(u);
        return u;
    }

    /**
     * Password cannot be changed by this method
     */
    @Override
    public HUser update(HUser u, HyperIoTContext ctx) {
        HUser dbUser = this.find(u.getId(), ctx);
        if (dbUser == null)
            throw new HyperIoTEntityNotFound();
        u.setPassword(dbUser.getPassword());
        u.setEntityVersion(dbUser.getEntityVersion());
        return super.update(u, ctx);
    }

    @Override
    public HUser changePassword(HUser user, String newPassword, String passwordConfirm) {
        if (user == null)
            throw new HyperIoTEntityNotFound();
        user.setPassword(HyperIoTUtil.getPasswordHash(newPassword));
        user.setPasswordConfirm(HyperIoTUtil.getPasswordHash(passwordConfirm));
        //forcing password reset code to be empty on each pwd change
        user.setPasswordResetCode(null);
        super.update(user, null);
        return user;
    }

    @Override
    public void activateUser(String email, String activationCode) {
        this.repository.activateUser(email, activationCode);
    }

    /**
     * This method checks if a user is not an admin and assigns him an administrator
     * role
     */
    @Activate
    public void checkHAdminExists() {
        HUser admin = repository.findHAdmin();
        if (admin == null) {
            admin = new HUser();
            admin.setAdmin(true);
            admin.setActive(true);
            admin.setEmail("hadmin@hyperiot.com");
            admin.setLastname("Admin");
            admin.setName("Admin");
            String password = HyperIoTUtil.getPasswordHash("admin");
            admin.setPassword(password);
            admin.setPasswordConfirm(password);
            admin.setUsername("hadmin");
            repository.save(admin);
        }
    }

}
