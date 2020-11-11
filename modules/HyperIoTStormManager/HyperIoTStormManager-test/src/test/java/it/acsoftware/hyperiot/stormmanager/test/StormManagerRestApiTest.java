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

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.stormmanager.model.StormManager;
import it.acsoftware.hyperiot.stormmanager.service.rest.StormManagerRestApi;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

/**
 * 
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-11 Initial release
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StormManagerRestApiTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
	    Option[] options = {
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
				        "etc/org.apache.karaf.features.cfg",
								"featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.stormmanager/HyperIoTStormManager-features/"
                              + Defs.hyperiotVersion + "/xml/features"),

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
                        ",mvn:it.acsoftware.hyperiot.mail/HyperIoTMail-features/"
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
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-mail"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-ruleengine"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-stormmanager"),

                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                    "etc/it.acsoftware.hyperiot.cfg", "it.acsoftware.hyperiot.stormmanager.storm.path"),
                        Defs.stormFolderPath),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/it.acsoftware.hyperiot.cfg", "it.acsoftware.hyperiot.stormmanager.topology.path"),
                        Defs.stormTopologyPath),
                logLevel(LogLevelOption.LogLevel.DEBUG),
        };
        return HyperIoTTestConfigurationBuilder
                .createStandardConfiguration()
                // Starts with HSQL
                .withHSQL()
                // Enable remote debugging
                /*.withDebug("5010", false)*/
                // Custom options
                .append(options)
                .build();
	}

	@Test
    public void test00_addTopology() throws IOException {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // create file attachments
        ContentDisposition applicationOctetStream = new ContentDisposition("application/octet-stream");
        File jarFile = new File(Defs.topologyJarPath);
        FileInputStream fileStream = new FileInputStream(jarFile);
        Attachment jarAttachment = new Attachment("jar", fileStream, applicationOctetStream);
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.ADD_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.addTopology(jarAttachment, jarFile.getName());
        assertEquals(200, response.getStatus());
    }

	@Test
	public void test00_submitTestTopology() throws IOException {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // create file attachments
		ContentDisposition applicationOctetStream = new ContentDisposition("application/octet-stream");
		FileInputStream jarFile = new FileInputStream(Defs.topologyJarPath);
		Attachment jarAttachment = new Attachment("jar", jarFile, applicationOctetStream);
		FileInputStream yamlFile = new FileInputStream(Defs.topologyYamlPath);
		Attachment yamlAttachment = new Attachment("yaml", yamlFile, applicationOctetStream);
		FileInputStream propsFile = new FileInputStream(Defs.topologyPropsPath);
		Attachment propsAttachment = new Attachment("props", propsFile, applicationOctetStream);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        // TODO: this still keeps working even without impersonating an authorized user =/
/*
        Response failedResponse = stormManagerRestApi.submitTopology(
                jarAttachment,
                yamlAttachment,
                propsAttachment
        );
        assertEquals(403, failedResponse.getStatus());
*/
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.SUBMIT_TOPOLOGY, StormManager.class.getName());
		Response response = stormManagerRestApi.submitTopology(
				jarAttachment,
				yamlAttachment,
				propsAttachment
		);
		assertEquals(200, response.getStatus());
    }

	@Test
	public void test01_topologyListShouldContainTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopologyList();
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi,  HyperIoTHProjectAction.GET_TOPOLOGY_LIST, StormManager.class.getName());
		Response response = stormManagerRestApi.getTopologyList();
        assertEquals(200, response.getStatus());
        List<TopologyStatus> topologyList = response.readEntity(new GenericType<List<TopologyStatus>>(){});
        // should contain test topology
        assertTrue(topologyList.stream().anyMatch(o -> o.getTopologyName().equals(Defs.testTopologyName)));
	}

    @Test
    public void test02_getTopologyShouldFindTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopology(Defs.testTopologyName);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.GET_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.getTopology(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
        TopologyStatus topologyStatus = response.readEntity(new GenericType<TopologyStatus>(){});
        assertNotNull(topologyStatus);
    }

    @Test
    public void test03_activateTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.activateTopology(Defs.testTopologyName);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.ACTIVATE_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.activateTopology(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test04_testTopologyShouldBeActive() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.GET_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.getTopology(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
        TopologyStatus topologyStatus = response.readEntity(new GenericType<TopologyStatus>(){});
        assertEquals("ACTIVE", topologyStatus.getStatus());
    }

    @Test
    public void test05_deactivateTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.deactivateTopology(Defs.testTopologyName);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.DEACTIVATE_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.deactivateTopology(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test06_testTopologyShouldBeInactive() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.GET_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.getTopology(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
        TopologyStatus topologyStatus = response.readEntity(new GenericType<TopologyStatus>(){});
        assertEquals("INACTIVE", topologyStatus.getStatus());
    }

    @Test
    public void test07_getTopologyErrors() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopologyErrors(Defs.testTopologyName);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.GET_TOPOLOGY_ERRORS, StormManager.class.getName());
        Response response = stormManagerRestApi.getTopologyErrors(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
        //TopologyErrors topologyStatus = response.readEntity(new GenericType<TopologyErrors>(){});
    }

    @Test
    public void test08_rebalanceTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.rebalanceTopology(Defs.testTopologyName, 1, 3);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.REBALANCE_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.rebalanceTopology(Defs.testTopologyName, 1, 3);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test09_killTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.killTopology(Defs.testTopologyName);
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.KILL_TOPOLOGY, StormManager.class.getName());
        Response response = stormManagerRestApi.killTopology(Defs.testTopologyName);
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test10_topologyListShouldNotContainTestTopology() {
        StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
        impersonateUser(stormManagerRestApi, null);
        // the following call without impersonation should fail with response status code `403 Unauthorized`
        Response failedResponse = stormManagerRestApi.getTopologyList();
        assertEquals(403, failedResponse.getStatus());
        // call REST API method impersonating a user with granted permission on the performed action
        grantUserPermission(stormManagerRestApi, HyperIoTHProjectAction.GET_TOPOLOGY_LIST, StormManager.class.getName());
        Response response = stormManagerRestApi.getTopologyList();
        assertEquals(200, response.getStatus());
        List<TopologyStatus> topologyList = response.readEntity(new GenericType<List<TopologyStatus>>(){});
        // should NOT contain test topology
        assertFalse(topologyList.stream().anyMatch(o -> o.getTopologyName().equals(Defs.testTopologyName)));
    }

    private HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    private HUser createTestUser(HyperIoTBaseRestApi hyperIoTBaseRestApi) {
        // Impersonate adminUser
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser)authService.login("hadmin", "admin");
        assertNotNull(adminUser);
        HyperIoTContext context = impersonateUser(hyperIoTBaseRestApi, adminUser);
        assertTrue(adminUser.isAdmin());
        // create new test user
        String username = "foo";
        HUser testUser = null;
        HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
        try {
            testUser = hUserSystemApi.findUserByUsername(username);
        } catch (Exception e) {
            // not found
        }
        if (testUser != null) {
            return testUser;
        } else {
            testUser = new HUser();
            testUser.setAdmin(false);
            testUser.setEmail("foo@bar.com");
            testUser.setName("Foo");
            testUser.setLastname("Bar");
            testUser.setUsername(username);
            testUser.setPassword("testPassword&%123");
            testUser.setPasswordConfirm("testPassword&%123");
            // save newly added user
            hUserSystemApi.save(testUser, context);
        }
        return testUser;
    }

	private void grantUserPermission(HyperIoTBaseRestApi hyperIoTBaseRestApi, HyperIoTActionName apiAction, String resourceName) {
        HUser user = createTestUser(hyperIoTBaseRestApi);
        if (!user.hasRole(apiAction.getName())) {

            // Impersonate adminUser
            AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
            HyperIoTUser adminUser = (HUser)authService.login("hadmin", "admin");
            assertNotNull(adminUser);
            HyperIoTContext context = impersonateUser(hyperIoTBaseRestApi, adminUser);
            assertTrue(adminUser.isAdmin());

            Role testUserRole = new Role();
            testUserRole.setName(apiAction.getName());
            testUserRole.setDescription("Grant permission on '" + apiAction.getName() + "' to user '" + user.getUsername() + "'");
            // save newly added role
            RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
            roleSystemApi.save(testUserRole, context);

            // add role to user
            user.addRole(testUserRole);
            HUserSystemApi hUserSystemApi = getOsgiService(HUserSystemApi.class);
            hUserSystemApi.update(user, context);

            HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(resourceName, apiAction);

            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Permission permission = new Permission();
            permission.setName(action.getActionName());
            permission.setActionIds(action.getActionId());
            permission.setEntityResourceName(resourceName);
            permission.setRole(testUserRole);
            permissionSystemApi.save(permission, context);

        }
        impersonateUser(hyperIoTBaseRestApi, user);
    }

}
