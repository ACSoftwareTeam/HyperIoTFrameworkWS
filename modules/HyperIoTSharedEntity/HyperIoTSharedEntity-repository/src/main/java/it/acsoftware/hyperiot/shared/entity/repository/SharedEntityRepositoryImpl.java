package it.acsoftware.hyperiot.shared.entity.repository;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.repository.HyperIoTBaseRepositoryImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityRepository;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.persistence.NoResultException;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.UniqueConstraint;
import javax.persistence.criteria.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino Implementation class of the SharedEntity. This
 * class is used to interact with the persistence layer.
 */
@Component(service = SharedEntityRepository.class, immediate = true)
public class SharedEntityRepositoryImpl extends HyperIoTBaseRepositoryImpl<SharedEntity> implements SharedEntityRepository {
    /**
     * Injecting the JpaTemplate to interact with database
     */
    private JpaTemplate jpa;

    /**
     * Constructor for a SharedEntityRepositoryImpl
     */
    public SharedEntityRepositoryImpl() {
        super(SharedEntity.class);
    }

    /**
     * @return The current jpaTemplate
     */
    @Override
    protected JpaTemplate getJpa() {
        getLog().log(Level.FINEST, "invoking getJpa, returning: {}", jpa);
        return jpa;
    }

    /**
     * @param jpa Injection of JpaTemplate
     */
    @Override
    @Reference(target = "(osgi.unit.name=hyperiot-sharedEntity-persistence-unit)")
    protected void setJpa(JpaTemplate jpa) {
        getLog().log(Level.FINEST, "invoking setJpa, setting: " + jpa);
        this.jpa = jpa;
    }

    @Override
    public SharedEntity save(SharedEntity entity) {
        //override save method in order to add check on duplicate by primary key because default checkDuplicate()
        //compare ids of this entity with whose returned by db, but for SharedEntity id is meaningless
        getLog().log(Level.FINE,
            "Repository Saving entity {0}: {1}", new Object[]{this.type.getSimpleName(), entity});
        this.checkDuplicateByPK(entity);
        return super.save(entity);
    }

    @Override
    public SharedEntity update(SharedEntity entity) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SharedEntity find(long id, HyperIoTContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SharedEntity find(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeByPK(String entityResourceName, long entityId, long userId) {
        this.getLog().log(Level.FINE,
            "Repository Remove entity {0} with primary key: (entityResourceName: {1}, entityId: {2}, userId: {3})",
            new Object[]{this.type.getSimpleName(), entityResourceName, entityId, userId});
        this.getJpa().tx(TransactionType.Required, (entityManager) -> {
            this.getLog().log(Level.FINE, "Transaction found, invoke remove");
            SharedEntity entity = findByPK(entityResourceName, entityId, userId, (HashMap) null);
            entityManager.remove(entity);
            //this..manageAssets(entity, true);
            entityManager.flush();
            this.getLog().log(Level.FINE,
                "Entity {0} with primary key: (entityResourceName: {1}, entityId: {2}, userId: {3}) removed",
                new Object[]{this.type.getSimpleName(), entityResourceName, entityId, userId});
            HyperIoTUtil.invokePostActions(entity, HyperIoTPostRemoveAction.class);
        });
    }

    @Override
    public SharedEntity findByPK(String entityResourceName, long entityId, long userId, HashMap<String, Object> filter) {
        this.getLog().log(Level.FINE,
            "Repository Find entity {0} with primary key: (entityResourceName: {1}, entityId: {2}, userId: {3})",
            new Object[]{this.type.getSimpleName(), entityResourceName, entityId, userId});
        return this.getJpa().txExpr(TransactionType.Required, (entityManager) -> {
            this.getLog().log(Level.FINE, "Transaction found, invoke find");
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<SharedEntity> query = criteriaBuilder.createQuery(this.type);
            Root<SharedEntity> entityDef = query.from(this.type);
            Predicate condition = criteriaBuilder.and(
                criteriaBuilder.equal(entityDef.get("entityResourceName"), entityResourceName),
                criteriaBuilder.equal(entityDef.get("entityId"), entityId),
                criteriaBuilder.equal(entityDef.get("userId"), userId));
            Predicate filterCondition;
            if (filter != null && filter.size() > 0) {
                for (Iterator it = filter.keySet().iterator(); it.hasNext(); condition = criteriaBuilder.and(condition, filterCondition)) {
                    String field = (String) it.next();
                    String[] dottedRelationships = field.split("\\.");
                    Path p = entityDef.get(dottedRelationships[0]);

                    for (int i = 1; i < dottedRelationships.length; ++i) {
                        p = p.get(dottedRelationships[i]);
                    }

                    filterCondition = criteriaBuilder.equal(p, filter.get(field));
                }
            }

            TypedQuery<SharedEntity> q = entityManager.createQuery(query.select(entityDef).where(condition));

            try {
                SharedEntity entity = q.getSingleResult();
                this.getLog().log(Level.FINE, "Found entity: {0}", entity);
                return entity;
            } catch (Exception var14) {
                this.getLog().log(Level.SEVERE, var14.getMessage(), var14);
                throw var14;
            }
        });
    }

    @Override
    public List<SharedEntity> findByEntity(String entityResourceName, long entityId, HashMap<String, Object> filter) {
        this.getLog().log(Level.FINE, "Repository Find entities {0} with entityResourceName {1} and entityId {2}", new Object[]{this.type.getSimpleName(), entityResourceName, entityId});
        return (List<SharedEntity>) this.getJpa().txExpr(TransactionType.RequiresNew, (entityManager) -> {
            this.getLog().log(Level.FINE, "Transaction found, invoke findByEntity");
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<SharedEntity> query = criteriaBuilder.createQuery(this.type);
            Root<SharedEntity> entityDef = query.from(this.type);
            Predicate condition = criteriaBuilder.and(
                criteriaBuilder.equal(entityDef.get("entityResourceName"), entityResourceName),
                criteriaBuilder.equal(entityDef.get("entityId"), entityId));
            Predicate filterCondition = null;
            if (filter != null && filter.size() > 0) {
                Iterator it = filter.keySet().iterator();

                while (it.hasNext()) {
                    String field = (String) it.next();
                    String[] dottedRelationships = field.split("\\.");
                    Path p = entityDef.get(dottedRelationships[0]);

                    for (int i = 1; i < dottedRelationships.length; ++i) {
                        p = p.get(dottedRelationships[i]);
                    }

                    filterCondition = criteriaBuilder.equal(p, filter.get(field));
                    if (filterCondition != null) {
                        condition = criteriaBuilder.and(condition, filterCondition);
                    }
                }
            }

            TypedQuery<SharedEntity> q = condition != null ? entityManager.createQuery(query.select(entityDef).where(condition)) : entityManager.createQuery(query.select(entityDef));

            try {
                List<SharedEntity> results = q.getResultList();
                this.getLog().log(Level.FINE, "Query results: {0}", results);
                return results;
            } catch (Exception var12) {
                this.getLog().log(Level.SEVERE, var12.getMessage(), var12);
                throw var12;
            }
        });
    }

    @Override
    public List<SharedEntity> findByUser(long userId, HashMap<String, Object> filter) {
        this.getLog().log(Level.FINE, "Repository Find entities {0} with userId {1}", new Object[]{this.type.getSimpleName(), userId});
        return this.getJpa().txExpr(TransactionType.RequiresNew, (entityManager) -> {
            this.getLog().log(Level.FINE, "Transaction found, invoke findByUser");
            CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
            CriteriaQuery<SharedEntity> query = criteriaBuilder.createQuery(this.type);
            Root<SharedEntity> entityDef = query.from(this.type);
            Predicate condition = criteriaBuilder.and(
                criteriaBuilder.equal(entityDef.get("userId"), userId));
            Predicate filterCondition = null;
            if (filter != null && filter.size() > 0) {
                Iterator it = filter.keySet().iterator();

                while (it.hasNext()) {
                    String field = (String) it.next();
                    String[] dottedRelationships = field.split("\\.");
                    Path p = entityDef.get(dottedRelationships[0]);

                    for (int i = 1; i < dottedRelationships.length; ++i) {
                        p = p.get(dottedRelationships[i]);
                    }

                    filterCondition = criteriaBuilder.equal(p, filter.get(field));
                    if (filterCondition != null) {
                        condition = criteriaBuilder.and(condition, filterCondition);
                    }
                }
            }

            TypedQuery<SharedEntity> q = condition != null ? entityManager.createQuery(query.select(entityDef).where(condition)) : entityManager.createQuery(query.select(entityDef));

            try {
                List<SharedEntity> results = q.getResultList();
                this.getLog().log(Level.FINE, "Query results: {0}", results);
                return results;
            } catch (Exception var12) {
                this.getLog().log(Level.SEVERE, var12.getMessage(), var12);
                throw var12;
            }
        });
    }

    @Override
    public List<HyperIoTUser> getSharingUsers(String entityResourceName, long entityId) {
        this.getLog().log(Level.FINE,
            "Repository invoke getSharingUsers with entityResourceName {0} and entityId {1}",
            new Object[]{this.type.getSimpleName(), entityResourceName, entityId});
        return this.getJpa().txExpr(TransactionType.RequiresNew, (entityManager) -> {
            List<HUser> users = entityManager
                .createQuery("SELECT u FROM SharedEntity se JOIN HUser u ON u.id = se.userId WHERE se.entityResourceName=:entityResourceName AND se.entityId=:entityId", HUser.class)
                .setParameter("entityResourceName", entityResourceName)
                .setParameter("entityId", entityId).getResultList();
            return users.stream().map(u -> (HyperIoTUser) u).collect(Collectors.toList());
        });
    }

    @Override
    public List<Long> getEntityIdsSharedWithUser(String entityResourceName, long userId) {
        return this.getJpa().txExpr(TransactionType.RequiresNew, (entityManager) -> {
            List<Long> entityIds = entityManager
                .createQuery("SELECT se.entityId FROM SharedEntity se WHERE se.entityResourceName=:entityResourceName AND se.userId=:userId", Long.class)
                .setParameter("entityResourceName", entityResourceName)
                .setParameter("userId", userId).getResultList();

            return entityIds;
        });
    }

    private void checkDuplicateByPK(SharedEntity entity) {
        getLog().log(Level.FINE, "Checking duplicates for entity {0}", this.type.getName());
        Table[] tableAnnotation = entity.getClass().getAnnotationsByType(Table.class);
        if (tableAnnotation != null && tableAnnotation.length > 0) {
            UniqueConstraint[] uniqueConstraints = tableAnnotation[0].uniqueConstraints();
            if (uniqueConstraints != null && uniqueConstraints.length > 0) {
                String[] columnNames = uniqueConstraints[0].columnNames();
                getLog().log(Level.FINE, "Found UniqueConstraints {0}", Arrays.toString(columnNames));
                try {
                    SharedEntity result = this.findByPK(entity.getEntityResourceName(), entity.getEntityId(), entity.getUserId(), null);
                    // if the entity is the same then it's duplicated
                    if (result != null)
                        throw new HyperIoTDuplicateEntityException(columnNames);
                } catch (NoResultException e) {
                    getLog().log(Level.FINE, "Entity duplicate check passed!");
                }
            }
        }
    }
}
