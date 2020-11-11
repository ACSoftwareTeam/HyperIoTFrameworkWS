/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * Written by Gene <generoso.martello@acsoftware.it>, March 2019
 *
 */
package it.acsoftware.hyperiot.stormmanager.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketField;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldMultiplicity;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.hpacket.model.HPacketFormat;
import it.acsoftware.hyperiot.hpacket.model.HPacketSerialization;
import it.acsoftware.hyperiot.hpacket.model.HPacketType;
import it.acsoftware.hyperiot.hproject.api.HProjectSystemApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.model.actions.events.SendMailAction;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.TopologyConfig;
import it.acsoftware.hyperiot.stormmanager.model.TopologyErrors;

/**
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-11 Initial release
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StormManagerSystemApiTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        Option[] options = {
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:org.kie/kie-karaf-features/7.20.0.Final/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.area/HyperIoTArea-features/"
                                + Defs.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.rule/HyperIoTRuleEngine-features/"
                                + Defs.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.stormmanager/HyperIoTStormManager-features/"
                                + Defs.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-area"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-ruleengine"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-stormmanager"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/it.acsoftware.hyperiot.cfg", "it.acsoftware.hyperiot.stormmanager.storm.path"), Defs.stormFolderPath)
        };
        /*
        Option[] postgresSQL = {
                // Datasource configuration with Postgres
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "databaseName", "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "portNumber", "5432"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "serverName", "localhost"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "user", "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "password", "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "dataSourceName", "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "osgi.jdbc.driver.class",
                        "org.postgresql.Driver"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "xa", "true"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "pool", "dbcp2")
        };
        */
        return HyperIoTTestConfigurationBuilder
                .createStandardConfiguration()
                // Starts with HSQL
                .withHSQL()
                // Starts with Postgres
                //.append(postgresSQL)
                // Enable remote debugging
                .withDebug("5010", false)
                // Custom options
                .append(options)
                .build();
    }

    @Test
    public void test0_buildTopology() throws IOException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        HPacketSystemApi packetSystemApi = getOsgiService(HPacketSystemApi.class);
        HDeviceSystemApi deviceSystemApi = getOsgiService(HDeviceSystemApi.class);
        HProjectSystemApi projectSystemApi = getOsgiService(HProjectSystemApi.class);
        HUserSystemApi userSystemApi = getOsgiService(HUserSystemApi.class);
        RuleEngineSystemApi ruleEngineSystemApi = getOsgiService(RuleEngineSystemApi.class);

        // create test HProject + HDevice + HPacket(s)
        HUser user = createTestHUser(userSystemApi);
        HyperIoTContext context = null;
        HProject project = new HProject();
        project.setName("Test");
        project.setDescription("A test project");
        project.setUser(user);
        projectSystemApi.save(project, context);

        HDevice device = new HDevice();
        device.setProject(project);
        device.setDeviceName("TestDevice" + UUID.randomUUID().toString().replaceAll("-", ""));
        device.setDescription("A test device");
        device.setBrand("ACSoftware");
        device.setModel("MultiSensor");
        device.setFirmwareVersion("1.0");
        device.setSoftwareVersion("1.0");
        device.setPassword("passwordPass&01");
        device.setPasswordConfirm("passwordPass&01");
        deviceSystemApi.save(device, context);

        HPacket packet1 = new HPacket();
        packet1.setDevice(device);
        packet1.setVersion("1.0");
        packet1.setName("MultiSensor data");
        packet1.setType(HPacketType.OUTPUT);
        packet1.setFormat(HPacketFormat.JSON);
        packet1.setSerialization(HPacketSerialization.AVRO);

        HPacketField field1 = new HPacketField();
        field1.setPacket(packet1);
        field1.setName("temperature");
        field1.setDescription("Temperature");
        field1.setType(HPacketFieldType.DOUBLE);
        field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field1.setValue(23.52d);

        HPacketField field2 = new HPacketField();
        field2.setPacket(packet1);
        field2.setName("humidity");
        field2.setDescription("Humidity");
        field2.setType(HPacketFieldType.DOUBLE);
        field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field2.setValue(42.75);

        packet1.setFields(new ArrayList<HPacketField>() {
            {
                add(field1);
                add(field2);
            }
        });

        HPacket packet2 = new HPacket();
        packet2.setDevice(device);
        packet2.setVersion("1.0");
        packet2.setName("GPS data");
        packet2.setType(HPacketType.OUTPUT);
        packet2.setFormat(HPacketFormat.JSON);
        packet2.setSerialization(HPacketSerialization.AVRO);

        HPacketField field3 = new HPacketField();
        field3.setPacket(packet2);
        field3.setName("gps");
        field3.setDescription("GPS");
        field3.setType(HPacketFieldType.OBJECT);
        field3.setMultiplicity(HPacketFieldMultiplicity.ARRAY);
        HPacketField field3_1 = new HPacketField();
        field3_1.setName("longitude");
        field3_1.setDescription("GPS Longitude");
        field3_1.setType(HPacketFieldType.DOUBLE);
        field3_1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field3_1.setParentField(field3);
        field3_1.setValue(48.243d);
        HPacketField field3_2 = new HPacketField();
        field3_2.setName("latitude");
        field3_2.setDescription("GPS Latitude");
        field3_2.setType(HPacketFieldType.DOUBLE);
        field3_2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        field3_2.setParentField(field3);
        field3_2.setValue(38.123d);

        HashSet<HPacketField> gpsFields = new HashSet<>();
        gpsFields.add(field3_1);
        gpsFields.add(field3_2);
        field3.setInnerFields(gpsFields);

        packet2.setFields(new ArrayList<HPacketField>() {
            {
                add(field3);
            }
        });

        packetSystemApi.save(packet1, context);
        packetSystemApi.save(packet2, context);

        // KIE-Drools

        Rule rule1 = new Rule();
        rule1.setName("Add category rule 1");
        rule1.setDescription("Ambarabaccicicocò");
        rule1.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action = new AddCategoryRuleAction();
        action.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(action);
        rule1.setActions(actions);
        rule1.setProject(project);
        rule1.setPacket(packet1);
        rule1.setRuleDefinition("temperature >= 23 AND humidity > 36");
        ruleEngineSystemApi.save(rule1, context);
        //System.out.println(rule1.droolsDefinition());

        Rule rule2 = new Rule();
        rule2.setName("Add category rule 2");
        rule2.setDescription("Ambarabaccicicocò");
        rule2.setType(RuleType.ENRICHMENT);
        AddCategoryRuleAction action2 = new AddCategoryRuleAction();
        action2.setCategoryIds(new long[]{123});
        List<RuleAction> actions2 = new ArrayList<>();
        actions2.add(action2);
        rule2.setActions(actions2);
        rule2.setProject(project);
        rule2.setPacket(packet2);
        rule2.setRuleDefinition("gps.latitude >= 3 AND temperature > 6");
        ruleEngineSystemApi.save(rule2, context);
        //System.out.println(rule2.droolsDefinition());

        Rule rule3 = new Rule();
        rule3.setName("Event action rule 1");
        rule3.setDescription("Send email");
        rule3.setType(RuleType.EVENT);
        SendMailAction action3 = new SendMailAction();
        action3.setRecipients("someone@somewhere.net");
        action3.setSubject("Sensor alert");
        action3.setBody("This is a test message.\nHello World!\n");
        List<RuleAction> actions3 = new ArrayList<>();
        actions3.add(action3);
        rule3.setActions(actions3);
        rule3.setProject(project);
        rule3.setPacket(packet1);
        rule3.setRuleDefinition("humidity >= 40 AND temperature > 21");
        ruleEngineSystemApi.save(rule3, context);
        //System.out.println(rule3.droolsDefinition());
        // Example code to verify an action rule and get the results
        /*
        String droolsCode = ruleEngineSystemApi.getDroolsForProject(project.getId(), RuleType.EVENT);
        System.out.println(droolsCode);
        RuleEngine engine = new RuleEngine(droolsCode);
        engine.check(packet1);
        StatelessKieSession session = engine.getSession();
        ArrayList<String> ruleActions = (ArrayList<String>)session.getGlobals().get("actions");
        System.out.println(ruleActions);
        */

        // Generate topology properties and YAML files
        HashMap<Long, TopologyConfig> topologyConfig = stormManagerSystemApi.getProjectTopology(project.getId());
        assertFalse(topologyConfig.isEmpty());

        // Print out the results
        topologyConfig.forEach((deviceId, config) -> {
            assertFalse(config.properties == null || config.properties.isEmpty());
            assertFalse(config.yaml == null || config.yaml.isEmpty());
            System.out.println("############## Device id = " + deviceId + " ##############");
            System.out.println("\n--------------[ topology.properties ]--------------\n");
            System.out.println(config.properties);
            System.out.println("\n-----------------[ topology.yaml ]-----------------\n");
            System.out.println(config.yaml);
            System.out.println("----------------------------------------------\n\n");
        });

    }

    @Test
    public void test1_submitTestTopology() throws InterruptedException, IOException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.submitTopology(
                Defs.topologyJarPath,
                Defs.topologyYamlPath,
                Defs.topologyPropsPath
        );
    }

    @Test
    public void test2_topologyListShouldContainTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        List<TopologyStatus> topologyList = stormManagerSystemApi.getTopologyList();
        // should contain test topology
        assertTrue(topologyList.stream().anyMatch(o -> o.getTopologyName().equals(Defs.testTopologyName)));
    }

    @Test
    public void test3_activateTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.activateTopology(Defs.testTopologyName);
    }

    @Test
    public void test4_testTopologyShouldBeActive() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        TopologyStatus topologyStatus = stormManagerSystemApi.getTopologyStatus(Defs.testTopologyName);
        assertTrue(topologyStatus != null && topologyStatus.getStatus().equals("ACTIVE"));
    }

    @Test
    public void test5_deactivateTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.deactivateTopology(Defs.testTopologyName);
    }

    @Test
    public void test6_testTopologyShouldBeInactive() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        TopologyStatus topologyStatus = stormManagerSystemApi.getTopologyStatus(Defs.testTopologyName);
        assertTrue(topologyStatus != null && topologyStatus.getStatus().equals("INACTIVE"));
    }

    @Test
    public void test7_testTopologyShouldNotHaveErrors() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        TopologyErrors topologyErrors = stormManagerSystemApi.getTopologyErrors(Defs.testTopologyName);
        System.out.println(topologyErrors);
        assertTrue(topologyErrors.getErrors().isEmpty());
    }

    @Test
    public void test8_killTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        stormManagerSystemApi.killTopology(Defs.testTopologyName, 1);
    }

    @Test
    public void test9_topologyListShouldNotContainTestTopology() throws IOException, InterruptedException {
        StormManagerSystemApi stormManagerSystemApi = getOsgiService(StormManagerSystemApi.class);
        List<TopologyStatus> topologyList = stormManagerSystemApi.getTopologyList();
        boolean acsTopologyExists = false;
        for (TopologyStatus topology : topologyList) {
            System.out.println(topology);
            acsTopologyExists = acsTopologyExists || topology.getTopologyName().equals(Defs.testTopologyName);
        }
        assertFalse(acsTopologyExists);
    }

    // Utility: create new HUser
    private HUser createTestHUser(HUserSystemApi userSystemApi) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser)authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        HUser testUser = new HUser();
        testUser.setName("name" + java.util.UUID.randomUUID());
        testUser.setLastname("lastname" + java.util.UUID.randomUUID());
        testUser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        testUser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
        testUser.setPassword("passwordPass&01");
        testUser.setPasswordConfirm("passwordPass&01");
        testUser.setAdmin(false);
        userSystemApi.save(testUser, null);
        return testUser;
    }
}
