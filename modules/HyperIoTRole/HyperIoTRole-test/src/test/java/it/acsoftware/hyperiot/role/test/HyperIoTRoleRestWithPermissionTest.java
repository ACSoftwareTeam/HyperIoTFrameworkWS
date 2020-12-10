package it.acsoftware.hyperiot.role.test;

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
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.actions.HyperIoTRoleAction;
import it.acsoftware.hyperiot.role.api.RoleRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.role.test.HyperIoTRoleConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTRoleRestWithPermissionTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTRoleConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration()
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
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTCompany-features ", features);
        assertContains("HyperIoTRole-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
        assertContains("HyperIoTAssetCategory-features ", features);
        assertContains("HyperIoTAssetTag-features ", features);
        assertContains("HyperIoTSharedEntity-features ", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_roleModuleShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // the following call checkModuleWorking checks if Role module working correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(roleRestService, adminUser);
        Response restResponse = roleRestService.checkModuleWorking();
        Assert.assertNotNull(roleRestService);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test02_saveRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, save Role with the following call saveRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test03_saveRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to save Role with the following call
        // saveRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(roleRestService, huser);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test04_updateRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, update Role with the following call updateRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setDescription("description edited");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test05_updateRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to update Role with the following call
        // updateRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createRole();
        role.setDescription("description edited");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, find Role with the following call findRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test07_findRoleWithPermissionShouldFailIfEntityNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to find Role with the following call findRole,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findRole(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to find Role with the following call
        // findRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findRoleWithoutPermissionShouldFailAndEntityNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to find Role not found with the following
        // call findRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findRole(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_findAllRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, find all Role with the following call findAllRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRoles();
        List<Role> listRoles = restResponse.readEntity(new GenericType<List<Role>>() {
        });
        Assert.assertFalse(listRoles.isEmpty());
        boolean roleFound = false;
        for (Role roles : listRoles) {
            if (role.getId() == roles.getId())
                roleFound = true;
        }
        Assert.assertTrue(roleFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_findAllRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to find all Role with the following call
        // findAllRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRoles();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, delete Role with the following call deleteRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test13_deleteRoleWithPermissionShouldFailIfEntityNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to delete Role with the following call
        // deleteRole, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteRole(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_deleteRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to delete Role with the following call
        // deleteRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteRole(role.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test15_deleteRoleWithoutPermissionShouldFailAndEntityNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to delete Role not found with the following
        // call deleteRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteRole(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_saveRoleWithPermissionShouldFailIfNameIsNull() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call
        // saveRole, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName(null);
        role.setDescription("Description");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
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
    public void test17_saveRoleWithPermissionShouldFailIfNameIsEmpty() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call
        // saveRole, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName("");
        role.setDescription("Description");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test18_saveRoleWithPermissionShouldFailIfNameIsMaliciousCode() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call
        // saveRole, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName("javascript:");
        role.setDescription("Description");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test19_saveRoleWithPermissionShouldFailIfDescriptionIsNull() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call
        // saveRole, but description is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test20_saveRoleWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call
        // saveRole, but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("vbscript:");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test21_saveRoleWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call
        // saveRole, but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription(testMaxDescription(3001));
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("length must be between 0 and 3000",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test22_updateRoleWithPermissionShouldFailIfNameIsNull() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setName(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
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
    public void test23_updateRoleWithPermissionShouldFailIfNameIsEmpty() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setName("");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test24_updateRoleWithPermissionShouldFailIfNameIsMaliciousCode() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setName("</script>");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test25_updateRoleWithPermissionShouldFailIfDescriptionIsNull() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but description is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setDescription(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test26_updateRoleWithPermissionShouldFailIfDescriptionIsMaliciousCode() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but description is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setDescription("vbscript:");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test27_updateRoleWithPermissionShouldFailIfMaxDescriptionIsOver3000Chars() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but description is over 3000 chars
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        role.setDescription(testMaxDescription(3001));
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("length must be between 0 and 3000",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test28_getUserRolesWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, find all user Role with the following call
        // findAllUserRoles
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllUserRoles(huser.getId());
        List<Role> listRoles = restResponse.readEntity(new GenericType<List<Role>>() {
        });
        Assert.assertFalse(listRoles.isEmpty());
        Assert.assertEquals(1, listRoles.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test29_getUserRolesWithPermissionShouldWorkIfUserNotHasRole() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, find all user Role with the following call
        // findAllUserRoles, huser2 not has Role and listRoles is empty.
        // response status code '200'
        HUser huser2 = createHUser(null);
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllUserRoles(huser2.getId());
        List<Role> listRoles = restResponse.readEntity(new GenericType<List<Role>>() {
        });
        Assert.assertTrue(listRoles.isEmpty());
        Assert.assertEquals(0, listRoles.size());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test30_getUserRolesWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to find all user Role with the following
        // call findAllUserRoles
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllUserRoles(huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test31_saveUserRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, save user Role with the following call saveUserRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.ASSIGN_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createRole();
        huser.addRole(role);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test32_saveUserRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to save user Role with the following call
        // saveUserRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createRole();
        huser.addRole(role);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test33_saveUserRoleWithPermissionShouldFailIfRoleNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save user Role with the following call
        // saveUserRole, but Role not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.ASSIGN_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createRole();
        huser.addRole(role);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveUserRole(0, huser.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test34_saveUserRoleWithPermissionShouldFailIfUserNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save user Role with the following call
        // saveUserRole, but HUser not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.ASSIGN_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createRole();
        huser.addRole(role);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveUserRole(role.getId(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test35_saveUserRoleWithPermissionShouldFailIfEntityIsDuplicated() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save user Role with the following call
        // saveUserRole, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.ASSIGN_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createUserRole(huser);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveUserRole(role.getId(), huser.getId());
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test36_deleteUserRoleWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, delete user Role with the following call
        // deleteUserRole
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.REMOVE_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createUserRole(huser);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteUserRole(role.getId(), huser.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test37_deleteUserRoleWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to delete user Role with the following call
        // deleteUserRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createUserRole(huser);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteUserRole(role.getId(), huser.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test38_deleteUserRoleWithPermissionShouldFailIfRoleNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to delete user Role with the following call
        // deleteUserRole, but Role not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.REMOVE_MEMBERS);
        HUser huser = createHUser(action);
        createUserRole(huser);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteUserRole(0, huser.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test39_deleteUserRoleWithPermissionShouldFailIfUserNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to delete user Role with the following call
        // deleteUserRole, but HUser not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.REMOVE_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createUserRole(huser);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteUserRole(role.getId(), 0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test40_deleteUserRoleWithPermissionShouldFailHUserNotHasRole() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to delete user Role with the following call
        // deleteUserRole, but HUser not has a role
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTRoleAction.REMOVE_MEMBERS);
        HUser huser = createHUser(action);
        Role role = createRole();
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteUserRole(role.getId(), huser.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test41_saveRoleWithPermissionShouldFailIfEntityIsDuplicated() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to save Role with the following call saveRole,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Role duplicateRole = new Role();
        duplicateRole.setName(role.getName());
        duplicateRole.setDescription("Description");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(duplicateRole);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                nameIsDuplicated = true;
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertTrue(nameIsDuplicated);
    }

    @Test
    public void test42_saveRoleWithoutPermissionShouldFailAndEntityIsDuplicated() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to save Role duplicated with the following
        // call saveRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = createRole();
        Role duplicateRole = new Role();
        duplicateRole.setName(role.getName());
        duplicateRole.setDescription("Description");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.saveRole(duplicateRole);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test43_updateRoleWithPermissionShouldFailIfEntityNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call
        // updateRole, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = new Role();
        role.setDescription("description edited");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test44_updateRoleWithoutPermissionShouldFailAndEntityNotFound() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, without permission, tries to update Role not found with the following
        // call updateRole
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Role role = new Role();
        role.setDescription("description edited");
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(role);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test45_updateRoleWithPermissionShouldFailIfEntityIsDuplicated() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, tries to update Role with the following call updateRole,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Role role = createRole();
        Role duplicateRole = createRole();
        duplicateRole.setName(role.getName());
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.updateRole(duplicateRole);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean nameIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                nameIsDuplicated = true;
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertTrue(nameIsDuplicated);
    }


    /*
     *
     *
     * CUSTOM TESTS
     *
     *
     */


    @Test
    public void test46_getUserRolesShouldFailIfDeleteUserAfterCallSaveUserRole() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // huser with permission FIND, tries to finds all user roles
        // if huser has been deleted after call saveUserRole
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        //Create HUser, save UserRole and assign FIND permission
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FIND);
        HUser huserWithPermission = createHUser(action);

        HUser huserWithRole = createHUser();
        Role role = createUserRole(huserWithRole);
        this.impersonateUser(roleRestService, huserWithPermission);
        Response restResponse = roleRestService.findAllUserRoles(huserWithRole.getId());
        List<Role> listRoles = restResponse.readEntity(new GenericType<List<Role>>() {
        });
        Assert.assertFalse(listRoles.isEmpty());
        Assert.assertEquals(1, listRoles.size());
        Assert.assertTrue(huserWithRole.hasRole(role.getId()));
        Assert.assertEquals(200, restResponse.getStatus());

        //hadmin deletes huser with call deleteHUser, not will deletes Role
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        this.impersonateUser(hUserRestApi, adminUser);
        Response restRemoveHUser = hUserRestApi.deleteHUser(huserWithRole.getId());
        Assert.assertEquals(200, restRemoveHUser.getStatus());

        // role is still stored in database
        this.impersonateUser(roleRestService, huserWithPermission);
        restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        //huser not found
        this.impersonateUser(roleRestService, huserWithPermission);
        restResponse = roleRestService.findAllUserRoles(huserWithRole.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test47_tryToFindRoleWithPermissionIfRoleHasBeenDeletedInCascadeModeShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // firstHUser deletes role in cascade mode, with call deleteRole,
        // the call deleteRole also deletes the permission entity and
        // secondHUser will lose FIND permission
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HyperIoTAction firstAction = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser firstHUser = createHUser(firstAction);

        // secondHUser has FIND permission
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FIND);
        HUser secondHUser = createHUser();
        Role role = createUserRole(secondHUser);
        Permission permission = utilGrantPermission(secondHUser, role, action);
        // Test secondHUser permission
        this.impersonateUser(roleRestService, secondHUser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        // firstHUser delete role with call deleteRole, role has been removed in cascade mode
        // this call also deletes the permission entity
        // with this call secondHUser will lose FIND permission
        this.impersonateUser(roleRestService, firstHUser);
        Response restRemoveRole = roleRestService.deleteRole(role.getId());
        Assert.assertEquals(200, restRemoveRole.getStatus());

        // hadmin try to delete User Role, this call fail because
        // Role has been removed with call deleteRole by firstHUser
        this.impersonateUser(roleRestService, adminUser);
        restRemoveRole = roleRestService.deleteUserRole(role.getId(), secondHUser.getId());
        Assert.assertEquals(404, restRemoveRole.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restRemoveRole.getEntity()).getType());

        // permission not found, permission has been removed with
        // call deleteRole by firstHUser
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponsePermission = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(404, restResponsePermission.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponsePermission.getEntity()).getType());

        // this call fail because secondHUser not has user role,
        // Role has been deleted with call deleteRole by firstHUser
        this.impersonateUser(roleRestService, secondHUser);
        restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test48_tryToFindRoleWithPermissionIfRoleHasBeenDeletedShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // firstHUser deletes user role with call deleteUserRole,
        // this call does not deletes the permission entity
        // with deleteUserRole call huser will lose FIND permission
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HyperIoTAction firstAction = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTRoleAction.REMOVE_MEMBERS);
        HUser firstHUser = createHUser(firstAction);

        // secondHUser has FIND permission
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FIND);
        HUser secondHUser = createHUser();
        Role role = createUserRole(secondHUser);
        Permission permission = utilGrantPermission(secondHUser, role, action);
        // Test secondHUser permission
        this.impersonateUser(roleRestService, secondHUser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        // delete user role with call deleteUserRole,
        // this call does not deletes the permission entity
        this.impersonateUser(roleRestService, firstHUser);
        Response restRemoveRole = roleRestService.deleteUserRole(role.getId(), secondHUser.getId());
        Assert.assertEquals(200, restRemoveRole.getStatus());

        // permission is still stored in database
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponsePermission = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(200, restResponsePermission.getStatus());

        //fail because secondHUser not has user role, deleted with call deleteUserRole
        this.impersonateUser(roleRestService, secondHUser);
        restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test49_deleteUserRoleNotDeletePermissionOrHUserEntityInCascadeMode() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // firstHUser deletes user role with call deleteUserRole,
        // this call does not deletes the permission or huser entity
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HyperIoTAction firstAction = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTRoleAction.REMOVE_MEMBERS);
        HUser firstHUser = createHUser(firstAction);

        // secondHUser has FIND permission
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FIND);
        HUser secondHUser = createHUser();
        Role role = createUserRole(secondHUser);
        Permission permission = utilGrantPermission(secondHUser, role, action);
        // Test secondHUser permission
        this.impersonateUser(roleRestService, secondHUser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        // delete user role with call deleteUserRole,
        // this call does not deletes the permission entity
        this.impersonateUser(roleRestService, firstHUser);
        Response restRemoveRole = roleRestService.deleteUserRole(role.getId(), secondHUser.getId());
        Assert.assertEquals(200, restRemoveRole.getStatus());

        // permission is still stored in database
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponsePermission = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(200, restResponsePermission.getStatus());

        // secondHUser is still stored in database
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        this.impersonateUser(hUserRestApi, adminUser);
        Response restResponseHUser = hUserRestApi.findHUser(secondHUser.getId());
        Assert.assertEquals(200, restResponseHUser.getStatus());
    }


    @Test
    public void test50_deleteRoleInCascadeModeNotDeleteHUserButDeletePermissionEntity() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // firstHUser deletes user role with call deleteRole,
        // this call deletes the permission entity
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        HyperIoTAction firstAction = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser firstHUser = createHUser(firstAction);

        // secondHUser has FIND permission
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FIND);
        HUser secondHUser = createHUser();
        Role role = createUserRole(secondHUser);
        Permission permission = utilGrantPermission(secondHUser, role, action);
        // Test secondHUser permission
        this.impersonateUser(roleRestService, secondHUser);
        Response restResponse = roleRestService.findRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());

        // delete user role with call deleteUserRole,
        // this call does not deletes the permission entity
        this.impersonateUser(roleRestService, firstHUser);
        Response restRemoveRole = roleRestService.deleteRole(role.getId());
        Assert.assertEquals(200, restRemoveRole.getStatus());

        // permission not found, permission has been removed with call deleteRole
        PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
        this.impersonateUser(permissionRestApi, adminUser);
        Response restResponsePermission = permissionRestApi.findPermission(permission.getId());
        Assert.assertEquals(404, restResponsePermission.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponsePermission.getEntity()).getType());

        // secondHUser is still stored in database
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        this.impersonateUser(hUserRestApi, adminUser);
        Response restResponseHUser = hUserRestApi.findHUser(secondHUser.getId());
        Assert.assertEquals(200, restResponseHUser.getStatus());
    }


    @Test
    public void test51_defaultRoleAndPermissionsCreatedInHyperIoTFramework() {
        RoleRepository roleRepository = getOsgiService(RoleRepository.class);
        // This test checks if default role "RegisteredUser" and permissions has been created in HyperIoTFramework
        Role role = roleRepository.findByName("RegisteredUser");
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
        Assert.assertFalse(listPermissions.isEmpty());
        Assert.assertEquals(2, listPermissions.size());
        boolean resourceNameAssetCategory = false;
        boolean resourceNameAssetTag = false;
        for (int i = 0; i < listPermissions.size(); i++) {
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
        }
        Assert.assertTrue(resourceNameAssetCategory);
        Assert.assertTrue(resourceNameAssetTag);
    }


    @Test
    public void test52_deleteInCascadeModeDefaultRoleAndPermissionInHyperIoTFramework() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // HUser, with permission, delete default role "RegisteredUser" with the following call
        // deleteRole
        RoleRepository roleRepository = getOsgiService(RoleRepository.class);
        Role role = roleRepository.findByName("RegisteredUser");
        PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
        Collection<Permission> listPermissions = permissionSystemApi.findByRole(role);
        Assert.assertFalse(listPermissions.isEmpty());
        Assert.assertEquals(2, listPermissions.size());
        boolean resourceNameAssetCategory = false;
        boolean resourceNameAssetTag = false;
        for (int i = 0; i < listPermissions.size(); i++) {
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
        }
        Assert.assertTrue(resourceNameAssetCategory);
        Assert.assertTrue(resourceNameAssetTag);

        // huser delete, in cascade mode, role and permissions
        // with the following call deleteRole
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName, HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.deleteRole(role.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        // checks: list of permissions is empty
        listPermissions = permissionSystemApi.findByRole(role);
        Assert.assertTrue(listPermissions.isEmpty());
        Assert.assertEquals(0, listPermissions.size());
    }


    @Test
    public void test53_findAllRolesPaginationWithPermissionShouldWork() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, with permission,
        // find all Roles with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = 2;
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(delta, page);
        HyperIoTPaginableResult<Role> listRoles = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Role>>() {
                });
        Assert.assertEquals(5, listRoles.getResults().size());
        Assert.assertFalse(listRoles.getResults().isEmpty());
        Assert.assertEquals(2, listRoles.getCurrentPage());
        Assert.assertEquals(5, listRoles.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test54_findAllRolesPaginationWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, with permission,
        // find all Roles with pagination if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(delta, page);
        HyperIoTPaginableResult<Role> listRoles = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Role>>() {
                });
        Assert.assertEquals(10, listRoles.getResults().size());
        Assert.assertFalse(listRoles.getResults().isEmpty());
        Assert.assertEquals(1, listRoles.getCurrentPage());
        Assert.assertEquals(10, listRoles.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test55_findAllRolesPaginationWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, with permission,
        // find all Roles with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 2;
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(-1, page);
        HyperIoTPaginableResult<Role> listRoles = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Role>>() {
                });
        Assert.assertEquals(10, listRoles.getResults().size());
        Assert.assertFalse(listRoles.getResults().isEmpty());
        Assert.assertEquals(2, listRoles.getCurrentPage());
        Assert.assertEquals(10, listRoles.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test56_findAllRolesPaginationWithPermissionShouldWorkIfDeltaIsZero() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, with permission,
        // find all Roles  with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 3;
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(0, page);
        HyperIoTPaginableResult<Role> listRoles = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Role>>() {
                });
        Assert.assertEquals(10, listRoles.getResults().size());
        Assert.assertFalse(listRoles.getResults().isEmpty());
        Assert.assertEquals(3, listRoles.getCurrentPage());
        Assert.assertEquals(10, listRoles.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test57_findAllRolesPaginationWithPermissionShouldWorkIfPageIsLowerThanZero() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, with permission,
        // find all Roles with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = -1;
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(delta, page);
        HyperIoTPaginableResult<Role> listRoles = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Role>>() {
                });
        Assert.assertEquals(5, listRoles.getResults().size());
        Assert.assertFalse(listRoles.getResults().isEmpty());
        Assert.assertEquals(1, listRoles.getCurrentPage());
        Assert.assertEquals(5, listRoles.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test58_findAllRolesPaginationWithPermissionShouldWorkIfPageIsZero() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, with permission,
        // find all Roles with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(roleResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        List<Role> roles = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Role role = createRole();
            roles.add(role);
        }
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(delta, 0);
        HyperIoTPaginableResult<Role> listRoles = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Role>>() {
                });
        Assert.assertEquals(5, listRoles.getResults().size());
        Assert.assertFalse(listRoles.getResults().isEmpty());
        Assert.assertEquals(1, listRoles.getCurrentPage());
        Assert.assertEquals(5, listRoles.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test59_findAllRolePaginationWithoutPermissionShouldFail() {
        RoleRestApi roleRestService = getOsgiService(RoleRestApi.class);
        // In this following call findAllRolesPaginated HUser, without permission, tries to find
        // all Roles with pagination
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(roleRestService, huser);
        Response restResponse = roleRestService.findAllRolesPaginated(null, null);
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

    private HUser createHUser() {
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hUserRestApi, adminUser);
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
        Response restResponse = hUserRestApi.saveHUser(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        return huser;
    }


    private HUser createHUser(HyperIoTAction action) {
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hUserRestApi, adminUser);
        String username = "TestUser";
        HUser huser = new HUser();
        huser.setName("name");
        huser.setLastname("lastname");
        huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
        huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        Response restResponse = hUserRestApi.saveHUser(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        if (action != null) {
            Role role = createRole();
            huser.addRole(role);
            RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
            this.impersonateUser(roleRestApi, adminUser);
            Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
            Assert.assertEquals(200, restUserRole.getStatus());
            Assert.assertTrue(huser.hasRole(role));
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
            } else {
                this.impersonateUser(permissionRestApi, adminUser);
                testPermission.addPermission(action);
                Response restResponseUpdate = permissionRestApi.updatePermission(testPermission);
                Assert.assertEquals(200, restResponseUpdate.getStatus());
            }
            return testPermission;
        }
    }

    private Role createRole() {
        RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(roleRestApi, adminUser);
        Role role = new Role();
        role.setName("Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestApi.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
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

    private String testMaxDescription(int lengthDescription) {
        String symbol = "a";
        String description = String.format("%" + lengthDescription + "s", " ").replaceAll(" ", symbol);
        Assert.assertEquals(lengthDescription, description.length());
        return description;
    }

}
