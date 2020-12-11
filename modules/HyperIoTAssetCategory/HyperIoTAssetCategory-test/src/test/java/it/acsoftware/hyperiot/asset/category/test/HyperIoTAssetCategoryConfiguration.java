package it.acsoftware.hyperiot.asset.category.test;

import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.company.model.Company;
import org.ops4j.pax.exam.Option;


public class HyperIoTAssetCategoryConfiguration {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String assetCategoryResourceName = AssetCategory.class.getName();
    static final String companyResourceName = Company.class.getName();

    static final String permissionAssetCategory = "it.acsoftware.hyperiot.asset.category.model.AssetCategory";
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
