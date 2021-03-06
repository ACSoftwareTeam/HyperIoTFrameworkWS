package it.acsoftware.hyperiot.shared.entity.test;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public class HyperIoTSharedEntityConfiguration {
    static final String hyperiotVersion = "1.0.0";
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String sharedEntityResourceName = SharedEntity.class.getName();
    static final String hProjectSharedResourceName = HProject.class.getName();
    static final String hDeviceResourceName = HDevice.class.getName();
    static final String hPacketResourceName = HPacket.class.getName();
    static final String areaResourceName = Area.class.getName();
    static final String hUserResourceName = HUser.class.getName();

    protected static Option[] getBaseConfiguration() {
        return new Option[]{
                // HADOOPMANAGER PROPERTIES
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.hadoopmanager.cfg",
                        "it.acsoftware.hyperiot.hadoopmanager.defaultFS", "${env:HYPERIOT_HADOOPMANAGER_DEFAULTFS:-hdfs://namenode:8020}"),
                // ALGORITHM PROPERTIES
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.algorithm.cfg",
                        "it.acsoftware.hyperiot.algorithm.jar.base.path", "${env:HYPERIOT_ALGORITHM_JAR_BASE_PATH:-/spark/jobs}"),
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
                        ",mvn:it.acsoftware.hyperiot.hproject/HyperIoTHProject-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hdevice/HyperIoTHDevice-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hpacket/HyperIoTHPacket-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.stormmanager/HyperIoTStormManager-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.rule/HyperIoTRuleEngine-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.area/HyperIoTArea-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hbase.connector/HyperIoTHBaseConnector-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.core/HyperIoTCore-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.hproject.algorithm/HyperIoTHProjectAlgorithm-features/" + hyperiotVersion
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
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
                        ",hyperiot-algorithm,hyperiot-hadoopmanager,hyperiot-hprojectalgorithm,hyperiot-core-clustered," +
                                "hyperiot-dashboard,hyperiot-dashboardwidget, hyperiot-hproject,hyperiot-hdevice," +
                                "hyperiot-hpacket,hyperiot-ruleengine,hyperiot-area,hyperiot-stormmanager," +
                                "hyperiot-hbaseconnector"
                )
        };
    }

}
