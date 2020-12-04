package it.acsoftware.hyperiot.shared.entity.service;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntityRepository;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;

import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;

/**
 *
 * @author Aristide Cittadino Implementation class of the SharedEntitySystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = SharedEntitySystemApi.class, immediate = true)
public final class SharedEntitySystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<SharedEntity>  implements SharedEntitySystemApi {

	/**
	 * Injecting the SharedEntityRepository to interact with persistence layer
	 */
	private SharedEntityRepository repository;

	/**
	 * Constructor for a SharedEntitySystemServiceImpl
	 */
	public SharedEntitySystemServiceImpl() {
		super(SharedEntity.class);
	}

	/**
	 * Return the current repository
	 */
	protected SharedEntityRepository getRepository() {
		log.log(Level.FINEST, "invoking getRepository, returning: {}" , this.repository);
		return repository;
	}

	/**
	 * @param sharedEntityRepository The current value of SharedEntityRepository to interact with persistence layer
	 */
	@Reference
	protected void setRepository(SharedEntityRepository sharedEntityRepository) {
		log.log(Level.FINEST, "invoking setRepository, setting: {}" , sharedEntityRepository);
		this.repository = sharedEntityRepository;
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
	public void removeByPK(String entityResourceName, long entityId, long userId, HyperIoTContext ctx) {
		this.log.log(Level.FINE,
				"System Service Removing entity " + this.getEntityType().getSimpleName() + " with primary key: (entityResourceName: {0}, entityId: {1}, userId: {2})",
				new Object[]{entityResourceName, entityId, userId});
		this.getRepository().removeByPK(entityResourceName, entityId, userId);
	}

	@Override
	public SharedEntity findByPK(String entityResourceName, long entityId, long userId, HashMap<String, Object> filter, HyperIoTContext ctx) {
		this.log.log(Level.FINE,
				"System Service Finding entity " + this.getEntityType().getSimpleName() + " with primary key: (entityResourceName: {0}, entityId: {1}, userId: {2})",
				new Object[]{entityResourceName, entityId, userId});
		return this.getRepository().findByPK(entityResourceName, entityId, userId, filter);
	}

	@Override
	public List<SharedEntity> findByUser(long userId, HashMap<String, Object> filter, HyperIoTContext ctx) {
		this.log.log(Level.FINE, "System Service Finding entity " + this.getEntityType().getSimpleName() + " with userId: {0}", userId);
		return this.getRepository().findByUser(userId, filter);
	}

	@Override
	public List<SharedEntity> findByEntity(String entityResourceName, long entityId, HashMap<String, Object> filter, HyperIoTContext ctx) {
		this.log.log(Level.FINE, "System Service Finding entity " + this.getEntityType().getSimpleName() + " with entityResourceName: {0} and entityId: {1}",
				new Object[]{entityId, entityResourceName});
		return this.getRepository().findByEntity(entityResourceName, entityId, filter);
	}

	@Override
	public List<HyperIoTUser> getSharingUsers(String entityResourceName, long entityId, HyperIoTContext context) {
		this.log.log(Level.FINE, "System Service getSharingUsers with entityResourceName: {0} and entityId: {1}",
				new Object[]{entityResourceName, entityId});
		return this.getRepository().getSharingUsers(entityResourceName, entityId);
	}

	@Override
	public List<Long> getEntityIdsSharedWithUser(String entityResourceName, long userId, HyperIoTContext context) {
		this.log.log(Level.FINE, "System Service getEntityIdsSharedWithUser with entityResourceName: {0} and userId: {1}",
				new Object[]{entityResourceName, userId});
		return this.getRepository().getEntityIdsSharedWithUser(entityResourceName, userId);
	}
}
