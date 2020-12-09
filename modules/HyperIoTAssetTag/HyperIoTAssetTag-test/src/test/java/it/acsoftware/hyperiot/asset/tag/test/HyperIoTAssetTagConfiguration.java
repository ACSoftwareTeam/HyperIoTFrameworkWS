package it.acsoftware.hyperiot.asset.tag.test;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.company.model.Company;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;

public class HyperIoTAssetTagConfiguration {
    static final String hyperiotVersion = "1.0.0";
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String assetTagResourceName = AssetTag.class.getName();
    static final String companyResourceName = Company.class.getName();

    protected static Option[] getConfiguration() {
        return new Option[]{};
    }

}
