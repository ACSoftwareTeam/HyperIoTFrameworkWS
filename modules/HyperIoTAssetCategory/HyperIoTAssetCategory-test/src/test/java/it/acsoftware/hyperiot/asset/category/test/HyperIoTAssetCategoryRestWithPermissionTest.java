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
import it.acsoftware.hyperiot.company.model.Company;
import it.acsoftware.hyperiot.company.service.rest.CompanyRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.After;
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

import static it.acsoftware.hyperiot.asset.category.test.HyperIoTAssetCategoryConfiguration.*;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAssetCategoryRestWithPermissionTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTAssetCategoryConfiguration class
//        return HyperIoTTestConfigurationBuilder.createStandardConfiguration()
//				.withDebug("5010", false)
//                .append(getConfiguration()).build();
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration()
                .withDebug("5010", false)
                .build();
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
    public void test01_saveAssetCategoryWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with permission, save AssetCategory with the following call saveAssetCategory
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(), ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(companyResourceName,
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals((Long)company.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(huser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
    }

    @Test
    public void test02_saveAssetCategoryWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to save AssetCategory with the
        // following call saveAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
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
        Assert.assertNotEquals(0, assetCategory.getId());
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
        Assert.assertNotEquals(0, assetCategory.getId());
        assetCategory.setName("edit name failed...");
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
        //AssetCategory isn't stored in database
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("AssetCategory not found");
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_updateAssetCategoryNotFoundWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to update AssetCategory not found with
        // the following call updateAssetCategory
        // response status code '404' HyperIoTEntityNotFound
        HUser huser = createHUser(null);
        //AssetCategory isn't stored in database
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
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(assetCategory.getId(), ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(), ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceName(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(assetCategory.getOwner().getUserId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
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
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_findAssetCategoryNotFoundWithoutPermissionShouldFail() {
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
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        List<AssetCategory> listCategories = restResponse.readEntity(new GenericType<List<AssetCategory>>() {
        });
        Assert.assertFalse(listCategories.isEmpty());
        boolean assetCategoryFound = false;
        for (AssetCategory category : listCategories) {
            if (category.getId() == assetCategory.getId()) {
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
        AssetCategory assetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, assetCategory.getId());
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
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
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
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_deleteAssetCategoryNotFoundWithoutPermissionShouldFail() {
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
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName(null);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean msgValidationErrorsIsNull = false;
        boolean msgValidationErrorsIsEmpty = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals(validatorMustNotBeNull)) {
                msgValidationErrorsIsNull = true;
                Assert.assertEquals(validatorMustNotBeNull,
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals(validatorMustNotBeEmpty)) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals(validatorMustNotBeEmpty,
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
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
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals(validatorMustNotBeEmpty,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
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
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        String maliciousCode = "javascript:";
        assetCategory.setName(maliciousCode);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals(validatorNoMaliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
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
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals(validatorMustNotBeNull,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-owner",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
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
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean msgValidationErrorsIsNull = false;
        boolean msgValidationErrorsIsEmpty = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals(validatorMustNotBeNull)) {
                msgValidationErrorsIsNull = true;
                Assert.assertEquals(validatorMustNotBeNull,
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals(validatorMustNotBeEmpty)) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals(validatorMustNotBeEmpty,
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
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
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals(validatorMustNotBeEmpty,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
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
        String maliciousCode = "vbscript:";
        assetCategory.setName(maliciousCode);
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals(validatorNoMaliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
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
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals(validatorMustNotBeNull,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-owner",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
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
        Assert.assertNotEquals(0, assetCategory.getId());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory duplicateAssetCategory = new AssetCategory();
        duplicateAssetCategory.setName(assetCategory.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        duplicateAssetCategory.setOwner(owner);

        Assert.assertEquals(assetCategory.getName(), duplicateAssetCategory.getName());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
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
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(parentIdIsDuplicated);
    }

    @Test
    public void test26_saveAssetCategoryDuplicatedWithoutPermissionShouldFail() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, without permission, tries to save duplicate AssetCategory with
        // the following call saveAssetCategory
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetCategory assetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, assetCategory.getId());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory duplicateAssetCategory = new AssetCategory();
        duplicateAssetCategory.setName(assetCategory.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        duplicateAssetCategory.setOwner(owner);
        Assert.assertEquals(assetCategory.getName(), duplicateAssetCategory.getName());
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
        Assert.assertNotEquals(0, assetCategory.getId());

        AssetCategory duplicateAssetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, duplicateAssetCategory.getId());

        duplicateAssetCategory.setName(assetCategory.getName());
        Assert.assertEquals(assetCategory.getName(), duplicateAssetCategory.getName());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
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
        Assert.assertNotEquals(0, assetCategory.getId());

        AssetCategory duplicateAssetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, duplicateAssetCategory.getId());

        duplicateAssetCategory.setName(assetCategory.getName());
        Assert.assertEquals(assetCategory.getName(), duplicateAssetCategory.getName());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(duplicateAssetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test29_findAllAssetCategoriesPaginatedWithPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        int delta = 5;
        int page = 2;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(defaultDelta, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategories.getResults().size());
        Assert.assertEquals(delta, listAssetCategories.getDelta());
        Assert.assertEquals(page, listAssetCategories.getCurrentPage());
        Assert.assertEquals(page - 1, listAssetCategories.getNextPage());
        // delta is 5, page 2: 10 entities stored in database
        Assert.assertEquals(2, listAssetCategories.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage1 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 1);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategoriesPage1.getResults().size());
        Assert.assertEquals(delta, listAssetCategoriesPage1.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage1.getCurrentPage());
        Assert.assertEquals(page, listAssetCategoriesPage1.getNextPage());
        // delta is 5, page is 1: 10 entities stored in database
        Assert.assertEquals(2, listAssetCategoriesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test30_findAllAssetCategoriesPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
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
        int numbEntities = 6;
        for (int i = 0; i < numbEntities; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(numbEntities, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(numbEntities, listAssetCategories.getResults().size());
        Assert.assertEquals(defaultDelta, listAssetCategories.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategories.getCurrentPage());
        Assert.assertEquals(defaultPage, listAssetCategories.getNextPage());
        // default delta is 10, default page is 1: 6 entities stored in database
        Assert.assertEquals(1, listAssetCategories.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test31_findAllAssetCategoriesPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        int delta = -1;
        int page = 2;
        List<AssetCategory> categories = new ArrayList<>();
        int numbEntities = 17;
        for (int i = 0; i < numbEntities; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(numbEntities, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage2 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage2.getResults().isEmpty());
        Assert.assertEquals(7, listAssetCategoriesPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage2.getDelta());
        Assert.assertEquals(page, listAssetCategoriesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage2.getNextPage());
        // default delta is 10, page is 2: 17 entities stored in database
        Assert.assertEquals(2, listAssetCategoriesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());

        //checks with page = 1
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage1 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 1);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage1.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage1.getCurrentPage());
        Assert.assertEquals(page, listAssetCategoriesPage1.getNextPage());
        // default delta is 10, page is 1: 17 entities stored in database
        Assert.assertEquals(2, listAssetCategoriesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());
    }

    @Test
    public void test32_findAllAssetCategoriesPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        int delta = 0;
        int page = 3;
        List<AssetCategory> categories = new ArrayList<>();
        int numEntities = 22;
        for (int i = 0; i < numEntities; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(numEntities, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage3 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage3 = restResponsePage3
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        //entities showed in page 3
        Assert.assertFalse(listAssetCategoriesPage3.getResults().isEmpty());
        Assert.assertEquals(2, listAssetCategoriesPage3.getResults().size());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage3.getDelta());
        Assert.assertEquals(page, listAssetCategoriesPage3.getCurrentPage());
        Assert.assertEquals(1, listAssetCategoriesPage3.getNextPage());
        // default delta is 10, page is 3: 22 entities stored in database
        Assert.assertEquals(3, listAssetCategoriesPage3.getNumPages());
        Assert.assertEquals(200, restResponsePage3.getStatus());

        //checks with page = 1
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage1 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 1);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage1.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage1.getResults().size());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage1.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAssetCategoriesPage1.getNextPage());
        // default delta is 10, page is 1: 22 entities stored in database
        Assert.assertEquals(3, listAssetCategoriesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage2 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 2);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage2.getResults().isEmpty());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage2.getResults().size());
        Assert.assertEquals(defaultDelta, listAssetCategoriesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAssetCategoriesPage2.getCurrentPage());
        Assert.assertEquals(page, listAssetCategoriesPage2.getNextPage());
        // default delta is 10, page is 2: 22 entities stored in database
        Assert.assertEquals(3, listAssetCategoriesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test33_findAllAssetCategoriesPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        int delta = 5;
        int page = -1;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(delta, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategories.getResults().size());
        Assert.assertEquals(delta, listAssetCategories.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategories.getCurrentPage());
        Assert.assertEquals(defaultPage, listAssetCategories.getNextPage());
        // delta is 5, page 1: 5 entities stored in database
        Assert.assertEquals(1, listAssetCategories.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test34_findAllAssetCategoriesPaginatedWithPermissionShouldWorkIfPageIsZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with permission,
        // find all AssetCategories with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetCategoryResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        int delta = 5;
        int page = 0;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < defaultDelta; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(defaultDelta, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage1 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategoriesPage1.getResults().size());
        Assert.assertEquals(delta, listAssetCategoriesPage1.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAssetCategoriesPage1.getNextPage());
        // delta is 5, default page is 1: 10 entities stored in database
        Assert.assertEquals(2, listAssetCategoriesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage2 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 2);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage2.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategoriesPage2.getResults().size());
        Assert.assertEquals(delta, listAssetCategoriesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAssetCategoriesPage2.getCurrentPage());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage2.getNextPage());
        // delta is 5, page is 2: 10 entities stored in database
        Assert.assertEquals(2, listAssetCategoriesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());
    }

    @Test
    public void test35_findAllAssetCategoriesPaginatedWithoutPermissionShouldFail() {
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
                testPermission = permission;
                this.impersonateUser(permissionRestApi, adminUser);
                Response restResponse = permissionRestApi.savePermission(testPermission);
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

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(), ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(companyResourceName,
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals((Long)company.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(huser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
        return assetCategory;
    }

    private Company createCompany(HUser huser) {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, adminUser);
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


    /*
     *
     *
     * TEST WITH DEFAULT PERMISSIONS
     *
     *
     */


    private HUser huserWithDefaultPermissionInHyperIoTFramework(boolean isActive) {
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
            Assert.assertEquals(2, listPermissions.size());
            boolean resourceNameAssetCategory = false;
            for (int i = 0; i < listPermissions.size(); i++) {
                if (((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName().contains(permissionAssetCategory)) {
                    resourceNameAssetCategory = true;
                    Assert.assertEquals("it.acsoftware.hyperiot.asset.category.model.AssetCategory", ((Permission) ((ArrayList) listPermissions).get(i)).getEntityResourceName());
                    Assert.assertEquals(permissionAssetCategory + nameRegisteredPermission, ((Permission) ((ArrayList) listPermissions).get(i)).getName());
                    Assert.assertEquals(31, ((Permission) ((ArrayList) listPermissions).get(i)).getActionIds());
                    Assert.assertEquals(role.getName(), ((Permission) ((ArrayList) listPermissions).get(i)).getRole().getName());
                }
            }
            Assert.assertTrue(resourceNameAssetCategory);
        }
        return huser;
    }


    // AssetCategory action save: 1
    @Test
    public void test36_saveAssetCategoryWithDefaultPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, save AssetCategory with the following call saveAssetCategory
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category " + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetCategory.setOwner(owner);

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(), ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(companyResourceName,
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals((Long)company.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(huser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
    }


    // AssetCategory action update: 2
    @Test
    public void test37_updateAssetCategoryWithDefaultPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, update AssetCategory with the following call updateAssetCategory
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        AssetCategory assetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, assetCategory.getId());

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
    public void test38_deleteAssetCategoryWithDefaultPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, delete AssetCategory with the following call deleteAssetCategory
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        AssetCategory assetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, assetCategory.getId());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    // AssetCategory action find: 8
    @Test
    public void test39_findAssetCategoryWithDefaultPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, find AssetCategory with the following call findAssetCategory
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        AssetCategory assetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, assetCategory.getId());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(assetCategory.getId(), ((AssetCategory) restResponse.getEntity()).getId());
        Assert.assertEquals(assetCategory.getName(), ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceName(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(assetCategory.getOwner().getOwnerResourceId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(assetCategory.getOwner().getUserId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
    }


    // AssetCategory action find-all: 16
    @Test
    public void test40_findAllAssetCategoriesWithDefaultPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // HUser, with default permission, find all AssetCategories with the following call findAllAssetCategories
        // response status code '200'
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());

        AssetCategory assetCategory = createAssetCategory(huser);
        Assert.assertNotEquals(0, assetCategory.getId());

        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        Assert.assertEquals(200, restResponse.getStatus());
        List<AssetCategory> listCategories = restResponse.readEntity(new GenericType<List<AssetCategory>>() {
        });
        Assert.assertFalse(listCategories.isEmpty());
        boolean assetCategoryFound = false;
        for (AssetCategory category : listCategories) {
            if (category.getId() == assetCategory.getId()) {
                assetCategoryFound = true;
            }
        }
        Assert.assertTrue(assetCategoryFound);
    }


    // AssetCategory action find-all: 16
    @Test
    public void test41_findAllAssetCategoriesPaginatedWithDefaultPermissionShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, HUser, with default permission,
        // find all AssetCategories with pagination
        // response status code '200'
        int delta = 5;
        int page = 4;
        HUser huser = huserWithDefaultPermissionInHyperIoTFramework(true);
        Assert.assertNotEquals(0, huser.getId());
        Assert.assertTrue(huser.isActive());
        List<AssetCategory> categories = new ArrayList<>();
        int numEntities = 16;
        for (int i = 0; i < numEntities; i++) {
            AssetCategory assetCategory = createAssetCategory(huser);
            Assert.assertNotEquals(0, assetCategory.getId());
            categories.add(assetCategory);
        }
        Assert.assertEquals(numEntities, categories.size());
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategories = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategories.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategories.getResults().size());
        Assert.assertEquals(delta, listAssetCategories.getDelta());
        Assert.assertEquals(page, listAssetCategories.getCurrentPage());
        Assert.assertEquals(1, listAssetCategories.getNextPage());
        // delta is 5, page 4: 16 entities stored in database
        Assert.assertEquals(4, listAssetCategories.getNumPages());
        Assert.assertEquals(200, restResponse.getStatus());

        //checks with page = 1
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage1 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 1);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage1 = restResponsePage1
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage1.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategoriesPage1.getResults().size());
        Assert.assertEquals(delta, listAssetCategoriesPage1.getDelta());
        Assert.assertEquals(defaultPage, listAssetCategoriesPage1.getCurrentPage());
        Assert.assertEquals(defaultPage + 1, listAssetCategoriesPage1.getNextPage());
        // delta is 5, page 1: 16 entities stored in database
        Assert.assertEquals(4, listAssetCategoriesPage1.getNumPages());
        Assert.assertEquals(200, restResponsePage1.getStatus());

        //checks with page = 2
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage2 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 2);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage2 = restResponsePage2
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage2.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategoriesPage2.getResults().size());
        Assert.assertEquals(delta, listAssetCategoriesPage2.getDelta());
        Assert.assertEquals(defaultPage + 1, listAssetCategoriesPage2.getCurrentPage());
        Assert.assertEquals(page - 1, listAssetCategoriesPage2.getNextPage());
        // delta is 5, page 2: 16 entities stored in database
        Assert.assertEquals(4, listAssetCategoriesPage2.getNumPages());
        Assert.assertEquals(200, restResponsePage2.getStatus());

        //checks with page = 3
        this.impersonateUser(assetCategoryRestApi, huser);
        Response restResponsePage3 = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 3);
        HyperIoTPaginableResult<AssetCategory> listAssetCategoriesPage3 = restResponsePage3
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertFalse(listAssetCategoriesPage3.getResults().isEmpty());
        Assert.assertEquals(delta, listAssetCategoriesPage3.getResults().size());
        Assert.assertEquals(delta, listAssetCategoriesPage3.getDelta());
        Assert.assertEquals(page - 1, listAssetCategoriesPage3.getCurrentPage());
        Assert.assertEquals(page, listAssetCategoriesPage3.getNextPage());
        // delta is 5, page 3: 16 entities stored in database
        Assert.assertEquals(4, listAssetCategoriesPage3.getNumPages());
        Assert.assertEquals(200, restResponsePage3.getStatus());
    }

    @After
    public void afterTest() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        List<AssetCategory> listCategories = restResponse.readEntity(new GenericType<List<AssetCategory>>() {
        });
        if (!listCategories.isEmpty()) {
            Assert.assertFalse(listCategories.isEmpty());
            for (AssetCategory assetCategory : listCategories) {
                this.impersonateUser(assetCategoryRestApi, adminUser);
                Response restResponse1 = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
                Assert.assertEquals(200, restResponse1.getStatus());
            }
        }
    }

}
