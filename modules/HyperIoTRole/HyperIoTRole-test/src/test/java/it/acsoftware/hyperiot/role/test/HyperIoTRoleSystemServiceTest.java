package it.acsoftware.hyperiot.role.test;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.huser.api.HUserApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.role.api.RoleApi;
import it.acsoftware.hyperiot.role.api.RoleSystemApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import org.apache.karaf.features.FeaturesService;
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

import java.util.Collection;
import java.util.UUID;

import static it.acsoftware.hyperiot.role.test.HyperIoTRoleConfiguration.getBaseConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRoleSystemServiceTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTRoleConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//                .withDebug("5010", false)
                .append(getBaseConfiguration()).build();
    }

    @Test
    public void test00s_hyperIoTFrameworkShouldBeInstalled() {
        // assert on an available service
        // hyperiot-core import the following features: base, mail, permission, huser, company, role, authentication,
        // assetcategory, assettag, sharedentity.
        assertServiceAvailable(FeaturesService.class);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTMail-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTCompany-features ", features);
        assertContains("HyperIoTRole-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTAssetCategory-features ", features);
        assertContains("HyperIoTAssetTag-features ", features);
        assertContains("HyperIoTSharedEntity-features ", features);
        assertContains("HyperIoTArea-features ", features);
        assertContains("HyperIoTAlgorithm-features ", features);
        assertContains("HyperIoTHProjectAlgorithm-features ", features);
        assertContains("HyperIoTDashboard-features ", features);
        assertContains("HyperIoTDashboardWidget-features ", features);
        assertContains("HyperIoTWidget-features ", features);
        assertContains("HyperIoTRuleEngine-features ", features);
        assertContains("HyperIoTHadoopManager-features ", features);
        assertContains("HyperIoTHBaseConnector-features", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01s_getUserRolesShouldWorkIfUserNotHasRole() {
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        // the following call getUserRoles tries to find all user Role, HUser not
        // has role and listRoles is empty
        HUser huser = createHUser();
        Collection<Role> listRoles = roleSystemApi.getUserRoles(huser.getId());
        Assert.assertTrue(listRoles.isEmpty());
        Assert.assertEquals(0, listRoles.size());
    }


    @Test
    public void test02s_getUserRolesShouldWork() {
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        // the following call getUserRoles find all user Role
        RoleApi roleApi = getOsgiService(RoleApi.class);
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HyperIoTContext ctx = roleRestService.impersonate(adminUser);
        HUser huser = createHUser();
        Role role = createRole();
        huser.addRole(role);
        roleApi.saveUserRole(huser.getId(), role.getId(), ctx);
        Collection<Role> roles = roleSystemApi.getUserRoles(huser.getId());
        Assert.assertFalse(roles.isEmpty());
        boolean roleFound = false;
        for (Role listRoles : roles) {
            if (role.getId() == listRoles.getId())
                roleFound = true;
        }
        Assert.assertTrue(roleFound);
        Assert.assertEquals(1, roles.size());
    }


    @Test
    public void test03s_findByNameShouldWork() {
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        // the following call findByName find specific Role by name
        Role role = createRole();
        Role findRole = roleSystemApi.findByName(role.getName());
        Assert.assertEquals(role.getName(), findRole.getName());
        Assert.assertNotNull(findRole);
    }

    @Test
    public void test04s_findByNameShouldFailIfEntityNotFound() {
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        // the following call tries to Role by name with the following call findByName,
        // but entity not found
        Role findRole = roleSystemApi.findByName(null);
        Assert.assertNull(findRole);
    }

    @Test
    public void test05s_findByNameShouldFailIfRoleIsNotStoredInDB() {
        RoleSystemApi roleSystemApi = getOsgiService(RoleSystemApi.class);
        // the following call findByName tries to find Role by name;
        // this call fails because Role is not stored in database
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        Role findRole = roleSystemApi.findByName(role.getName());
        Assert.assertNull(findRole);
    }



    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    private HUser createHUser() {
        HUserApi hUserApi = getOsgiService(HUserApi.class);
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HyperIoTContext ctx = hUserRestApi.impersonate(adminUser);
        String username = "TestUser";
        HUser huser = new HUser();
        huser.setName("name");
        huser.setLastname("lastname");
        huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
        huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        huser.setActive(true);
        hUserApi.save(huser, ctx);
        return huser;
    }

    private Role createRole() {
        RoleApi roleApi = getOsgiService(RoleApi.class);
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HyperIoTContext ctx = roleRestService.impersonate(adminUser);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        roleApi.save(role, ctx);
        Assert.assertNotNull(role);
        return role;
    }

}
