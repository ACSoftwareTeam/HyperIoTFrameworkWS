package it.acsoftware.hyperiot.role.test;

import it.acsoftware.hyperiot.role.model.Role;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;

import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

public class HyperIoTRoleConfiguration {
    static final String hyperiotVersion = "1.0.0";
    static final String hyperIoTException = "it.acsoftware.hyperiot.base.exception.";
    static final String roleResourceName = Role.class.getName();

    static final String permissionArea = "it.acsoftware.hyperiot.area.model.Area";
    static final String permissionAlgorithm = "it.acsoftware.hyperiot.algorithm.model.Algorithm";
    static final String permissionAssetCategory = "it.acsoftware.hyperiot.asset.category.model.AssetCategory";
    static final String permissionAssetTag = "it.acsoftware.hyperiot.asset.tag.model.AssetTag";
    static final String permissionHProject = "it.acsoftware.hyperiot.hproject.model.HProject";
    static final String permissionHProjectAlgorithm = "it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm";
    static final String permissionHDevice = "it.acsoftware.hyperiot.hdevice.model.HDevice";
    static final String permissionHPacket = "it.acsoftware.hyperiot.hpacket.model.HPacket";
    static final String permissionRule = "it.acsoftware.hyperiot.rule.model.Rule";
    static final String permissionDashboard = "it.acsoftware.hyperiot.dashboard.model.Dashboard";
    static final String permissionDashboardWidget = "it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget";
    static final String permissionWidget = "it.acsoftware.hyperiot.widget.model.Widget";
    static final String nameRegisteredPermission = " RegisteredUser Permissions";

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
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.widget/HyperIoTWidget-features/" + hyperiotVersion
                                + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
                        ",hyperiot-algorithm,hyperiot-hadoopmanager,hyperiot-hprojectalgorithm,hyperiot-core-clustered," +
                                "hyperiot-dashboard,hyperiot-dashboardwidget,hyperiot-hproject,hyperiot-hdevice," +
                                "hyperiot-hpacket,hyperiot-ruleengine,hyperiot-area,hyperiot-stormmanager," +
                                "hyperiot-hbaseconnector,hyperiot-widget"
                )
        };
    }

}
