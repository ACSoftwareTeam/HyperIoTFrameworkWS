package it.acsoftware.hyperiot.authentication.test;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;

public class HyperIoTAuthenticationConfiguration {
    static final String hyperiotVersion = "1.0.0";

    protected static Option[] getBaseConfiguration() {
        return new Option[]{
                // hyperiot-core import the following features: base, mail, authentication, permission, huser, company, role,
                // assetcategory, assettag, sharedentity.
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.core/HyperIoTCore-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
                        ",hyperiot-core"
                )
        };
    }

}
