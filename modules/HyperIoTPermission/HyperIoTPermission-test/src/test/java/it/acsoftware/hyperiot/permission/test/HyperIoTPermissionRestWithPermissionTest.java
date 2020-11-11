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
import it.acsoftware.hyperiot.permission.actions.HyperIoTPermissionAction;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
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
import java.util.*;

import static it.acsoftware.hyperiot.permission.test.HyperIoTPermissionConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTPermissionRestWithPermissionTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTPermissionConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//                .withDebug("5010", false)
                .append(getBaseConfiguration()).build();
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
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTRole-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTMail-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTHBaseConnector-features", features);
        String datasource = executeCommand("jdbc:ds-list");
        // checks that datasource is installed correctly
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
    public void test02_saveWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, save Permission with the following call
        // savePermission
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test03_saveWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to save Permission with the following call
        // savePermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createRole();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
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

    @Test
    public void test04_updateWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, update Permission with the following call
        // updatePermission
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test05_updateWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to update Permission with the following call
        // updatePermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Permission permission = createPermission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, find Permission with the following call
        // findPermission
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test07_findWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to find Permission with the following call
        // findPermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findWithPermissionShouldFailIfEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, find Permission with the following call
        // findPermission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
//		createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findPermission(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findWithoutPermissionShouldFailAndEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to find Permission not found with the
        // following call findPermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
//		createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findPermission(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_deleteWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, delete Permission with the following call
        // deletePermission
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.deletePermission(permission.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_deleteWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to delete Permission with the following call
        // deletePermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Permission permission = createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.deletePermission(permission.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteWithPermissionShouldFailIfEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, delete Permission with the following call
        // deletePermission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
//		createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.deletePermission(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deleteWithoutPermissionShouldFailAndEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to delete Permission not found with the
        // following call deletePermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
//		createPermission();
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.deletePermission(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_findAllWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, find all Permission with the following call
        // findAllPermission
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        Assert.assertNotEquals(0, permission.getId());
        this.impersonateUser(permissionRestApi, huser);
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
    public void test15_findAllWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to find all Permission with the following
        // call findAllPermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Permission permission = createPermission();
        Assert.assertNotEquals(0, permission.getId());
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllPermission();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_saveWithPermissionShouldFailIfNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName("</script>");
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test17_saveWithPermissionShouldFailIfNameIsBlank() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but name is blank
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName(" ");
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be blank",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test18_saveWithPermissionShouldFailIfActionIdsIsNegative() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but actionIds is not a positive number
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(-1);
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must be greater than 0",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test19_saveWithPermissionShouldFailIfEntityResourceNameIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but entityResourceName is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(null);
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
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
    public void test20_saveWithPermissionShouldFailIfEntityResourceNameIsEmpty() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but entityResourceName is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName("");
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test21_saveWithPermissionShouldFailIfEntityResourceNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but entityResourceName is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName("vbscript:");
        permission.setRole(role);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test22_saveWithPermissionShouldFailIfRoleIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but Role is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Permission permission = new Permission();
        permission.setName(action.getActionName());
        permission.setActionIds(action.getActionId());
        permission.setEntityResourceName(action.getResourceName());
        permission.setRole(null);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.savePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test23_updateWithPermissionShouldFailIfNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setName("eval(malicious code)");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test24_updateWithPermissionShouldFailIfNameIsBlank() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but name is blank
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setName(" ");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be blank",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test25_updateWithPermissionShouldFailIfActionIdsIsNegative() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but actionIds is not a positive number
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setActionIds(-1);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must be greater than 0",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test26_updateWithPermissionShouldFailIfEntityResourceNameIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but entityResourceName is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setEntityResourceName(null);
        this.impersonateUser(permissionRestApi, huser);
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
    public void test27_updateWithPermissionShouldFailIfEntityResourceNameIsEmpty() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but entityResourceName is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setEntityResourceName("");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test28_updateWithPermissionShouldFailIfEntityResourceNameIsMaliciousCode() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but entityResourceName is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setEntityResourceName("</script>");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test29_updateWithPermissionShouldFailIfRoleIsNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but Role is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = createPermission();
        permission.setRole(null);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test30_findAllActionsWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, find all Actions with the following call
        // findAllActions
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTPermissionAction.LIST_ACTIONS);
        HUser huser = createHUser(action);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllActions();
        HashMap<String, List<HyperIoTAction>> actions = restResponse
                .readEntity(new GenericType<HashMap<String, List<HyperIoTAction>>>() {
                });
        Assert.assertFalse(actions.isEmpty());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test31_findAllActionsWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to find all Actions with the following call
        // findAllActions
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllActions();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test32_findAllPaginationWithPermissionShouldWork() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionPaginated, HUser, with permission,
        // find all Permissions with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        List<Permission> permissions = new ArrayList<>();
        Integer delta = 10;
        Integer page = 1;
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
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
    public void test33_findAllPaginationWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionsPaginated HUser, with permission,
        // find all Permissions with pagination if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
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
    public void test34_findAllPaginationWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionsPaginated HUser, with permission,
        // find all Permissions with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 2;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
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
    public void test35_findAllPaginationWithPermissionShouldWorkIfDeltaIsZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionsPaginated HUser, with permission,
        // find all Permissions with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 3;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
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
    public void test36_findAllPaginationWithPermissionShouldWorkIfPageIsLowerThanZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionsPaginated HUser, with permission,
        // find all Permissions with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = -1;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
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
    public void test37_findAllPaginationWithPermissionShouldWorkIfPageIsZero() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermissionsPaginated HUser, with permission,
        // find all Permissions with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        List<Permission> permissions = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
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
    public void test38_findAllPaginationWithoutPermissionShouldFail() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // In this following call findAllPermission, HUser, without permission, tries to
        // find all Permissions with pagination
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        List<Permission> permissions = new ArrayList<>();
        Integer delta = 10;
        Integer page = 1;
        for (int i = 0; i < 10; i++) {
            Permission permission = createPermission();
            permissions.add(permission);
        }
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.findAllPermissionPaginated(delta, page);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test39_saveWithPermissionShouldFailIfEntityIsDuplicated() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to save Permission with the following call
        // savePermission, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        Permission permission = createPermission();
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Permission duplicatePermission = new Permission();
        duplicatePermission.setName(action.getActionName());
        duplicatePermission.setActionIds(action.getActionId());
        duplicatePermission.setEntityResourceName(permission.getEntityResourceName());
        duplicatePermission.setRole(permission.getRole());
        this.impersonateUser(permissionRestApi, huser);
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
    public void test40_updateWithPermissionShouldFailIfEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, with permission, tries to update Permission with the following call
        // updatePermission, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(permissionResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Permission permission = new Permission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, huser);
        Response restResponse = permissionRestApi.updatePermission(permission);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test41_updateWithoutPermissionShouldFailAndEntityNotFound() {
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        // HUser, without permission, tries to update Permission not found with the
        // following call updatePermission
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Permission permission = new Permission();
        permission.setName("name edited");
        this.impersonateUser(permissionRestApi, huser);
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

    private HUser createHUser(HyperIoTAction action) {
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
        if (action != null) {
            Role role = createRole();
            huser.addRole(role);
            RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
            Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
            Assert.assertEquals(200, restUserRole.getStatus());
            Assert.assertTrue(huser.hasRole(role));
            roles = Arrays.asList(huser.getRoles().toArray());
            Assert.assertFalse(roles.isEmpty());
            utilGrantPermission(huser, role, action);
        }
        return huser;
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
        HUser huser = createHUser(action);
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
