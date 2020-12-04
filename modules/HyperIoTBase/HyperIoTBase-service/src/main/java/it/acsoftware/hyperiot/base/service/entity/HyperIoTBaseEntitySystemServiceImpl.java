package it.acsoftware.hyperiot.base.service.entity;

import it.acsoftware.hyperiot.base.api.HyperIoTAuthenticationProvider;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.*;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTScreenNameAlreadyExistsException;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseAbstractSystemService;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.apache.aries.jpa.template.EmConsumer;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.framework.ServiceReference;

import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Interface component for HyperIoTBaseEntityApi.
 * This interface defines the methods for basic CRUD operations. This
 * methods are reusable by all entities in order to interact with the
 * persistence layer.
 */
public abstract class HyperIoTBaseEntitySystemServiceImpl<T extends HyperIoTBaseEntity>
    extends HyperIoTBaseAbstractSystemService implements HyperIoTBaseEntitySystemApi<T> {

    /**
     * Generic class for HyperIoT platform
     */
    private Class<T> type;

    /**
     * Constructor for HyperIoTBaseEntitySystemServiceImpl
     *
     * @param type parameter that indicates a generic entity
     */
    public HyperIoTBaseEntitySystemServiceImpl(Class<T> type) {
        this.type = type;
    }

    /**
     * Validates all constraints of entity
     *
     * @param entity parameter that indicates the object to validate
     * @return entity validated
     */
    protected void validate(T entity) {
        log.log(Level.FINE,
            "System Service Validating entity {0}: {1}", new Object[]{this.type.getSimpleName(), entity});
        //avoiding to have different entities which can login but username amongst them must be alwats unique
        if (entity instanceof HyperIoTAuthenticable) {
            HyperIoTAuthenticable authenticable = (HyperIoTAuthenticable) entity;
            ServiceReference<HyperIoTAuthenticationProvider>[] providers = HyperIoTUtil.getServices(HyperIoTAuthenticationProvider.class, null);
            for (int i = 0; i < providers.length; i++) {
                HyperIoTAuthenticationProvider p = HyperIoTUtil.getBundleContext(this).getService(providers[i]);
                if (p.screeNameAlreadyExists(authenticable)) {
                    throw new HyperIoTScreenNameAlreadyExistsException(authenticable.getScreenNameFieldName(), authenticable.getScreenName());
                }
            }
        }
        super.validate(entity);
    }

    /**
     * Save an entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @param ctx    user context of HyperIoT platform
     * @return entity saved
     */
    @Override
    public T save(T entity, HyperIoTContext ctx) {
        log.log(Level.FINE,
            "System Service Saving entity {0}: {1}", new Object[]{this.type.getSimpleName(), entity});
        //throws runtime exception if validation is not met
        this.validate(entity);
        try {
            return this.getRepository().save(entity);
        } catch (HyperIoTDuplicateEntityException e) {
            log.log(Level.WARNING, "Save failed: entity is duplicated!");
            throw e;
        } catch (Exception e1) {
            throw new HyperIoTRuntimeException(e1.getMessage());
        }
    }

    /**
     * Update an existing entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @param ctx    user context of HyperIoT platform
     */
    @Override
    public T update(T entity, HyperIoTContext ctx) {
        log.log(Level.FINE,
            "System Service Updating entity {0}: {1}", new Object[]{this.type.getSimpleName(), entity});
        //throws runtime exception if validation is not met
        this.validate(entity);
        try {
            return this.getRepository().update(entity);
        } catch (HyperIoTDuplicateEntityException e) {
            log.log(Level.WARNING, "Update failed: entity is duplicated!");
            throw e;
        } catch (Exception e1) {
            throw new HyperIoTRuntimeException(e1.getMessage());
        }
    }

    /**
     * Remove an entity in database
     *
     * @param id  parameter that indicates a entity id
     * @param ctx user context of HyperIoT platform
     */
    @Override
    public void remove(long id, HyperIoTContext ctx) {
        log.log(Level.FINE,
            "System Service Removing entity {0} with id {1}", new Object[]{this.type.getSimpleName(), id});
        HyperIoTBaseEntity entity = find(id, ctx);
        if (entity != null) {
            this.getRepository().remove(id);
            return;
        }
        throw new HyperIoTEntityNotFound();
    }

    /**
     * @param id
     * @param ctx
     * @return
     */
    @Override
    public T find(long id, HyperIoTContext ctx) {
        return this.getRepository().find(id, ctx);
    }


    /**
     * @param filter filter
     * @param ctx    user context of HyperIoT platform
     * @return
     */
    @Override
    public T find(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        return this.getRepository().find(filter, ctx);
    }

    /**
     * Find all entity in database
     *
     * @param ctx user context of HyperIoT platform
     * @return Collection of entity
     */
    @Override
    public Collection<T> findAll(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        log.log(Level.FINE, "System Service Finding All entities of " + this.type.getSimpleName());
        return this.getRepository().findAll(filter);
    }

    @Override
    public HyperIoTPaginableResult<T> findAll(HyperIoTQueryFilter filter, HyperIoTContext ctx, int delta,
                                              int page) {
        log.log(Level.FINE, "System Service Finding All entities of " + this.type.getSimpleName()
            + " with delta: " + delta + " num page:" + page);
        return this.getRepository().findAll(delta, page, filter);
    }

    /**
     * Find all entity with a query. This function return a collection of entity
     *
     * @param query  parameter that defines the query in the hql language
     * @param params parameter that indicates the value to set within the query
     * @return Collection of entity
     */
    public Collection<T> queryForResultList(String query, HashMap<String, Object> params) {
        log.log(Level.FINE, "System Service queryForResultList " + query);
        return this.getRepository().queryForResultList(query, params);

    }

    /**
     * Find an entity with a query
     *
     * @param query  parameter that defines the query in the hql language
     * @param params parameter that indicates the value to set within the query
     * @return Entity if found
     */
    public T queryForSingleResult(String query, HashMap<String, Object> params) {
        log.log(Level.FINE, "System Service queryForSingleResult " + query);
        return this.getRepository().queryForSingleResult(query, params);
    }

    /**
     * Return current entity type
     */
    @Override
    public Class<T> getEntityType() {
        return this.type;
    }

    /**
     * Executes transaction
     *
     * @param txType
     * @param function
     */
    @Override
    public void executeTransaction(TransactionType txType, EmConsumer function) {
        this.getRepository().executeTransaction(txType, function);
    }

    /**
     * Executes transaction with a return type
     *
     * @param txType
     * @param function
     * @param <R>
     * @return
     */
    @Override
    public <R> R executeTransactionWithReturn(TransactionType txType, EmFunction<R> function) {
        return this.getRepository().executeTransactionWithReturn(txType, function);
    }

    /**
     * Return the current repository
     */
    protected abstract HyperIoTBaseRepository<T> getRepository();

}
