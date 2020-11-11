package it.acsoftware.hyperiot.asset.tag.service;

import it.acsoftware.hyperiot.asset.tag.api.AssetTagApi;
import it.acsoftware.hyperiot.asset.tag.api.AssetTagSystemApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntityServiceImpl;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of AssetTagApi interface.
 * It is used to implement all additional methods in order to interact with the system layer.
 */
@Component(service = AssetTagApi.class, immediate = true)
public final class AssetTagServiceImpl extends HyperIoTBaseEntityServiceImpl<AssetTag> implements AssetTagApi {
    /**
     * Injecting the AssetTagSystemApi
     */
    private AssetTagSystemApi systemService;

    /**
     * Constructor for a AssetTagServiceImpl
     */
    public AssetTagServiceImpl() {
        super(AssetTag.class);
    }

    /**
     * @return The current AssetTagSystemApi
     */
    protected AssetTagSystemApi getSystemService() {
        log.log(Level.FINEST, "invoking getSystemService, returning: {0}" , this.systemService);
        return systemService;
    }

    /**
     * @param assetTagSystemService Injecting via OSGi DS current systemService
     */
    @Reference
    protected void setSystemService(AssetTagSystemApi assetTagSystemService) {
        log.log(Level.FINEST, "invoking setSystemService, setting: {0}" , systemService);
        this.systemService = assetTagSystemService;
    }

}
