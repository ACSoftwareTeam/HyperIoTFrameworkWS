package it.acsoftware.hyperiot.asset.category.test;

import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.asset.category.service.rest.AssetCategoryRestApi;
import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTAssetOwnerImpl;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.asset.category.test.HyperIoTAssetCategoryConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAssetCategoryRestWithPermissionTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTAssetCategoryConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//				.withDebug("5010", false)
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
        assertContains("HyperIoTHProject-features ", features);
        assertContains("HyperIoTHDevice-features ", features);
        assertContains("HyperIoTHPacket-features ", features);
        assertContains("HyperIoTArea-features", features);
        assertContains("HyperIoTDashboard-features", features);
        assertContains("HyperIoTAssetCategory-features", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_saveAssetCategoryWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, save AssetCategory with the following call saveAssetCategory
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        HProject hProject = createHProject(huser);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetCategory) restResponse.getEntity()).getId());
    }

    @Test
    public void test02_saveAssetCategoryWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to save AssetCategory with the
        // following call saveAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        HProject hProject = createHProject(huser);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test03_updateAssetCategoryWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, update AssetCategory with the following call updateAssetCategory
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        Date date = new Date();
        assetCategory.setName("edited in date: " + date);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("edited in date: " + date,
                ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getEntityVersion() + 1,
                (((AssetCategory) restResponse.getEntity()).getEntityVersion()));
    }

    @Test
    public void test04_updateAssetCategoryWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to update AssetCategory with
        // the following call updateAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetCategory assetCategory = createAssetCategory(huser);
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_updateAssetCategoryWithPermissionShouldFailIfEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to update AssetCategory with the following call
        // updateAssetCategory, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("AssetCategory not found");
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_updateAssetCategoryWithoutPermissionShouldFailAndEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to update AssetCategory not found with
        // the following call updateAssetCategory
        // response status code '404' HyperIoTEntityNotFound
        HUser huser = createHUser(null);
        AssetCategory assetCategory = new AssetCategory();
        Date date = new Date();
        assetCategory.setName("edited failed in date: " + date);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test07_findAssetCategoryWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, find AssetCategory with the following call findAssetCategory
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test08_findAssetCategoryWithPermissionShouldFailIfEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to find AssetCategory with the
        // following call findAssetCategory, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findAssetCategoryWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to find AssetCategory with
        // the following call findAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetCategory assetCategory = createAssetCategory(huser);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_findAssetCategoryWithoutPermissionShouldFailAndEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to find AssetCategory not found with
        // the following call findAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test11_findAllAssetCategoriesWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, find all AssetCategories with the following call findAllAssetCategories
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        List<AssetCategory> listCategories = restResponse.readEntity(new GenericType<List<AssetCategory>>() {
        });
        Assert.assertFalse(listCategories.isEmpty());
        boolean assetCategoryFound = false;
        for (AssetCategory categories : listCategories) {
            if (assetCategory.getId() == categories.getId()) {
                assetCategoryFound = true;
            }
        }
        Assert.assertTrue(assetCategoryFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test12_findAllAssetCategoriesWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to find all AssetCategories with the following
        // call findAllAssetCategories
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        createAssetCategory(huser);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deleteAssetCategoryWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, delete AssetCategory with the following call deleteAssetCategory
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test14_deleteAssetCategoryWithPermissionShouldFailIfEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to delete AssetCategory with the following call
        // deleteAssetCategory, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test15_deleteAssetCategoryWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to delete AssetCategory with the following
        // call deleteAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetCategory assetCategory = createAssetCategory(huser);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_deleteAssetCategoryWithoutPermissionShouldFailAndEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to delete AssetCategory not found with the
        // following call deleteAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test17_saveAssetCategoryWithPermissionShouldFailIfNameIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to save AssetCategory with the following call
        // saveAssetCategory, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        HProject hProject = createHProject(huser);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName(null);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
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
    public void test18_saveAssetCategoryWithPermissionShouldFailIfNameIsEmpty() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to save AssetCategory with the following call
        // saveAssetCategory, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        HProject hProject = createHProject(huser);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test19_saveAssetCategoryWithPermissionShouldFailIfNameIsMaliciousCode() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to save AssetCategory with the following call
        // saveAssetCategory, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        HProject hProject = createHProject(huser);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("javascript:");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test20_saveAssetCategoryWithPermissionShouldFailIfOwnerIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to save AssetCategory with the following
        // call saveAssetCategory, but owner is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        assetCategory.setOwner(null);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test21_updateAssetCategoryWithPermissionShouldFailIfNameIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to update AssetCategory with the following call
        // updateAssetCategory, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        assetCategory.setName(null);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
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
    public void test22_updateAssetCategoryWithPermissionShouldFailIfNameIsEmpty() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to update AssetCategory with the following call
        // updateAssetCategory, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        assetCategory.setName("");
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test23_updateAssetCategoryWithPermissionShouldFailIfNameIsMaliciousCode() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to update AssetCategory with the following call
        // updateAssetCategory, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        assetCategory.setName("vbscript:");
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test24_updateAssetCategoryWithPermissionShouldFailIfOwnerIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to update AssetCategory with the following call
        // updateAssetCategory, but owner is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        assetCategory.setOwner(null);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test25_saveAssetCategoryWithPermissionShouldFailIfEntityIsDuplicated() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to save AssetCategory with the following call
        // saveAssetCategory, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        HProject hProject = createHProject(huser);
        AssetCategory duplicateAssetCategory = new AssetCategory();
        duplicateAssetCategory.setName(assetCategory.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        duplicateAssetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean nameIsDuplicated = false;
        boolean parentIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                nameIsDuplicated = true;
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("parent_id")) {
                parentIdIsDuplicated = true;
                Assert.assertEquals("parent_id",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(parentIdIsDuplicated);
    }

    @Test
    public void test26_saveAssetCategoryWithoutPermissionShouldFailAndEntityIsDuplicated() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to save duplicate AssetCategory with
        // the following call saveAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetCategory assetCategory = createAssetCategory(huser);
        HProject hProject = createHProject(huser);
        AssetCategory duplicateAssetCategory = new AssetCategory();
        duplicateAssetCategory.setName(assetCategory.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(huser.getId());
        duplicateAssetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test27_updateAssetCategoryWithPermissionShouldFailIfEntityIsDuplicated() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, tries to update AssetCategory with the following call
        // updateAssetCategory, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetCategory assetCategory = createAssetCategory(huser);
        AssetCategory duplicateAssetCategory = createAssetCategory(huser);
        duplicateAssetCategory.setName(assetCategory.getName());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean nameIsDuplicated = false;
        boolean parentIdIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                nameIsDuplicated = true;
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("parent_id")) {
                parentIdIsDuplicated = true;
                Assert.assertEquals("parent_id",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(parentIdIsDuplicated);
    }

    @Test
    public void test28_updateAssetCategoryWithoutPermissionShouldFailAndEntityIsDuplicated() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to update duplicate AssetCategory with
        // the following call updateAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetCategory assetCategory = createAssetCategory(huser);
        AssetCategory duplicateAssetCategory = createAssetCategory(huser);
        duplicateAssetCategory.setName(assetCategory.getName());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test29_findAllAssetCategoriesPaginationWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = 2;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
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

    @Test
    public void test30_findAllAssetCategoriesPaginationWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            categories.add(assetCategory);
        }
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(10, listAssetCategories.getResults().size());
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategories.getCurrentPage());
        Assert.assertEquals(10, listAssetCategories.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test31_findAllAssetCategoriesPaginationWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 2;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            categories.add(assetCategory);
        }
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(-1, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(10, listAssetCategories.getResults().size());
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(2, listAssetCategories.getCurrentPage());
        Assert.assertEquals(10, listAssetCategories.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test32_findAllAssetCategoriesPaginationWithPermissionShouldWorkIfDeltaIsZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 3;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            categories.add(assetCategory);
        }
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(0, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(10, listAssetCategories.getResults().size());
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(3, listAssetCategories.getCurrentPage());
        Assert.assertEquals(10, listAssetCategories.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test33_findAllAssetCategoriesPaginationWithPermissionShouldWorkIfPageIsLowerThanZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = -1;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            categories.add(assetCategory);
        }
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(5, listAssetCategories.getResults().size());
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategories.getCurrentPage());
        Assert.assertEquals(5, listAssetCategories.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test34_findAllAssetCategoriesPaginationWithPermissionShouldWorkIfPageIsZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            categories.add(assetCategory);
        }
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 0);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(5, listAssetCategories.getResults().size());
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategories.getCurrentPage());
        Assert.assertEquals(5, listAssetCategories.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test35_findAllAssetCategoriesPaginationWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, without permission,
        // tries to find all AssetCategories with pagination
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(null, null);
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

    private HProject createHProject(HUser huser) {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(hprojectRestService, adminUser);
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Description");
        hproject.setUser(huser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals("Description",
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(huser.getId(),
                ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
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

    private AssetCategory createAssetCategory(HUser huser) {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        HProject hproject = createHProject(huser);
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName + java.util.UUID.randomUUID());
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        return assetCategory;
    }

}
