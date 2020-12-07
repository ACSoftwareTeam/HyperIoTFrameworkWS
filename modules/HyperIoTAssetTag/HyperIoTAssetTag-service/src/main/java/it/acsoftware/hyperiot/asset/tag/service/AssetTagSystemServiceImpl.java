package it.acsoftware.hyperiot.asset.tag.service;

import it.acsoftware.hyperiot.asset.tag.api.AssetTagRepository;
import it.acsoftware.hyperiot.asset.tag.api.AssetTagSystemApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.service.entity.HyperIoTBaseEntitySystemServiceImpl;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.List;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the AssetTagSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 */
@Component(service = AssetTagSystemApi.class, immediate = true)
public final class AssetTagSystemServiceImpl extends HyperIoTBaseEntitySystemServiceImpl<AssetTag>
        implements AssetTagSystemApi {

    /**
     * Injecting the AssetTagRepository to interact with persistence layer
     */
    private AssetTagRepository repository;

    private PermissionSystemApi permissionSystemApi;

    /**
     * Constructor for a AssetTagSystemServiceImpl
     */
    public AssetTagSystemServiceImpl() {
        super(AssetTag.class);
    }

    /**
     * Return the current repository
     */
    protected AssetTagRepository getRepository() {
        getLog().log(Level.FINEST, "invoking getRepository, returning: {0}" , this.repository);
        return repository;
    }

    /**
     * @param assetTagRepository The current value of AssetTagRepository to interact
     *                           with persistence layer
     */
    @Reference
    protected void setRepository(AssetTagRepository assetTagRepository) {
        getLog().log(Level.FINEST, "invoking setRepository, setting: {0}" , assetTagRepository);
        this.repository = assetTagRepository;
    }

    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    @Activate
    public void onActivate() {
        this.checkRegisteredUserRoleExists();
    }

    private void checkRegisteredUserRoleExists() {
        String resourceName = AssetTag.class.getName();
        List<HyperIoTAction> actions = HyperIoTActionsUtil.getHyperIoTCrudActions(resourceName);
        this.permissionSystemApi
                .checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

}
