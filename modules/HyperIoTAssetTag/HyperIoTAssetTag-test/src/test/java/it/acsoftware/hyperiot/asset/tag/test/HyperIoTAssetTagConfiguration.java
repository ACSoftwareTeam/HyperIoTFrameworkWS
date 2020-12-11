package it.acsoftware.hyperiot.asset.tag.test;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.company.model.Company;
import org.ops4j.pax.exam.Option;

public class HyperIoTAssetTagConfiguration {
    static final String hyperiotVersion = "1.0.0";
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String assetTagResourceName = AssetTag.class.getName();
    static final String companyResourceName = Company.class.getName();

    static final String permissionAssetTag = "it.acsoftware.hyperiot.asset.tag.model.AssetTag";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

    static final String validatorMustNotBeNull = "must not be null";
    static final String validatorMustNotBeEmpty = "must not be empty";
    static final String validatorNoMaliciousCode = "{it.acsoftware.hyperiot.validator.nomalitiuscode.message}";

    static final int defaultDelta = 10;
    static final int defaultPage = 1;

    protected static Option[] getConfiguration() {
        return new Option[]{};
    }

}
