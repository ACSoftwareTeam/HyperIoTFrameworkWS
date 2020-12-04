package it.acsoftware.hyperiot.base.api.entity;

import java.util.Collection;
import java.util.HashMap;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;

/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Interface component for HyperIoTBaseEntityApi.
 * This interface defines the methods for basic CRUD operations. This
 * methods are reusable by all entities that interact with the HyperIoT
 * platform.
 */
public interface HyperIoTBaseEntityApi<T extends HyperIoTBaseEntity> {

    /**
     * Save an entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @param ctx    user context of HyperIoT platform
     * @return entity saved
     */
    public T save(T entity, HyperIoTContext ctx);

    /**
     * Update an existing entity in database
     *
     * @param entity parameter that indicates a generic entity
     * @param ctx    user context of HyperIoT platform
     */
    public T update(T entity, HyperIoTContext ctx);

    /**
     * Remove an entity in database
     *
     * @param id  parameter that indicates a entity id
     * @param ctx user context of HyperIoT platform
     */
    public void remove(long id, HyperIoTContext ctx);

    /**
     *
     * @param id
     * @param ctx
     * @return
     */
    public T find(long id, HyperIoTContext ctx);


    /**
     * Find an existing entity in database
     * @param filter field-value pair which will be merged in "and" condition
     * @param ctx user context of HyperIoT platform
     * @return Entity if found
     */
    public T find(HashMap<String,Object> filter, HyperIoTContext ctx);


    /**
     * Find an existing entity in database
     * @param filter filter
     * @param ctx user context of HyperIoT platform
     * @return Entity if found
     */
    public T find(HyperIoTQueryFilter filter, HyperIoTContext ctx);

    /**
     * Find all entity in database
     *
     * @param ctx user context of HyperIoT platform
     * @return Collection of entity
     */
    public Collection<T> findAll(HashMap<String,Object> filter,HyperIoTContext ctx);

    /**
     * Find all entity in database
     *
     * @param ctx   user context of HyperIoT platform
     * @param delta
     * @param page
     * @return Collection of entity
     */
    public HyperIoTPaginableResult<T> findAll(HashMap<String,Object> filter,HyperIoTContext ctx, int delta, int page);

    /**
     * Find all entity in database
     * @param filter filter
     * @param ctx user context of HyperIoT platform
     * @return Collection of entity
     */
    public Collection<T> findAll(HyperIoTQueryFilter filter,HyperIoTContext ctx);

    /**
     * Find all entity in database
     * @param filter filter
     * @param ctx   user context of HyperIoT platform
     * @param delta
     * @param page
     * @return Collection of entity
     */
    public HyperIoTPaginableResult<T> findAll(HyperIoTQueryFilter filter,HyperIoTContext ctx, int delta, int page);

    /**
     * Return current entity type
     */
    public Class<T> getEntityType();
}
