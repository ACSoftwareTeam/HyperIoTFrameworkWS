package it.acsoftware.hyperiot.asset.category.test;

import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.company.model.Company;
import org.ops4j.pax.exam.Option;


public class HyperIoTAssetCategoryConfiguration {
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String assetCategoryResourceName = AssetCategory.class.getName();
    static final String companyResourceName = Company.class.getName();

    protected static Option[] getConfiguration() {
        return new Option[]{};
    }

}
