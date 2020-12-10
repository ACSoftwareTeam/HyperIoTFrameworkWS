package it.acsoftware.hyperiot.role.test;

import it.acsoftware.hyperiot.role.model.Role;
import org.ops4j.pax.exam.Option;

public class HyperIoTRoleConfiguration {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String roleResourceName = Role.class.getName();

    static final String permissionAssetCategory = "it.acsoftware.hyperiot.asset.category.model.AssetCategory";
    static final String permissionAssetTag = "it.acsoftware.hyperiot.asset.tag.model.AssetTag";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    protected static Option[] getConfiguration() { return new Option[]{}; }

}
