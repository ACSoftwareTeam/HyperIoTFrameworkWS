package it.acsoftware.hyperiot.base.service.rest;

import it.acsoftware.hyperiot.base.api.HyperIoTRestAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntityApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;

import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Model class for HyperIoTBaseEntityRestApi. This
 * class implements all methods to test every response, and any
 * exceptions, produced in the CRUD operations invoked by the Rest
 * services.
 */
public abstract class HyperIoTBaseEntityRestApi<T extends HyperIoTBaseEntity> extends HyperIoTBaseRestApi {
    protected Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * Response and any exceptions for save operation
     *
     * @param entity parameter that indicates a generic entity
     * @return response for save operation
     */
    public Response save(T entity) {
        log.log(Level.FINER, "Invoking Save entity from rest service for {0} {1}"
                , new Object[]{this.getEntityService().getEntityType().getSimpleName(), entity});
        try {
            entity = this.getEntityService().save(entity, this.getHyperIoTContext());
            return Response.ok(entity).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for update operation
     *
     * @param entity parameter that indicates a generic entity
     * @return response for update operation
     */
    public Response update(T entity) {
        log.log(Level.FINER, "Invoking Update entity from rest service for {0} {1}"
                , new Object[]{this.getEntityService().getEntityType().getSimpleName(), entity});
        try {
            T updatedEntity = this.getEntityService().update(entity, this.getHyperIoTContext());
            return Response.ok(updatedEntity).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for remove operation
     *
     * @param id parameter that indicates a entity id
     * @return response for remove operation
     */
    public Response remove(long id) {
        try {
            log.log(Level.FINER, "Invoking Remove entity from rest service for {0} with id: {1}"
                    , new Object[]{this.getEntityService().getEntityType().getSimpleName(), id});
            this.getEntityService().remove(id, this.getHyperIoTContext());
            return Response.ok().build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for find operation
     *
     * @param id parameter that indicates a entity id
     * @return response for find operation
     */
    public Response find(long id) {
        log.log(Level.FINER, "Invoking Find entity from rest service for {0} with id: {1}"
                , new Object[]{this.getEntityService().getEntityType().getSimpleName(), id});
        try {
            HyperIoTBaseEntity entity = this.getEntityService().find(id, this.getHyperIoTContext());
            return Response.ok(entity).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for find all
     *
     * @return response for find all
     */
    public Response findAll() {
        log.log(Level.FINER, "Invoking Find All entity from rest service for {0}"
                , this.getEntityService().getEntityType().getSimpleName());
        try {
            Collection<T> list = this.getEntityService().findAll((HyperIoTQueryFilter)null, this.getHyperIoTContext());
            return Response.ok(list).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for find all
     *
     * @return response for find all
     */
    public Response findAll(HashMap<String, Object> filter) {
        log.log(Level.FINER, "Invoking Find All entity from rest service for {0}"
                ,this.getEntityService().getEntityType().getSimpleName());
        try {
            Collection<T> list = this.getEntityService().findAll(filter, this.getHyperIoTContext());
            return Response.ok(list).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for find all
     *
     * @return response for find all
     */
    public Response findAll(Integer delta, Integer page) {
        log.log(Level.FINER, "Invoking Find All entity from rest service for {0}"
                ,this.getEntityService().getEntityType().getSimpleName());
        if (delta == null || delta <= 0)
            delta = HyperIoTConstants.HYPERIOT_DEFAULT_PAGINATION_DELTA;
        if (page == null || page <= 0)
            page = 1;
        try {
            HyperIoTPaginableResult<T> result = this.getEntityService().findAll((HyperIoTQueryFilter)null, this.getHyperIoTContext(), delta, page);
            return Response.ok(result).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Response and any exceptions for find all
     *
     * @return response for find all
     */
    public Response findAll(Integer delta, Integer page, HashMap<String, Object> filter) {
        log.log(Level.FINER, "Invoking Find All entity from rest service for {0}"
                , this.getEntityService().getEntityType().getSimpleName());
        if (delta == null || delta <= 0)
            delta = HyperIoTConstants.HYPERIOT_DEFAULT_PAGINATION_DELTA;
        if (page == null || page <= 0)
            page = 1;
        try {
            HyperIoTPaginableResult<T> result = this.getEntityService().findAll(filter, this.getHyperIoTContext(), delta, page);
            return Response.ok(result).build();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Return a response, and any exception, during a call in the Rest services
     *
     * @param action parameter that indicates current call by Rest service
     * @return response during a call in the Rest services
     */
    public Response createResponse(HyperIoTRestAction action) {
        try {
            return action.doAction();
        } catch (Throwable e) {
            return handleException(e);
        }
    }

    /**
     * Return current EntityService
     */
    protected abstract HyperIoTBaseEntityApi<T> getEntityService();

}
