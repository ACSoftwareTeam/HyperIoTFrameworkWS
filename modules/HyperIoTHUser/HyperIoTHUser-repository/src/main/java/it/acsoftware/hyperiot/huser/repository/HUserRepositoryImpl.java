package it.acsoftware.hyperiot.huser.repository;

import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTUserAlreadyActivated;
import it.acsoftware.hyperiot.base.exception.HyperIoTWrongUserActivationCode;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.huser.api.HUserRepository;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.role.api.RoleRepository;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the HUserRepository. It is
 * used to interact with the persistence layer.
 */
@Component(service = HUserRepository.class, immediate = true)
public class HUserRepositoryImpl extends HyperIoTBaseRepositoryImpl<HUser> implements HUserRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Injecting role Repository
     */
    private RoleRepository roleRepository;

    /**
     * Constructor for HUserRepositoryImpl
     */
    public HUserRepositoryImpl() {
        super(HUser.class);
    }

    /**
     * @return The current jpa is related to database operations
     */
    @Override
    protected JpaTemplate getJpa() {
        log.log(Level.FINEST, "invoking getJpa, returning: {0}" , jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-hUser-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        log.log(Level.FINEST, "invoking setJpa, setting: {0}" , jpa);
        this.jpa = jpa;
    }

    /**
     *
     * @param roleRepository Injecting RoleRepository
     */
    @Reference
    public void setRoleRepository(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    /**
     * Find a user with admin role via query
     *
     * @return the user with admin role
     */
    @Override
    public HUser findHAdmin() {
        log.log(Level.FINE, "Invoking findHAdmin ");
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke persist");
            HUser entity = null;
            try {
                entity = entityManager.createQuery("from HUser h where h.username = 'hadmin'", HUser.class)
                        .getSingleResult();
                log.log(Level.FINE, "Entity persisted: " + entity);
            } catch (NoResultException e) {
                log.log(Level.FINE, "Entity NOT FOUND ");
            } catch(Exception e){
                log.log(Level.SEVERE, e.getMessage(),e);
            }
            return entity;
        });
    }

    /**
     * Find an existing user by username via query
     *
     * @param username parameter required to find an existing user
     * @return the user with username entered
     */
    public HUser findByUsername(String username) {
        log.log(Level.FINE, "Repository findByUsername {0}", username);
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke findByUsername");
            HUser user = null;
            try {
                user = entityManager
                        .createQuery("from HUser h left join fetch h.roles where lower(h.username)=lower(:username) ", HUser.class)
                        .setParameter("username", username).getSingleResult();
                log.log(Level.FINE, "Query results: {0}" , user);
            } catch (NoResultException e) {
                log.log(Level.FINE, "Entity Not Found ");
            } catch(Exception e){
                log.log(Level.SEVERE, e.getMessage(),e);
            }
            return user;
        });
    }

    @Override
    public HUser findByEmail(String email) {
        log.log(Level.FINE, "Repository findByEmail {0}" , email);
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke findByEmail");
            HUser user = null;
            try {
                user = entityManager
                        .createQuery("from HUser h left join fetch h.roles where h.email=:email ", HUser.class)
                        .setParameter("email", email).getSingleResult();
                log.log(Level.FINE, "Query results: {0}" , user);
            } catch (NoResultException e) {
                log.log(Level.FINE, "Entity Not Found ");
            } catch(Exception e){
                log.log(Level.SEVERE, e.getMessage(),e);
            }
            return user;
        });
    }

    /**
     * Activates and adds RegisteredUser Role to the activated user
     * @param email Email for activation
     * @param activationCode Activation Code
     */

    public void activateUser(String email, String activationCode) {
        this.getJpa().tx(TransactionType.Required, entityManger -> {
            HUser user = findByEmail(email);
            Role registeredUser = roleRepository.findByName(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER);
            if (user != null) {
                if (user.isActive()) {
                    throw new HyperIoTUserAlreadyActivated();
                }

                if (user.getActivateCode().equals(activationCode)) {
                    user.setActive(true);
                    user.setActivateCode(null);
                    //adding registered user role
                    user.addRole(registeredUser);
                    entityManger.persist(user);
                } else {
                    throw new HyperIoTWrongUserActivationCode();
                }
            } else {
                throw new HyperIoTEntityNotFound();
            }
        });

    }
}
