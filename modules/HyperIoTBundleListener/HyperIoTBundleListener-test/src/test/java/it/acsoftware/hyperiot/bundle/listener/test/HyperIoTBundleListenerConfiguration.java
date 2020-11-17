package it.acsoftware.hyperiot.bundle.listener.test;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;

public class HyperIoTBundleListenerConfiguration {
    static final String hyperiotVersion = "1.0.0";

    protected static Option[] getBaseConfiguration() {
        return new Option[]{
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.bundle.listener/HyperIoTBundleListener-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.core/HyperIoTCore-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.algorithm/HyperIoTAlgorithm-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hadoopmanager/HyperIoTHadoopManager-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hproject/HyperIoTHProject-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hbase.connector/HyperIoTHBaseConnector-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hpacket/HyperIoTHPacket-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.area/HyperIoTArea-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.rule/HyperIoTRuleEngine-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.dashboard/HyperIoTDashboard-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.dashboard.widget/HyperIoTDashboardWidget-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hproject.algorithm/HyperIoTHProjectAlgorithm-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hdevice/HyperIoTHDevice-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.stormmanager/HyperIoTStormManager-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
                        ",hyperiot-core-clustered,hyperiot-bundlelistener,hyperiot-algorithm,hyperiot-hadoopmanager," +
                                "hyperiot-hproject,hyperiot-hbaseconnector,hyperiot-hpacket,hyperiot-area,hyperiot-ruleengine," +
                                "hyperiot-dashboard,hyperiot-dashboardwidget,hyperiot-hprojectalgorithm,hyperiot-hdevice," +
                                "hyperiot-stormmanager"
                )
        };
    }

}
