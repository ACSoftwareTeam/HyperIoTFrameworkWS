package it.acsoftware.hyperiot.shared.entity.service.postactions;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTSharedEntity;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component(service = HyperIoTPostRemoveAction.class, property = {"type=it.acsoftware.hyperiot.base.api.entity.HyperIoTSharedEntity"})
public class HyperIoTSharedEntityPostRemoveAction<T extends HyperIoTBaseEntity> implements HyperIoTPostRemoveAction<T> {
    private static Logger log = Logger.getLogger(HyperIoTSharedEntityPostRemoveAction.class.getName());

    private SharedEntitySystemApi sharedEntitySystemService;

    /**
     *
     * @param sharedEntitySystemService SharedEntitySystemApi service
     */
    @Reference
    public void setSharedEntitySystemApi(SharedEntitySystemApi sharedEntitySystemService) {
        this.sharedEntitySystemService = sharedEntitySystemService;
    }

    @Override
    public void execute(T entity) {
        if (HyperIoTSharedEntity.class.isAssignableFrom(entity.getClass())) {
            log.log(Level.FINE, "Removing records with entityResourceName {0} and entityId {1} from SharedEntity table after deleting entity {2}", new Object[]{entity.getResourceName(), entity.getId(), entity.getClass().getSimpleName()});

            List<SharedEntity> sharedEntityList = sharedEntitySystemService.findByEntity(entity.getResourceName(), entity.getId(), null, null);
            sharedEntityList.forEach(sharedEntity -> sharedEntitySystemService.removeByPK(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId(), sharedEntity.getUserId(), null));
        }
    }
}
