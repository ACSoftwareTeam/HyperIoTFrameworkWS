package it.acsoftware.hyperiot.permission.test;

import it.acsoftware.hyperiot.permission.model.Permission;
import org.ops4j.pax.exam.Option;

public class HyperIoTPermissionConfiguration {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String permissionResourceName = Permission.class.getName();

    protected static Option[] getConfiguration() {
        return new Option[]{};
    }

}
