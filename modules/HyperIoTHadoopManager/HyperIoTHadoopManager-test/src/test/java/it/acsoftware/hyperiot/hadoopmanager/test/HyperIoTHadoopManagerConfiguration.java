package it.acsoftware.hyperiot.hadoopmanager.test;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public class HyperIoTHadoopManagerConfiguration {
    static final String hyperiotVersion = "1.0.0";

    //jar file
    static final String jarName = "algorithm_test_copy_file_001.jar";
    static final String jarPath = "/spark/jobs/";

    protected static Option[] getConfiguration() {
        return new Option[]{
                // HADOOPMANAGER file cfg
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.hadoopmanager.cfg",
                        "it.acsoftware.hyperiot.hadoopmanager.defaultFS", "${env:HYPERIOT_HADOOPMANAGER_DEFAULTFS:-hdfs://namenode:8020}"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hadoopmanager/HyperIoTHadoopManager-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
                        ",hyperiot-hadoopmanager"
                )
        };
    }

}
