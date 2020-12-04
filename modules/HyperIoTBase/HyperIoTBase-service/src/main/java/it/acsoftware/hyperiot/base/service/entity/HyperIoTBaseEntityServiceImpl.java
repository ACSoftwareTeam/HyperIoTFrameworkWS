package it.acsoftware.hyperiot.base.service.entity;

import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTOwnershipResourceService;
import it.acsoftware.hyperiot.base.api.HyperIoTResource;
import it.acsoftware.hyperiot.base.api.entity.*;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryEqualsFilter;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryFilterBuilder;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryInFilter;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.persistence.NoResultException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Model class for HyperIoTBaseEntityServiceImpl.
 * This class implements the methods for basic CRUD operations. This
 * methods are reusable by all entities in order to interact with the
 * system layer.
 */
public abstract class HyperIoTBaseEntityServiceImpl<T extends HyperIoTBaseEntity>
    implements HyperIoTBaseEntityApi<T> {
    protected Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * Generic class for HyperIoT platform
     */
    private Class<T> type;

    /**
     * Constructor for HyperIoTBaseEntityServiceImpl
     *
     * @param type parameter that indicates a generic entity
     */
    public HyperIoTBaseEntityServiceImpl(Class<T> type) {
        this.type = type;
    }

    /**
     * Save an entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @param ctx    user context of HyperIoT platform
     * @return entity saved
     */
    public T save(T entity, HyperIoTContext ctx) {
        log.log(Level.FINE, "Service Saving entity {0}: {1} with context: {2}", new Object[]{this.type.getSimpleName(), entity, ctx});

        if (HyperIoTSecurityUtil.checkPermission(ctx, entity,
            this.getAction(entity.getResourceName(), HyperIoTCrudAction.SAVE)))
            return this.getSystemService().save(entity, ctx);

        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Update an existing entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @param ctx    user context of HyperIoT platform
     */
    public T update(T entity, HyperIoTContext ctx) {
        log.log(Level.FINE, "Service Updating entity entity {0}: {1} with context: {2}", new Object[]{this.type.getSimpleName(), entity, ctx});
        if (entity.getId() > 0) {
            if (HyperIoTSecurityUtil.checkPermission(ctx, entity,
                this.getAction(entity.getResourceName(), HyperIoTCrudAction.UPDATE))) {
                HyperIoTResource r;
                try {
                    r = this.getSystemService().find(entity.getId(), ctx);
                } catch (NoResultException e) {
                    throw new HyperIoTEntityNotFound();
                }
                if (r != null) {
                    T updatedEntity = this.getSystemService().update(entity, ctx);
                    return updatedEntity;
                } else {
                    throw new HyperIoTEntityNotFound();
                }
            }
            throw new HyperIoTUnauthorizedException();
        } else
            throw new HyperIoTEntityNotFound();
    }

    /**
     * Remove an entity in database
     *
     * @param id  parameter that indicates a entity id
     * @param ctx user context of HyperIoT platform
     */
    public void remove(long id, HyperIoTContext ctx) {
        log.log(Level.FINE, "Service Removing entity {0} with id {1} with context: {2}", new Object[]{this.type.getSimpleName(), id, ctx});
        if (HyperIoTSecurityUtil.checkPermission(ctx, type.getName(),
            this.getAction(type.getName(), HyperIoTCrudAction.REMOVE))) {
            HyperIoTResource r;
            try {
                r = this.getSystemService().find(id, ctx);
            } catch (NoResultException e) {
                throw new HyperIoTEntityNotFound();
            }
            if (r != null) {
                if (HyperIoTSecurityUtil.checkPermission(ctx, r,
                    this.getAction(this.type.getName(), HyperIoTCrudAction.REMOVE))) {
                    this.getSystemService().remove(id, ctx);
                    return;
                }
            } else {
                throw new HyperIoTEntityNotFound();
            }
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * Find an existing entity in database
     *
     * @param id  parameter that indicates a entity id
     * @param ctx user context of HyperIoT platform
     * @return Entity if found
     */
    public T find(long id, HyperIoTContext ctx) {
        HyperIoTQueryFilter queryFilter = new HyperIoTQueryEqualsFilter("id", id);
        return this.find(queryFilter, ctx);
    }

    /**
     * @param filter filter field-value pair which will be merged in "and" condition
     * @param ctx    user context of HyperIoT platform
     * @return
     */
    @Override
    public T find(HashMap<String, Object> filter, HyperIoTContext ctx) {
        HyperIoTQueryFilter finalFilter = HyperIoTQueryFilterBuilder.createFromMapWithAndCondition(filter);
        return this.find(finalFilter, ctx);
    }

    /**
     * @param filter filter
     * @param ctx    user context of HyperIoT platform
     * @return
     */
    @Override
    public T find(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        log.log(Level.FINE, "Service Find entity {0} with id {1} with context: {2}", new Object[]{this.type.getSimpleName(), filter, ctx});
        if (HyperIoTSecurityUtil.checkPermission(ctx, type.getName(),
            this.getAction(type.getName(), HyperIoTCrudAction.FIND))) {
            T entity;
            try {
                filter = this.createConditionForOwnedOrSharedResource(filter, ctx);
                entity = this.getSystemService().find(filter, ctx);
            } catch (NoResultException e) {
                throw new HyperIoTEntityNotFound();
            }

            if (entity != null) {
                if (HyperIoTSecurityUtil.checkPermission(ctx, entity,
                    this.getAction(entity.getResourceName(), HyperIoTCrudAction.FIND)))
                    return entity;
                else
                    throw new HyperIoTUnauthorizedException();
            } else {
                throw new HyperIoTEntityNotFound();
            }
        } else
            throw new HyperIoTUnauthorizedException();
    }

    /**
     * Find all entity in database
     *
     * @param ctx user context of HyperIoT platform
     * @return Collection of entity
     */
    public Collection<T> findAll(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        log.log(Level.FINE,
            "Service Find all entities {0} with context: {1}", new Object[]{this.type.getSimpleName(), ctx});
        if (HyperIoTSecurityUtil.checkPermission(ctx, type.getName(),
            this.getAction(type.getName(), HyperIoTCrudAction.FINDALL))) {
            filter = this.createConditionForOwnedOrSharedResource(filter, ctx);
            return this.getSystemService().findAll(filter, ctx);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param filter filter
     * @param ctx    user context of HyperIoT platform
     * @param delta
     * @param page
     * @return
     */
    @Override
    public HyperIoTPaginableResult<T> findAll(HyperIoTQueryFilter filter, HyperIoTContext ctx, int delta, int page) {
        log.log(Level.FINE,
            "Service Find all entities {0} with context: {1}", new Object[]{this.type.getSimpleName(), ctx});
        if (HyperIoTSecurityUtil.checkPermission(ctx, type.getName(),
            this.getAction(type.getName(), HyperIoTCrudAction.FINDALL))) {
            filter = this.createConditionForOwnedOrSharedResource(filter, ctx);
            return this.getSystemService().findAll(filter, ctx, delta, page);
        }
        throw new HyperIoTUnauthorizedException();
    }

    /**
     * @param filter
     * @param ctx    user context of HyperIoT platform
     * @return
     */
    @Override
    public Collection<T> findAll(HashMap<String, Object> filter, HyperIoTContext ctx) {
        HyperIoTQueryFilter finalFilter = HyperIoTQueryFilterBuilder.createFromMapWithAndCondition(filter);
        return this.findAll(finalFilter, ctx);
    }

    /**
     * @param filter
     * @param ctx    user context of HyperIoT platform
     * @param delta
     * @param page
     * @return
     */
    @Override
    public HyperIoTPaginableResult<T> findAll(HashMap<String, Object> filter, HyperIoTContext ctx, int delta, int page) {
        HyperIoTQueryFilter finalFilter = HyperIoTQueryFilterBuilder.createFromMapWithAndCondition(filter);
        return this.findAll(filter, ctx, delta, page);
    }

    /**
     *
     * @param initialFilter
     * @param ctx
     * @return
     */
    private HyperIoTQueryFilter createConditionForOwnedOrSharedResource(HyperIoTQueryFilter initialFilter, HyperIoTContext ctx) {
        try {
            if (HyperIoTOwnershipResourceService.class.isAssignableFrom(this.getClass())) {
                HyperIoTOwnershipResourceService ownedRes = (HyperIoTOwnershipResourceService) this;
                HyperIoTQueryFilter ownedResourceFilter = null;
                if (HyperIoTSharingEntityService.class.isAssignableFrom(this.getClass())) {
                    //forcing the condition that user must own the entity or is shared to it
                    List<Long> entityIds = getSharedEntitySystemService().getEntityIdsSharedWithUser(type.getName(), ctx.getLoggedEntityId(), null);
                    ownedResourceFilter = new HyperIoTQueryEqualsFilter(ownedRes.getOwnerFieldPath(), ctx.getLoggedEntityId())
                        .or(new HyperIoTQueryInFilter<>("id", entityIds));
                } else {
                    //forcing the condition that user must own the entity
                    ownedResourceFilter = new HyperIoTQueryEqualsFilter(ownedRes.getOwnerFieldPath(), ctx.getLoggedEntityId());
                }
                if (initialFilter == null)
                    initialFilter = ownedResourceFilter;
                else {
                    initialFilter = initialFilter.and(ownedResourceFilter);
                }
            }
            return initialFilter;
        } catch (NoResultException e) {
            throw new HyperIoTEntityNotFound();
        }
    }

    protected abstract HyperIoTBaseEntitySystemApi<T> getSystemService();

    /**
     * Return current class name and action name registered as OSGi components
     *
     * @param className parameter that indicates the class name
     * @param action    parameter that indicates the action name
     * @return class name and action name registered as OSGi components
     */
    protected HyperIoTAction getAction(String className, HyperIoTCrudAction action) {
        log.log(Level.FINE,
            "Service getAction for {0} and action {1}", new Object[]{className, action.getName()});
        Collection<ServiceReference<HyperIoTAction>> serviceReferences;
        try {
            String actionFilter = OSGiFilterBuilder
                .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, className)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
            log.log(Level.FINE,
                "Searching for OSGi registered action with filter: {0}", actionFilter);
            serviceReferences = HyperIoTUtil.getBundleContext(this)
                .getServiceReferences(HyperIoTAction.class, actionFilter);
            if (serviceReferences.size() > 1) {
                log.log(Level.SEVERE, "More OSGi action found for filter: {0}", actionFilter);
                throw new HyperIoTRuntimeException();
            } else if (serviceReferences.size() == 0) {
                return null;
            }
            HyperIoTAction act = HyperIoTUtil.getBundleContext(this)
                .getService(serviceReferences.iterator().next());
            log.log(Level.FINE, "OSGi action found {0}", act);
            return act;
        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, "Invalid OSGi Syntax", e);
            throw new HyperIoTRuntimeException();
        }
    }

    /**
     * Return current entity type
     */
    @Override
    public Class<T> getEntityType() {
        return this.type;
    }

    /**
     * Retrieve from OSGi the SharedEntitySystemApi
     *
     * @return the SharedEntitySystemApi
     */
    public SharedEntitySystemApi getSharedEntitySystemService() {
        SharedEntitySystemApi sharedEntitySystemService = (SharedEntitySystemApi) HyperIoTUtil.getService(SharedEntitySystemApi.class);
        return sharedEntitySystemService;
    }

}
