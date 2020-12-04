package it.acsoftware.hyperiot.shared.entity.service;


import java.util.HashMap;
import java.util.List;

import java.util.logging.Level;

import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.action.util.HyperIoTShareAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntitySystemApi;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTQueryFilter;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTSharedEntity;
import it.acsoftware.hyperiot.base.exception.*;
import it.acsoftware.hyperiot.base.security.util.HyperIoTSecurityUtil;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl ;

import javax.persistence.NoResultException;


/**
 *
 * @author Aristide Cittadino Implementation class of SharedEntityApi interface.
 *         It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = SharedEntityApi.class, immediate = true)
public final class SharedEntityServiceImpl extends HyperIoTBaseEntityServiceImpl<SharedEntity>  implements SharedEntityApi {
	/**
	 * Injecting the SharedEntitySystemApi
	 */
	private SharedEntitySystemApi systemService;

	/**
	 * Constructor for a SharedEntityServiceImpl
	 */
	public SharedEntityServiceImpl() {
		super(SharedEntity.class);
	}

	/**
	 *
	 * @return The current SharedEntitySystemApi
	 */
	protected SharedEntitySystemApi getSystemService() {
		log.log(Level.FINEST, "invoking getSystemService, returning: {}" , this.systemService);
		return systemService;
	}

	/**
	 *
	 * @param sharedEntitySystemService Injecting via OSGi DS current systemService
	 */
	@Reference
	protected void setSystemService(SharedEntitySystemApi sharedEntitySystemService) {
		log.log(Level.FINEST, "invoking setSystemService, setting: {}" , systemService);
		this.systemService = sharedEntitySystemService ;
	}

	@Override
	public SharedEntity save(SharedEntity entity, HyperIoTContext ctx) {
		if(entity.getEntityResourceName() != null) {
			Class<?> entityClass = getEntityClass(entity.getEntityResourceName());

			if (entityClass == null || !HyperIoTSharedEntity.class.isAssignableFrom(entityClass)) {
				throw new HyperIoTRuntimeException("Entity " + entity.getEntityResourceName() + " is not a HyperIoTSharedEntity");
			}

			//check if the user has the share permission for the entity identified by entityResourceName
			if (!HyperIoTSecurityUtil.checkPermission(ctx, entityClass.getName(), HyperIoTActionsUtil.getHyperIoTAction(entityClass.getName(), HyperIoTShareAction.SHARE))) {
				throw new HyperIoTUnauthorizedException();
			}
			else {
				//get the system service of the entity identified by entityResourceName
				HyperIoTBaseEntitySystemApi<? extends HyperIoTSharedEntity> systemService = getEntitySystemService(entityClass);

				//find the entity
				HyperIoTSharedEntity e;
				try {
					e = systemService.find(entity.getEntityId(), ctx);
				}catch (NoResultException ex) {
					throw new HyperIoTEntityNotFound();
				}

				//check if the user owner of the entity is the logged one
				HyperIoTUser u = e.getUserOwner();
				if(u.getId() != ctx.getLoggedEntityId()) {
					throw new HyperIoTUnauthorizedException();
				}
			}
		}

		//do not check save permission for SharedEntity entities because if the share permission for an HyperIoTSharedEntity
		//implicitly has the permission to save a SharedEntity
		return systemService.save(entity, ctx);
	}

	@Override
	public SharedEntity update(SharedEntity entity, HyperIoTContext ctx) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(long id, HyperIoTContext ctx) {
		throw new UnsupportedOperationException();
	}

    @Override
    public SharedEntity find(long id, HyperIoTContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SharedEntity find(HashMap<String, Object> filter, HyperIoTContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SharedEntity find(HyperIoTQueryFilter filter, HyperIoTContext ctx) {
        throw new UnsupportedOperationException();
    }

    @Override
	public void removeByPK(String entityResourceName, long entityId, long userId, HyperIoTContext ctx) {
		this.log.log(Level.FINE,
				"Service Remove entity {0} with primary key (entityResourceName: {1}, entityId: {2}, userId: {3}) with context: {4}",
				new Object[]{this.getEntityType().getSimpleName(), entityResourceName, entityId, userId, ctx});

		Class<?> entityClass = getEntityClass(entityResourceName);

		//check if the user has the share permission for the entity identified by entityResourceName
		if (!HyperIoTSecurityUtil.checkPermission(ctx, entityClass.getName(), HyperIoTActionsUtil.getHyperIoTAction(entityClass.getName(), HyperIoTShareAction.SHARE))) {
			throw new HyperIoTUnauthorizedException();
		}
		else {
			//get the system service of the entity identified by entityResourceName
			HyperIoTBaseEntitySystemApi<? extends HyperIoTSharedEntity> systemService = getEntitySystemService(entityClass);

			//find the entity
			HyperIoTSharedEntity e;
			try {
				e = systemService.find(entityId, ctx);
			}catch (NoResultException ex) {
				throw new HyperIoTEntityNotFound();
			}

			//check if the user owner of the entity is the logged one
			HyperIoTUser u = e.getUserOwner();
			if(u.getId() != ctx.getLoggedEntityId()) {
				throw new HyperIoTUnauthorizedException();
			}
		}

		SharedEntity entity;
		try {
			entity = this.getSystemService().findByPK(entityResourceName, entityId, userId, null, ctx);
		} catch (NoResultException var7) {
			throw new HyperIoTEntityNotFound();
		}

		if (entity != null) {
			getSystemService().removeByPK(entityResourceName, entityId, userId, ctx);
		} else {
			throw new HyperIoTEntityNotFound();
		}

	}

	@Override
	public SharedEntity findByPK(String entityResourceName, long entityId, long userId, HashMap<String, Object> filter, HyperIoTContext ctx) {
		this.log.log(Level.FINE,
				"Service Find entity {0} with primary key (entityResourceName: {1}, entityId: {2}, userId: {3}) with context: {4}",
				new Object[]{this.getEntityType().getSimpleName(), entityResourceName, entityId, userId, ctx});

		if (!HyperIoTSecurityUtil.checkPermission(
				ctx, this.getEntityType().getName(), this.getAction(this.getEntityType().getName(), HyperIoTCrudAction.FIND))) {
			throw new HyperIoTUnauthorizedException();
		} else {
			SharedEntity entity;
			try {
				entity = this.getSystemService().findByPK(entityResourceName, entityId, userId, filter, ctx);
			} catch (NoResultException var7) {
				throw new HyperIoTEntityNotFound();
			}

			if (entity != null) {
				if (HyperIoTSecurityUtil.checkPermission(ctx, entity, this.getAction(entity.getResourceName(), HyperIoTCrudAction.FIND))) {
					return entity;
				} else {
					throw new HyperIoTUnauthorizedException();
				}
			} else {
				throw new HyperIoTEntityNotFound();
			}
		}
	}

	@Override
	public List<SharedEntity> findByEntity(String entityResourceName, long entityId, HashMap<String, Object> filter, HyperIoTContext ctx) {
		this.log.log(Level.FINE, "Service Find entity {0} with entityResourceName {1}, entityId {2} with context: {3}",
				new Object[]{this.getEntityType().getSimpleName(), entityResourceName, entityId, ctx});
		if (HyperIoTSecurityUtil.checkPermission(ctx, this.getEntityType().getName(), this.getAction(this.getEntityType().getName(), HyperIoTCrudAction.FIND))) {
			return this.getSystemService().findByEntity(entityResourceName, entityId, filter, ctx);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public List<SharedEntity> findByUser(long userId, HashMap<String, Object> filter, HyperIoTContext ctx) {
		this.log.log(Level.FINE, "Service Find entity {0} with userId {1} with context: {2}", new Object[]{this.getEntityType().getSimpleName(), userId, ctx});
		if (HyperIoTSecurityUtil.checkPermission(ctx, this.getEntityType().getName(), this.getAction(this.getEntityType().getName(), HyperIoTCrudAction.FIND))) {
			return this.getSystemService().findByUser(userId, filter, ctx);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public List<HyperIoTUser> getSharingUsers(String entityResourceName, long entityId, HyperIoTContext context) {
		this.log.log(Level.FINE, "Service getSharingUsers {0} with entityResourceName {1}, entityId {2} with context: {3}",
				new Object[]{this.getEntityType().getSimpleName(), entityResourceName, entityId, context});
		if (HyperIoTSecurityUtil.checkPermission(context, this.getEntityType().getName(), this.getAction(this.getEntityType().getName(), HyperIoTCrudAction.FIND))) {
			return this.getSystemService().getSharingUsers(entityResourceName, entityId, context);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	@Override
	public List<Long> getEntityIdsSharedWithUser(String entityResourceName, long userId, HyperIoTContext context) {
		this.log.log(Level.FINE, "Service getEntityIdsSharedWithUser {0} with entityResourceName {1}, userId {2} with context: {3}",
				new Object[]{this.getEntityType().getSimpleName(), entityResourceName, userId, context});
		if (HyperIoTSecurityUtil.checkPermission(context, this.getEntityType().getName(), this.getAction(this.getEntityType().getName(), HyperIoTCrudAction.FIND))) {
			return this.getSystemService().getEntityIdsSharedWithUser(entityResourceName, userId, context);
		} else {
			throw new HyperIoTUnauthorizedException();
		}
	}

	private HyperIoTBaseEntitySystemApi<? extends HyperIoTSharedEntity> getEntitySystemService(Class<?> entityClass) {
		this.log.log(Level.FINE, "Get system service of entity {0}", new Object[]{this.getEntityType().getSimpleName()});

		Class<?> systemApiClass = null;
		try {
			systemApiClass = Class.forName(entityClass.getName().replace(".model.", ".api.") + "SystemApi");
		} catch (ClassNotFoundException e) {}

		HyperIoTBaseEntitySystemApi<? extends HyperIoTSharedEntity> systemApi = null;
		if(systemApiClass != null) {
			systemApi = (HyperIoTBaseEntitySystemApi<? extends HyperIoTSharedEntity>) HyperIoTUtil.getService(systemApiClass);
		}

		if(systemApi == null) {
			throw new HyperIoTRuntimeException("No such system service found for entity " + entityClass.getSimpleName());
		}
		return systemApi;
	}

	private Class<?> getEntityClass(String resourceName) {
		try {
			return Class.forName(resourceName);
		}catch (ClassNotFoundException e) {
			throw new HyperIoTRuntimeException("Entity " + resourceName + " is not a HyperIoTSharedEntity");
		}
	}

}
