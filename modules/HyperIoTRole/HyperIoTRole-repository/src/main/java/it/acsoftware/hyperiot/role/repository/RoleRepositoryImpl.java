package it.acsoftware.hyperiot.role.repository;

import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.role.api.RoleRepository;
import it.acsoftware.hyperiot.role.model.Role;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the RoleRepository. This
 * class is used to interact with the persistence layer.
 */
@Component(service = RoleRepository.class, immediate = true)
public class RoleRepositoryImpl extends HyperIoTBaseRepositoryImpl<Role> implements RoleRepository {

    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Constructor for a RoleRepositoryImpl
     */
    public RoleRepositoryImpl() {
        super(Role.class);
    }

    /**
     * @return The current jpa related to database operations
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
    @Reference(target = "(osgi.unit.name=hyperiot-role-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        log.log(Level.FINEST, "invoking setJpa, setting: {0}" , jpa);
        this.jpa = jpa;
    }

    @Override
    public Role findByName(String name) {
        log.log(Level.FINE, "Invoking findByName: " + name);
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke persist");

            Role entity = null;
            try {
                entity = entityManager.createQuery("from Role r where r.name = :name", Role.class).setParameter("name", name)
                        .getSingleResult();
                log.log(Level.FINE, "Entity found: " + entity);
            } catch (NoResultException e) {
                log.log(Level.FINE, "Entity Not Found ");
            }
            return entity;
        });
    }
}
