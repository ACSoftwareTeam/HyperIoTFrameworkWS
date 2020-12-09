package it.acsoftware.hyperiot.asset.category.test;

import it.acsoftware.hyperiot.asset.category.model.AssetCategory;
import it.acsoftware.hyperiot.asset.category.service.rest.AssetCategoryRestApi;
import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTAssetOwnerImpl;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.company.model.Company;
import it.acsoftware.hyperiot.company.service.rest.CompanyRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
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

import static it.acsoftware.hyperiot.asset.category.test.HyperIoTAssetCategoryConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for AssetCategory System
 * Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAssetCategoryRestTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTAssetCategoryConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
				.withDebug("5010", false)
                .append(getConfiguration()).build();
    }

    public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
        return restApi.impersonate(user);
    }

    @SuppressWarnings("unused")
    private HyperIoTAction getHyperIoTAction(String resourceName, HyperIoTActionName action, long timeout) {
        String actionFilter = OSGiFilterBuilder.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
                .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
        return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
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
    public void test01_saveAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin save AssetCategory with the following call saveAssetCategory
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
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
        Assert.assertEquals(adminUser.getId(),
                ((AssetCategory) restResponse.getEntity()).getOwner().getUserId());
    }

    @Test
    public void test02_saveAssetCategoryShouldFailIfNotLogged() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // the following call tries to save AssetCategory, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
        assetCategory.setOwner(owner);
        //user not logged
        this.impersonateUser(assetCategoryRestApi, null);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test03_updateAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin update AssetCategory with the following call updateAssetCategory
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        Date date = new Date();
        assetCategory.setName("edited in date: " + date);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("edited in date: " + date,
                ((AssetCategory) restResponse.getEntity()).getName());
        Assert.assertEquals(assetCategory.getEntityVersion() + 1,
                (((AssetCategory) restResponse.getEntity()).getEntityVersion()));
    }

    @Test
    public void test04_updateAssetCategoryShouldFailIfNotLogged() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // the following call tries to update AssetCategory, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        Date date = new Date();
        assetCategory.setName("edited failed in date: " + date);
        this.impersonateUser(assetCategoryRestApi, null);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_findAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin find AssetCategory with the following call findAssetCategory
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, adminUser);
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
    public void test06_findAssetCategoryShouldFailIfNotLogged() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // the following call tries to find AssetCategory, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, null);
        Response restResponse = assetCategoryRestApi.findAssetCategory(assetCategory.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test07_findAssetCategoryShouldFailIfEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to find AssetCategory with the following call findAssetCategory,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.findAssetCategory(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findAllAssetCategoriesShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin find all AssetCategories with the following call findAllAssetCategories
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, adminUser);
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
    public void test09_findAllAssetCategoriesShouldFailIfNotLogged() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // the following call tries to find all AssetCategories, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, null);
        Response restResponse = assetCategoryRestApi.findAllAssetCategory();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_deleteAssetCategoryShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin delete AssetCategory with the following call deleteAssetCategory
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test11_deleteAssetCategoryShouldFailIfNotLogged() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // the following call tries to delete AssetCategory, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());
        this.impersonateUser(assetCategoryRestApi, null);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(assetCategory.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteAssetCategoryShouldFailIfEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to delete AssetCategory with the following call deleteAssetCategory,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.deleteAssetCategory(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_saveAssetCategoryShouldFailIfNameIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to save AssetCategory with the following call saveAssetCategory,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName(null);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean msgValidationErrorsIsNull = false;
        boolean msgValidationErrorsIsEmpty = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be null")) {
                msgValidationErrorsIsNull = true;
                Assert.assertEquals("must not be null",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test14_saveAssetCategoryShouldFailIfNameIsEmpty() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to save AssetCategory with the following call saveAssetCategory,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test15_saveAssetCategoryShouldFailIfNameIsMaliciousCode() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to save AssetCategory with the following call saveAssetCategory,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        AssetCategory assetCategory = new AssetCategory();
        String maliciousCode = "javascript:";
        assetCategory.setName(maliciousCode);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
        assetCategory.setOwner(owner);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test16_saveAssetCategoryShouldFailIfOwnerIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to save AssetCategory with the following call saveAssetCategory,
        // but owner is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        assetCategory.setOwner(null);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.saveAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-owner",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test17_updateAssetCategoryShouldFailIfNameIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to update AssetCategory with the following call updateAssetCategory,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        assetCategory.setName(null);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        boolean msgValidationErrorsIsNull = false;
        boolean msgValidationErrorsIsEmpty = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be null")) {
                msgValidationErrorsIsNull = true;
                Assert.assertEquals("must not be null",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assetcategory-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test18_updateAssetCategoryShouldFailIfNameIsEmpty() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to update AssetCategory with the following call updateAssetCategory,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        assetCategory.setName("");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test19_updateAssetCategoryShouldFailIfNameIsMaliciousCode() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to update AssetCategory with the following call updateAssetCategory,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        String maliciousCode = "</script>";
        assetCategory.setName(maliciousCode);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test20_updateAssetCategoryShouldFailIfOwnerIsNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to update AssetCategory with the following call updateAssetCategory,
        // but owner is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        assetCategory.setOwner(null);
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assetcategory-owner",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test21_saveAssetCategoryShouldFailIfEntityIsDuplicated() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to save AssetCategory with the following call saveAssetCategory,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());

        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());

        AssetCategory duplicateAssetCategory = new AssetCategory();
        duplicateAssetCategory.setName(assetCategory.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
        duplicateAssetCategory.setOwner(owner);

        Assert.assertEquals(assetCategory.getName(), duplicateAssetCategory.getName());

        this.impersonateUser(assetCategoryRestApi, adminUser);
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
    public void test22_updateAssetCategoryShouldFailIfEntityIsDuplicated() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to update AssetCategory with the following call updateAssetCategory,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = createAssetCategory();
        Assert.assertNotEquals(0, assetCategory.getId());

        AssetCategory duplicateAssetCategory = createAssetCategory();
        Assert.assertNotEquals(0, duplicateAssetCategory.getId());

        duplicateAssetCategory.setName(assetCategory.getName());
        Assert.assertEquals(assetCategory.getName(), duplicateAssetCategory.getName());

        this.impersonateUser(assetCategoryRestApi, adminUser);
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
    public void test23_updateAssetCategoryShouldFailIfEntityNotFound() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // hadmin tries to update AssetCategory with the following call updateAssetCategory,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("AssetCategory not found");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Response restResponse = assetCategoryRestApi.updateAssetCategory(assetCategory);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test24_findAllAssetCategoriesPaginatedShouldWork() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategory, hadmin find all AssetCategories with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Integer delta = 5;
        Integer page = 2;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetCategory assetCategory = createAssetCategory();
            categories.add(assetCategory);
        }
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategory = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(5, listAssetCategory.getResults().size());
        Assert.assertFalse(listAssetCategory.getResults().isEmpty());
        Assert.assertEquals(2, listAssetCategory.getCurrentPage());
        Assert.assertEquals(1, listAssetCategory.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test25_findAllAssetCategoriesPaginatedShouldWorkIfDeltaAndPageAreNull() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, hadmin find all AssetCategories with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Integer delta = null;
        Integer page = null;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory();
            categories.add(assetCategory);
        }
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategory = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(10, listAssetCategory.getResults().size());
        Assert.assertFalse(listAssetCategory.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategory.getCurrentPage());
        Assert.assertEquals(10, listAssetCategory.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test26_findAllAssetCategoriesPaginatedShouldWorkIfDeltaIsLowerThanZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, hadmin find all AssetCategories with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Integer page = 2;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory();
            categories.add(assetCategory);
        }
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(-1, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategory = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(10, listAssetCategory.getResults().size());
        Assert.assertFalse(listAssetCategory.getResults().isEmpty());
        Assert.assertEquals(2, listAssetCategory.getCurrentPage());
        Assert.assertEquals(10, listAssetCategory.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test27_findAllAssetCategoriesPaginatedShouldWorkIfDeltaIsZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, hadmin find all AssetCategories with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Integer page = 3;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory();
            categories.add(assetCategory);
        }
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(0, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategory = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(10, listAssetCategory.getResults().size());
        Assert.assertFalse(listAssetCategory.getResults().isEmpty());
        Assert.assertEquals(3, listAssetCategory.getCurrentPage());
        Assert.assertEquals(10, listAssetCategory.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test28_findAllAssetCategoriesPaginatedShouldWorkIfPageIsLowerThanZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, hadmin find all AssetCategories with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Integer delta = 5;
        Integer page = -1;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory();
            categories.add(assetCategory);
        }
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, page);
        HyperIoTPaginableResult<AssetCategory> listAssetCategory = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(5, listAssetCategory.getResults().size());
        Assert.assertFalse(listAssetCategory.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategory.getCurrentPage());
        Assert.assertEquals(5, listAssetCategory.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test29_findAllAssetCategoriesPaginatedShouldWorkIfPageIsZero() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // In this following call findAllAssetCategories, hadmin find all AssetCategories with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetCategoryRestApi, adminUser);
        Integer delta = 5;
        List<AssetCategory> categories = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetCategory assetCategory = createAssetCategory();
            categories.add(assetCategory);
        }
        Response restResponse = assetCategoryRestApi.findAllAssetCategoryPaginated(delta, 0);
        HyperIoTPaginableResult<AssetCategory> listAssetCategory = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetCategory>>() {
                });
        Assert.assertEquals(5, listAssetCategory.getResults().size());
        Assert.assertFalse(listAssetCategory.getResults().isEmpty());
        Assert.assertEquals(1, listAssetCategory.getCurrentPage());
        Assert.assertEquals(5, listAssetCategory.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test30_findAllAssetCategoriesPaginatedShouldFailIfNotLogged() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        // the following call tries to find all AssetCategories with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(assetCategoryRestApi, null);
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

    private AssetCategory createAssetCategory() {
        AssetCategoryRestApi assetCategoryRestApi = getOsgiService(AssetCategoryRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());

        AssetCategory assetCategory = new AssetCategory();
        assetCategory.setName("Category" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(adminUser.getId());
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
        Assert.assertEquals(adminUser.getId(),
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

}
