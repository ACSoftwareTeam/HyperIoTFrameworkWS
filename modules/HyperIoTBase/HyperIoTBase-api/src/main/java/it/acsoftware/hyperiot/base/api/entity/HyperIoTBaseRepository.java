package it.acsoftware.hyperiot.base.api.entity;

import org.apache.aries.jpa.template.EmConsumer;
import org.apache.aries.jpa.template.EmFunction;
import org.apache.aries.jpa.template.TransactionType;

import java.util.Collection;
import java.util.HashMap;

/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Interface component for HyperIoTBaseRepository.
 * This interface defines the methods for basic CRUD operations. This
 * methods are reusable by all entities that interact with the HyperIoT
 * platform.
 */
public interface HyperIoTBaseRepository<T extends HyperIoTBaseEntity> {

    /**
     * Save an entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @return entity saved
     */
    public T save(T entity);

    /**
     * Update an existing entity in database
     *
     * @param entity parameter that indicates a generic entity
     */
    public T update(T entity);

    /**
     * Remove an entity in database
     *
     * @param id parameter that indicates a entity id
     */
    public void remove(long id);


    /**
     * Find an existing entity in database
     *
     * @param id parameter that indicates a entity id
     * @return Entity if found
     */
    public T find(long id, HashMap<String, Object> filter);


    /**
     * Find all entity in database
     *
     * @return Collection of entity
     */
    public Collection<T> findAll(HashMap<String, Object> filter);


    /**
     * Find all entity in database with paginated result
     *
     * @return Collection of entity
     */
    public HyperIoTPaginableResult<T> findAll(int delta, int page, HashMap<String, Object> filter);

    /**
     * Find all entity with a query. This function return a collection of entity
     *
     * @param query  parameter that defines the query in the hql language
     * @param params parameter that indicates the value to set within the query
     * @return Collection of entity
     */
    public Collection<T> queryForResultList(String query, HashMap<String, Object> params);

    /**
     * Find all entity with a query with pagination. This function return a
     * collection of entity
     *
     * @param query  parameter that defines the query in the hql language
     * @param params parameter that indicates the value to set within the query
     * @param delta  num of items per page
     * @param page   page number
     * @return Collection of entity
     */
    public HyperIoTPaginableResult<T> queryForResultList(String query,
                                                         HashMap<String, Object> params, int delta, int page);

    /**
     * Find an entity with a query
     *
     * @param query  parameter that defines the query in the hql language
     * @param params parameter that indicates the value to set within the query
     * @return Entity if found
     */
    public T queryForSingleResult(String query, HashMap<String, Object> params);

    /**
     * Executes code inside a transaction without returning  result
     *
     * @param txType
     * @param function
     */
    public void executeTransaction(TransactionType txType, EmConsumer function);

    /**
     * Executes code inside a transaction returning a result
     *
     * @param txType
     * @param function
     * @param <R>
     * @return
     */
    public <R> R executeTransactionWithReturn(TransactionType txType, EmFunction<R> function);
}
