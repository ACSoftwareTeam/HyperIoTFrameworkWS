package it.acsoftware.hyperiot.hbase.connector.service.postactions;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostRemoveAction;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class deletes HBase table after removing HProject.
 * @param <T>
 */
@Component(service = HyperIoTPostRemoveAction.class, property = {"type=it.acsoftware.hyperiot.hproject.model.HProject"})
public class HProjectPostRemoveAction <T extends HyperIoTBaseEntity> implements HyperIoTPostRemoveAction<T> {

    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    /**
     * Injecting the HProjectPostActionThreadPool
     */
    private HProjectPostActionThreadPool hProjectPostActionThreadPool;

    @Override
    public void execute(T entity) {
        HProject hProject = (HProject) entity;
        log.log(Level.FINE, "Drop HBase tables after deleting HProject with id {0}", hProject.getId());
        hProjectPostActionThreadPool.runPostRemoveAction(hProject.getId());
    }

    /**
     *
     * @param hProjectPostActionThreadPool HProjectPostActionThreadPool service
     */
    @Reference
    public void sethProjectPostActionThreadPool(HProjectPostActionThreadPool hProjectPostActionThreadPool) {
        this.hProjectPostActionThreadPool = hProjectPostActionThreadPool;
    }

}
