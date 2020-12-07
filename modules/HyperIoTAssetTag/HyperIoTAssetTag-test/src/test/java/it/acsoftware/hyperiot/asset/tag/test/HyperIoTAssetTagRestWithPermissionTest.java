package it.acsoftware.hyperiot.asset.tag.test;

import it.acsoftware.hyperiot.asset.tag.model.AssetTag;
import it.acsoftware.hyperiot.asset.tag.service.rest.AssetTagRestApi;
import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
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
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
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

import static it.acsoftware.hyperiot.asset.tag.test.HyperIoTAssetTagConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for AssetTag System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAssetTagRestWithPermissionTest extends KarafTestSupport {

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
        // hyperiot-core import the following features: base, mail, authentication, permission, huser, company, role,
        // assetcategory, assettag, sharedentity.
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
    public void test01_saveAssetTagWithPermissionShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, save AssetTag with the following call saveAssetTag
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetTag) restResponse.getEntity()).getId());
        Assert.assertEquals(assetTag.getName(), ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(companyResourceName,
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals((Long)company.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(huser.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getUserId());
    }

    @Test
    public void test02_saveAssetTagWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to save AssetTag with the following call saveAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test03_updateAssetTagWithPermissionShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, update AssetTag with the following call updateAssetTag
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        Date date = new Date();
        assetTag.setName("edited in date: " + date);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("edited in date: " + date,
                ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getEntityVersion() + 1,
                (((AssetTag) restResponse.getEntity()).getEntityVersion()));
    }

    @Test
    public void test04_updateAssetTagWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to update AssetTag with the following call updateAssetTag
        // response status code '403' UnauthorizedException
        HUser huser = createHUser(null);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        assetTag.setName("edit name failed...");
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test05_updateAssetTagWithPermissionShouldFailIfEntityNotFound() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag with the following
        // call updateAssetTag, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        //AssetTag isn't stored in database
        AssetTag assetTag = new AssetTag();
        assetTag.setName("AssetTag not found");
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test06_findAssetTagWithPermissionShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, find AssetTag with the following call findAssetTag
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAssetTag(assetTag.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(assetTag.getId(), ((AssetTag) restResponse.getEntity()).getId());
        Assert.assertEquals(assetTag.getName(), ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(assetTag.getOwner().getOwnerResourceName(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals(assetTag.getOwner().getOwnerResourceId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(assetTag.getOwner().getUserId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getUserId());
    }

    @Test
    public void test07_findAssetTagWithPermissionShouldFailIfEntityNotFound() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to find AssetTag with the following call
        // findAssetTag, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAssetTag(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test08_findAssetTagWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to find AssetTag with the following
        // call findAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAssetTag(assetTag.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test09_findAssetTagNotFoundWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to find AssetTag not found with the
        // following call findAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAssetTag(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test10_findAllAssetTagWithPermissionShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, find all AssetTag with the following
        // call findAllAssetTag
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAllAssetTag();
        List<AssetTag> listTags = restResponse.readEntity(new GenericType<List<AssetTag>>() {
        });
        Assert.assertFalse(listTags.isEmpty());
        boolean assetTagFound = false;
        for (AssetTag tag : listTags) {
            if (tag.getId() == assetTag.getId()) {
                assetTagFound = true;
            }
        }
        Assert.assertTrue(assetTagFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test11_findAllAssetTagWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to find all AssetTag with the
        // following call findAllAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAllAssetTag();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test12_deleteAssetTagWithPermissionShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, delete AssetTag with the following call deleteAssetTag
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.deleteAssetTag(assetTag.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }

    @Test
    public void test13_deleteAssetTagWithPermissionShouldFailIfEntityNotFound() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to delete AssetTag with the following
        // call deleteAssetTag, but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.deleteAssetTag(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test14_deleteAssetTagWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to delete AssetTag with the following
        // call deleteAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.deleteAssetTag(assetTag.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test15_deleteAssetTagNotFoundWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to delete AssetTag not found with
        // the following call deleteAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.deleteAssetTag(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test16_saveAssetTagWithPermissionShouldFailIfNameIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to save AssetTag with the following call saveAssetTag,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag assetTag = new AssetTag();
        assetTag.setName(null);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
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
                Assert.assertEquals("assettag-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assettag-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test17_saveAssetTagWithPermissionShouldFailIfNameIsEmpty() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to save AssetTag with the following call saveAssetTag,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag assetTag = new AssetTag();
        assetTag.setName("");
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assettag-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test18_saveAssetTagWithPermissionShouldFailIfNameIsMaliciousCode() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to save AssetTag with the following call saveAssetTag,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag assetTag = new AssetTag();
        String maliciousCode = "javascript:";
        assetTag.setName(maliciousCode);
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assettag-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test19_saveAssetTagWithPermissionShouldFailIfOwnerIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to save AssetTag with the following call saveAssetTag,
        // but owner is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        AssetTag assetTag = new AssetTag();
        assetTag.setName("Test Failed" + java.util.UUID.randomUUID());
        assetTag.setOwner(null);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assettag-owner",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test20_updateAssetTagWithPermissionShouldFailIfNameIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag with the following call
        // updateAssetTag, but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        Assert.assertNotNull(assetTag.getName());

        assetTag.setName(null);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
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
                Assert.assertEquals("assettag-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("assettag-name",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test21_updateAssetTagWithPermissionShouldFailIfNameIsEmpty() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag with the following call
        // updateAssetTag, but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        assetTag.setName("");
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assettag-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals("",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test22_updateAssetTagWithPermissionShouldFailIfNameIsMaliciousCode() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag with the following call
        // updateAssetTag, but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        String maliciousCode = "vbscript:";
        assetTag.setName(maliciousCode);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assettag-name",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(maliciousCode,
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test23_updateAssetTagWithPermissionShouldFailIfOwnerIsNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag with the following call
        // updateAssetTag, but owner is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());
        Assert.assertNotNull(assetTag.getOwner());
        assetTag.setOwner(null);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be null",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("assettag-owner",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test24_findAllAssetTagPaginationWithPermissionShouldWork() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with permission,
        // find all AssetTag with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = 2;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag(huser);
            Assert.assertNotEquals(0, assetTag.getId());
            tags.add(assetTag);
        }
        Assert.assertEquals((int)delta, tags.size());
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


    @Test
    public void test25_findAllAssetTagPaginationWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with permission,
        // find all AssetTag with pagination if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTag assetTag = createAssetTag(huser);
            Assert.assertNotEquals(0, assetTag.getId());
            tags.add(assetTag);
        }
        Assert.assertEquals(10, tags.size());
        this.impersonateUser(assetTagRestApi, huser);
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
    public void test26_findAllAssetTagPaginationWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with permission,
        // find all AssetTag with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 2;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTag assetTag = createAssetTag(huser);
            Assert.assertNotEquals(0, assetTag.getId());
            tags.add(assetTag);
        }
        Assert.assertEquals(10, tags.size());
        this.impersonateUser(assetTagRestApi, huser);
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
    public void test27_findAllAssetTagPaginationWithPermissionShouldWorkIfDeltaIsZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with permission,
        // find all AssetTag with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 3;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AssetTag assetTag = createAssetTag(huser);
            Assert.assertNotEquals(0, assetTag.getId());
            tags.add(assetTag);
        }
        Assert.assertEquals(10, tags.size());
        this.impersonateUser(assetTagRestApi, huser);
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
    public void test28_findAllAssetTagPaginationWithPermissionShouldWorkIfPageIsLowerThanZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with permission,
        // find all AssetTag with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = -1;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag(huser);
            Assert.assertNotEquals(0, assetTag.getId());
            tags.add(assetTag);
        }
        Assert.assertEquals((int)delta, tags.size());
        this.impersonateUser(assetTagRestApi, huser);
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
    public void test29_findAllAssetTagPaginationWithPermissionShouldWorkIfPageIsZero() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, with permission,
        // find all AssetTag with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        List<AssetTag> tags = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            AssetTag assetTag = createAssetTag(huser);
            Assert.assertNotEquals(0, assetTag.getId());
            tags.add(assetTag);
        }
        Assert.assertEquals((int)delta, tags.size());
        this.impersonateUser(assetTagRestApi, huser);
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
    public void test30_findAllAssetTagPaginationWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // In this following call findAllAssetTag, HUser, without permission,
        // tries to find all AssetTag with pagination
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.findAllAssetTagPaginated(null, null);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test31_saveAssetTagWithPermissionShouldFailIfEntityIsDuplicated() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to save AssetTag with the following call saveAssetTag,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag duplicateAssetTag = new AssetTag();
        duplicateAssetTag.setName(assetTag.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        duplicateAssetTag.setOwner(owner);

        Assert.assertEquals(assetTag.getName(), duplicateAssetTag.getName());

        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(duplicateAssetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
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
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(ownerresourcenameIsDuplicated);
        Assert.assertTrue(ownerresourceidIsDuplicated);
    }


    @Test
    public void test32_saveAssetTagWithoutPermissionShouldFailAndEntityIsDuplicated() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to save duplicate AssetTag with the
        // following call saveAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag duplicateAssetTag = new AssetTag();
        duplicateAssetTag.setName(assetTag.getName());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        duplicateAssetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.saveAssetTag(duplicateAssetTag);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test33_updateAssetTagNotFoundWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag not found with
        // the following call updateAssetTag
        // response status code '404' HyperIoTEntityNotFound
        HUser huser = createHUser(null);
        // AssetTag isn't stored in database
        AssetTag assetTag = new AssetTag();
        Date date = new Date();
        assetTag.setName("edited failed in date: " + date);
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(assetTag);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test34_updateAssetTagWithPermissionShouldFailIfEntityIsDuplicated() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, with permission, tries to update AssetTag with the following call
        // updateAssetTag, but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(assetTagResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());

        AssetTag duplicateAssetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, duplicateAssetTag.getId());

        duplicateAssetTag.setName(assetTag.getName());
        Assert.assertEquals(assetTag.getName(), duplicateAssetTag.getName());

        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(duplicateAssetTag);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
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
        Assert.assertTrue(nameIsDuplicated);
        Assert.assertTrue(ownerresourcenameIsDuplicated);
        Assert.assertTrue(ownerresourceidIsDuplicated);
    }

    @Test
    public void test35_updateAssetTagDuplicatedWithoutPermissionShouldFail() {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        // HUser, without permission, tries to update duplicate AssetTag with the
        // following call updateAssetTag
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        AssetTag assetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, assetTag.getId());

        AssetTag duplicateAssetTag = createAssetTag(huser);
        Assert.assertNotEquals(0, duplicateAssetTag.getId());

        duplicateAssetTag.setName(assetTag.getName());
        this.impersonateUser(assetTagRestApi, huser);
        Response restResponse = assetTagRestApi.updateAssetTag(duplicateAssetTag);
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
        role.setName("Test Role" + java.util.UUID.randomUUID());
        role.setDescription("Description");
        Response restResponse = roleRestApi.saveRole(role);
        Assert.assertEquals(200, restResponse.getStatus());
        return role;
    }

    private AssetTag createAssetTag(HUser huser) {
        AssetTagRestApi assetTagRestApi = getOsgiService(AssetTagRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        AssetTag assetTag = new AssetTag();
        assetTag.setName("Tag" + java.util.UUID.randomUUID());
        HyperIoTAssetOwnerImpl owner = new HyperIoTAssetOwnerImpl();
        owner.setOwnerResourceName(companyResourceName);
        owner.setOwnerResourceId(company.getId());
        owner.setUserId(huser.getId());
        assetTag.setOwner(owner);
        this.impersonateUser(assetTagRestApi, adminUser);
        Response restResponse = assetTagRestApi.saveAssetTag(assetTag);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0,
                ((AssetTag) restResponse.getEntity()).getId());
        Assert.assertEquals(assetTag.getName(), ((AssetTag) restResponse.getEntity()).getName());
        Assert.assertEquals(companyResourceName,
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceName());
        Assert.assertEquals((Long)company.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getOwnerResourceId());
        Assert.assertEquals(huser.getId(),
                ((AssetTag) restResponse.getEntity()).getOwner().getUserId());
        return assetTag;
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
