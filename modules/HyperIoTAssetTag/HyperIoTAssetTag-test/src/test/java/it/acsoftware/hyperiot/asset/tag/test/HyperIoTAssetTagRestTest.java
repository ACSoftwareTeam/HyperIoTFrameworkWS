package it.acsoftware.hyperiot.asset.tag.test;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.asset.tag.service.rest.AssetTagRestApi;
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
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
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

import static it.acsoftware.hyperiot.asset.tag.test.HyperIoTAssetTagConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for AssetTag System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAssetTagRestTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTAssetTagConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//				.withDebug("5010", false)
                .append(getBaseConfiguration()).build();
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
        assertContains("HyperIoTPermission-features ", features);
        assertContains("HyperIoTRole-features ", features);
        assertContains("HyperIoTHUser-features ", features);
        assertContains("HyperIoTMail-features ", features);
        assertContains("HyperIoTAuthentication-features ", features);
//        assertContains("HyperIoTHProject-features ", features);
//        assertContains("HyperIoTHDevice-features ", features);
//        assertContains("HyperIoTHPacket-features ", features);
//        assertContains("HyperIoTArea-features", features);
//        assertContains("HyperIoTDashboard-features", features);
        assertContains("HyperIoTAssetTag-features", features);
        String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
        assertContains("hyperiot", datasource);
    }

    @Test
    public void test01_saveAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin save AssetTag with the following call saveAssetTag
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hProject = createHProject();
        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(adminUser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test02_saveAssetTagShouldFailIfNotLogged() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // the following call tries to save AssetTag, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hProject = createHProject();
        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(adminUser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, null);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test03_updateAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin update AssetTag with the following call updateAssetTag
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = createAssetTag();
        Date date = new Date();
        assetTag.setName("edited in date: " + date);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("edited in date: " + date,
                ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getEntityVersion() + 1,
                (((AssetTag) restResponse.getEntity()).getEntityVersion()));
    }

    @Test
    public void test04_updateAssetTagShouldFailIfNotLogged() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // the following call tries to update AssetTag, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetTag assetTag = createAssetTag();
        Date date = new Date();
        assetTag.setName("edited failed in date: " + date);
        this.impersonateUser(assetTagRestApi, null);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_findAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin find AssetTag with the following call findAssetTag
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = createAssetTag();
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.findAssetTag(assetTag.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test06_findAssetTagShouldFailIfNotLogged() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // the following call tries to find AssetTag, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetTag assetTag = createAssetTag();
        this.impersonateUser(assetTagRestApi, null);
        Response restResponse = assetTagRestApi.findAssetTag(assetTag.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test07_findAssetTagShouldFailIfEntityNotFound() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to find AssetTag with the following call findAssetTag,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.findAssetTag(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findAllAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin find all AssetTag with the following call findAllAssetTag
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        AssetTag assetTag = createAssetTag();
        Response restResponse = assetTagRestApi.findAllAssetTag();
        List<AssetTag> listTags = restResponse.readEntity(new GenericType<List<AssetTag>>() {
        });
        Assert.assertFalse(listTags.isEmpty());
        boolean assetTagFound = false;
        for (AssetTag tags : listTags) {
            if (assetTag.getId() == tags.getId()) {
                assetTagFound = true;
            }
        }
        Assert.assertTrue(assetTagFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test09_findAllAssetTagShouldFailIfNotLogged() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // the following call tries to find all AssetTag, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        createAssetTag();
        this.impersonateUser(assetTagRestApi, null);
        Response restResponse = assetTagRestApi.findAllAssetTag();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_deleteAssetTagShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin delete AssetTag with the following call deleteAssetTag
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = createAssetTag();
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.deleteAssetTag(assetTag.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_deleteAssetTagShouldFailIfNotLogged() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // the following call tries to delete AssetTag, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        AssetTag assetTag = createAssetTag();
        this.impersonateUser(assetTagRestApi, null);
        Response restResponse = assetTagRestApi.deleteAssetTag(assetTag.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteAssetTagShouldFailIfEntityNotFound() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to delete AssetTag with the following call deleteAssetTag,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.deleteAssetTag(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_saveAssetTagShouldFailIfNameIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to save AssetTag with the following call saveAssetTag,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        AssetTag assetTag = new AssetTag();
        assetTag.setName(null);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(adminUser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
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
    public void test14_saveAssetTagShouldFailIfNameIsEmpty() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to save AssetTag with the following call saveAssetTag,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        AssetTag assetTag = new AssetTag();
        assetTag.setName("");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(adminUser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test15_saveAssetTagShouldFailIfNameIsMaliciousCode() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to save AssetTag with the following call saveAssetTag,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        AssetTag assetTag = new AssetTag();
        assetTag.setName("javascript:");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(adminUser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test16_saveAssetTagShouldFailIfOwnerIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to save AssetTag with the following call saveAssetTag,
        // but owner is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        assetTag.setOwner(null);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }


    @Test
    public void test17_updateAssetTagShouldFailIfNameIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to update AssetTag with the following call updateAssetTag,
        // but name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        AssetTag assetTag = createAssetTag();
        assetTag.setName(null);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
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
    public void test18_updateAssetTagShouldFailIfNameIsEmpty() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to update AssetTag with the following call updateAssetTag,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        AssetTag assetTag = createAssetTag();
        assetTag.setName("");
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test19_updateAssetTagShouldFailIfNameIsMaliciousCode() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to update AssetTag with the following call updateAssetTag,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        AssetTag assetTag = createAssetTag();
        assetTag.setName("vbscript:");
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test20_updateAssetTagShouldFailIfOwnerIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to update AssetTag with the following call updateAssetTag,
        // but owner is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = createAssetTag();
        assetTag.setOwner(null);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
    }

    @Test
    public void test21_saveAssetTagShouldFailIfEntityIsDuplicated() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to save AssetTag with the following call saveAssetTag,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = createAssetTag();
        HProject hProject = createHProject();
        AssetTag duplicateAssetTag = new AssetTag();
        duplicateAssetTag.setName(assetTag.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName);
        owner.setOwnerResourceId(hProject.getId());
        owner.setUserId(adminUser.getId());
        duplicateAssetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(duplicateAssetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean nameIsDuplicated = false;
        boolean ownerresourcenameIsDuplicated = false;
        boolean ownerresourceidIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                nameIsDuplicated = true;
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("ownerresourcename")) {
                ownerresourcenameIsDuplicated = true;
                Assert.assertEquals("ownerresourcename",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("ownerresourceid")) {
                ownerresourceidIsDuplicated = true;
                Assert.assertEquals("ownerresourceid",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(ownerresourcenameIsDuplicated);
        Assert.assertTrue(ownerresourceidIsDuplicated);
    }

    @Test
    public void test22_updateAssetTagShouldFailIfEntityIsDuplicated() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to update AssetTag with the following call updateAssetTag,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = createAssetTag();
        AssetTag duplicateAssetTag = createAssetTag();
        duplicateAssetTag.setName(assetTag.getName());
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.updateAssetTag(duplicateAssetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        boolean nameIsDuplicated = false;
        boolean ownerresourcenameIsDuplicated = false;
        boolean ownerresourceidIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("name")) {
                nameIsDuplicated = true;
                Assert.assertEquals("name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("ownerresourcename")) {
                ownerresourcenameIsDuplicated = true;
                Assert.assertEquals("ownerresourcename",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("ownerresourceid")) {
                ownerresourceidIsDuplicated = true;
                Assert.assertEquals("ownerresourceid",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(ownerresourcenameIsDuplicated);
        Assert.assertTrue(ownerresourceidIsDuplicated);
    }

    @Test
    public void test23_updateAssetTagShouldFailIfEntityNotFound() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // hadmin tries to update AssetTag with the following call updateAssetTag,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        AssetTag assetTag = new AssetTag();
        assetTag.setName("AssetTag not found");
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test24_findAllAssetTagPaginationShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, hadmin find all AssetTag with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Integer delta = 5;
        Integer page = 2;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag();
            tags.add(assetTag);
        }
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

    @Test
    public void test25_findAllAssetTagPaginationShouldWorkIfDeltaAndPageAreNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, hadmin find all AssetTag with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Integer delta = null;
        Integer page = null;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTag assetTag = createAssetTag();
            tags.add(assetTag);
        }
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(delta, page);
        HyperIoTPaginableResult<AssetTag> listAssetTag = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetTag>>() {
                });
        Assert.assertEquals(10, listAssetTag.getResults().size());
        Assert.assertFalse(listAssetTag.getResults().isEmpty());
        Assert.assertEquals(1, listAssetTag.getCurrentPage());
        Assert.assertEquals(10, listAssetTag.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test26_findAllAssetTagPaginationShouldWorkIfDeltaIsLowerThanZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, hadmin find all AssetTag with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Integer page = 2;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTag assetTag = createAssetTag();
            tags.add(assetTag);
        }
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(-1, page);
        HyperIoTPaginableResult<AssetTag> listAssetTag = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetTag>>() {
                });
        Assert.assertEquals(10, listAssetTag.getResults().size());
        Assert.assertFalse(listAssetTag.getResults().isEmpty());
        Assert.assertEquals(2, listAssetTag.getCurrentPage());
        Assert.assertEquals(10, listAssetTag.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test27_findAllAssetTagPaginationShouldWorkIfDeltaIsZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, hadmin find all AssetTag with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Integer page = 3;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTag assetTag = createAssetTag();
            tags.add(assetTag);
        }
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(0, page);
        HyperIoTPaginableResult<AssetTag> listAssetTag = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetTag>>() {
                });
        Assert.assertEquals(10, listAssetTag.getResults().size());
        Assert.assertFalse(listAssetTag.getResults().isEmpty());
        Assert.assertEquals(3, listAssetTag.getCurrentPage());
        Assert.assertEquals(10, listAssetTag.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test28_findAllAssetTagPaginationShouldWorkIfPageIsLowerThanZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, hadmin find all AssetTag with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Integer delta = 5;
        Integer page = -1;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag();
            tags.add(assetTag);
        }
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(delta, page);
        HyperIoTPaginableResult<AssetTag> listAssetTag = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetTag>>() {
                });
        Assert.assertEquals(5, listAssetTag.getResults().size());
        Assert.assertFalse(listAssetTag.getResults().isEmpty());
        Assert.assertEquals(1, listAssetTag.getCurrentPage());
        Assert.assertEquals(5, listAssetTag.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test29_findAllAssetTagPaginationShouldWorkIfPageIsZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, hadmin find all AssetTag with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(assetTagRestApi, adminUser);
        Integer delta = 5;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag();
            tags.add(assetTag);
        }
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(delta, 0);
        HyperIoTPaginableResult<AssetTag> listAssetTag = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<AssetTag>>() {
                });
        Assert.assertEquals(5, listAssetTag.getResults().size());
        Assert.assertFalse(listAssetTag.getResults().isEmpty());
        Assert.assertEquals(1, listAssetTag.getCurrentPage());
        Assert.assertEquals(5, listAssetTag.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test30_findAllAssetTagPaginationShouldFailIfNotLogged() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // the following call tries to find all AssetTag with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(assetTagRestApi, null);
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(null, null);
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

    private HProject createHProject() {
        HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        this.impersonateUser(hprojectRestService, adminUser);
        HProject hproject = new HProject();
        hproject.setName("Project " + java.util.UUID.randomUUID());
        hproject.setDescription("Description");
        hproject.setUser((HUser) adminUser);
        Response restResponse = hprojectRestService.saveHProject(hproject);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((HProject) restResponse.getEntity()).getId());
        Assert.assertEquals("Description",
                ((HProject) restResponse.getEntity()).getDescription());
        Assert.assertEquals(adminUser.getId(),
                ((HProject) restResponse.getEntity()).getUser().getId());
        return hproject;
    }

    private AssetTag createAssetTag() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HProject hproject = createHProject();
        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(hProjectResourceName + java.util.UUID.randomUUID());
        owner.setOwnerResourceId(hproject.getId());
        owner.setUserId(adminUser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        return assetTag;
    }

}