package it.acsoftware.hyperiot.hbase.connector.service.postactions;

import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPostUpdateAction;
import it.acsoftware.hyperiot.hproject.model.HProject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class creates HBase table after updating HProject.
 * This table is going to contains not deserialized HProject packets because of errors received from Kafka (bad packets)
 * @param <T>
 */
@Component(service = HyperIoTPostUpdateAction.class, property = {"type=it.acsoftware.hyperiot.hproject.model.HProject"})
public class HProjectPostUpdateAction<T extends HyperIoTBaseEntity> implements HyperIoTPostUpdateAction<T> {

    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    /**
     * Injecting the HProjectPostActionThreadPool
     */
    private HProjectPostActionThreadPool hProjectPostActionThreadPool;

    @Override
    public void execute(T entity) {
        HProject hProject = (HProject) entity;
        log.log(Level.FINE, "Create HBase tables, if necessary, after updating HProject with id {0}", hProject.getId());
        hProjectPostActionThreadPool.runPostUpdateAction(hProject.getId());
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
