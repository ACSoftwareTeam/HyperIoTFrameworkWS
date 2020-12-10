package it.acsoftware.hyperiot.permission.test;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
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
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.permission.test.HyperIoTPermissionConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTPermissionRestTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTPermissionConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//                .withDebug("5010", false)
                .append(getConfiguration()).build();
    }

    public void impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        restApi.impersonate(user);
    }

    @Test
    public void test00_hyperIoTFrameworkShouldBeInstalled() {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class);
        String features = executeCommand("feature:list -i");
        assertContains("HyperIoTBase-features ", features);
        assertContains("HyperIoTMail-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTCompany-features ", features);
        assertContains("HyperIoTRole-features ", features);
        assertContains("HyperIoTAssetCategory-features", features);
        assertContains("HyperIoTAssetTag-features", features);
        assertContains("HyperIoTSharedEntity-features", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_permissionModuleShouldWork() {
        PermissionRestApi permissionRestService = getOsgiService(PermissionRestApi.class);
        // the following call sayHi checks if Permission module working correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(permissionRestService, adminUser);
        Response response = permissionRestService.sayHi();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void test02_savePermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin save Permission with the following call savePermission
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test03_savePermissionShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to save Permission, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updatePermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin update Permission with the following call updatePermission
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test05_updatePermissionShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to update Permission, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Permission permission = createPermission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin find Permission with the following call findPermission
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test07_findPermissionShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to find Permission, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findPermissionShouldFailIfEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to find Permission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findPermission(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findAllPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin find all Permission with the following call findAllPermission
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermission();
        List<Permission> listPermissions = restResponse.readEntity(new GenericType<List<Permission>>() {
        });
        Assert.assertNotEquals(0, listPermissions.size());
        Assert.assertFalse(listPermissions.isEmpty());
        boolean permissionFound = false;
        for (Permission permissions : listPermissions) {
            if (permission.getId() == permissions.getId())
                permissionFound = true;
        }
        Assert.assertTrue(permissionFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test10_findAllPermissionShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to find all Permission, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.findAllPermission();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test11_deletePermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin delete Permission with the following call deletePermission
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.deletePermission(permission.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test12_deletePermissionShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to delete Permission, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.deletePermission(permission.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deletePermissionShouldFailIfEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to delete Permission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.deletePermission(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_savePermissionShouldFailIfNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName("</script>");
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test15_savePermissionShouldFailIfNameIsBlank() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // name is blank
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName("");
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be blank",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test16_savePermissionShouldFailIfActionIdsIsNegative() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // actionIds is not a positive number
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(-1);
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must be greater than 0",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test17_savePermissionShouldFailIfEntityResourceNameIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // entityResourceName is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(null);
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean msgValidationErrorsIsNull = false;
        boolean msgValidationErrorsIsEmpty = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be null")) {
                msgValidationErrorsIsNull = true;
                Assert.assertEquals("must not be null",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test18_savePermissionShouldFailIfEntityResourceNameIsEmpty() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // entityResourceName is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName("");
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test19_savePermissionShouldFailIfEntityResourceNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // entityResourceName is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName("eval(malicious code)");
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test20_savePermissionShouldFailIfRoleIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // Role is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(null);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test21_updatePermissionShouldFailIfNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setName("expression(malicious code)");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test22_updatePermissionShouldFailIfNameIsBlank() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but name is blank
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setName(" ");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be blank",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test23_updatePermissionShouldFailIfActionIdsIsNegative() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but actionIds is not a positive number
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setActionIds(-1);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must be greater than 0",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test24_updatePermissionShouldFailIfEntityResourceNameIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but entityResourceName is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setEntityResourceName(null);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean msgValidationErrorsIsNull = false;
        boolean msgValidationErrorsIsEmpty = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be null")) {
                msgValidationErrorsIsNull = true;
                Assert.assertEquals("must not be null",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test25_updatePermissionShouldFailIfEntityResourceNameIsEmpty() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but entityResourceName is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setEntityResourceName("");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test26_updatePermissionShouldFailIfEntityResourceNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but entityResourceName is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setEntityResourceName("</script>");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test27_updatePermissionShouldFailIfRoleIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but Role is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        permission.setRole(null);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test28_findAllActionsShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin find all Actions with the following call findAllActions
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllActions();
        HashMap<String, List<HyperIoTAction>> actions = restResponse
                .readEntity(new GenericType<HashMap<String, List<HyperIoTAction>>>() {
                });
        Assert.assertFalse(actions.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test29_findAllActionsShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to find all Actions, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.findAllActions();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test30_findAllPermissionPaginationShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, hadmin find
        // all Permissions with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        List<Permission> permissions = new ArrayList<>();
        Integer delta = 10;
        Integer page = 1;
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, page);
        HyperIoTPaginableResult<Permission> listPermissions = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Permission>>() {
                });
        Assert.assertEquals(10, listPermissions.getResults().size());
        Assert.assertFalse(listPermissions.getResults().isEmpty());
        Assert.assertEquals(1, listPermissions.getCurrentPage());
        Assert.assertEquals(10, listPermissions.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test31_findAllPermissionsPaginationShouldWorkIfDeltaAndPageAreNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, hadmin find
        // all Permissions with pagination, if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, page);
        HyperIoTPaginableResult<Permission> listPermissions = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Permission>>() {
                });
        Assert.assertEquals(10, listPermissions.getResults().size());
        Assert.assertFalse(listPermissions.getResults().isEmpty());
        Assert.assertEquals(1, listPermissions.getCurrentPage());
        Assert.assertEquals(10, listPermissions.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test32_findAllPermissionsPaginationShouldWorkIfDeltaIsLowerThanZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, hadmin find
        // all Permissions with pagination, if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer page = 2;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(-1, page);
        HyperIoTPaginableResult<Permission> listPermissions = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Permission>>() {
                });
        Assert.assertEquals(10, listPermissions.getResults().size());
        Assert.assertFalse(listPermissions.getResults().isEmpty());
        Assert.assertEquals(2, listPermissions.getCurrentPage());
        Assert.assertEquals(10, listPermissions.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test33_findAllPermissionsPaginationShouldWorkIfDeltaIsZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, hadmin find
        // all Permissions with pagination, if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer page = 3;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(0, page);
        HyperIoTPaginableResult<Permission> listPermissions = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Permission>>() {
                });
        Assert.assertEquals(10, listPermissions.getResults().size());
        Assert.assertFalse(listPermissions.getResults().isEmpty());
        Assert.assertEquals(3, listPermissions.getCurrentPage());
        Assert.assertEquals(10, listPermissions.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test34_findAllPermissionsPaginationShouldWorkIfPageIsLowerThanZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, hadmin find
        // all Permissions with pagination, if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = 5;
        Integer page = -1;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, page);
        HyperIoTPaginableResult<Permission> listPermissions = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Permission>>() {
                });
        Assert.assertEquals(5, listPermissions.getResults().size());
        Assert.assertFalse(listPermissions.getResults().isEmpty());
        Assert.assertEquals(1, listPermissions.getCurrentPage());
        Assert.assertEquals(5, listPermissions.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test35_findAllPermissionsPaginationShouldWorkIfPageIsZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, hadmin find
        // all Permissions with pagination, if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = 5;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, 0);
        HyperIoTPaginableResult<Permission> listPermissions = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Permission>>() {
                });
        Assert.assertEquals(5, listPermissions.getResults().size());
        Assert.assertFalse(listPermissions.getResults().isEmpty());
        Assert.assertEquals(1, listPermissions.getCurrentPage());
        Assert.assertEquals(5, listPermissions.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test36_findAllPermissionPaginationShouldFailIfNotLogged() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // the following call tries to find all Permission with pagination, but HUser is
        // not logged
        // response status code '403' HyperIoTUnauthorizedException
        List<Permission> permissions = new ArrayList<>();
        Integer delta = 10;
        Integer page = 1;
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, null);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test37_savePermissionShouldFailIfEntityIsDuplicated() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to save Permission with the following call savePermission, but
        // entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = createPermission();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        Permission duplicatePermission = new Permission();
        duplicatePermission.setName(action.getActionName());
        duplicatePermission.setActionIds(action.getActionId());
        duplicatePermission.setEntityResourceName(permission.getEntityResourceName());
        duplicatePermission.setRole(permission.getRole());
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.savePermission(duplicatePermission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean roleIdIsDuplicated = false;
        boolean entityResourceNameIsDuplicated = false;
        boolean resourceIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("role_id")) {
                roleIdIsDuplicated = true;
                Assert.assertEquals("role_id",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("entityResourceName")) {
                entityResourceNameIsDuplicated = true;
                Assert.assertEquals("entityResourceName",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("resourceId")) {
                resourceIdIsDuplicated = true;
                Assert.assertEquals("resourceId",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertTrue(roleIdIsDuplicated);
        Assert.assertTrue(entityResourceNameIsDuplicated);
        Assert.assertTrue(resourceIdIsDuplicated);
    }

    @Test
    public void test38_updatePermissionShouldFailIfEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // hadmin tries to update Permission with the following call updatePermission,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Permission permission = new Permission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    /*
     *
     *
     * UTILITY METHODS
     *
     *
     */

    private HUser createHUser() {
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
        huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        huser.setActive(true);
        Response restResponse = hUserRestApi.saveHUser(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
        Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
        Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
        Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
        Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
        Assert.assertFalse(huser.isAdmin());
        Assert.assertTrue(huser.isActive());
        Assert.assertTrue(roles.isEmpty());
        return huser;
    }

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

    private Permission createPermission() {
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser();
        Role role = createRole();
        huser.addRole(role);
        this.impersonateUser(hUserRestApi, adminUser);
        Response restResponse = roleRestApi.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
        return permission;
    }

}
