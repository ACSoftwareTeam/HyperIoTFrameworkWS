package it.acsoftware.hyperiot.shared.entity.service.postactions;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.shared.entity.api.SharedEntitySystemApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component(service = HyperIoTPostRemoveAction.class, property = {"type=it.acsoftware.hyperiot.huser.model.HUser"})
public class HyperIoTHUserPostRemoveAction<T extends HyperIoTBaseEntity> implements HyperIoTPostRemoveAction<T> {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

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
        log.log(Level.FINE, "Removing records with userId {0} from SharedEntity table after deleting the user {1}", new Object[]{entity.getId(), entity});

        List<SharedEntity> sharedEntityList = sharedEntitySystemService.findByUser(entity.getId(), null, null);
        sharedEntityList.forEach(sharedEntity -> sharedEntitySystemService.removeByPK(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId(), sharedEntity.getUserId(), null));
    }
}
