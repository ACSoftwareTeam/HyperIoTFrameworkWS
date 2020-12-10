package it.acsoftware.hyperiot.huser.test;

import it.acsoftware.hyperiot.algorithm.model.*;
import it.acsoftware.hyperiot.algorithm.service.rest.AlgorithmRestApi;
import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.model.AreaDevice;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.asset.category.service.rest.AssetCategoryRestApi;
import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.asset.tag.service.rest.AssetTagRestApi;
import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.authentication.service.rest.AuthenticationRestApi;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.action.util.HyperIoTShareAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTAssetOwnerImpl;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.company.model.Company;
import it.acsoftware.hyperiot.company.service.rest.CompanyRestApi;
import it.acsoftware.hyperiot.dashboard.model.Dashboard;
import it.acsoftware.hyperiot.dashboard.model.DashboardType;
import it.acsoftware.hyperiot.dashboard.service.rest.DashboardRestApi;
import it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget;
import it.acsoftware.hyperiot.dashboard.widget.service.rest.DashboardWidgetRestApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.api.RoleRepository;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import it.acsoftware.hyperiot.rule.model.Rule;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.rule.model.actions.AddCategoryRuleAction;
import it.acsoftware.hyperiot.rule.model.actions.RuleAction;
import it.acsoftware.hyperiot.rule.model.actions.ValidateHPacketRuleAction;
import it.acsoftware.hyperiot.rule.service.rest.RuleEngineRestApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
import it.acsoftware.hyperiot.widget.model.Widget;
import it.acsoftware.hyperiot.widget.model.WidgetCategory;
import it.acsoftware.hyperiot.widget.service.rest.WidgetRestApi;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.karaf.itests.KarafTestSupport;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import org.apache.karaf.features.FeaturesService;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

import static it.acsoftware.hyperiot.huser.test.HyperIoTHUserConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRegisteredUser extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTHUserConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration()
//				.withDebug("5010", false)
                .append(getBaseConfiguration()).build();
    }


    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }


    @Test
    public void test000_hyperIoTFrameworkShouldBeInstalled() {
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
    public void test001_checksIfFileHadoopManagerCfgExists() {
        // checks if it.acsoftware.hyperiot.hadoopmanager.cfg exists.
        // If file not found HyperIoTHadoopManager-service bundle is in Waiting state
        String hyperIoTHadoopManagerService = executeCommand("bundle:list | grep HyperIoTHadoopManager-service");
        boolean fileCfgHadoopManagerFound = false;
        String fileConfigHadoopManager = executeCommand("config:list | grep it.acsoftware.hyperiot.hadoopmanager.cfg");
        if (hyperIoTHadoopManagerService.contains("│ Active  │")) {
            Assert.assertTrue(hyperIoTHadoopManagerService.contains("│ Active  │"));
            if (fileConfigHadoopManager.contains("it.acsoftware.hyperiot.hadoopmanager.cfg")) {
                Assert.assertTrue(fileConfigHadoopManager.contains("it.acsoftware.hyperiot.hadoopmanager.cfg"));
                fileCfgHadoopManagerFound = true;
            }
        }
        if (hyperIoTHadoopManagerService.contains("│ Waiting │")) {
            Assert.assertTrue(hyperIoTHadoopManagerService.contains("│ Waiting │"));
            if (fileConfigHadoopManager.isEmpty()) {
                Assert.assertTrue(fileConfigHadoopManager.isEmpty());
                Assert.assertFalse(fileCfgHadoopManagerFound);
                System.out.println("file ect/it.acsoftware.hyperiot.hadoopmanager.cfg not found...");
            }
        }
        Assert.assertTrue(fileCfgHadoopManagerFound);
    }


    @Test
    public void test002_checksIfFileAlgorithmCfgExists() {
        // checks if it.acsoftware.hyperiot.algorithm.cfg exists.
        boolean fileCfgHadoopManagerFound = false;
        String fileConfigHadoopManager = executeCommand("config:list | grep it.acsoftware.hyperiot.algorithm.cfg");
        if (!fileConfigHadoopManager.isEmpty()) {
            if (fileConfigHadoopManager.contains("it.acsoftware.hyperiot.algorithm.cfg")){
                Assert.assertTrue(fileConfigHadoopManager.contains("it.acsoftware.hyperiot.algorithm.cfg"));
                fileCfgHadoopManagerFound = true;
            }
        }
        Assert.assertTrue(fileCfgHadoopManagerFound);
    }


    @Test
    public void test003_createDefaultRoleAndPermissionForRegisteredUser() {
        RoleRepository roleRepository = getOsgiService(RoleRepository.class);
        // This test checks if role and permissions has been created in the database
        Role role = roleRepository.findByName("RegisteredUser");
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
        Assert.assertFalse(listPermissions.isEmpty());
        Assert.assertEquals(12, listPermissions.size());
        boolean resourceNameArea = false;
        boolean resourceNameAlgorithm = false;
        boolean resourceNameAssetCategory = false;
        boolean resourceNameAssetTag = false;
        boolean resourceNameHProject = false;
        boolean resourceNameHDevice = false;
        boolean resourceNameHPacket = false;
        boolean resourceNameHProjectAlgorithm = false;
        boolean resourceNameRule = false;
        boolean resourceNameDashboard = false;
        boolean resourceNameDashboardWidget = false;
        boolean resourceNameWidget = false;
        for (int i = 0; i < listPermissions.size(); i++) {
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAlgorithm)) {
                resourceNameAlgorithm = true;
                Assert.assertEquals("it.acsoftware.hyperiot.algorithm.model.Algorithm", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionAlgorithm + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(16, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionArea)) {
                resourceNameArea = true;
                Assert.assertEquals("it.acsoftware.hyperiot.area.model.Area", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionArea + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetCategory)) {
                resourceNameAssetCategory = true;
                Assert.assertEquals("it.acsoftware.hyperiot.asset.category.model.AssetCategory", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionAssetCategory + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetTag)) {
                resourceNameAssetTag = true;
                Assert.assertEquals("it.acsoftware.hyperiot.asset.tag.model.AssetTag", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionAssetTag + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHProject)) {
                resourceNameHProject = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hproject.model.HProject", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHProject + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(262143, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHDevice)) {
                resourceNameHDevice = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hdevice.model.HDevice", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHDevice + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHPacket)) {
                resourceNameHPacket = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hpacket.model.HPacket", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHPacket + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHProjectAlgorithm)) {
                resourceNameHProjectAlgorithm = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHProjectAlgorithm + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionRule)) {
                resourceNameRule = true;
                Assert.assertEquals("it.acsoftware.hyperiot.rule.model.Rule", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionRule + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionDashboard)) {
                resourceNameDashboard = true;
                Assert.assertEquals("it.acsoftware.hyperiot.dashboard.model.Dashboard", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionDashboard + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionDashboardWidget)) {
                resourceNameDashboardWidget = true;
                Assert.assertEquals("it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionDashboardWidget + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionWidget)) {
                resourceNameWidget = true;
                Assert.assertEquals("it.acsoftware.hyperiot.widget.model.Widget", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionWidget + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(24, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
        }
        Assert.assertTrue(resourceNameAlgorithm);
        Assert.assertTrue(resourceNameArea);
        Assert.assertTrue(resourceNameAssetCategory);
        Assert.assertTrue(resourceNameAssetTag);
        Assert.assertTrue(resourceNameHProject);
        Assert.assertTrue(resourceNameHDevice);
        Assert.assertTrue(resourceNameHPacket);
        Assert.assertTrue(resourceNameHProjectAlgorithm);
        Assert.assertTrue(resourceNameRule);
        Assert.assertTrue(resourceNameDashboard);
        Assert.assertTrue(resourceNameDashboardWidget);
        Assert.assertTrue(resourceNameWidget);
    }


    @Test
    public void test004_huserRegistrationAccountIsNotActivated() {
        List<Object> roles = new ArrayList<>();
        HUser huser = huserRegistration(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());

        //checks: huser is not active
        AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
        Response restResponse = authRestService.login(huser.getUsername(), huser.getPassword());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUserNotActivated",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertEquals("it.acsoftware.hyperiot.authentication.error.user.not.active",
                ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
        Assert.assertTrue(roles.isEmpty());
    }


    @Test
    public void test005_huserRegistrationAndActivationAccountStep() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        List<Object> roles = new ArrayList<>();
        HUser huser = huserRegistration(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());

        //checks: huser is not active
        AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
        Response restResponse = authRestService.login(huser.getUsername(), huser.getPassword());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUserNotActivated",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertEquals("it.acsoftware.hyperiot.authentication.error.user.not.active",
                ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
        Assert.assertTrue(roles.isEmpty());

        // Activate huser
        String activationCode = huser.getActivateCode();
        Assert.assertNotNull(activationCode);
        Response restResponseActivateUser = huserRestService.activate(huser.getEmail(), activationCode);
        Assert.assertEquals(200, restResponseActivateUser.getStatus());
        Role role = null;
        huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
        roles = Arrays.asList(huser.getRoles().toArray());
        Assert.assertFalse(roles.isEmpty());
        Assert.assertTrue(huser.isActive());

        // checks: default role has been assigned to new huser
        Assert.assertEquals(1, huser.getRoles().size());
        Assert.assertEquals(roles.size(), huser.getRoles().size());
        Assert.assertFalse(roles.isEmpty());
        for (int i = 0; i < roles.size(); i++){
            role = ((Role) roles.get(i));
        }
        Assert.assertNotNull(role);
        Assert.assertEquals("RegisteredUser", role.getName());
        Assert.assertEquals("Role associated with the registered user",
                role.getDescription());
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
        Assert.assertFalse(listPermissions.isEmpty());
        Assert.assertEquals(12, listPermissions.size());
        boolean resourceNameArea = false;
        boolean resourceNameAlgorithm = false;
        boolean resourceNameAssetCategory = false;
        boolean resourceNameAssetTag = false;
        boolean resourceNameHProject = false;
        boolean resourceNameHDevice = false;
        boolean resourceNameHPacket = false;
        boolean resourceNameHProjectAlgorithm = false;
        boolean resourceNameRule = false;
        boolean resourceNameDashboard = false;
        boolean resourceNameDashboardWidget = false;
        boolean resourceNameWidget = false;
        for (int i = 0; i < listPermissions.size(); i++) {
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAlgorithm)) {
                resourceNameAlgorithm = true;
                Assert.assertEquals("it.acsoftware.hyperiot.algorithm.model.Algorithm", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionAlgorithm + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(16, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionArea)) {
                resourceNameArea = true;
                Assert.assertEquals("it.acsoftware.hyperiot.area.model.Area", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionArea + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetCategory)) {
                resourceNameAssetCategory = true;
                Assert.assertEquals("it.acsoftware.hyperiot.asset.category.model.AssetCategory", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionAssetCategory + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetTag)) {
                resourceNameAssetTag = true;
                Assert.assertEquals("it.acsoftware.hyperiot.asset.tag.model.AssetTag", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionAssetTag + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHProject)) {
                resourceNameHProject = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hproject.model.HProject", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHProject + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(262143, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHDevice)) {
                resourceNameHDevice = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hdevice.model.HDevice", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHDevice + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHPacket)) {
                resourceNameHPacket = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hpacket.model.HPacket", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHPacket + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHProjectAlgorithm)) {
                resourceNameHProjectAlgorithm = true;
                Assert.assertEquals("it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionHProjectAlgorithm + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionRule)) {
                resourceNameRule = true;
                Assert.assertEquals("it.acsoftware.hyperiot.rule.model.Rule", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionRule + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionDashboard)) {
                resourceNameDashboard = true;
                Assert.assertEquals("it.acsoftware.hyperiot.dashboard.model.Dashboard", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionDashboard + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionDashboardWidget)) {
                resourceNameDashboardWidget = true;
                Assert.assertEquals("it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionDashboardWidget + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
            if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionWidget)) {
                resourceNameWidget = true;
                Assert.assertEquals("it.acsoftware.hyperiot.widget.model.Widget", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                Assert.assertEquals(permissionWidget + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                Assert.assertEquals(24, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
            }
        }
        Assert.assertTrue(resourceNameAlgorithm);
        Assert.assertTrue(resourceNameArea);
        Assert.assertTrue(resourceNameAssetCategory);
        Assert.assertTrue(resourceNameAssetTag);
        Assert.assertTrue(resourceNameHProject);
        Assert.assertTrue(resourceNameHDevice);
        Assert.assertTrue(resourceNameHPacket);
        Assert.assertTrue(resourceNameHProjectAlgorithm);
        Assert.assertTrue(resourceNameRule);
        Assert.assertTrue(resourceNameDashboard);
        Assert.assertTrue(resourceNameDashboardWidget);
        Assert.assertTrue(resourceNameWidget);
    }


    @Test
    public void test006_huserNotActiveCreateHProjectShouldFail() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, without default permission, tries to save HProject with the following call saveHProject
        // response status code '403'
        HUser huser = huserRegistration(false);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertFalse(huser.isActive());

        this.impersonateUser(hprojectRestService, huser);
        HProject hproject = new HProject();
        hproject.setName("Project of " + huser.getUsername());
        hproject.setDescription("Description");
        hproject.setUser(huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test007_huserActiveChangePasswordShouldWork() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser changes password with the following call changePassword
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        String oldPassword = "passwordPass&01";
        String newPassword = "testPass01/";
        String passwordConfirm = "testPass01/";
        this.impersonateUser(huserRestService, huser);
        Assert.assertTrue(HyperIoTUtil.getPasswordHash(oldPassword).equals(huser.getPassword()));
        Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
                passwordConfirm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(HyperIoTUtil.getPasswordHash(newPassword), ((HUser) restResponse.getEntity()).getPassword());
        Assert.assertEquals(HyperIoTUtil.getPasswordHash(passwordConfirm), ((HUser) restResponse.getEntity()).getPasswordConfirm());
    }


    @Test
    public void test008_huserActiveChangeHisAccountInfoShouldWork() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // HUser changes his account info with the following call changeAccountInfo
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        huser.setName("Paolo");
        huser.setLastname("Maldini");

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.updateAccountInfo(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Paolo", ((HUser) restResponse.getEntity()).getName());
        Assert.assertEquals("Maldini", ((HUser) restResponse.getEntity()).getLastname());
        Assert.assertEquals(huser.getEntityVersion(),
                ((HUser) restResponse.getEntity()).getEntityVersion());
    }


    @Test
    public void test009_huserActiveTriesToChangeHUser2AccountInfoShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // HUser tries to change huser2 account info with the following call changeAccountInfo
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HUser huser2 = huserRegistration(true);
        Assert.assertNotEquals(0, huser2.getId());
        Assert.assertTrue(huser2.isActive());
        huser2.setName("Frank");
        huser2.setLastname("Lampard");

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.updateAccountInfo(huser2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action save: 1 not assigned in default permission
    @Test
    public void test010_createNewHUserWithDefaultPermissionShouldFail() {
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to save new HUser with the following call saveHUser.
        // huser to save a new huser needs the "save huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HUser huser2 = new HUser();
        huser2.setName("name");
        huser2.setLastname("lastname");
        huser2.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
        huser2.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
        huser2.setPassword("passwordPass&01");
        huser2.setPasswordConfirm("passwordPass&01");

        this.impersonateUser(hUserRestApi, huser);
        Response restResponse = hUserRestApi.saveHUser(huser2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action update: 2 not assigned in default permission
    @Test
    public void test011_updateHUserWithDefaultPermissionShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to update HUser with the following call updateHUser.
        // huser to update a huser needs the "update huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HUser huser2 = huserRegistration(true);
        Assert.assertNotEquals(0, huser2.getId());
        Assert.assertTrue(huser2.isActive());

        huser2.setName("edited failed");

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.updateHUser(huser2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action update: 2 not assigned in default permission
    @Test
    public void test012_huserActiveTriesToUpdateHisDataWithDefaultPermissionShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to update his data with the following call updateHUser.
        // huser to update a huser needs the "update huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        huser.setName("new name");

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.updateHUser(huser);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action remove: 4 not assigned in default permission
    @Test
    public void test013_removeHUserWithDefaultPermissionShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to remove huser2 with the following call removeHUser.
        // huser to delete a huser needs the "remove huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HUser huser2 = huserRegistration(true);
        Assert.assertNotEquals(0, huser2.getId());
        Assert.assertTrue(huser2.isActive());

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.deleteHUser(huser2.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action find: 8 not assigned in default permission
    @Test
    public void test014_findHUserWithDefaultPermissionShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to find huser2 with the following call findHUser.
        // huser to find a huser needs the "find huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HUser huser2 = huserRegistration(true);
        Assert.assertNotEquals(0, huser2.getId());
        Assert.assertTrue(huser2.isActive());

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.findHUser(huser2.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action find-all: 16 not assigned in default permission
    @Test
    public void test015_findAllHUserWithDefaultPermissionShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to find all husers with the following call findAllHUser.
        // huser to find all husers needs the "find-all huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.findAllHUser();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HUser action find-all: 16 not assigned in default permission
    @Test
    public void test016_findAllHUserPaginatedWithDefaultPermissionShouldFail() {
        HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
        // huser, with default permission, tries to find all husers paginated with the following call findAllHUserPaginated.
        // huser to find all husers paginated needs the "find-all huser" permission
        // response status code '403' HyperIoTUnauthorizedException
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<HUser> husers = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HUser huser2 = huserRegistration(true);
            Assert.assertNotEquals(0, huser2.getId());
            Assert.assertTrue(huser2.isActive());
            husers.add(huser);
        }
        this.impersonateUser(huserRestService, huser);
        Response restResponse = huserRestService.findAllHUserPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     * HProject
     *
     */

    // HProject action save: 1
    @Test
    public void test017_createHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, save HProject with the following call saveHProject
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hprojectRestService, huser);
        HProject hproject = new HProject();
        hproject.setName("Project "+ UUID.randomUUID() +" of user: " + huser.getUsername());
        Date date = new Date();
        hproject.setDescription("Description inserted in date: " + date);
        hproject.setUser(huser);
        Response restResponseSaveProject = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponseSaveProject.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponseSaveProject.getEntity()).getId());
        Assert.assertEquals(hproject.getName(),
                ((HProject) restResponseSaveProject.getEntity()).getName());
        Assert.assertEquals("Description inserted in date: " + date,
                ((HProject) restResponseSaveProject.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponseSaveProject.getEntity()).getUser().getId());
    }

    // HProject  action save: 1
    // Dashboard action find-all: 16
    @Test
    public void test018_saveHProjectAndCreateDefaultDashboardShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, save HProject with the following call saveHProject
        // and dashboards (Offline, Realtime) will be created automatically
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hprojectRestService, huser);
        HProject hproject = new HProject();
        hproject.setName("Project "+ UUID.randomUUID() +" of user: " + huser.getUsername());
        Date date = new Date();
        hproject.setDescription("Description inserted in date: " + date);
        hproject.setUser(huser);
        Response restResponseSaveProject = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponseSaveProject.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponseSaveProject.getEntity()).getId());
        Assert.assertEquals(hproject.getName(),
                ((HProject) restResponseSaveProject.getEntity()).getName());
        Assert.assertEquals("Description inserted in date: " + date,
                ((HProject) restResponseSaveProject.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponseSaveProject.getEntity()).getUser().getId());

        //checks if dashboards has been created
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);

        //checks if dashboard Offline has been created
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseDashboardOffline = dashboardRestApi.findHProjectOfflineDashboard(hproject.getId());
        Assert.assertEquals(200, restResponseDashboardOffline.getStatus());
        List<Dashboard> dashboardsOffline = restResponseDashboardOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOffline.get(0).getId());
        Assert.assertEquals(hproject.getName()+ " Offline Dashboard",
                dashboardsOffline.get(0).getName());
        Assert.assertEquals("OFFLINE",
                dashboardsOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                dashboardsOffline.get(0).getHProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                dashboardsOffline.get(0).getHProject().getUser().getId());

        //checks if dashboard Realtime has been created
        Response restResponseDashboardOnline = dashboardRestApi.findHProjectRealtimeDashboard(hproject.getId());
        Assert.assertEquals(200, restResponseDashboardOnline.getStatus());
        List<Dashboard> dashboardsOnline = restResponseDashboardOnline.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertNotEquals(0, dashboardsOnline.get(0).getId());
        Assert.assertEquals(hproject.getName()+ " Online Dashboard",
                dashboardsOnline.get(0).getName());
        Assert.assertEquals("REALTIME",
                dashboardsOnline.get(0).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                dashboardsOnline.get(0).getHProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                dashboardsOnline.get(0).getHProject().getUser().getId());
    }


    // HProject action update: 2
    @Test
    public void test019_updateHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, update HProject with the following call updateHProject
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Date date = new Date();
        hproject.setDescription("Description edited in date: " + date);
        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.updateHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hproject.getEntityVersion() + 1,
                (((HProject) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("Description edited in date: " + date,
                (((HProject) restResponse.getEntity()).getDescription()));
        Assert.assertEquals(hproject.getId(),
                ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponse.getEntity()).getUser().getId());
    }


    // HProject action remove: 4
    @Test
    public void test020_deleteHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, delete HProject with the following call deleteHProject
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HProject action find: 8
    @Test
    public void test021_findHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find HProject with the following call findHProject
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findHProject(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(), ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName(), ((HProject) restResponse.getEntity()).getName());
        Assert.assertEquals(hproject.getDescription(), ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(), ((HProject) restResponse.getEntity()).getUser().getId());
    }


    // HProject action find-all: 16
    @Test
    public void test022_findAllHProjectShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find all HProject with the following call findAllHProject
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject1 = createHProject(huser);
        Assert.assertNotEquals(0, hproject1.getId());
        Assert.assertEquals(huser.getId(), hproject1.getUser().getId());

        HProject hproject2 = createHProject(huser);
        Assert.assertNotEquals(0, hproject2.getId());
        Assert.assertEquals(huser.getId(), hproject2.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.findAllHProject();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        boolean hprojectFound1 = false;
        boolean hprojectFound2 = false;
        for (HProject hprojects : listHProjects) {
            if (hproject1.getId() == hprojects.getId()) {
                Assert.assertEquals(hproject1.getId(),
                        ((HProject) ((ArrayList) listHProjects).get(0)).getId());
                Assert.assertEquals(hproject1.getName(),
                        ((HProject) ((ArrayList) listHProjects).get(0)).getName());
                Assert.assertEquals(hproject1.getDescription(),
                        ((HProject) ((ArrayList) listHProjects).get(0)).getDescription());
                Assert.assertEquals(huser.getId(),
                        ((HProject) ((ArrayList) listHProjects).get(0)).getUser().getId());
                hprojectFound1 = true;
            }
            if (hproject2.getId() == hprojects.getId()) {
                Assert.assertEquals(hproject2.getId(),
                        ((HProject) ((ArrayList) listHProjects).get(1)).getId());
                Assert.assertEquals(hproject2.getName(),
                        ((HProject) ((ArrayList) listHProjects).get(1)).getName());
                Assert.assertEquals(hproject2.getDescription(),
                        ((HProject) ((ArrayList) listHProjects).get(1)).getDescription());
                Assert.assertEquals(huser.getId(),
                        ((HProject) ((ArrayList) listHProjects).get(1)).getUser().getId());
                hprojectFound2 = true;
            }
        }
        Assert.assertTrue(hprojectFound1);
        Assert.assertTrue(hprojectFound2);
    }


    // HProject action find-all: 16
    @Test
    public void test023_findAllHProjectPaginatedShouldWork() {
        HProjectRestApi hProjectRestApi = getOsgiService(HProjectRestApi.class);
        // In this following call findAllHProjectPaginated, huser finds all
        // HProjects with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<HProject> hprojects = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HProject hproject = createHProject(huser);
            Assert.assertNotEquals(0, hproject.getId());
            Assert.assertEquals(huser.getId(), hproject.getUser().getId());
            hprojects.add(hproject);
        }
        this.impersonateUser(hProjectRestApi, huser);
        Response restResponse = hProjectRestApi.findAllHProjectPaginated(delta, page);
        HyperIoTPaginableResult<HProject> listHProjects = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HProject>>() {
                });
        Assert.assertEquals(5, listHProjects.getResults().size());
        Assert.assertFalse(listHProjects.getResults().isEmpty());
        Assert.assertEquals(2, listHProjects.getCurrentPage());
        Assert.assertEquals(5, listHProjects.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // HProject action find-all: 16
    @Test
    public void test024_cardsViewShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find all HProject with the following call cardsView
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.cardsView();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
        });
        Assert.assertFalse(listHProjects.isEmpty());
        boolean hprojectFound = false;
        for (HProject hprojects : listHProjects) {
            if (hproject.getId() == hprojects.getId()) {
                Assert.assertEquals(hproject.getId(), hprojects.getId());
                Assert.assertEquals(hproject.getName(), hprojects.getName());
                Assert.assertEquals(hproject.getDescription(), hprojects.getDescription());
                Assert.assertEquals(huser.getId(), hprojects.getUser().getId());
                hprojectFound = true;
            }
        }
        Assert.assertTrue(hprojectFound);
    }


    // HProject action find-all: 16 (RuleEngine)
    @Test
    public void test025_findAllRuleByProjectIdShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with permission, find all Rule by projectId with the following
        // call findAllRuleByProjectId
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRuleEngine(huser, hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
        Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
        Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByProjectId(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<Rule> listRulesByProjectId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRulesByProjectId.isEmpty());
        boolean ruleFound = false;
        for (Rule rules : listRulesByProjectId) {
            if (rule.getId() == rules.getId()) {
                Assert.assertEquals(rule.getId(),
                        ((Rule) ((ArrayList) listRulesByProjectId).get(0)).getId());
                Assert.assertEquals(rule.getProject().getId(),
                        ((Rule) ((ArrayList) listRulesByProjectId).get(0)).getProject().getId());
                Assert.assertEquals(rule.getProject().getUser().getId(),
                        ((Rule) ((ArrayList) listRulesByProjectId).get(0)).getProject().getUser().getId());
                Assert.assertEquals(rule.getPacket().getId(),
                        ((Rule) ((ArrayList) listRulesByProjectId).get(0)).getPacket().getId());
                Assert.assertEquals(rule.getPacket().getDevice().getProject().getUser().getId(),
                        ((Rule) ((ArrayList) listRulesByProjectId).get(0)).getPacket().getDevice().getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
    }


    // HProject action areas_management: 64
    @Test
    public void test026_getHProjectAreaListShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // huser, with default permission, find all HProject Area list with the
        // following call getHProjectAreaList
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectAreaList(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Collection<Area> listHProjectAreas = restResponse.readEntity(new GenericType<Collection<Area>>() {
        });
        Assert.assertFalse(listHProjectAreas.isEmpty());
        boolean hprojectAreaFound = false;
        for (Area areas : listHProjectAreas) {
            if (area.getId() == areas.getId()) {
                Assert.assertEquals(area.getId(), areas.getId());
                Assert.assertEquals(area.getName(), areas.getName());
                Assert.assertEquals(area.getDescription(), areas.getDescription());
                Assert.assertEquals(area.getProject().getId(), areas.getProject().getId());
                Assert.assertEquals(area.getProject().getUser().getId(), areas.getProject().getUser().getId());
                hprojectAreaFound = true;
            }
        }
        Assert.assertTrue(hprojectAreaFound);
    }


    // HProject action device_list: 128 (HDevice)
    @Test
    public void test027_findAllHDeviceByProjectIdShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, finds list of all available HDevice for the given project id
        // with the following call findAllHDeviceByProjectId
        // response status code 200
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice1 = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice1.getId());
        Assert.assertEquals(hproject.getId(), hdevice1.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice1.getProject().getUser().getId());

        HDevice hdevice2 = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice2.getId());
        Assert.assertEquals(hproject.getId(), hdevice2.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice2.getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDeviceByProjectId(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        boolean hdeviceFound1 = false;
        boolean hdeviceFound2 = false;
        for (HDevice hdevices : listHDevice) {
            if (hdevice1.getId() == hdevices.getId()) {
                Assert.assertEquals(hdevice1.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getId());
                Assert.assertEquals(hdevice1.getDeviceName(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getDeviceName());
                Assert.assertEquals(hdevice1.getBrand(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getBrand());
                Assert.assertEquals(hdevice1.getDescription(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getDescription());
                Assert.assertEquals(hdevice1.getFirmwareVersion(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getFirmwareVersion());
                Assert.assertEquals(hdevice1.getModel(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getModel());
                Assert.assertEquals(hdevice1.getSoftwareVersion(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getSoftwareVersion());
                Assert.assertEquals(hproject.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getProject().getId());
                Assert.assertEquals(hproject.getUser().getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getProject().getUser().getId());
                Assert.assertEquals(huser.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getProject().getUser().getId());
                hdeviceFound1 = true;
            }
            if (hdevice2.getId() == hdevices.getId()) {
                Assert.assertEquals(hdevice2.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getId());
                Assert.assertEquals(hdevice2.getDeviceName(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getDeviceName());
                Assert.assertEquals(hdevice2.getBrand(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getBrand());
                Assert.assertEquals(hdevice2.getDescription(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getDescription());
                Assert.assertEquals(hdevice2.getFirmwareVersion(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getFirmwareVersion());
                Assert.assertEquals(hdevice2.getModel(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getModel());
                Assert.assertEquals(hdevice2.getSoftwareVersion(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getSoftwareVersion());
                Assert.assertEquals(hproject.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getProject().getId());
                Assert.assertEquals(hproject.getUser().getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getProject().getUser().getId());
                Assert.assertEquals(huser.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(1)).getProject().getUser().getId());
                hdeviceFound2 = true;
            }
        }
        Assert.assertTrue(hdeviceFound1);
        Assert.assertTrue(hdeviceFound2);
    }


    // HProject action device_list: 128
    @Test
    public void test028_getProjectTreeViewJsonShouldWork() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        // HUser, with default permission, finds all HPacket by hprojectId with the following
        // call getProjectTreeViewJson
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        this.impersonateUser(hprojectRestService, huser);
        Response restResponse = hprojectRestService.getHProjectTreeView(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {});
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(2, listHPackets.size());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket hpackets : listHPackets) {
            if (hpacket1.getId() == hpackets.getId()) {
                Assert.assertEquals(hpacket1.getDevice().getProject().getUser().getId(), hpackets.getDevice().getProject().getUser().getId());
                Assert.assertEquals(2, hpacket1.getFields().size());
                Assert.assertEquals(hpacket1.getFields().size(), hpackets.getFields().size());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == hpackets.getId()) {
                Assert.assertEquals(hpacket2.getDevice().getProject().getUser().getId(),
                        hpackets.getDevice().getProject().getUser().getId());
                Assert.assertEquals(hpackets.getFields(),
                        hpackets.getFields());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
    }


    // HProject action share: 262144 not assigned in default permission (SharedEntity)
    @Test
    public void test029_saveSharedEntityWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to save SharedEntity with the following call saveSharedEntity
        // ownerUser to save SharedEntity needs the "share hproject" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        // ownerUser tries to share his hproject with huser
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = new SharedEntity();
        sharedEntity.setEntityId(hproject.getId());
        sharedEntity.setEntityResourceName(hProjectResourceName); // "it.acsoftware.hyperiot.hproject.model.HProject"
        sharedEntity.setUserId(huser.getId());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // HProject action share: 262144 not assigned in default permission (SharedEntity)
    @Test
    public void test030_deleteSharedEntityWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to delete SharedEntity with the following call deleteSharedEntity
        // ownerUser to save SharedEntity needs the "share hproject" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     * HDevice
     *
     */


    // HDevice action save: 1
    @Test
    public void test031_createNewHDevice() {
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        // huser, with default permission, save HDevice with the following call saveHDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);

        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("Brand",
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals("Description",
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals("model",
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
    }


    // HDevice action update: 2
    @Test
    public void test032_updateHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, update HDevice with the following call updateHDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Date date = new Date();
        hdevice.setDescription("Description edited in date: " + date);
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.updateHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Description edited in date: " + date,
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hdevice.getEntityVersion() + 1,
                ((HDevice) restResponse.getEntity()).getEntityVersion());
    }


    // HDevice action remove: 4
    @Test
    public void test033_deleteHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, delete HDevice with the following call deleteHDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.deleteHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HDevice action find: 8
    @Test
    public void test034_findHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, find HDevice with the following call findHDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findHDevice(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hdevice.getId(),
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getDeviceName(),
                ((HDevice) restResponse.getEntity()).getDeviceName());
        Assert.assertEquals(hdevice.getBrand(),
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals(hdevice.getDescription(),
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hdevice.getFirmwareVersion(),
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals(hdevice.getModel(),
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals(hdevice.getSoftwareVersion(),
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
    }


    // HDevice action find-all: 16
    @Test
    public void test035_findAllHDeviceShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // HUser, with default permission, find all HDevice with the following call
        // findAllHDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevice();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HDevice> listHDevice = restResponse.readEntity(new GenericType<List<HDevice>>() {
        });
        Assert.assertFalse(listHDevice.isEmpty());
        boolean hdeviceFound = false;
        for (HDevice hdevices : listHDevice) {
            if (hdevice.getId() == hdevices.getId()) {
                Assert.assertEquals(hdevice.getId(),
                    ((HDevice) ((ArrayList) listHDevice).get(0)).getId());
                Assert.assertEquals(hdevice.getDeviceName(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getDeviceName());
                Assert.assertEquals(hdevice.getBrand(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getBrand());
                Assert.assertEquals(hdevice.getDescription(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getDescription());
                Assert.assertEquals(hdevice.getFirmwareVersion(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getFirmwareVersion());
                Assert.assertEquals(hdevice.getModel(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getModel());
                Assert.assertEquals(hdevice.getSoftwareVersion(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getSoftwareVersion());
                Assert.assertEquals(hproject.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getProject().getId());
                Assert.assertEquals(hproject.getUser().getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getProject().getUser().getId());
                Assert.assertEquals(huser.getId(),
                        ((HDevice) ((ArrayList) listHDevice).get(0)).getProject().getUser().getId());
                hdeviceFound = true;
            }
        }
        Assert.assertTrue(hdeviceFound);
    }


    // HDevice action find-all: 16
    @Test
    public void test036_findAllHDevicePaginatedShouldWork() {
        HDeviceRestApi hDeviceRestService = getOsgiService(HDeviceRestApi.class);
        // In this following call findAllHDevicePaginated, huser finds all
        // HProjects with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<HDevice> devices = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HDevice hdevice = createHDevice(huser, hproject);
            Assert.assertNotEquals(0, hdevice.getId());
            Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
            Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());
            devices.add(hdevice);
        }
        this.impersonateUser(hDeviceRestService, huser);
        Response restResponse = hDeviceRestService.findAllHDevicePaginated(delta, page);
        HyperIoTPaginableResult<HDevice> listHDevices = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HDevice>>() {
                });
        Assert.assertEquals(5, listHDevices.getResults().size());
        Assert.assertFalse(listHDevices.getResults().isEmpty());
        Assert.assertEquals(2, listHDevices.getCurrentPage());
        Assert.assertEquals(5, listHDevices.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // HDevice action packets_management: 32
    @Test
    public void test037_getHDevicePacketListShouldWork() {
        HPacketRestApi hPacketRestService = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, find list HDevice packets with the following call
        // getHDevicePacketList
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestService, huser);
        Response restResponse = hPacketRestService.getHDevicePacketList(hdevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHDevicePackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertEquals(2, listHDevicePackets.size());
        Assert.assertFalse(listHDevicePackets.isEmpty());

        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket hpackets : listHDevicePackets) {
            if (hpacket1.getId() == hpackets.getId()) {
                Assert.assertEquals(hpacket1.getDevice().getProject().getUser().getId(),
                        ((HPacket) ((ArrayList) listHDevicePackets).get(0)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(2, hpacket1.getFields().size());
                Assert.assertEquals(hpacket1.getFields().size(),
                        ((HPacket) ((ArrayList) listHDevicePackets).get(0)).getFields().size());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == hpackets.getId()) {
                Assert.assertEquals(hpacket2.getDevice().getProject().getUser().getId(),
                        ((HPacket) ((ArrayList) listHDevicePackets).get(1)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(hpackets.getFields(),
                        ((HPacket) ((ArrayList) listHDevicePackets).get(1)).getFields());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
    }


    // HDevice action packets_management: 32 (rest of HPacket)
    @Test
    public void test038_findAllHPacketByProjectIdShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with permission, finds all HPacket by hprojectId with the following
        // call findAllHPacketByProjectId
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        HPacket hpacket3 = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket3.getId());
        Assert.assertEquals(hdevice.getId(), hpacket3.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket3.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket3.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketByProjectId(hproject.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {});
        Assert.assertFalse(listHPackets.isEmpty());
        Assert.assertEquals(3, listHPackets.size());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        boolean hpacket3Found = false;
        for (HPacket hpackets : listHPackets) {
            if (hpacket1.getId() == hpackets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(0)).getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(0)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(2, hpacket1.getFields().size());
                Assert.assertEquals(hpacket1.getFields().size(),
                        ((HPacket) ((ArrayList) listHPackets).get(0)).getFields().size());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == hpackets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(1)).getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(1)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(hpackets.getFields(),
                        ((HPacket) ((ArrayList) listHPackets).get(1)).getFields());
                hpacket2Found = true;
            }
            if (hpacket3.getId() == hpackets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(2)).getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(2)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(hpackets.getFields(),
                        ((HPacket) ((ArrayList) listHPackets).get(2)).getFields());
                hpacket3Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
        Assert.assertTrue(hpacket3Found);
    }


    /*
     *
     * HPacket
     *
     */

    // HPacket action save: 1
    @Test
    public void test039_createNewHPacket() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, save HPacket with the following call saveHPacket
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    // HPacket action update: 2
    @Test
    public void test040_updateHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, update HPacket with the following call updateHPacket
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Date date = new Date();
        hpacket.setVersion("version edited in date: " + date);
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hpacket.getEntityVersion() + 1,
                (((HPacket) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals(hpacket.getVersion(),
                (((HPacket) restResponse.getEntity()).getVersion()));
        Assert.assertEquals(hproject.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    // HPacket action remove: 4
    @Test
    public void test041_deleteHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, delete HPacket with the following call
        // deleteHPacket
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HPacket action find: 8
    @Test
    public void test042_findHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, find HPacket with the following call
        // findHPacket
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findHPacket(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hpacket.getId(), ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    // HPacket action find-all: 16
    @Test
    public void test043_findAllHPacketShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, find all HPacket with the following call
        // findAllHPacket
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket1 = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket1.getId());
        Assert.assertEquals(hdevice.getId(), hpacket1.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket1.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

        HPacket hpacket2 = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket2.getId());
        Assert.assertEquals(hdevice.getId(), hpacket2.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket2.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket2.getDevice().getProject().getUser().getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacket();
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacket> listHPackets = restResponse.readEntity(new GenericType<List<HPacket>>() {
        });
        Assert.assertNotEquals(0, listHPackets.size());
        Assert.assertFalse(listHPackets.isEmpty());
        boolean hpacket1Found = false;
        boolean hpacket2Found = false;
        for (HPacket hpackets : listHPackets) {
            if (hpacket1.getId() == hpackets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(0)).getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(0)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(hpackets.getFields(),
                        ((HPacket) ((ArrayList) listHPackets).get(0)).getFields());
                hpacket1Found = true;
            }
            if (hpacket2.getId() == hpackets.getId()) {
                Assert.assertEquals(hproject.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(1)).getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((HPacket) ((ArrayList) listHPackets).get(1)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(hpackets.getFields(),
                        ((HPacket) ((ArrayList) listHPackets).get(1)).getFields());
                hpacket2Found = true;
            }
        }
        Assert.assertTrue(hpacket1Found);
        Assert.assertTrue(hpacket2Found);
    }


    // HPacket action find-all: 16
    @Test
    public void test044_findAllHPacketPaginatedShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // In this following call findAllHPacketPaginated, huser finds all
        // HPackets with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        List<HPacket> hpackets = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
            Assert.assertNotEquals(0, hpacket.getId());
            Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
            Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
            Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
            hpackets.add(hpacket);
        }
        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findAllHPacketPaginated(delta, page);
        HyperIoTPaginableResult<HPacket> listHPackets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<HPacket>>() {
                });
        Assert.assertEquals(5, listHPackets.getResults().size());
        Assert.assertFalse(listHPackets.getResults().isEmpty());
        Assert.assertEquals(2, listHPackets.getCurrentPage());
        Assert.assertEquals(5, listHPackets.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // HPacket action find-all: 16 (RuleEngine)
    @Test
    public void test045_findAllRuleByPacketIdShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find all Rule by packetId with the following
        // call findAllRuleByPacketId
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRuleEngine(huser, hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
        Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
        Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRuleByPacketId(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<Rule> listRulesByPacketId = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertFalse(listRulesByPacketId.isEmpty());
        boolean ruleFound = false;
        for (Rule rules : listRulesByPacketId) {
            if (rule.getId() == rules.getId()) {
                Assert.assertEquals(rule.getId(),
                        ((Rule) ((ArrayList) listRulesByPacketId).get(0)).getId());
                Assert.assertEquals(rule.getProject().getId(),
                        ((Rule) ((ArrayList) listRulesByPacketId).get(0)).getProject().getId());
                Assert.assertEquals(rule.getProject().getUser().getId(),
                        ((Rule) ((ArrayList) listRulesByPacketId).get(0)).getProject().getUser().getId());
                Assert.assertEquals(rule.getPacket().getId(),
                        ((Rule) ((ArrayList) listRulesByPacketId).get(0)).getPacket().getId());
                Assert.assertEquals(rule.getPacket().getDevice().getProject().getUser().getId(),
                        ((Rule) ((ArrayList) listRulesByPacketId).get(0)).getPacket().getDevice().getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
    }


    // HPacket action save: 1 (fieldHPacket)
    @Test
    public void test046_addHPacketFieldShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // HUser, with default permission, add new fieldHPacket with the following call
        // addHPacketField
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());
        Assert.assertNull(hpacket.getFields());

        HPacketField fieldHPacket = new HPacketField();
        fieldHPacket.setPacket(hpacket);
        fieldHPacket.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        fieldHPacket.setDescription("Temperature");
        fieldHPacket.setType(HPacketFieldType.DOUBLE);
        fieldHPacket.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
        fieldHPacket.setValue(24.0);

        hpacket.setFields(new ArrayList<HPacketField>() {
            {
                add(fieldHPacket);
            }
        });

        this.impersonateUser(hPacketRestApi, huser);
        Response responseAddField = hPacketRestApi.addHPacketField(hpacket.getId(), fieldHPacket);
        Assert.assertEquals(200, responseAddField.getStatus());
        //check if fieldHPacket has been saved inside hpacket
        Assert.assertNotEquals(0, ((HPacketField) responseAddField.getEntity()).getId());
        Assert.assertEquals(fieldHPacket.getName(), ((HPacketField) responseAddField.getEntity()).getName());
        Assert.assertEquals(fieldHPacket.getDescription(), ((HPacketField) responseAddField.getEntity()).getDescription());
        Assert.assertEquals(fieldHPacket.getType(), ((HPacketField) responseAddField.getEntity()).getType());
        Assert.assertEquals(fieldHPacket.getMultiplicity(), ((HPacketField) responseAddField.getEntity()).getMultiplicity());

        Assert.assertEquals(hpacket.getId(),
                ((HPacketField) responseAddField.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacketField) responseAddField.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(),
                ((HPacketField) responseAddField.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacketField) responseAddField.getEntity()).getPacket().getDevice().getProject().getUser().getId());
    }


    // HPacket action update: 2 (fieldHPacket)
    @Test
    public void test047_updateHPacketFieldShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with default permission, update HPacket fields with the following call updateHPacketField
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().get(0);
        Assert.assertNotEquals(0, field.getId());

        Date date = new Date();
        field.setDescription("Temperature edited in date " + date);

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.updateHPacketField(hpacket.getId(), field);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(field.getEntityVersion() + 1,
                (((HPacketField) restResponse.getEntity()).getEntityVersion()));
        Assert.assertEquals("Temperature edited in date " + date,
                (((HPacketField) restResponse.getEntity()).getDescription()));

        Assert.assertEquals(hpacket.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getDevice().getId());
        Assert.assertEquals(hproject.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(),
                ((HPacketField) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
    }


    // HPacket action remove: 4 (fieldHPacket)
    @Test
    public void test048_deleteHPacketFieldShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with default permission, delete HPacket field with the following call deleteHPacketField
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field = hpacket.getFields().get(0);
        Assert.assertNotEquals(0, field.getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.deleteHPacketField(field.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // HPacket action find-all: 16 (fieldHPacket)
    @Test
    public void test049_findTreeFieldsShouldWork() {
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        // huser, with default permission, finds tree fields with the following call findTreeFields
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, true);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        HPacketField field1 = hpacket.getFields().get(0);
        Assert.assertNotEquals(0, field1.getId());
        HPacketField field2 = hpacket.getFields().get(1);
        Assert.assertNotEquals(0, field2.getId());

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.findTreeFields(hpacket.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<HPacketField> listHPacketFields = restResponse.readEntity(new GenericType<List<HPacketField>>() {
        });
        Assert.assertFalse(listHPacketFields.isEmpty());
        Assert.assertEquals(2, listHPacketFields.size());
        boolean field1Found = false;
        boolean field2Found = false;
        for (HPacketField fields : listHPacketFields) {
            if (field1.getId() == fields.getId()) {
                Assert.assertEquals(field1.getId(),
                        fields.getId());
                Assert.assertEquals(hpacket.getId(),
                        fields.getPacket().getId());
                Assert.assertEquals(hdevice.getId(),
                        fields.getPacket().getDevice().getId());
                Assert.assertEquals(hproject.getId(),
                        fields.getPacket().getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        fields.getPacket().getDevice().getProject().getUser().getId());
                field1Found = true;
            }
            if (field2.getId() == fields.getId()) {
                Assert.assertEquals(field2.getId(),
                        fields.getId());
                Assert.assertEquals(hpacket.getId(),
                        fields.getPacket().getId());
                Assert.assertEquals(hdevice.getId(),
                        fields.getPacket().getDevice().getId());
                Assert.assertEquals(hproject.getId(),
                        fields.getPacket().getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        fields.getPacket().getDevice().getProject().getUser().getId());
                field2Found = true;
            }
        }
        Assert.assertTrue(field1Found);
        Assert.assertTrue(field2Found);
    }


    /*
     *
     * Area
     *
     */


    // Area action save: 1
    @Test
    public void test050_createNewArea() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, save Area with the following call saveArea
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponse.getEntity()).getName());
        Assert.assertEquals("Description", ((Area) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    // Area action update: 2
    @Test
    public void test051_updateAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, update Area with the following call updateArea
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Date date = new Date();
        area.setDescription("Description updated in date: " + date);

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.updateArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponse.getEntity()).getEntityVersion());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals("Description updated in date: " + date,
                ((Area) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    // Area action find: 8
    @Test
    public void test052_findAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, find Area with the following call findArea
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(area.getName(), ((Area) restResponse.getEntity()).getName());
        Assert.assertEquals(area.getDescription(), ((Area) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
    }


    // Area action update and find: 10
    @Test
    public void test053_setAreaImageShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, set an Area image with the following call setAreaImage
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);

        File file = new File( System.getProperty( "karaf.etc" )  );
        String areaImagePath = String.valueOf(file).replaceAll("/etc", "/");

        FileInputStream imageFile = null;
        try {
            imageFile = new FileInputStream(areaImagePath + areaImageName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNotNull(((Area) restResponseImage.getEntity()).getImagePath());

        String newImageName = String.valueOf(area.getId()).concat("_img.").concat(fileExtension);
        Assert.assertTrue(((Area) restResponseImage.getEntity()).getImagePath().contains(newImageName));
    }


    // Area action update and find: 10
    @Test
    public void test054_getAreaImageShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, get an Area image with the following call getAreaImage
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createAreaAndSetAreaImage(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.getAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(new File(area.getImagePath()), restResponseImage.getEntity());
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Type")
                .contains("application/octet-stream"));
        Assert.assertTrue(restResponseImage.getMetadata().get("Content-Disposition")
                .contains("attachment; filename=\""+area.getId()+"_img.jpg\""));
    }


    // Area action update and find: 10
    @Test
    public void test055_unsetAreaImageShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, unset an Area image with the following call unsetAreaImage
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createAreaAndSetAreaImage(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
        Assert.assertNotNull(area.getImagePath());

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.unsetAreaImage(area.getId());
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNull(((Area) restResponseImage.getEntity()).getImagePath());
    }


    // Area action find-all: 16
    @Test
    public void test056_findAllAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, find all Area with the following call findAllArea
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllArea();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Area> listAreas = restResponse.readEntity(new GenericType<List<Area>>() {
        });
        Assert.assertFalse(listAreas.isEmpty());
        boolean areaFound = false;
        for (Area areas : listAreas) {
            if (area.getId() == areas.getId()) {
                Assert.assertEquals(area.getName(),
                        ((Area) ((ArrayList) listAreas).get(0)).getName());
                Assert.assertEquals(area.getDescription(),
                        ((Area) ((ArrayList) listAreas).get(0)).getDescription());
                Assert.assertEquals(hproject.getId(),
                        ((Area) ((ArrayList) listAreas).get(0)).getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((Area) ((ArrayList) listAreas).get(0)).getProject().getUser().getId());
                areaFound = true;
            }
        }
        Assert.assertTrue(areaFound);
    }


    // Area action find-all: 16
    @Test
    public void test057_findAllAreaPaginatedShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // In this following call findAllAreaPaginated, huser find all areas with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Area> areas = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Area area = createArea(huser, hproject);
            Assert.assertNotEquals(0, area.getId());
            Assert.assertEquals(hproject.getId(), area.getProject().getId());
            Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());
            areas.add(area);
        }
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findAllAreaPaginated(delta, page);
        HyperIoTPaginableResult<Area> listAreas = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Area>>() {
                });
        Assert.assertEquals(5, listAreas.getResults().size());
        Assert.assertFalse(listAreas.getResults().isEmpty());
        Assert.assertEquals(2, listAreas.getCurrentPage());
        Assert.assertEquals(5, listAreas.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // Area action area_device_manager: 32
    @Test
    public void test058_deleteAreaShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, delete Area with the following call deleteArea
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.deleteArea(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // Area action area_device_manager: 32
    @Test
    public void test059_addAreaDeviceShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, add an AreaDevice with the following call addAreaDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(),
                ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(),
                ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(huser.getId(),
                ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
    }


    // Area action area_device_manager: 32
    @Test
    public void test060_removeAreaDeviceShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with default permission, remove an AreaDevice with the following call removeAreaDevice
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = createAreaDevice(huser, area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(huser.getId(),
                areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.removeAreaDevice(area.getId(), areaDevice.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // Area action area_device_manager: 32
    @Test
    public void test061_getAreaDevicesListShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // HUser, with permission, find AreaDevice list with the following call getAreaDeviceList
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = createAreaDevice(huser, area, hdevice);
        Assert.assertNotEquals(0, areaDevice.getId());
        Assert.assertEquals(huser.getId(),
                areaDevice.getArea().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                areaDevice.getDevice().getProject().getUser().getId());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.getAreaDeviceList(area.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<AreaDevice> listAreaDevices = restResponse.readEntity(new GenericType<List<AreaDevice>>() {
        });
        Assert.assertFalse(listAreaDevices.isEmpty());
        Assert.assertNotEquals(0, listAreaDevices.size());
        boolean areaHDeviceFound = false;
        for (AreaDevice areaDevices : listAreaDevices) {
            if (areaDevice.getId() == areaDevices.getId()) {
                Assert.assertEquals(hdevice.getId(),
                        ((AreaDevice) ((ArrayList) listAreaDevices).get(0)).getDevice().getId());
                Assert.assertEquals(hproject.getId(),
                        ((AreaDevice) ((ArrayList) listAreaDevices).get(0)).getDevice().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((AreaDevice) ((ArrayList) listAreaDevices).get(0)).getDevice().getProject().getUser().getId());
                Assert.assertEquals(area.getId(),
                        ((AreaDevice) ((ArrayList) listAreaDevices).get(0)).getArea().getId());
                Assert.assertEquals(hproject.getId(),
                        ((AreaDevice) ((ArrayList) listAreaDevices).get(0)).getArea().getProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((AreaDevice) ((ArrayList) listAreaDevices).get(0)).getArea().getProject().getUser().getId());
                areaHDeviceFound = true;
            }
        }
        Assert.assertTrue(areaHDeviceFound);
    }


    // Area action area_device_manager: 32
    @Test
    public void test062_findInnerAreasShouldWork() {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        // huser, with default permission, find inner areas with the following call findInnerAreas
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area1 = createArea(huser, hproject);
        Assert.assertNotEquals(0, area1.getId());
        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());

        Area parentArea = createArea(huser, hproject);
        Assert.assertNotEquals(0, parentArea.getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());
        Assert.assertEquals(huser.getId(), parentArea.getProject().getUser().getId());

        Assert.assertEquals(huser.getId(), hproject.getUser().getId());
        Assert.assertEquals(huser.getId(), area1.getProject().getUser().getId());
        Assert.assertEquals(huser.getId(), parentArea.getProject().getUser().getId());

        Assert.assertEquals(hproject.getId(), area1.getProject().getId());
        Assert.assertEquals(hproject.getId(), parentArea.getProject().getId());

        this.impersonateUser(areaRestApi, huser);
        parentArea.setParentArea(area1);
        Response restResponseUpdate1 = areaRestApi.updateArea(parentArea);
        Assert.assertEquals(200, restResponseUpdate1.getStatus());

        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.findInnerAreas(area1.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonAreaTree = (String) restResponse.getEntity();

        Assert.assertTrue(
                jsonAreaTree.contains(
                        "{\"id\":"+area1.getId()+"," +
                            "\"entityVersion\":"+area1.getEntityVersion()+"," +
                            "\"entityCreateDate\":"+area1.getEntityCreateDate().getTime()+"," +
                            "\"entityModifyDate\":"+area1.getEntityModifyDate().getTime()+"," +
                            "\"categoryIds\":null," +
                            "\"tagIds\":null," +
                            "\"name\":\""+area1.getName()+"\"," +
                            "\"description\":\""+area1.getDescription()+"\"," +
                            "\"imagePath\":null," +
                            "\"mapInfo\":null," +
                            "\"innerArea\":[" +
                                "{\"id\":"+parentArea.getId()+"," +
                                "\"entityVersion\":"+((Area)restResponseUpdate1.getEntity()).getEntityVersion()+"," +
                                "\"entityCreateDate\":"+parentArea.getEntityCreateDate().getTime()+"," +
                                "\"entityModifyDate\":"+((Area)restResponseUpdate1.getEntity()).getEntityModifyDate().getTime()+"," +
                                "\"categoryIds\":null," +
                                "\"tagIds\":null," +
                                "\"name\":\""+parentArea.getName()+"\"," +
                                "\"description\":\""+parentArea.getDescription()+"\"," +
                                "\"imagePath\":null," +
                                "\"mapInfo\":null," +
                                "\"innerArea\":[]}" +
                            "]}"
                )
        );
    }


    /*
     *
     * Rule
     *
     */


    // Rule action save: 1
    @Test
    public void test063_createNewRule() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, save Rule with the following call saveRule
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule 1");
        rule.setDescription("Everybody wants to rule the world.");
        rule.setType(RuleType.ENRICHMENT);
        rule.setRuleDefinition("latitude >= 3 AND temperature > 23");
        rule.setProject(hproject);
        rule.setPacket(hpacket);
        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        ValidateHPacketRuleAction hpacketAction = new ValidateHPacketRuleAction();
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        actions.add(hpacketAction);
        rule.setActions(actions);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());

        Assert.assertNotEquals(0,
                ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals("Add category rule 1",
                ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Everybody wants to rule the world.",
                ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals("latitude >= 3 AND temperature > 23",
                ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.enrichments",
                ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals(rule.getType().getDroolsPackage(),
                ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("ENRICHMENT",
                ((Rule) restResponse.getEntity()).getType().name());
        Assert.assertEquals(rule.getType().name(),
                ((Rule) restResponse.getEntity()).getType().name());

        Assert.assertEquals(hproject.getId(),
                ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                ((Rule) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((Rule) restResponse.getEntity()).getProject().getUser().getId());

        Assert.assertEquals(hpacket.getId(),
                ((Rule) restResponse.getEntity()).getPacket().getId());
        Assert.assertEquals(hpacket.getDevice().getProject().getUser().getId(),
                ((Rule) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((Rule) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
    }


    // Rule action update: 2
    @Test
    public void test064_updateRuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, update Rule with the following call updateRule
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRuleEngine(huser, hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
        Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
        Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());

        Date date = new Date();
        rule.setName("Rule edited in date: " + date);
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.updateRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getEntityVersion() + 1,
                ((Rule) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("Rule edited in date: " + date,
                ((Rule) restResponse.getEntity()).getName());
    }


    // Rule action remove: 4
    @Test
    public void test065_deleteRuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, delete Rule with the following call deleteRule
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRuleEngine(huser, hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
        Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
        Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.deleteRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // Rule action find: 8
    @Test
    public void test066_findRuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find Rule with the following call findRule
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRuleEngine(huser, hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
        Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
        Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findRule(rule.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(rule.getId(), ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals(rule.getDescription(),
                ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals(rule.getRuleDefinition(),
                ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events",
                ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals(rule.getType().getDroolsPackage(),
                ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("EVENT",
                ((Rule) restResponse.getEntity()).getType().name());
        Assert.assertEquals(rule.getType().name(),
                ((Rule) restResponse.getEntity()).getType().name());

        Assert.assertEquals(rule.getProject().getId(),
                ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(rule.getProject().getUser().getId(),
                ((Rule) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((Rule) restResponse.getEntity()).getProject().getUser().getId());

        Assert.assertEquals(rule.getPacket().getId(),
                ((Rule) restResponse.getEntity()).getPacket().getId());
        Assert.assertEquals(rule.getPacket().getDevice().getProject().getUser().getId(),
                ((Rule) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((Rule) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
    }


    // Rule action find-all: 16
    @Test
    public void test067_findAllRuleShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // HUser, with default permission, find all Rule with the following call findAllRule
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = createRuleEngine(huser, hproject, hpacket);
        Assert.assertNotEquals(0, rule.getId());
        Assert.assertEquals(hproject.getId(), rule.getProject().getId());
        Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
        Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
        Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRule();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Rule> listRules = restResponse.readEntity(new GenericType<List<Rule>>() {
        });
        Assert.assertNotEquals(0, listRules.size());
        Assert.assertFalse(listRules.isEmpty());
        boolean ruleFound = false;
        for (Rule rules : listRules) {
            if (rule.getId() == rules.getId()) {
                Assert.assertEquals(rule.getId(),
                        ((Rule) ((ArrayList) listRules).get(0)).getId());
                Assert.assertEquals(rule.getProject().getId(),
                        ((Rule) ((ArrayList) listRules).get(0)).getProject().getId());
                Assert.assertEquals(rule.getProject().getUser().getId(),
                        ((Rule) ((ArrayList) listRules).get(0)).getProject().getUser().getId());
                Assert.assertEquals(rule.getPacket().getId(),
                        ((Rule) ((ArrayList) listRules).get(0)).getPacket().getId());
                Assert.assertEquals(rule.getPacket().getDevice().getProject().getUser().getId(),
                        ((Rule) ((ArrayList) listRules).get(0)).getPacket().getDevice().getProject().getUser().getId());
                ruleFound = true;
            }
        }
        Assert.assertTrue(ruleFound);
    }


    // Rule action find-all: 16
    @Test
    public void test068_findAllRulePaginatedShouldWork() {
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        // In this following call findAllRule, HUser, with default permission,
        // find all Rules with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = createHDevice(huser, hproject);
        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(hproject.getId(), hdevice.getProject().getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = createHPacketAndAddHPacketField(huser, hdevice, false);
        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hdevice.getId(), hpacket.getDevice().getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        List<Rule> rules = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Rule rule = createRuleEngine(huser, hproject, hpacket);
            Assert.assertNotEquals(0, rule.getId());
            Assert.assertEquals(hproject.getId(), rule.getProject().getId());
            Assert.assertEquals(huser.getId(), rule.getProject().getUser().getId());
            Assert.assertEquals(hpacket.getId(), rule.getPacket().getId());
            Assert.assertEquals(huser.getId(), rule.getPacket().getDevice().getProject().getUser().getId());
            rules.add(rule);
        }
        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.findAllRulePaginated(delta, page);
        HyperIoTPaginableResult<Rule> listRules = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Rule>>() {
                });
        Assert.assertEquals(5, listRules.getResults().size());
        Assert.assertFalse(listRules.getResults().isEmpty());
        Assert.assertEquals(2, listRules.getCurrentPage());
        Assert.assertEquals(5, listRules.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     * Dashboard
     *
     */


    // Dashboard action save: 1
    @Test
    public void test069_createNewDashboard() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, save Dashboard with the following call saveDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = new Dashboard();
        dashboard.setName(hproject.getName() + " Online Dashboard");
        dashboard.setDashboardType(DashboardType.REALTIME);
        dashboard.setHProject(hproject);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.saveDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
        Assert.assertEquals(hproject.getName() + " Online Dashboard",
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals("REALTIME",
                ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(hproject.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action update: 2
    @Test
    public void test070_updateDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, update Dashboard with the following call updateDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        Date date = new Date();
        dashboard.setName("Dashboard edited in date: " + date);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getEntityVersion() + 1,
                ((Dashboard) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("Dashboard edited in date: " + date,
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action update: 2
    @Test
    public void test071_updateDashboardRealTimeInOfflineShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, update Dashboard with the following call updateDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        dashboard.setDashboardType(DashboardType.OFFLINE);
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.updateDashboard(dashboard);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getEntityVersion() + 1,
                ((Dashboard) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("OFFLINE",
                ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action remove: 4
    @Test
    public void test072_deleteDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, delete Dashboard with the following call deleteDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.deleteDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // Dashboard action find: 8
    @Test
    public void test073_findDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, find Dashboard with the following call findDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "REALTIME");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("REALTIME", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findDashboard(dashboard.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboard.getId(), ((Dashboard) restResponse.getEntity()).getId());
        Assert.assertEquals(dashboard.getName(),
                ((Dashboard) restResponse.getEntity()).getName());
        Assert.assertEquals(dashboard.getDashboardType().getType(),
                ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
        Assert.assertEquals(dashboard.getHProject().getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
    }


    // Dashboard action find-all: 16
    @Test
    public void test074_findAllDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // HUser, with default permission, find all Dashboard with the following call findAllDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboard();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Dashboard> listDashboards = restResponse.readEntity(new GenericType<List<Dashboard>>() {
        });
        Assert.assertFalse(listDashboards.isEmpty());
        boolean dashboardFound = false;
        for (Dashboard dashboards : listDashboards) {
            if (dashboard.getId() == dashboards.getId()) {
                Assert.assertEquals(dashboard.getName(),
                        ((Dashboard) ((ArrayList) listDashboards).get(0)).getName());
                Assert.assertEquals(dashboard.getDashboardType().getType(),
                        ((Dashboard) ((ArrayList) listDashboards).get(0)).getDashboardType().getType());
                Assert.assertEquals(dashboard.getHProject().getId(),
                        ((Dashboard) ((ArrayList) listDashboards).get(0)).getHProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((Dashboard) ((ArrayList) listDashboards).get(0)).getHProject().getUser().getId());
                dashboardFound = true;
            }
        }
        Assert.assertTrue(dashboardFound);
    }


    // Dashboard action find-all: 16
    @Test
    public void test075_findAllDashboardPaginatedShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // In this following call findAllDashboard, HUser, with default permission,
        // find all Dashboard with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<Dashboard> dashboards = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
            Assert.assertNotEquals(0, dashboard.getId());
            Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
            Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());
            dashboards.add(dashboard);
        }
        this.impersonateUser(dashboardRestApi, huser);
        Response restResponse = dashboardRestApi.findAllDashboardPaginated(delta, page);
        HyperIoTPaginableResult<Dashboard> listDashboard = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Dashboard>>() {
                });
        Assert.assertEquals(5, listDashboard.getResults().size());
        Assert.assertFalse(listDashboard.getResults().isEmpty());
        Assert.assertEquals(2, listDashboard.getCurrentPage());
        Assert.assertEquals(5, listDashboard.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // Dashboard action find-all: 16
    @Test
    public void test076_findAreaRealtimeDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with default permission, finds all realtime Dashboard associated to the area
        // with the following call findAreaRealtimeDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaRealtime = dashboardRestApi.findAreaRealtimeDashboard(area.getId());
        Assert.assertEquals(200, restResponseAreaRealtime.getStatus());
        List<Dashboard> areaRealtime = restResponseAreaRealtime.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "REALTIME" has been setting in findAreaRealtimeDashboard()
        Assert.assertEquals(1, areaRealtime.size());
        Assert.assertNotEquals(0, areaRealtime.get(0).getId());
        Assert.assertEquals(area.getId(), areaRealtime.get(0).getArea().getId());
        Assert.assertEquals(area.getName() + " Online Dashboard", areaRealtime.get(0).getName());
        Assert.assertEquals("REALTIME", areaRealtime.get(0).getDashboardType().getType());
        Assert.assertEquals(huser.getId(), areaRealtime.get(0).getArea().getProject().getUser().getId());
        Assert.assertEquals(hproject.getId(), areaRealtime.get(0).getHProject().getId());
        Assert.assertEquals(huser.getId(), areaRealtime.get(0).getHProject().getUser().getId());
    }


    // Dashboard action find-all: 16
    @Test
    public void test077_findAreaOfflineDashboardShouldWork() {
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        // huser, with default permission, finds all offline Dashboard associated to the area
        // with the following call findAreaOfflineDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        this.impersonateUser(dashboardRestApi, huser);
        Response restResponseAreaOffline = dashboardRestApi.findAreaOfflineDashboard(area.getId());
        Assert.assertEquals(200, restResponseAreaOffline.getStatus());
        List<Dashboard> areaOffline = restResponseAreaOffline.readEntity(new GenericType<List<Dashboard>>() {
        });
        //dashboardType "OFFLINE" has been setting in findAreaOfflineDashboard()
        Assert.assertEquals(1, areaOffline.size());
        Assert.assertNotEquals(0, areaOffline.get(0).getId());
        Assert.assertEquals(area.getId(), areaOffline.get(0).getArea().getId());
        Assert.assertEquals(area.getName() + " Offline Dashboard", areaOffline.get(0).getName());
        Assert.assertEquals("OFFLINE", areaOffline.get(0).getDashboardType().getType());
        Assert.assertEquals(huser.getId(), areaOffline.get(0).getArea().getProject().getUser().getId());
        Assert.assertEquals(hproject.getId(), areaOffline.get(0).getHProject().getId());
        Assert.assertEquals(huser.getId(), areaOffline.get(0).getHProject().getUser().getId());
    }


    // Dashboard action find_widgets: 32 (DashboardWidget)
    @Test
    public void test078_findAllDashboardWidgetInDashboardShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find all available dashboard widgets inside a particular
        // dashboard with the following call findAllDashboardWidgetInDashboard
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetInDashboard(dashboardWidget.getDashboard().getId());
        Assert.assertEquals(200, restResponse.getStatus());
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertNotEquals(0, listDashboardWidgets.size());
        Assert.assertFalse(listDashboardWidgets.isEmpty());
        boolean dashboardWidgetFound = false;
        for (DashboardWidget dashboardWidgets : listDashboardWidgets) {
            if (dashboardWidget.getId() == dashboardWidgets.getId()) {
                Assert.assertEquals(dashboardWidget.getWidgetConf(), dashboardWidgets.getWidgetConf());
                Assert.assertEquals(dashboard.getId(), dashboardWidgets.getDashboard().getId());
                Assert.assertEquals(hproject.getId(), dashboardWidgets.getDashboard().getHProject().getId());
                Assert.assertEquals(huser.getId(), dashboardWidgets.getDashboard().getHProject().getUser().getId());
                dashboardWidgetFound = true;
            }
        }
        Assert.assertTrue(dashboardWidgetFound);
    }


    /*
     *
     * DashboardWidget
     *
     */


    // DashboardWidget action save: 1
    @Test
    public void test079_createNewDashboardWidget() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, save DashboardWidget with the following call saveDashboardWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"just a simple description\"}");
        dashboardWidget.setDashboard(dashboard);
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals("{\"description\":\"just a simple description\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(hproject.getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(dashboardWidget.getDashboard().getHProject().getUser().getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
    }


    // DashboardWidget action update: 2
    @Test
    public void test080_updateDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, update DashboardWidget with the following call updateDashboardWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        Date date = new Date();
        dashboardWidget.setWidgetConf("{\"description\":\"description edited in date: \"" + date + "\"}");
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.updateDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getEntityVersion() + 1,
                ((DashboardWidget) restResponse.getEntity()).getEntityVersion());
        Assert.assertEquals("{\"description\":\"description edited in date: \"" + date + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }


    // DashboardWidget action update: 2
    @Test
    public void test081_setDashboardWidgetConfShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, update dashboard widget configuration with
        // the following call setDashboardWidgetConf
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        String widgetConf = "{\"description\":\"test setDashboardWidgetConf\"}";
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.setDashboardWidgetConf(dashboardWidget.getId(), widgetConf);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(widgetConf, ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
    }


    // DashboardWidget action remove: 4
    @Test
    public void test082_deleteDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, delete DashboardWidget with the following call deleteDashboardWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.deleteDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // DashboardWidget action find: 8
    @Test
    public void test083_findDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find DashboardWidget with the following call findDashboardWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidget(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(dashboardWidget.getId(), ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals(dashboardWidget.getWidgetConf(), ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(hproject.getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
    }


    // DashboardWidget action find: 8
    @Test
    public void test084_findDashboardWidgetConfShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find dashboard widget configuration with the following
        // call findDashboardWidgetConf
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findDashboardWidgetConf(dashboardWidget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        String jsonDashboardWidgetConf = (String) restResponse.getEntity();
        Assert.assertEquals(dashboardWidget.getWidgetConf(), jsonDashboardWidgetConf);
    }


    // DashboardWidget action find-all: 16
    @Test
    public void test085_findAllDashboardWidgetShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // HUser, with default permission, find all DashboardWidget with the following call findAllDashboardWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
        Assert.assertNotEquals(0, dashboardWidget.getId());
        Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
        Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidget();
        Assert.assertEquals(200, restResponse.getStatus());
        List<DashboardWidget> listDashboardWidgets = restResponse.readEntity(new GenericType<List<DashboardWidget>>() {
        });
        Assert.assertNotEquals(0, listDashboardWidgets.size());
        Assert.assertFalse(listDashboardWidgets.isEmpty());
        boolean dashboardWidgetFound = false;
        for (DashboardWidget dashboardWidgets : listDashboardWidgets) {
            if (dashboardWidget.getId() == dashboardWidgets.getId()) {
                Assert.assertEquals(dashboardWidget.getId(),
                        ((DashboardWidget) ((ArrayList) listDashboardWidgets).get(0)).getId());
                Assert.assertEquals(dashboardWidget.getWidgetConf(),
                        ((DashboardWidget) ((ArrayList) listDashboardWidgets).get(0)).getWidgetConf());
                Assert.assertEquals(dashboard.getId(),
                        ((DashboardWidget) ((ArrayList) listDashboardWidgets).get(0)).getDashboard().getId());
                Assert.assertEquals(hproject.getId(),
                        ((DashboardWidget) ((ArrayList) listDashboardWidgets).get(0)).getDashboard().getHProject().getId());
                Assert.assertEquals(huser.getId(),
                        ((DashboardWidget) ((ArrayList) listDashboardWidgets).get(0)).getDashboard().getHProject().getUser().getId());
                dashboardWidgetFound = true;
            }
        }
        Assert.assertTrue(dashboardWidgetFound);
    }


    // DashboardWidget action find-all: 16
    @Test
    public void test086_findAllDashboardWidgetPaginatedShouldWork() {
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);
        // In this following call findAllDashboardWidget HUser, with default permission,
        // find all DashboardWidget with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = createDashboard(huser, hproject, "OFFLINE");
        Assert.assertNotEquals(0, dashboard.getId());
        Assert.assertEquals("OFFLINE", dashboard.getDashboardType().getType());
        Assert.assertEquals(hproject.getId(), dashboard.getHProject().getId());
        Assert.assertEquals(huser.getId(), dashboard.getHProject().getUser().getId());

        List<DashboardWidget> dashboardWidgets = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            DashboardWidget dashboardWidget = createDashboardWidget(huser, dashboard);
            Assert.assertNotEquals(0, dashboardWidget.getId());
            Assert.assertEquals(dashboard.getId(), dashboardWidget.getDashboard().getId());
            Assert.assertEquals(hproject.getId(), dashboardWidget.getDashboard().getHProject().getId());
            Assert.assertEquals(huser.getId(), dashboardWidget.getDashboard().getHProject().getUser().getId());
            dashboardWidgets.add(dashboardWidget);
        }
        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.findAllDashboardWidgetPaginated(delta, page);
        HyperIoTPaginableResult<DashboardWidget> listDashboardWidget = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<DashboardWidget>>() {
                });
        Assert.assertEquals(5, listDashboardWidget.getResults().size());
        Assert.assertFalse(listDashboardWidget.getResults().isEmpty());
        Assert.assertEquals(2, listDashboardWidget.getCurrentPage());
        Assert.assertEquals(5, listDashboardWidget.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     * Widget
     *
     */


    // Widget action save: 1 not assigned in default permission
    @Test
    public void test087_createNewWidgetWithDefaultPermissionShouldFail() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, tries to save Widget with the following call saveWidget.
        // huser to save a new widget needs the "save widget" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = new Widget();
        widget.setName("image-data" + UUID.randomUUID().toString().replaceAll("-", ""));
        widget.setDescription(widgetDescription);
        widget.setWidgetCategory(WidgetCategory.ALL);
        widget.setBaseConfig("image-data");
        widget.setType("image-data");
        widget.setCols(2);
        widget.setRows(3);
        widget.setImage(widgetImageData);
        widget.setPreView(widgetImageDataPreview);
        widget.setOffline(true);
        widget.setRealTime(false);

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.saveWidget(widget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Widget action update: 2 not assigned in default permission
    @Test
    public void test088_updateWidgetWithDefaultPermissionShouldFail() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, tries to update Widget with the following call updateWidget
        // huser to update a new widget needs the "update widget" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget();
        Assert.assertNotEquals(0, widget.getId());

        widget.setDescription("Edit description failed...");

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.updateWidget(widget);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Widget action remove: 4 not assigned in default permission
    @Test
    public void test089_deleteWidgetWithDefaultPermissionShouldFail() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, tries to delete Widget with the following call deleteWidget
        // huser to delete a new widget needs the "remove widget" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget();
        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.deleteWidget(widget.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Widget action find: 8
    @Test
    public void test090_findWidgetShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, find Widget with the following call findWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget();
        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.findWidget(widget.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(widget.getId(), ((Widget) restResponse.getEntity()).getId());
        Assert.assertEquals(widget.getName(),
                ((Widget) restResponse.getEntity()).getName());
        Assert.assertEquals(widget.getDescription(),
                ((Widget) restResponse.getEntity()).getDescription());
        Assert.assertEquals(widget.getWidgetCategory().getId(),
                ((Widget) restResponse.getEntity()).getWidgetCategory().getId());
        Assert.assertEquals(widget.getWidgetCategory().getName(),
                ((Widget) restResponse.getEntity()).getWidgetCategory().getName());
        Assert.assertEquals(widget.getWidgetCategory().getFontIcon(),
                ((Widget) restResponse.getEntity()).getWidgetCategory().getFontIcon());
        Assert.assertEquals(widget.getWidgetCategory().name(),
                ((Widget) restResponse.getEntity()).getWidgetCategory().name());
        Assert.assertEquals(widget.getWidgetCategory().ordinal(),
                ((Widget) restResponse.getEntity()).getWidgetCategory().ordinal());
        Assert.assertEquals(widget.getBaseConfig(), ((Widget) restResponse.getEntity()).getBaseConfig());
        Assert.assertEquals(widget.getType(), ((Widget) restResponse.getEntity()).getType());
        Assert.assertEquals(widget.getCols(), ((Widget) restResponse.getEntity()).getCols());
        Assert.assertEquals(widget.getRows(), ((Widget) restResponse.getEntity()).getRows());
        Assert.assertFalse(((Widget) restResponse.getEntity()).isOffline());
        Assert.assertTrue(((Widget) restResponse.getEntity()).isRealTime());
    }


    // Widget action find-all: 16
    @Test
    public void test091_findAllWidgetShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser, with default permission, find all Widget with the following call findAllWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget();
        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.findAllWidget();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Widget> listWidgets = restResponse.readEntity(new GenericType<List<Widget>>() {
        });
        Assert.assertFalse(listWidgets.isEmpty());
        boolean widgetFound = false;
        for (Widget widgets : listWidgets) {
            if (widget.getId() == widgets.getId()) {
                Assert.assertEquals(widget.getId(), widgets.getId());
                Assert.assertEquals(widget.getName(), widgets.getName());
                Assert.assertEquals(widget.getDescription(), widgets.getDescription());
                Assert.assertEquals(widget.getWidgetCategory().getId(), widgets.getWidgetCategory().getId());
                Assert.assertEquals(widget.getWidgetCategory().getName(), widgets.getWidgetCategory().getName());
                Assert.assertEquals(widget.getWidgetCategory().getFontIcon(), widgets.getWidgetCategory().getFontIcon());
                Assert.assertEquals(widget.getWidgetCategory().name(), widgets.getWidgetCategory().name());
                Assert.assertEquals(widget.getWidgetCategory().ordinal(), widgets.getWidgetCategory().ordinal());
                Assert.assertEquals(widget.getBaseConfig(), widgets.getBaseConfig());
                Assert.assertEquals(widget.getType(), widgets.getType());
                Assert.assertEquals(widget.getCols(), widgets.getCols());
                Assert.assertEquals(widget.getRows(), widgets.getRows());
                widgetFound = true;
            }
        }
        Assert.assertTrue(widgetFound);
    }


    // Widget action find-all: 16
    @Test
    public void test092_findAllWidgetPaginatedShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // In this following call findAllWidgetPaginated, huser, with default permission,
        // find all Widget with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<Widget> widgets = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Widget widget = createWidget();
            widgets.add(widget);
        }
        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.findAllWidgetPaginated(delta, page);
        HyperIoTPaginableResult<Widget> listWidgets = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Widget>>() {
                });
        Assert.assertEquals(5, listWidgets.getResults().size());
        Assert.assertFalse(listWidgets.getResults().isEmpty());
        Assert.assertEquals(2, listWidgets.getCurrentPage());
        Assert.assertEquals(5, listWidgets.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // Widget nothing action
    @Test
    public void test093_rateWidgetShouldWork() {
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        // huser rate Widget with the following call rateWidget
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Widget widget = createWidget();
        Assert.assertNotEquals(0, widget.getId());

        this.impersonateUser(widgetRestApi, huser);
        Response restResponse = widgetRestApi.rateWidget(5, widget);
        Assert.assertEquals(200, restResponse.getStatus());

        //checks if widgetId inside WidgetRating table is equals to widget.getId()
        String sqlWidgetId = "select wr.widget_id from widgetrating wr";
        String resultWidgetId = executeCommand("jdbc:query hyperiot " + sqlWidgetId);
        String[] wrId = resultWidgetId.split("\\n");

        this.impersonateUser(widgetRestApi, huser);
        Response restResponseFindWidget = widgetRestApi.findWidget(Long.parseLong(wrId[2]));
        Assert.assertEquals(200, restResponseFindWidget.getStatus());
        Assert.assertEquals(widget.getId(), ((Widget) restResponseFindWidget.getEntity()).getId());

        //checks if avgRating has been updated inside Widget table
        String sqlRating = "select wr.rating from widgetrating wr";
        String resultRating = executeCommand("jdbc:query hyperiot " + sqlRating);
        String[] wrRating = resultRating.split("\\n");
        Assert.assertEquals(((Float)Float.parseFloat(wrRating[2])), ((Widget) restResponseFindWidget.getEntity()).getAvgRating());

        //checks if huserId inside widgetRating table is equals to huser.getId()
        String sqlHuserId = "select wr.user_id from widgetrating wr";
        String resultHuserId = executeCommand("jdbc:query hyperiot " + sqlHuserId);
        String[] wrHuserId = resultHuserId.split("\\n");
        Assert.assertEquals(Long.parseLong(wrHuserId[2]), huser.getId());
    }


    /*
     *
     * AssetCategory
     *
     */


    // AssetCategory action save: 1
    @Test
    public void test094_createNewAssetCategory() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, save AssetCategory with the following call saveAssetCategory
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category " + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName + java.util.UUID.randomUUID());
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(),
                ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceName(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(hproject.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
    }


    // AssetCategory action update: 2
    @Test
    public void test095_updateAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, update AssetCategory with the following call updateAssetCategory
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetCategory assetCategory = createAssetCategory(huser, hproject);
        Assert.assertNotEquals(0, assetCategory.getId());
        Assert.assertEquals(hproject.getId(), assetCategory.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetCategory.getOwner().getUserId());

        Date date = new Date();
        assetCategory.setName("name edited in date: " + date);

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("name edited in date: " + date,
                ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getEntityVersion() + 1,
                (((AssetCategory) restResponse.getEntity()).getEntityVersion()));
    }


    // AssetCategory action remove: 4
    @Test
    public void test096_deleteAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, delete AssetCategory with the following call deleteAssetCategory
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetCategory assetCategory = createAssetCategory(huser, hproject);
        Assert.assertNotEquals(0, assetCategory.getId());
        Assert.assertEquals(hproject.getId(), assetCategory.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetCategory.getOwner().getUserId());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // AssetCategory action find: 8
    @Test
    public void test097_findAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, find AssetCategory with the following call findAssetCategory
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetCategory assetCategory = createAssetCategory(huser, hproject);
        Assert.assertNotEquals(0, assetCategory.getId());
        Assert.assertEquals(hproject.getId(), assetCategory.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetCategory.getOwner().getUserId());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(assetCategory.getId(),
                ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(),
                ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceName(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(hproject.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
    }


    // AssetCategory action find-all: 16
    @Test
    public void test098_findAllAssetCategoriesShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, find all AssetCategories with the following call findAllAssetCategories
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetCategory assetCategory = createAssetCategory(huser, hproject);
        Assert.assertNotEquals(0, assetCategory.getId());
        Assert.assertEquals(hproject.getId(), assetCategory.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetCategory.getOwner().getUserId());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        Assert.assertEquals(200, restResponse.getStatus());
        List<AssetCategory> listCategories = restResponse.readEntity(new GenericType<List<AssetCategory>>() {
        });
        Assert.assertFalse(listCategories.isEmpty());
        boolean assetCategoryFound = false;
        for (AssetCategory categories : listCategories) {
            if (assetCategory.getId() == categories.getId()) {
                Assert.assertEquals(assetCategory.getId(), categories.getId());
                Assert.assertEquals(assetCategory.getName(), categories.getName());
                Assert.assertEquals(assetCategory.getOwner().getOwnerResourceName(), categories.getOwner().getOwnerResourceName());
                Assert.assertEquals(hproject.getId(), categories.getOwner().getOwnerResourceId().longValue());
                Assert.assertEquals(huser.getId(), categories.getOwner().getUserId());
                assetCategoryFound = true;
            }
        }
        Assert.assertTrue(assetCategoryFound);
    }


    // AssetCategory action find-all: 16
    @Test
    public void test099_findAllAssetCategoriesPaginatedShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with default permission,
        // find all AssetCategories with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetCategory assetCategory = createAssetCategory(huser, hproject);
            Assert.assertNotEquals(0, assetCategory.getId());
            Assert.assertEquals(hproject.getId(), assetCategory.getOwner().getOwnerResourceId().longValue());
            Assert.assertEquals(huser.getId(), assetCategory.getOwner().getUserId());
            categories.add(assetCategory);
        }
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(5, listAssetCategories.getResults().size());
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(2, listAssetCategories.getCurrentPage());
        Assert.assertEquals(5, listAssetCategories.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     * AssetTag
     *
     */


    // AssetTag action save: 1
    @Test
    public void test100_createNewAssetTag() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with default permission, save AssetTag with the following call saveAssetTag
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag " + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName + java.util.UUID.randomUUID());
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetTag) restResponse.getEntity()).getId());
        Assert.assertEquals(assetTag.getName(),
                ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getOwner().getOwnerResourceName(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(hproject.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getUserId());
    }


    // AssetTag action update: 2
    @Test
    public void test101_updateAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with default permission, update AssetTag with the following call updateAssetTag
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetTag assetTag = createAssetTag(huser, hproject);
        Assert.assertNotEquals(0, assetTag.getId());
        Assert.assertEquals(hproject.getId(), assetTag.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetTag.getOwner().getUserId());

        Date date = new Date();
        assetTag.setName("name edited in date: " + date);

        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("name edited in date: " + date,
                ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getEntityVersion() + 1,
                (((AssetTag) restResponse.getEntity()).getEntityVersion()));
    }


    // AssetTag action remove: 4
    @Test
    public void test102_deleteAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with default permission, delete AssetTag with the following call deleteAssetTag
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetTag assetTag = createAssetTag(huser, hproject);
        Assert.assertNotEquals(0, assetTag.getId());
        Assert.assertEquals(hproject.getId(), assetTag.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetTag.getOwner().getUserId());

        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.deleteAssetTag(assetTag.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // AssetTag action find: 8
    @Test
    public void test103_findAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with default permission, find AssetTag with the following call findAssetTag
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetTag assetTag = createAssetTag(huser, hproject);
        Assert.assertNotEquals(0, assetTag.getId());
        Assert.assertEquals(hproject.getId(), assetTag.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetTag.getOwner().getUserId());

        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAssetTag(assetTag.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(assetTag.getId(),
                ((AssetTag) restResponse.getEntity()).getId());
        Assert.assertEquals(assetTag.getName(),
                ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getOwner().getOwnerResourceName(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(hproject.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getUserId());
    }


    // AssetTag action find-all: 16
    @Test
    public void test104_findAllAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with default permission, find all AssetTag with the following
        // call findAllAssetTag
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        AssetTag assetTag = createAssetTag(huser, hproject);
        Assert.assertNotEquals(0, assetTag.getId());
        Assert.assertEquals(hproject.getId(), assetTag.getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(), assetTag.getOwner().getUserId());

        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAllAssetTag();
        List<AssetTag> listTags = restResponse.readEntity(new GenericType<List<AssetTag>>() {
        });
        Assert.assertFalse(listTags.isEmpty());
        boolean assetTagFound = false;
        for (AssetTag tags : listTags) {
            if (assetTag.getId() == tags.getId()) {
                Assert.assertEquals(assetTag.getId(), tags.getId());
                Assert.assertEquals(assetTag.getName(), tags.getName());
                Assert.assertEquals(assetTag.getOwner().getOwnerResourceName(), tags.getOwner().getOwnerResourceName());
                Assert.assertEquals(hproject.getId(), tags.getOwner().getOwnerResourceId().longValue());
                Assert.assertEquals(huser.getId(), tags.getOwner().getUserId());
                assetTagFound = true;
            }
        }
        Assert.assertTrue(assetTagFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // AssetTag action find-all: 16
    @Test
    public void test105_findAllAssetTagPaginatedShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with default permission,
        // find all AssetTag with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        HProject hproject = createHProject(huser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag(huser, hproject);
            Assert.assertNotEquals(0, assetTag.getId());
            Assert.assertEquals(hproject.getId(), assetTag.getOwner().getOwnerResourceId().longValue());
            Assert.assertEquals(huser.getId(), assetTag.getOwner().getUserId());
            tags.add(assetTag);
        }
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(delta, page);
        HyperIoTPaginableResult<AssetTag> listAssetTag = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetTag>>() {
                });
        Assert.assertEquals(5, listAssetTag.getResults().size());
        Assert.assertFalse(listAssetTag.getResults().isEmpty());
        Assert.assertEquals(2, listAssetTag.getCurrentPage());
        Assert.assertEquals(5, listAssetTag.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    /*
     *
     * Role
     *
     */


    // Role action save: 1 not assigned in default permission
    @Test
    public void test106_createNewRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to save new Role with the following call saveRole
        // huser to save a new role needs the "save role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");

        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action update: 2 not assigned in default permission
    @Test
    public void test107_updateRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to update Role with the following call updateRole
        // huser to update a new role needs the "update role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = createRole();
        role.setDescription("description edited");

        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action remove: 4 not assigned in default permission
    @Test
    public void test108_deleteRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to delete Role with the following call deleteRole
        // huser to delete a new role needs the "remove role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteRole(role.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action find: 8 not assigned in default permission
    @Test
    public void test109_findRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to find Role with the following call findRole
        // huser to find a new role needs the "find role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action find: 8 not assigned in default permission
    @Test
    public void test110_getUserRolesWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to find all huserRoles with the following call findAllUserRoles
        // huser to find all huser roles needs the "find role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllUserRoles(huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action find-all: 16 not assigned in default permission
    @Test
    public void test111_findAllRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to find all Role with the following call findAllRole
        // huser to find all role needs the "find-all role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRoles();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action find-all: 16 not assigned in default permission
    @Test
    public void test112_findAllRolesPaginatedWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to find all Role with the following call findAllRolesPaginated
        // huser to find all role needs the "find-all role" permission
        // response status code '403' HyperIoTUnauthorizedException
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action assign_members: 32 not assigned in default permission
    @Test
    public void test113_saveUserRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to save user Role with the following call saveUserRole
        // huser to assign huser role needs the "assign_members role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Role role = createRole();
        huser.addRole(role);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Role action unassign: 64 not assigned in default permission
    @Test
    public void test114_deleteUserRoleWithDefaultPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser, with default permission, tries to remove user Role with the following call deleteUserRole
        // huser to remove huser role needs the "unassign role" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Role role = createUserRole(huser);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteUserRole(role.getId(), huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     * Permission
     *
     */


    // Permission action save: 1 not assigned in default permission
    @Test
    public void test115_createNewPermissionWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to save new Permission with the following call savePermission
        // huser to save a new permission needs the "save permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Permission action update: 2 not assigned in default permission
    @Test
    public void test116_updatePermissionWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to update Permission with the following call updatePermission
        // huser to update permission needs the "update permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = createRole();
        Assert.assertNotEquals(0, role.getId());

        Permission permission = createPermission(role);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertNotEquals(0, permission.getActionIds());
        Assert.assertEquals(role.getId(), permission.getRole().getId());

        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Permission action remove: 4 not assigned in default permission
    @Test
    public void test117_deletePermissionWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to delete Permission with the following call deletePermission
        // huser to delete permission needs the "remove permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = createRole();
        Assert.assertNotEquals(0, role.getId());

        Permission permission = createPermission(role);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertNotEquals(0, permission.getActionIds());
        Assert.assertEquals(role.getId(), permission.getRole().getId());

        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.deletePermission(permission.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Permission action find: 8 not assigned in default permission
    @Test
    public void test118_findPermissionWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to find Permission with the following call findPermission
        // huser to find permission needs the "find permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = createRole();
        Assert.assertNotEquals(0, role.getId());

        Permission permission = createPermission(role);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertNotEquals(0, permission.getActionIds());
        Assert.assertEquals(role.getId(), permission.getRole().getId());

        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Permission action find-all: 16 not assigned in default permission
    @Test
    public void test119_findAllPermissionWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to find all Permissions with the following call findAllPermission
        // huser to find all permissions needs the "find-all permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = createRole();
        Assert.assertNotEquals(0, role.getId());

        Permission permission = createPermission(role);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertNotEquals(0, permission.getActionIds());
        Assert.assertEquals(role.getId(), permission.getRole().getId());

        Assert.assertNotEquals(0, permission.getId());
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllPermission();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Permission action find-all: 16 not assigned in default permission
    @Test
    public void test120_findAllPermissionPaginatedWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to find all Permissions with the following call findAllPermission
        // huser to find all permissions needs the "find-all permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Role role = createRole();
        Assert.assertNotEquals(0, role.getId());

        List<Permission> permissions = new ArrayList<>();
        Integer delta = 10;
        Integer page = 1;
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission(role);
            Assert.assertNotEquals(0, permission.getId());
            Assert.assertNotEquals(0, permission.getActionIds());
            Assert.assertEquals(role.getId(), permission.getRole().getId());
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Permission action list_actions: 64 not assigned in default permission
    @Test
    public void test121_findAllActionsWithDefaultPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // huser, with default permission, tries to find all actions with the following call findAllActions
        // huser to find all actions needs the "list_actions permission" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllActions();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     * Company
     *
     */


    // Company action save: 1 not assigned in default permission
    @Test
    public void test122_createNewCompanyWithDefaultPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with default permission, tries to save new Company with the following call saveCompany
        // huser to save a new Company needs the "save company" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Company action update: 2 not assigned in default permission
    @Test
    public void test123_updateCompanyWithDefaultPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with default permission, tries to update Company with the following call updateCompany
        // huser to update Company needs the "update company" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        company.setBusinessName("Bologna");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Company action remove: 4 not assigned in default permission
    @Test
    public void test124_deleteCompanyWithDefaultPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with default permission, tries to delete Company with the following call deleteCompany
        // huser to delete Company needs the "delete company" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Company action find: 8 not assigned in default permission
    @Test
    public void test125_findCompanyWithDefaultPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with default permission, tries to find Company with the following call findCompany
        // huser to find Company needs the "find company" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findCompany(company.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Company action find-all: 16 not assigned in default permission
    @Test
    public void test126_findCompanyWithDefaultPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with default permission, tries to find all Companies with the following call findAllCompany
        // huser to find all Companies needs the "find-all company" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompany();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Company action find-all: 16 not assigned in default permission
    @Test
    public void test127_findAllCompanyPaginatedWithDefaultPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with default permission, tries to find all Companies with the following call findAllCompany
        // huser to find all Companies needs the "find-all company" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Integer delta = 5;
        Integer page = 2;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Company company = createCompany(huser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompanyPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     * Algorithm
     *
     */


    // Algorithm action save: 1 not assigned in default permission
    @Test
    public void test128_createNewAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to save new Algorithm with the following call saveAlgorithm
        // huser to save a new Algorithm needs the "save algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("everybody wants to rule the world");
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");
        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update: 2 not assigned in default permission
    @Test
    public void test129_updateAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update Algorithm with the following call updateAlgorithm
        // huser to update Algorithm needs the "update algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        algorithm.setDescription("edit description failed...");

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateAlgorithm(algorithm);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action remove: 4 not assigned in default permission
    @Test
    public void test130_deleteAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to delete Algorithm with the following call deleteAlgorithm
        // huser to delete Algorithm needs the "remove algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.deleteAlgorithm(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action find: 8 not assigned in default permission
    @Test
    public void test131_findAlgorithmWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to find Algorithm with the following call findAlgorithm
        // huser to find Algorithm needs the "find algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.findAlgorithm(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action find-all: 16
    @Test
    public void test132_findAllAlgorithmWithDefaultPermissionShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, find all Algorithms with the following call findAllAlgorithm
        // response status code '200'
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.findAllAlgorithm();
        Assert.assertEquals(200, restResponse.getStatus());
        List<Algorithm> algorithmList = restResponse.readEntity(new GenericType<List<Algorithm>>() {
        });
        Assert.assertFalse(algorithmList.isEmpty());
        boolean algorithmFound = false;
        for (Algorithm algorithms : algorithmList) {
            if (algorithm.getId() == algorithms.getId()) {
                Assert.assertEquals(algorithm.getId(), algorithms.getId());
                Assert.assertEquals(algorithm.getName(), algorithms.getName());
                Assert.assertEquals(algorithm.getDescription(), algorithms.getDescription());
                Assert.assertEquals(algorithm.getBaseConfig(), algorithms.getBaseConfig());
                algorithmFound = true;
            }
        }
        Assert.assertTrue(algorithmFound);
    }


    // Algorithm action find-all: 16
    @Test
    public void test133_findAllAlgorithmPaginatedWithDefaultPermissionShouldWork() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // In this following call findAllAlgorithmPaginated, huser, with default permission
        // find all Algorithm with pagination
        // response status code '200'
        Integer delta = 5;
        Integer page = 2;
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        List<Algorithm> algorithms = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Algorithm algorithm = createAlgorithm();
            Assert.assertNotEquals(0, algorithm.getId());
            algorithms.add(algorithm);
        }
        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.findAllAlgorithmPaginated(delta, page);
        HyperIoTPaginableResult<Algorithm> listAlgorithms = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Algorithm>>() {
                });
        Assert.assertEquals(5, listAlgorithms.getResults().size());
        Assert.assertFalse(listAlgorithms.getResults().isEmpty());
        Assert.assertEquals(2, listAlgorithms.getCurrentPage());
        Assert.assertEquals(5, listAlgorithms.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    // Algorithm action add_io_field: 32 not assigned in default permission
    @Test
    public void test134_addInputFieldWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to add input field with the following call addIOField
        // huser to add ioField needs the "add_io_field algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOField of " + algorithm.getName());
        algorithmIOField.setDescription("IOField of " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.INTEGER);
        algorithmIOField.setType(AlgorithmFieldType.INPUT);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action delete_io_field: 64 not assigned in default permission
    @Test
    public void test135_deleteInputFieldWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to delete input field with the following call deleteIOField
        // huser to delete ioField needs the "delete_io_field algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm,"INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.deleteIOField(algorithm.getId(), algorithmIOField.getType(), algorithmIOField.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action read_base_config: 128 not assigned in default permission
    @Test
    public void test136_getBaseConfigWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to find BaseConfig with the following call getBaseConfig
        // huser to find BaseConfig needs the "read_base_config algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm,"INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("INPUT"), algorithmIOField1.getType());
        AlgorithmIOField algorithmIOField2 = createAlgorithmIOField(algorithm,"OUTPUT");
        Assert.assertNotEquals(0, algorithmIOField2.getId());
        Assert.assertEquals(AlgorithmFieldType.valueOf("OUTPUT"), algorithmIOField2.getType());

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.getBaseConfig(algorithm.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update_base_config: 256 not assigned in default permission
    @Test
    public void test137_updateBaseConfigWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update BaseConfig with the following call updateBaseConfig
        // huser to update BaseConfig needs the "read_base_config algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField1 = createAlgorithmIOField(algorithm,"INPUT");
        Assert.assertNotEquals(0, algorithmIOField1.getId());
        Date date = new Date();
        algorithmIOField1.setDescription("description edited in date: " + date);

        AlgorithmConfig algorithmConfig = new AlgorithmConfig();
        List<AlgorithmIOField> input = new ArrayList<>();
        input.add(algorithmIOField1);
        algorithmConfig.setInput(input);

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateBaseConfig(algorithm.getId(), algorithmConfig);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update_jar: 512 not assigned in default permission
    @Test
    public void test138_updateJarWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update jar file field with the following call updateJar
        // huser to update jar file needs the "update_jar algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());
        Assert.assertNull(algorithm.getJarName());
        File file = new File( System.getProperty( "karaf.data" )  );
        String jarFilePath = String.valueOf(file).replaceAll("/data", "/");
        File algorithmFile = new File(jarFilePath + "algorithm_test001.jar");

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateJar(algorithm.getId(),"", algorithmFile);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // Algorithm action update_io_field: 1024 not assigned in default permission
    @Test
    public void test139_updateInputFieldWithDefaultPermissionShouldFail() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        // huser, with default permission, tries to update input field with the following call updateIOField
        // huser to update ioField needs the "update_io_field algorithm" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Algorithm algorithm = createAlgorithm();
        Assert.assertNotEquals(0, algorithm.getId());

        AlgorithmIOField algorithmIOField = createAlgorithmIOField(algorithm,"INPUT");
        Assert.assertNotEquals(0, algorithmIOField.getId());
        algorithmIOField.setDescription("edit failed...");

        this.impersonateUser(algorithmRestApi, huser);
        Response restResponse = algorithmRestApi.updateIOField(algorithm.getId(), algorithmIOField);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     * SharedEntity
     *
     */


    // SharedEntity action find: 8 not assigned in default permission
    @Test
    public void test140_findByPKWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to find SharedEntity by primary key with the following call findByPK
        // ownerUser to find SharedEntity by primary key needs the "find shared-entity" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // SharedEntity action find: 8 not assigned in default permission
    @Test
    public void test141_findByEntityWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to find SharedEntity by entity with the following call findByEntity
        // ownerUser to find SharedEntity by entity needs the "find shared-entity" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi
                .findByEntity(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // SharedEntity action find: 8 not assigned in default permission
    @Test
    public void test142_findByUserWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to find SharedEntity by huser with the following call findByUser
        // ownerUser to find SharedEntity by huser needs the "find shared-entity" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.findByUser(huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // SharedEntity action find: 8 not assigned in default permission
    @Test
    public void test143_getUsersWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to find users shared by the entity with the following call getUsers
        // ownerUser to find users shared by the entity needs the "find shared-entity" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi
                .getUsers(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // SharedEntity action find-all: 16 not assigned in default permission
    @Test
    public void test144_findAllSharedEntityWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to find all SharedEntity with the following call findAllSharedEntity
        // ownerUser to find all SharedEntity needs the "find-all shared-entity" permission
        // response status code '403' HyperIoTUnauthorizedException
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        HUser huser = huserRegistration(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
        Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
        Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
        Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.findAllSharedEntity();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    // SharedEntity action find-all: 16 not assigned in default permission
    @Test
    public void test145_findAllSharedEntityPaginatedWithDefaultPermissionShouldFail() {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        // ownerUser, with default permission, tries to find all SharedEntity with pagination with the following call findAllSharedEntityPaginated
        // ownerUser to find all SharedEntity needs the "find-all shared-entity" permission
        // response status code '403' HyperIoTUnauthorizedException
        Integer delta = 5;
        Integer page = 2;
        HUser ownerUser = huserRegistration(true);
        Assert.assertNotEquals(0, ownerUser.getId());
        Assert.assertTrue(ownerUser.isActive());

        HProject hproject = createHProject(ownerUser);
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

        List<HUser> husers = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            HUser huser = huserRegistration(true);
            Assert.assertNotEquals(0, huser.getId());
            Assert.assertTrue(huser.isActive());
            husers.add(huser);
        }

        List<SharedEntity> sharedEntities = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
            Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
            Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
            Assert.assertEquals(hProjectResourceName, sharedEntity.getEntityResourceName());
            sharedEntities.add(sharedEntity);
        }
        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */


    private HUser huserRegistration(boolean isActive) {
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hUserRestApi, adminUser);
        String username = "TestUser";
        List<Object> roles = new ArrayList<>();
        HUser huser = new HUser();
        huser.setName("name");
        huser.setLastname("lastname");
        huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
        huser.setEmail(huser.getUsername() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        huser.setActive(false);
        Assert.assertNull(huser.getActivateCode());
        Response restResponse = hUserRestApi.register(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
        Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
        Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
        Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
        Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
        Assert.assertFalse(huser.isAdmin());
        Assert.assertFalse(huser.isActive());
        Assert.assertTrue(roles.isEmpty());
        if (isActive) {
            //Activate huser and checks if default role has been assigned
            Role role = null;
            Assert.assertFalse(huser.isActive());
            String activationCode = huser.getActivateCode();
            Assert.assertNotNull(activationCode);
            Response restResponseActivateUser = hUserRestApi.activate(huser.getEmail(), activationCode);
            Assert.assertEquals(200, restResponseActivateUser.getStatus());
            huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
            roles = Arrays.asList(huser.getRoles().toArray());
            Assert.assertFalse(roles.isEmpty());
            Assert.assertTrue(huser.isActive());

            // checks: default role has been assigned to new huser
            Assert.assertEquals(1, huser.getRoles().size());
            Assert.assertEquals(roles.size(), huser.getRoles().size());
            Assert.assertFalse(roles.isEmpty());
            for (int i = 0; i < roles.size(); i++){
                role = ((Role) roles.get(i));
            }
            Assert.assertNotNull(role);
            Assert.assertEquals("RegisteredUser", role.getName());
            Assert.assertEquals("Role associated with the registered user",
                    role.getDescription());
            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
            Assert.assertFalse(listPermissions.isEmpty());
            Assert.assertEquals(12, listPermissions.size());
            boolean resourceNameArea = false;
            boolean resourceNameAlgorithm = false;
            boolean resourceNameAssetCategory = false;
            boolean resourceNameAssetTag = false;
            boolean resourceNameHProject = false;
            boolean resourceNameHDevice = false;
            boolean resourceNameHPacket = false;
            boolean resourceNameHProjectAlgorithm = false;
            boolean resourceNameRule = false;
            boolean resourceNameDashboard = false;
            boolean resourceNameDashboardWidget = false;
            boolean resourceNameWidget = false;
            for (int i = 0; i < listPermissions.size(); i++) {
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAlgorithm)) {
                    resourceNameAlgorithm = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.algorithm.model.Algorithm", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionAlgorithm + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(16, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionArea)) {
                    resourceNameArea = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.area.model.Area", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionArea + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetCategory)) {
                    resourceNameAssetCategory = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.asset.category.model.AssetCategory", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionAssetCategory + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetTag)) {
                    resourceNameAssetTag = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.asset.tag.model.AssetTag", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionAssetTag + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHProject)) {
                    resourceNameHProject = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.hproject.model.HProject", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionHProject + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(262143, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHDevice)) {
                    resourceNameHDevice = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.hdevice.model.HDevice", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionHDevice + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHPacket)) {
                    resourceNameHPacket = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.hpacket.model.HPacket", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionHPacket + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionHProjectAlgorithm)) {
                    resourceNameHProjectAlgorithm = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.hproject.algorithm.model.HProjectAlgorithm", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionHProjectAlgorithm + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionRule)) {
                    resourceNameRule = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.rule.model.Rule", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionRule + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionDashboard)) {
                    resourceNameDashboard = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.dashboard.model.Dashboard", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionDashboard + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(63, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionDashboardWidget)) {
                    resourceNameDashboardWidget = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionDashboardWidget + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionWidget)) {
                    resourceNameWidget = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.widget.model.Widget", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionWidget + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(24, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
            }
            Assert.assertTrue(resourceNameAlgorithm);
            Assert.assertTrue(resourceNameArea);
            Assert.assertTrue(resourceNameAssetCategory);
            Assert.assertTrue(resourceNameAssetTag);
            Assert.assertTrue(resourceNameHProject);
            Assert.assertTrue(resourceNameHDevice);
            Assert.assertTrue(resourceNameHPacket);
            Assert.assertTrue(resourceNameHProjectAlgorithm);
            Assert.assertTrue(resourceNameRule);
            Assert.assertTrue(resourceNameDashboard);
            Assert.assertTrue(resourceNameDashboardWidget);
            Assert.assertTrue(resourceNameWidget);
        }
        return huser;
    }


    private HProject createHProject(HUser huser) {
        // it.acsoftware.hyperiot.hproject.model.HProject (262143)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        // algorithms_management    32
        // areas_management         64
        // device_list              128
        // manage_rules             256
        // get_topology_list        512
        // get_topology             1024
        // activate_topology        2048
        // deactivate_topology      4096
        // rebalance_toppology      8192
        // get_toppology_errors     16384
        // kill_toppology           32768
        // add_toppology            65536
        // submit_toppology         131072
        // share                    262144      not assigned
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        this.impersonateUser(hprojectRestService, huser);
        HProject hproject = new HProject();
        hproject.setName("Project "+ UUID.randomUUID() +" of user: " + huser.getUsername());
        Date date = new Date();
        hproject.setDescription("Description inserted in date: " + date);
        hproject.setUser(huser);
        Response restResponseSaveProject = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponseSaveProject.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponseSaveProject.getEntity()).getId());
        Assert.assertEquals(hproject.getName(),
                ((HProject) restResponseSaveProject.getEntity()).getName());
        Assert.assertEquals("Description inserted in date: " + date,
                ((HProject) restResponseSaveProject.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponseSaveProject.getEntity()).getUser().getId());
        return hproject;
    }


    private HDevice createHDevice(HUser huser, HProject hproject) {
        // it.acsoftware.hyperiot.hdevice.model.HDevice (63)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        // packets_management       32
        HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        HDevice hdevice = new HDevice();
        hdevice.setBrand("Brand");
        hdevice.setDescription("Description");
        hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hdevice.setFirmwareVersion("1.");
        hdevice.setModel("model");
        hdevice.setPassword("passwordPass&01");
        hdevice.setPasswordConfirm("passwordPass&01");
        hdevice.setSoftwareVersion("1.");
        hdevice.setAdmin(false);
        hdevice.setProject(hproject);

        this.impersonateUser(hDeviceRestApi, huser);
        Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HDevice) restResponse.getEntity()).getId());
        Assert.assertEquals("Brand",
                ((HDevice) restResponse.getEntity()).getBrand());
        Assert.assertEquals("Description",
                ((HDevice) restResponse.getEntity()).getDescription());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getFirmwareVersion());
        Assert.assertEquals("model",
                ((HDevice) restResponse.getEntity()).getModel());
        Assert.assertEquals("1.",
                ((HDevice) restResponse.getEntity()).getSoftwareVersion());
        Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
        Assert.assertEquals(hproject.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((HDevice) restResponse.getEntity()).getProject().getUser().getId());
        return hdevice;
    }


    private HPacket createHPacketAndAddHPacketField(HUser huser, HDevice hdevice, boolean createField) {
        // it.acsoftware.hyperiot.hpacket.model.HPacket (31)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        HPacket hpacket = new HPacket();
        hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        hpacket.setDevice(hdevice);
        hpacket.setFormat(HPacketFormat.JSON);
        hpacket.setSerialization(HPacketSerialization.AVRO);
        hpacket.setType(HPacketType.IO);
        hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));

        hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
        Date timestamp = new Date();
        hpacket.setTimestampField(String.valueOf(timestamp));
        hpacket.setTimestampFormat("String");

        this.impersonateUser(hPacketRestApi, huser);
        Response restResponse = hPacketRestApi.saveHPacket(hpacket);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HPacket) restResponse.getEntity()).getId());
        Assert.assertEquals(hdevice.getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(hdevice.getProject().getId(),
                ((HPacket) restResponse.getEntity()).getDevice().getProject().getId());

        //add hpacketField
        if (createField) {
            HPacketField field1 = new HPacketField();
            field1.setPacket(hpacket);
            field1.setName("temperature" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
            field1.setDescription("Temperature");
            field1.setType(HPacketFieldType.DOUBLE);
            field1.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field1.setValue(24.0);

            HPacketField field2 = new HPacketField();
            field2.setPacket(hpacket);
            field2.setName("humidity" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
            field2.setDescription("Humidity");
            field2.setType(HPacketFieldType.DOUBLE);
            field2.setMultiplicity(HPacketFieldMultiplicity.SINGLE);
            field2.setValue(40.00);

            hpacket.setFields(new ArrayList<HPacketField>() {
                {
                    add(field1);
                    add(field2);
                }
            });

            Assert.assertEquals(0,
                    ((HPacket) restResponse.getEntity()).getFields().get(0).getId());
            Assert.assertEquals(0,
                    ((HPacket) restResponse.getEntity()).getFields().get(1).getId());

            // add field1
            this.impersonateUser(hPacketRestApi, huser);
            Response responseAddField1 = hPacketRestApi.addHPacketField(hpacket.getId(), field1);
            Assert.assertEquals(200, responseAddField1.getStatus());
            // field1 has been saved
            Assert.assertNotEquals(0,
                    ((HPacket) restResponse.getEntity()).getFields().get(0).getId());
            // field2 hasn't been saved
            Assert.assertEquals(0,
                    ((HPacket) restResponse.getEntity()).getFields().get(1).getId());

            //check restSaveHPacket field1 is equals to responseAddField1 field1
            Assert.assertEquals(field1.getId(),
                    ((HPacketField) responseAddField1.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getFields().get(0).getId(),
                    ((HPacketField) responseAddField1.getEntity()).getId());

            Assert.assertEquals(((HPacketField) responseAddField1.getEntity()).getPacket().getId(),
                    ((HPacket) restResponse.getEntity()).getId());

            // add field2
            Response responseAddField2 = hPacketRestApi.addHPacketField(hpacket.getId(), field2);
            Assert.assertEquals(200, responseAddField2.getStatus());

            // field2 has been saved
            Assert.assertNotEquals(0,
                    ((HPacket) restResponse.getEntity()).getFields().get(1).getId());
            //check restSaveHPacket field2 is equals to responseAddField2 field2
            Assert.assertEquals(field2.getId(),
                    ((HPacketField) responseAddField2.getEntity()).getId());
            Assert.assertEquals(((HPacket) restResponse.getEntity()).getFields().get(1).getId(),
                    ((HPacketField) responseAddField2.getEntity()).getId());

            Assert.assertEquals(((HPacketField) responseAddField2.getEntity()).getPacket().getId(),
                    ((HPacket) restResponse.getEntity()).getId());
            Assert.assertEquals(2, ((HPacket) restResponse.getEntity()).getFields().size());
        }
        return hpacket;
    }


    private Area createArea(HUser huser, HProject hproject) {
        // it.acsoftware.hyperiot.area.model.Area (63)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        // area_device_manager      32
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = new Area();
        area.setName("Area " + java.util.UUID.randomUUID());
        area.setDescription("Description");
        area.setProject(hproject);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.saveArea(area);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Area) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getName(), ((Area) restResponse.getEntity()).getName());
        Assert.assertEquals("Description", ((Area) restResponse.getEntity()).getDescription());
        Assert.assertEquals(hproject.getId(), ((Area) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(huser.getId(), ((Area) restResponse.getEntity()).getProject().getUser().getId());
        return area;
    }


    private Area createAreaAndSetAreaImage(HUser huser, HProject hproject) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);

        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Area area = createArea(huser, hproject);
        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(hproject.getId(), area.getProject().getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        Assert.assertNull(area.getImagePath());

        // tries to create attachment file
        String octetStream = "attachment; filename=\"" + areaImageName + "\"";
        ContentDisposition applicationOctetStream = new ContentDisposition(octetStream);
        File file = new File( System.getProperty( "karaf.etc" )  );
        String areaImagePath = String.valueOf(file).replaceAll("/etc", "/");
        FileInputStream imageFile = null;
        try {
            imageFile = new FileInputStream(areaImagePath + areaImageName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String fileExtension = areaImageName.substring(areaImageName.lastIndexOf(".") + 1);
        Attachment jpgAttachment = new Attachment(fileExtension, imageFile, applicationOctetStream);

        this.impersonateUser(areaRestApi, huser);
        Response restResponseImage = areaRestApi.setAreaImage(area.getId(), jpgAttachment);
        Assert.assertEquals(200, restResponseImage.getStatus());
        Assert.assertEquals(area.getEntityVersion() + 1,
                ((Area) restResponseImage.getEntity()).getEntityVersion());
        Assert.assertNotNull(((Area) restResponseImage.getEntity()).getImagePath());

        String newImageName = String.valueOf(area.getId()).concat("_img.").concat(fileExtension);
        Assert.assertTrue(((Area) restResponseImage.getEntity()).getImagePath().contains(newImageName));
        return (Area) restResponseImage.getEntity();
    }


    private AreaDevice createAreaDevice(HUser huser, Area area, HDevice hdevice) {
        AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Assert.assertNotEquals(0, hdevice.getId());
        Assert.assertEquals(huser.getId(), hdevice.getProject().getUser().getId());

        Assert.assertNotEquals(0, area.getId());
        Assert.assertEquals(huser.getId(), area.getProject().getUser().getId());

        AreaDevice areaDevice = new AreaDevice();
        areaDevice.setDevice(hdevice);
        this.impersonateUser(areaRestApi, huser);
        Response restResponse = areaRestApi.addAreaDevice(area.getId(), areaDevice);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((AreaDevice) restResponse.getEntity()).getId());
        Assert.assertEquals(area.getId(),
                ((AreaDevice) restResponse.getEntity()).getArea().getId());
        Assert.assertEquals(hdevice.getId(),
                ((AreaDevice) restResponse.getEntity()).getDevice().getId());
        Assert.assertEquals(area.getProject().getUser().getId(),
                ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        Assert.assertEquals(hdevice.getProject().getUser().getId(),
                ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        Assert.assertEquals(area.getProject().getUser().getId(),
                hdevice.getProject().getUser().getId());
        Assert.assertEquals(((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId(),
                ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((AreaDevice) restResponse.getEntity()).getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((AreaDevice) restResponse.getEntity()).getArea().getProject().getUser().getId());
        return areaDevice;
    }


    private Rule createRuleEngine(HUser huser, HProject hproject, HPacket hpacket) {
        // it.acsoftware.hyperiot.rule.model.Rule (31)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        RuleEngineRestApi ruleEngineRestApi = getOsgiService(RuleEngineRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Assert.assertNotEquals(0, hpacket.getId());
        Assert.assertEquals(hproject.getId(), hpacket.getDevice().getProject().getId());
        Assert.assertEquals(huser.getId(), hpacket.getDevice().getProject().getUser().getId());

        Rule rule = new Rule();
        rule.setName("Add category rule " + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        rule.setDescription("Everybody wants to rule the world.");
        rule.setType(RuleType.EVENT);
        rule.setRuleDefinition("temperature >= 23 AND humidity > 40");
        rule.setProject(hproject);
        rule.setPacket(hpacket);
        AddCategoryRuleAction categoryAction = new AddCategoryRuleAction();
        categoryAction.setCategoryIds(new long[]{123});
        List<RuleAction> actions = new ArrayList<>();
        actions.add(categoryAction);
        rule.setActions(actions);

        this.impersonateUser(ruleEngineRestApi, huser);
        Response restResponse = ruleEngineRestApi.saveRule(rule);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Rule) restResponse.getEntity()).getId());
        Assert.assertEquals(rule.getName(), ((Rule) restResponse.getEntity()).getName());
        Assert.assertEquals("Everybody wants to rule the world.",
                ((Rule) restResponse.getEntity()).getDescription());
        Assert.assertEquals("temperature >= 23 AND humidity > 40",
                ((Rule) restResponse.getEntity()).getRuleDefinition());
        Assert.assertEquals("it.acsoftware.hyperiot.rules.events",
                ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals(rule.getType().getDroolsPackage(),
                ((Rule) restResponse.getEntity()).getType().getDroolsPackage());
        Assert.assertEquals("EVENT",
                ((Rule) restResponse.getEntity()).getType().name());
        Assert.assertEquals(rule.getType().name(),
                ((Rule) restResponse.getEntity()).getType().name());

        Assert.assertEquals(hproject.getId(),
                ((Rule) restResponse.getEntity()).getProject().getId());
        Assert.assertEquals(hproject.getUser().getId(),
                ((Rule) restResponse.getEntity()).getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((Rule) restResponse.getEntity()).getProject().getUser().getId());

        Assert.assertEquals(hpacket.getId(),
                ((Rule) restResponse.getEntity()).getPacket().getId());
        Assert.assertEquals(hpacket.getDevice().getProject().getUser().getId(),
                ((Rule) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
        Assert.assertEquals(huser.getId(),
                ((Rule) restResponse.getEntity()).getPacket().getDevice().getProject().getUser().getId());
        return rule;
    }


    private Dashboard createDashboard(HUser huser, HProject hproject, String dashboardType) {
        // it.acsoftware.hyperiot.dashboard.model.Dashboard (63)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        // find_widgets             32
        DashboardRestApi dashboardRestApi = getOsgiService(DashboardRestApi.class);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertNotEquals(0, hproject.getId());
        Assert.assertEquals(huser.getId(), hproject.getUser().getId());

        Dashboard dashboard = new Dashboard();
        if (dashboardType == "OFFLINE") {
            dashboard.setName(hproject.getName() + " Offline Dashboard");
            dashboard.setDashboardType(DashboardType.OFFLINE);
            dashboard.setHProject(hproject);
            this.impersonateUser(dashboardRestApi, huser);
            Response restResponse = dashboardRestApi.saveDashboard(dashboard);
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
            Assert.assertEquals(hproject.getName() + " Offline Dashboard",
                    ((Dashboard) restResponse.getEntity()).getName());
            Assert.assertEquals("OFFLINE",
                    ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getId());
            Assert.assertEquals(huser.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
        }
        if (dashboardType == "REALTIME") {
            dashboard.setName(hproject.getName() + " Online Dashboard");
            dashboard.setDashboardType(DashboardType.REALTIME);
            dashboard.setHProject(hproject);
            this.impersonateUser(dashboardRestApi, huser);
            Response restResponse = dashboardRestApi.saveDashboard(dashboard);
            Assert.assertEquals(200, restResponse.getStatus());
            Assert.assertNotEquals(0, ((Dashboard) restResponse.getEntity()).getId());
            Assert.assertEquals(hproject.getName() + " Online Dashboard",
                    ((Dashboard) restResponse.getEntity()).getName());
            Assert.assertEquals("REALTIME",
                    ((Dashboard) restResponse.getEntity()).getDashboardType().getType());
            Assert.assertEquals(hproject.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getId());
            Assert.assertEquals(huser.getId(),
                    ((Dashboard) restResponse.getEntity()).getHProject().getUser().getId());
        }
        if (dashboardType != "OFFLINE" && dashboardType != "REALTIME") {
            Assert.assertEquals(0, dashboard.getId());
            System.out.println("dashboardType is null, dashboard not created...");
            System.out.println("allowed values: OFFLINE, REALTIME");
            return null;
        }
        return dashboard;
    }


    private DashboardWidget createDashboardWidget(HUser huser, Dashboard dashboard) {
        //it.acsoftware.hyperiot.dashboard.widget.model.DashboardWidget (31)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        DashboardWidgetRestApi dashboardWidgetRestApi = getOsgiService(DashboardWidgetRestApi.class);

        DashboardWidget dashboardWidget = new DashboardWidget();
        dashboardWidget.setWidgetConf("{\"description\":\"it's a simple test description of " + dashboard.getName() + "\"}");
        dashboardWidget.setDashboard(dashboard);

        this.impersonateUser(dashboardWidgetRestApi, huser);
        Response restResponse = dashboardWidgetRestApi.saveDashboardWidget(dashboardWidget);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((DashboardWidget) restResponse.getEntity()).getId());
        Assert.assertEquals(dashboardWidget.getDashboard().getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals("{\"description\":\"it's a simple test description of " + dashboard.getName() + "\"}",
                ((DashboardWidget) restResponse.getEntity()).getWidgetConf());
        Assert.assertEquals(dashboard.getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getId());
        Assert.assertEquals(dashboardWidget.getDashboard().getHProject().getUser().getId(),
                ((DashboardWidget) restResponse.getEntity()).getDashboard().getHProject().getUser().getId());
        return dashboardWidget;
    }


    private Widget createWidget() {
        // it.acsoftware.hyperiot.widget.model.Widget (24)
        // find                     8
        // find_all                 16
        WidgetRestApi widgetRestApi = getOsgiService(WidgetRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Widget widget = new Widget();
        widget.setName("image-data" + UUID.randomUUID().toString().replaceAll("-", ""));
        widget.setDescription(widgetDescription);
        widget.setWidgetCategory(WidgetCategory.ALL);
        widget.setBaseConfig("image-data");
        widget.setType("image-data");
        widget.setCols(2);
        widget.setRows(3);
        widget.setImage(widgetImageData);
        widget.setPreView(widgetImageDataPreview);
        widget.setOffline(false);
        widget.setRealTime(true);

        this.impersonateUser(widgetRestApi, adminUser);
        Response restResponse = widgetRestApi.saveWidget(widget);
        Assert.assertEquals(200, restResponse.getStatus());

        Assert.assertNotEquals(0, ((Widget) restResponse.getEntity()).getId());
        Assert.assertEquals(widget.getName(),
                ((Widget) restResponse.getEntity()).getName());
        Assert.assertEquals(widgetDescription,
                ((Widget) restResponse.getEntity()).getDescription());
        Assert.assertEquals(0,
                ((Widget) restResponse.getEntity()).getWidgetCategory().getId());
        Assert.assertEquals("all",
                ((Widget) restResponse.getEntity()).getWidgetCategory().getName());
        Assert.assertEquals("icon-hyt_layout",
                ((Widget) restResponse.getEntity()).getWidgetCategory().getFontIcon());
        Assert.assertEquals("ALL",
                ((Widget) restResponse.getEntity()).getWidgetCategory().name());
        Assert.assertEquals(0,
                ((Widget) restResponse.getEntity()).getWidgetCategory().ordinal());

        Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getBaseConfig());
        Assert.assertEquals("image-data", ((Widget) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((Widget) restResponse.getEntity()).getCols());
        Assert.assertEquals(3, ((Widget) restResponse.getEntity()).getRows());
        Assert.assertEquals(widgetImageData, ((Widget) restResponse.getEntity()).getImage());
        Assert.assertEquals(widgetImageDataPreview, ((Widget) restResponse.getEntity()).getPreView());
        Assert.assertFalse(((Widget) restResponse.getEntity()).isOffline());
        Assert.assertTrue(((Widget) restResponse.getEntity()).isRealTime());
        return widget;
    }


    private AssetCategory createAssetCategory(HUser huser, HProject hproject) {
        // it.acsoftware.hyperiot.asset.category.model.AssetCategory (31)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category " + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName + java.util.UUID.randomUUID());
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(),
                ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceName(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(hproject.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
        return assetCategory;
    }


    private AssetTag createAssetTag(HUser huser, HProject hproject) {
        // it.acsoftware.hyperiot.asset.tag.model.AssetTag (31)
        // save                     1
        // update                   2
        // remove                   4
        // find                     8
        // find_all                 16
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag " + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName + java.util.UUID.randomUUID());
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetTag) restResponse.getEntity()).getId());
        Assert.assertEquals(assetTag.getName(),
                ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getOwner().getOwnerResourceName(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(hproject.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceId().longValue());
        Assert.assertEquals(huser.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getUserId());
        return assetTag;
    }

    // HProject         ok
    // HDevice          ok
    // HPacket          ok
    // Area             ok
    // Rule             ok
    // Dashboard        ok
    // DashboardWidget  ok
    // Widget           ok
    // AssetCategory    ok
    // AssetTag         ok


    private Role createRole() {
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(roleRestApi, adminUser);
        Role role = new Role();
        role.setName("Role" + UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestApi.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Role) restResponse.getEntity()).getId());
        Assert.assertEquals(role.getName(), ((Role) restResponse.getEntity()).getName());
        Assert.assertEquals("Description", ((Role) restResponse.getEntity()).getDescription());
        return role;
    }


    private Role createUserRole(HUser huser) {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(roleRestService, adminUser);
        Role role = createRole();
        huser.addRole(role);
        Response restResponse = roleRestService.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        return role;
    }


    private Permission createPermission(Role role) {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTCrudAction.FIND);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName() + UUID.randomUUID());
        permission.setRole(role);

        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Permission) restResponse.getEntity()).getId());
        Assert.assertEquals(permission.getName(), ((Permission) restResponse.getEntity()).getName());
        Assert.assertEquals(permission.getActionIds(), ((Permission) restResponse.getEntity()).getActionIds());
        Assert.assertEquals(permission.getEntityResourceName(), ((Permission) restResponse.getEntity()).getEntityResourceName());
        Assert.assertEquals(role.getId(), ((Permission) restResponse.getEntity()).getRole().getId());
        return permission;
    }


    private Company createCompany(HUser huser) {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Company) restResponse.getEntity()).getId());
        Assert.assertEquals("ACSoftware", ((Company) restResponse.getEntity()).getBusinessName());
        Assert.assertEquals("Lamezia Terme", ((Company) restResponse.getEntity()).getCity());
        Assert.assertEquals("Lamezia Terme", ((Company) restResponse.getEntity()).getInvoiceAddress());
        Assert.assertEquals("Italy", ((Company) restResponse.getEntity()).getNation());
        Assert.assertEquals("88046", ((Company) restResponse.getEntity()).getPostalCode());
        Assert.assertEquals(company.getVatNumber(), ((Company) restResponse.getEntity()).getVatNumber());
        Assert.assertEquals(huser.getId(), ((Company) restResponse.getEntity()).getHUserCreator().getId());
        return company;
    }


    private Algorithm createAlgorithm() {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Algorithm algorithm = new Algorithm();
        algorithm.setName("algorithm " + UUID.randomUUID().toString().replaceAll("-", ""));
        algorithm.setDescription("everybody wants to rule the world");
        // set baseConfig with the default value: {"input":[],"output":[]}
        algorithm.setBaseConfig("{}");

        this.impersonateUser(algorithmRestApi, adminUser);
        Response restResponse = algorithmRestApi.saveAlgorithm(algorithm);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Algorithm) restResponse.getEntity()).getId());
        Assert.assertEquals(algorithm.getName(), ((Algorithm) restResponse.getEntity()).getName());
        Assert.assertEquals("everybody wants to rule the world", ((Algorithm) restResponse.getEntity()).getDescription());
        String defaultValue = "{\"input\":[],\"output\":[]}";
        Assert.assertEquals(defaultValue, ((Algorithm) restResponse.getEntity()).getBaseConfig());
        return algorithm;
    }


    private AlgorithmIOField createAlgorithmIOField(Algorithm algorithm, String algorithmFieldType) {
        AlgorithmRestApi algorithmRestApi = getOsgiService(AlgorithmRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        if (algorithm == null) {
            algorithm = createAlgorithm();
        }

        AlgorithmIOField algorithmIOField = new AlgorithmIOField();
        algorithmIOField.setName("IOFieldName " + UUID.randomUUID().toString().replaceAll("-", "") );
        algorithmIOField.setDescription("IOField description " + algorithm.getDescription());
        algorithmIOField.setFieldType(AlgorithmIOFieldType.LONG);
        algorithmIOField.setMultiplicity(AlgorithmIOFieldMultiplicity.SINGLE);

        if (algorithmFieldType == "INPUT") {
            algorithmIOField.setType(AlgorithmFieldType.INPUT);
            this.impersonateUser(algorithmRestApi, adminUser);
            Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
            Assert.assertEquals(200, restResponse.getStatus());
            //checks if inputField has been added in baseConfig
            String jsonInputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
            if ((((Algorithm) restResponse.getEntity()).getEntityVersion()-1) == algorithmIOField.getId()) {
                Assert.assertTrue(
                        jsonInputField.contains(
                                "{\"id\":"+algorithmIOField.getId()+","  +
                                        "\"name\":\""+algorithmIOField.getName()+"\"," +
                                        "\"description\":\""+algorithmIOField.getDescription()+"\"," +
                                        "\"fieldType\":\""+algorithmIOField.getFieldType()+"\"," +
                                        "\"multiplicity\":\""+algorithmIOField.getMultiplicity()+"\"," +
                                        "\"type\":\""+algorithmIOField.getType()+"\"}"
                        )
                );
            }
        }
        if (algorithmFieldType == "OUTPUT") {
            algorithmIOField.setType(AlgorithmFieldType.OUTPUT);
            this.impersonateUser(algorithmRestApi, adminUser);
            Response restResponse = algorithmRestApi.addIOField(algorithm.getId(), algorithmIOField);
            Assert.assertEquals(200, restResponse.getStatus());
            //checks if outputField has been added in baseConfig
            String jsonOutputField = ((Algorithm) restResponse.getEntity()).getBaseConfig();
            if ((((Algorithm) restResponse.getEntity()).getEntityVersion()-1) == algorithmIOField.getId()) {
                Assert.assertTrue(
                        jsonOutputField.contains(
                                "{\"id\":"+algorithmIOField.getId()+","  +
                                        "\"name\":\""+algorithmIOField.getName()+"\"," +
                                        "\"description\":\""+algorithmIOField.getDescription()+"\"," +
                                        "\"fieldType\":\""+algorithmIOField.getFieldType()+"\"," +
                                        "\"multiplicity\":\""+algorithmIOField.getMultiplicity()+"\"," +
                                        "\"type\":\""+algorithmIOField.getType()+"\"}"
                        )
                );
            }
        }
        if (algorithmFieldType != "INPUT" && algorithmFieldType != "OUTPUT") {
            Assert.assertEquals(0, algorithmIOField.getId());
            System.out.println("algorithmIOField is null, field not created...");
            System.out.println("allowed values: INPUT, OUTPUT");
            return null;
        }
        return algorithmIOField;
    }


    private SharedEntity createSharedEntity(HyperIoTBaseEntity hyperIoTBaseEntity, HUser ownerUser, HUser huser) {
        SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectResourceName,
                HyperIoTShareAction.SHARE);
        addPermission(ownerUser, action);

        SharedEntity sharedEntity = new SharedEntity();
        sharedEntity.setEntityId(hyperIoTBaseEntity.getId());
        sharedEntity.setEntityResourceName(hProjectResourceName); // "it.acsoftware.hyperiot.hproject.model.HProject"
        sharedEntity.setUserId(huser.getId());

        this.impersonateUser(sharedEntityRestApi, ownerUser);
        Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(hyperIoTBaseEntity.getId(),
                ((SharedEntity) restResponse.getEntity()).getEntityId());
        Assert.assertEquals(huser.getId(),
                ((SharedEntity) restResponse.getEntity()).getUserId());
        Assert.assertEquals(hProjectResourceName,
                ((SharedEntity) restResponse.getEntity()).getEntityResourceName());
        removePermission(ownerUser, action);
        return sharedEntity;
    }

    private Permission addPermission(HUser huser, HyperIoTAction action){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser user = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        huser.addRole(role);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        this.impersonateUser(roleRestApi, user);
        Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restUserRole.getStatus());
        Assert.assertTrue(huser.hasRole(role));
        Permission permission = utilGrantPermission(huser, role, action);
        Assert.assertNotEquals(0, permission.getId());
        Assert.assertEquals(action.getActionName(), permission.getName());
        Assert.assertEquals(action.getActionId(), permission.getActionIds());
        Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
        Assert.assertEquals(role.getId(), permission.getRole().getId());
        return permission;
    }

    private void removePermission(HUser huser, HyperIoTAction action){
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = null;
        List<Object> roles = Arrays.asList(huser.getRoles().toArray());
        if (!roles.isEmpty()) {
            Assert.assertFalse(roles.isEmpty());
            if (action != null) {
                for (int i = 0; i < roles.size(); i++){
                    if (((Role) roles.get(i)).getName() != "RegisteredUser") {
                        role = ((Role) roles.get(i));
                    }
                }
                PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
                Permission permission = permissionSystemApi.findByRoleAndResourceName(role, action.getResourceName());
                if (permission != null) {
                    PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
                    this.impersonateUser(permissionRestApi, adminUser);
                    permission.removePermission(action);
                    Response restResponseUpdate = permissionRestApi.deletePermission(permission.getId());
                    Assert.assertEquals(200, restResponseUpdate.getStatus());

                    huser.removeRole(permission.getRole());
                    RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
                    this.impersonateUser(roleRestApi, adminUser);
                    Response restUserRole = roleRestApi.deleteUserRole(role.getId(), huser.getId());
                    Assert.assertEquals(200, restUserRole.getStatus());
                    Assert.assertFalse(huser.hasRole(role));
                } else {
                    Assert.assertNull(permission);
                }
            } else {
                Assert.assertNull(action);
            }
        }
    }

    private Permission utilGrantPermission(HUser huser, Role role, HyperIoTAction action) {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        if (action == null) {
            Assert.assertNull(action);
            return null;
        } else {
            PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
            Permission testPermission = permissionSystemApi.findByRoleAndResourceName(role, action.getResourceName());
            if (testPermission == null) {
                Permission permission = new Permission();
                permission.setName(action.getActionName());
                permission.setActionIds(action.getActionId());
                permission.setEntityResourceName(action.getResourceName());
                permission.setRole(role);
                this.impersonateUser(permissionRestApi, adminUser);
                Response restResponse = permissionRestApi.savePermission(permission);
                testPermission = permission;
                Assert.assertEquals(200, restResponse.getStatus());
                Assert.assertNotEquals(0, ((Permission) restResponse.getEntity()).getId());
                Assert.assertEquals(testPermission.getName(), ((Permission) restResponse.getEntity()).getName());
                Assert.assertEquals(testPermission.getActionIds(), ((Permission) restResponse.getEntity()).getActionIds());
                Assert.assertEquals(testPermission.getEntityResourceName(), ((Permission) restResponse.getEntity()).getEntityResourceName());
                Assert.assertEquals(testPermission.getRole().getId(), ((Permission) restResponse.getEntity()).getRole().getId());
            } else {
                this.impersonateUser(permissionRestApi, adminUser);
                testPermission.addPermission(action);
                Response restResponseUpdate = permissionRestApi.updatePermission(testPermission);
                Assert.assertEquals(200, restResponseUpdate.getStatus());
                Assert.assertEquals(testPermission.getActionIds(), ((Permission) restResponseUpdate.getEntity()).getActionIds());
                Assert.assertEquals(testPermission.getEntityVersion() + 1,
                        ((Permission) restResponseUpdate.getEntity()).getEntityVersion());
            }
            Assert.assertTrue(huser.hasRole(role.getId()));
            return testPermission;
        }
    }

}
