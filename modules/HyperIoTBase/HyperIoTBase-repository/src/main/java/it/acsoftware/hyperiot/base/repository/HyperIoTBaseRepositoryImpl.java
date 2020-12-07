package it.acsoftware.hyperiot.base.repository;

import it.acsoftware.hyperiot.base.api.HyperIoTAssetCategoryManager;
import it.acsoftware.hyperiot.base.api.HyperIoTAssetTagManager;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.entity.*;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTEntityNotFound;
import it.acsoftware.hyperiot.base.model.HyperIoTPaginatedResult;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilter;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryEqualsFilter;
import it.acsoftware.hyperiot.query.util.filter.HyperIoTQueryFilterBuilder;
import org.apache.aries.jpa.template.EmConsumer;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.apache.commons.lang3.ClassUtils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.criteria.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Model class for HyperIoTBaseRepositoryImpl. This
 * class implements all methods for basic CRUD operations defined in
 * HyperIoTBaseRepository interface. This methods are reusable by all
 * entities that interact with the HyperIoT platform.
 */
public abstract class HyperIoTBaseRepositoryImpl<T extends HyperIoTBaseEntity>
    implements HyperIoTBaseRepository<T> {
    private Logger log = Logger.getLogger(HyperIoTBaseRepositoryImpl.class.getName());

    /**
     * Generic class for HyperIoT platform
     */
    protected Class<T> type;

    /**
     * Managing asset categories
     */
    private HyperIoTAssetCategoryManager assetCategoryManager;

    /**
     * Managing asset tags
     */
    private HyperIoTAssetTagManager assetTagManager;

    /**
     * Constructor for HyperIoTBaseRepositoryImpl
     *
     * @param type parameter that indicates a generic entity
     */
    public HyperIoTBaseRepositoryImpl(Class<T> type) {
        this.type = type;
    }

    /**
     * @return The current jpaTemplate
     */
    protected abstract JpaTemplate getJpa();

    /**
     * @param jpa The jpaTemplate value to interact with database
     */
    protected abstract void setJpa(JpaTemplate jpa);

    /**
     * @return HyperIoTAssetCategoryManager
     */
    public HyperIoTAssetCategoryManager getAssetCategoryManager() {
        return assetCategoryManager;
    }

    /**
     * @param assetCategoryManager
     */
    @Reference
    public void setAssetCategoryManager(HyperIoTAssetCategoryManager assetCategoryManager) {
        this.assetCategoryManager = assetCategoryManager;
    }

    /**
     * @return HyperIoTAssetTagManager
     */
    public HyperIoTAssetTagManager getAssetTagManager() {
        return assetTagManager;
    }

    /**
     * @param assetTagManager
     */
    @Reference
    public void setAssetTagManager(HyperIoTAssetTagManager assetTagManager) {
        this.assetTagManager = assetTagManager;
    }

    /**
     * Save an entity in database
     */
    @Override
    public T save(T entity) {
        log.log(Level.FINE,
            "Repository Saving entity {0}: {1}", new Object[]{this.type.getSimpleName(), entity});
        this.checkDuplicate(entity);
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke persist");
            entityManager.persist(entity);
            manageAssets(entity, false);
            entityManager.flush();
            log.log(Level.FINE, "Entity persisted: {0}", entity);
            HyperIoTUtil.invokePostActions(entity, HyperIoTPostSaveAction.class); // execute post actions after saving
            return entity;
        });
    }

    /**
     * Update an entity in database
     */
    @Override
    public T update(T entity) {
        log.log(Level.FINE,
            "Repository Update entity {0}: {1}", new Object[]{this.type.getSimpleName(), entity});
        this.checkDuplicate(entity);
        if (entity.getId() > 0) {
            return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
                log.log(Level.FINE, "Transaction found, invoke find and merge");
                T dbEntity = (T) entityManager.find(entity.getClass(), entity.getId());
                //forcing to maintain old create date
                entity.setEntityCreateDate(dbEntity.getEntityCreateDate());
                entity.setEntityVersion(dbEntity.getEntityVersion());
                T updateEntity = entityManager.merge(entity);
                manageAssets(entity, false);
                entityManager.flush();
                log.log(Level.FINE, "Entity merged: {0}", entity);
                //Invoking global post actions
                HyperIoTUtil.invokePostActions(entity, HyperIoTPostUpdateAction.class);
                //invoking detailed update actions, for who wants identify what is changed inside entity
                invokePostUpdateDetailedAction(dbEntity, entity); // execute post actions after updating
                return updateEntity;
            });
        }
        throw new HyperIoTEntityNotFound();
    }

    /**
     * Remove an entity by id
     */
    @Override
    public void remove(long id) {
        log.log(Level.FINE,
            "Repository Remove entity {0} with id: {1}", new Object[]{this.type.getSimpleName(), id});
        this.getJpa().tx(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke remove");
            T entity = find(id, null);
            entityManager.remove(entity);
            manageAssets(entity, true);
            entityManager.flush();
            log.log(Level.FINE,
                "Entity {0}  with id: {1}  removed", new Object[]{this.type.getSimpleName(), id});
            //we can use global post actions since there's no need to pass "before" entity
            HyperIoTUtil.invokePostActions(entity, HyperIoTPostRemoveAction.class); // execute post actions after removing
        });
    }

    /**
     * @param id  parameter that indicates a entity id
     * @param ctx user context of HyperIoT platform
     * @return
     */
    @Override
    public T find(long id, HyperIoTContext ctx) {
        log.log(Level.FINE,
            "Repository Find entity {0} with id: {1}", new Object[]{this.type.getSimpleName(), id});
        HyperIoTQueryFilter idFilter = new HyperIoTQueryEqualsFilter("id", id);
        return this.find(idFilter, ctx);
    }


    /**
     * @param filter filter
     * @param ctx    user context of HyperIoT platform
     * @return
     */
    @Override
    public T find(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        log.log(Level.FINE,
            "Repository Find entity {0} with filter: {1}", new Object[]{this.type.getSimpleName(), filter});
        return this.getJpa().txExpr(TransactionType.Required, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke find");
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> query = criteriaBuilder.createQuery(this.type);
            Root<T> entityDef = query.from(this.type);
            Predicate condition = HyperIoTQueryFilterBuilder.createPredicateByQueryFilter(entityDef, criteriaBuilder, filter);
            Query q = entityManager.createQuery(query.select(entityDef).where(condition));
            try {
                T entity = (T) q.getSingleResult();
                log.log(Level.FINE, "Found entity: {0}", entity);
                return entity;
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw e;
            }
        });
    }

    /**
     * Find all entity
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<T> findAll(HyperIoTQueryFilter filter) {
        log.log(Level.FINE, "Repository Find All entities {0}", this.type.getSimpleName());
        return (Collection<T>) this.getJpa().txExpr(TransactionType.RequiresNew, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke findAll");
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<T> query = criteriaBuilder.createQuery(this.type);
            Root<T> entityDef = query.from(this.type);
            Predicate condition = HyperIoTQueryFilterBuilder.createPredicateByQueryFilter(entityDef, criteriaBuilder, filter);
            Query q = (condition != null) ? entityManager.createQuery(query.select(entityDef).where(condition)) : entityManager.createQuery(query.select(entityDef));
            try {
                Collection<T> results = (Collection<T>) q.getResultList();
                log.log(Level.FINE, "Query results: {0}", results);
                return results;
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw e;
            }
        });
    }

    /**
     * Find all entity
     */
    @SuppressWarnings("unchecked")
    @Override
    public HyperIoTPaginatedResult<T> findAll(int delta, int page, HyperIoTQueryFilter filter) {
        log.log(Level.FINE, "Repository Find All entities {0}", this.type.getSimpleName());
        return (HyperIoTPaginatedResult<T>) this.getJpa().txExpr(TransactionType.RequiresNew,
            entityManager -> {
                log.log(Level.FINE, "Transaction found, invoke findAll");
                CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
                //constructing query and count query
                CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
                CriteriaQuery<T> query = criteriaBuilder.createQuery(this.type);
                Root<T> entityDef = query.from(this.type);
                Predicate condition = HyperIoTQueryFilterBuilder.createPredicateByQueryFilter(entityDef, criteriaBuilder, filter);
                countQuery = (condition != null) ? countQuery.select(criteriaBuilder.count(entityDef)).where(condition) : countQuery.select(criteriaBuilder.count(entityDef));
                query = (condition != null) ? query.select(entityDef).where(condition) : query.select(entityDef);
                //Executing count query
                Query countQueryFinal = entityManager.createQuery(countQuery);
                Long countResults = (Long) countQueryFinal.getSingleResult();
                int lastPageNumber = (int) (Math.ceil(countResults / delta));
                int nextPage = (page <= lastPageNumber - 1) ? page + 1 : 1;
                //Executing paginated query
                Query q = entityManager.createQuery(query);
                int firstResult = (lastPageNumber - 1) * delta;
                if (lastPageNumber == 0) {
                    firstResult = 0;
                }
                q.setFirstResult(firstResult);
                q.setMaxResults(delta);
                try {
                    Collection<T> results = q.getResultList();
                    HyperIoTPaginatedResult<T> paginatedResult = new HyperIoTPaginatedResult<>(
                        lastPageNumber, page, delta, nextPage, results);
                    log.log(Level.FINE, "Query results: {0}", results);
                    return paginatedResult;
                } catch (Exception e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    throw e;
                }
            });
    }

    /**
     * Find all entity with a query. This function return a collection of entity
     */
    @SuppressWarnings("unchecked")

    public HyperIoTPaginatedResult<T> queryForResultList(String query,
                                                         HashMap<String, Object> params, int delta, int page) {
        log.log(Level.FINE, "Repository queryForResultList {0}", new Object[]{query, params});
        return (HyperIoTPaginatedResult<T>) this.getJpa().txExpr(TransactionType.RequiresNew,
            entityManager -> {
                log.log(Level.FINE, "Transaction found, invoke findAll");
                Query countQuery = entityManager.createQuery(
                    "Select count(*) " + query.substring(query.indexOf("from")));
                Query q = entityManager.createQuery(query, this.type);
                Iterator<String> it = params.keySet().iterator();
                while (it.hasNext()) {
                    String paramName = it.next();
                    q.setParameter(paramName, params.get(paramName));
                    countQuery.setParameter(paramName, params.get(paramName));
                }
                Long countResults = (Long) countQuery.getSingleResult();
                int lastPageNumber = (int) (Math.ceil(countResults / delta));
                int nextPage = (page <= lastPageNumber - 1) ? page + 1 : 1;
                try {
                    Collection<T> results = q.getResultList();
                    HyperIoTPaginatedResult<T> paginatedResult = new HyperIoTPaginatedResult<>(
                        lastPageNumber, page, delta, nextPage, results);
                    log.log(Level.FINE, "Query results: {0}", results);
                    return paginatedResult;
                } catch (Exception e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                    throw e;
                }
            });
    }

    /**
     * Find all entity with a query. This function return a collection of entity
     */
    @SuppressWarnings("unchecked")
    @Override
    public Collection<T> queryForResultList(String query, HashMap<String, Object> params) {
        log.log(Level.FINE, "Repository queryForResultList {0}", new Object[]{query, params});
        return (Collection<T>) this.getJpa().txExpr(TransactionType.RequiresNew, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke findAll");

            Query q = entityManager.createQuery(query, this.type);
            Iterator<String> it = params.keySet().iterator();
            while (it.hasNext()) {
                String paramName = it.next();
                q.setParameter(paramName, params.get(paramName));
            }
            try {
                Collection<T> results = q.getResultList();
                log.log(Level.FINE, "Query results: " + results);
                return results;
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw e;
            }
        });
    }

    /**
     * Find an entity with a query.
     */
    @SuppressWarnings("unchecked")
    @Override
    public T queryForSingleResult(String query, HashMap<String, Object> params) {
        log.log(Level.FINE, "Repository queryForSingleResult {0}", new Object[]{query, params});
        T returnResult = (T) this.getJpa().txExpr(TransactionType.RequiresNew, entityManager -> {
            log.log(Level.FINE, "Transaction found, invoke findAll");
            Query q = entityManager.createQuery(query, this.type);
            Iterator<String> it = params.keySet().iterator();
            while (it.hasNext()) {
                String paramName = it.next();
                q.setParameter(paramName, params.get(paramName));
            }
            try {
                Object result = q.getSingleResult();
                log.log(Level.FINE, "Query result: " + result);
                return (T) result;
            } catch (NoResultException e) {
                log.log(Level.FINE, e.getMessage(), e);
                return null;
            } catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw e;
            }
        });

        if (returnResult == null)
            throw new NoResultException();
        return returnResult;
    }

    /**
     * Executes code inside a transaction without returning  result
     *
     * @param txType
     * @param function
     */
    public void executeTransaction(TransactionType txType, EmConsumer function) {
        getJpa().tx(txType, function);
    }

    /**
     * Executes code inside a transaction returning  result
     *
     * @param txType
     * @param function
     */
    public <R> R executeTransactionWithReturn(TransactionType txType, EmFunction<R> function) {
        return getJpa().txExpr(txType, function);
    }

    /**
     * Based on @UniqueConstriant hibernate annotation this method tries to check if
     * the entity is already present in the database without generating rollback
     * exception
     *
     * @param entity the entity which must be persisted or updated
     */
    private void checkDuplicate(T entity) {
        log.log(Level.FINE, "Checking duplicates for entity {0}", this.type.getName());
        Table[] tableAnnotation = entity.getClass().getAnnotationsByType(Table.class);
        if (tableAnnotation != null && tableAnnotation.length > 0) {
            UniqueConstraint[] uniqueConstraints = tableAnnotation[0].uniqueConstraints();
            if (uniqueConstraints != null && uniqueConstraints.length > 0) {
                for (int i = 0; i < uniqueConstraints.length; i++) {
                    String[] columnNames = uniqueConstraints[i].columnNames();
                    log.log(Level.FINE, "Found UniqueConstraints {0}", Arrays.toString(columnNames));
                    StringBuilder sb = new StringBuilder();
                    HashMap<String, Object> params = new HashMap<>();
                    for (int j = 0; j < columnNames.length; j++) {
                        String fieldName = columnNames[j];
                        String innerField = null;
                        // Field is a relationship, so we need to do 2 invocations
                        if (fieldName.contains("_")) {
                            String temp = fieldName;
                            fieldName = fieldName.substring(0, fieldName.indexOf("_"));
                            innerField = temp.substring(temp.indexOf("_") + 1);
                        }

                        String getterMethod = "get" + fieldName.substring(0, 1).toUpperCase()
                            + fieldName.substring(1);
                        try {
                            Method m = this.type.getMethod(getterMethod);
                            Object value = null;
                            // when inner field is null, the relative getter is invoked on the
                            // target entity
                            if (innerField == null)
                                value = m.invoke(entity);
                                // when inner field is != null then, the getter method is called on the
                                // related
                                // entity
                            else {
                                String getterInnerMethod = "get"
                                    + innerField.substring(0, 1).toUpperCase()
                                    + innerField.substring(1);
                                Object innerEntity = m.invoke(entity);
                                if (innerEntity != null) {
                                    Method innerMethod = innerEntity.getClass()
                                        .getMethod(getterInnerMethod);
                                    value = innerMethod.invoke(innerEntity);
                                } else {
                                    value = null;
                                }
                            }
                            // append only if innerMethod succeed
                            if (j > 0)
                                sb.append(" and ");
                            if (innerField == null) {
                                sb.append(fieldName).append("=").append(":").append(fieldName);
                                params.put(fieldName, value);
                            } else {
                                if (value != null) {
                                    sb.append(fieldName).append(".").append(innerField).append("=:")
                                        .append(fieldName).append(innerField);
                                    params.put(fieldName + innerField, value);
                                } else {
                                    sb.append(fieldName).append(".").append(innerField).append(" is null");
                                }
                            }
                        } catch (Exception e) {
                            log.log(Level.SEVERE, "Impossible to find getter method {0}", new Object[]{getterMethod, this.type.getName(), e});
                        }
                    }
                    // executing the query
                    String query = "from " + this.type.getSimpleName() + " where " + sb.toString();
                    log.log(Level.FINE, "Executing the query {0} with parameters: {1}", new Object[]{query, params.toString()});
                    try {
                        T result = this.queryForSingleResult(query, params);
                        // if the entity has not the same id than it's duplicated
                        if (result.getId() != entity.getId())
                            throw new HyperIoTDuplicateEntityException(columnNames);
                    } catch (NoResultException e) {
                        log.log(Level.FINE, "Entity duplicate check passed!");
                    }
                }
            }
        }
    }

    private void manageAssets(T entity, boolean removed) {
        if (this.assetCategoryManager != null && entity.getCategoryIds() != null) {
            if (!removed) {
                this.assetCategoryManager.addAssetCategories(entity.getResourceName(),
                    entity.getId(), entity.getCategoryIds());
            } else {
                this.assetCategoryManager.removeAssetCategories(entity.getResourceName(),
                    entity.getId(), entity.getCategoryIds());
            }

        }

        if (this.assetTagManager != null && entity.getTagIds() != null) {
            if (!removed) {
                this.assetTagManager.addAssetTags(entity.getResourceName(), entity.getId(),
                    entity.getTagIds());
            } else {
                this.assetTagManager.removeAssetTags(entity.getResourceName(), entity.getId(),
                    entity.getTagIds());
            }

        }
    }

    private Predicate createPredicate(HashMap<String, Object> filter, Root<T> entityDef, CriteriaBuilder criteriaBuilder) {
        if (filter != null && filter.size() > 0) {
            Iterator<String> it = filter.keySet().iterator();
            while (it.hasNext()) {
                Predicate filterCondition = null;
                String field = it.next();
                Object value = filter.get(field);
                //simple (name, value) filter
                String[] dottedRelationships = field.split("\\.");
                Path p = entityDef.get(dottedRelationships[0]);
                for (int i = 1; i < dottedRelationships.length; i++) {
                    p = p.get(dottedRelationships[i]);
                    filterCondition = criteriaBuilder.equal(p, filter.get(field));
                }
                return filterCondition;
            }
        }
        return null;
    }

    /**
     * @param beforeCrudAction Entity before Crud Action
     * @param afterCrudAction  Entity after Crud Action
     */
    private void invokePostUpdateDetailedAction(HyperIoTBaseEntity beforeCrudAction, HyperIoTBaseEntity afterCrudAction) {
        log.log(Level.FINE, "Fetch post actions of type: PostUpdateAction");

        OSGiFilter osgiFilter = OSGiFilterBuilder.createFilter("type", beforeCrudAction.getClass().getName());

        //include OSGi filters for all interfaces implemented by resource and its superclasses
        String filter = ClassUtils.getAllInterfaces(beforeCrudAction.getClass())
            .stream()
            .map(interfaceClass -> OSGiFilterBuilder.createFilter("type", interfaceClass.getName()))
            .reduce(osgiFilter, OSGiFilter::or)
            .getFilter();

        ServiceReference<? extends HyperIoTPostUpdateDetailedAction>[] serviceReferences =
            HyperIoTUtil.getServices(HyperIoTPostUpdateAction.class, filter);
        if (serviceReferences == null)
            log.log(Level.FINE, "There are not post actions of type post update action");
        else {
            log.log(Level.FINE, "{0} post actions fetched", serviceReferences.length);
            for (ServiceReference<? extends HyperIoTPostUpdateDetailedAction> serviceReference : serviceReferences)
                try {
                    log.log(Level.FINE, "Executing post action: {0}", serviceReference);
                    HyperIoTPostUpdateDetailedAction hyperIoTPostDetailedAction = HyperIoTUtil.getBundleContext(HyperIoTPostUpdateDetailedAction.class).getService(serviceReference);
                    hyperIoTPostDetailedAction.execute(beforeCrudAction, afterCrudAction);
                } catch (Throwable e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
        }
    }

    /**
     *
     * @return logger of this class
     */
    protected Logger getLog() {
        return log;
    }
}
