package it.acsoftware.hyperiot.company.test;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
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
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.company.test.HyperIoTCompanyConfiguration.*;

/**
 * @author Aristide Cittadino Interface component for Company System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTCompanyWithPermissionRestTest extends KarafTestSupport {

    @Configuration
    public Option[] config() {
        // starts with HSQL
        // the standard configuration has been moved to the HyperIoTCompanyConfiguration class
        return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//                .withDebug("5010", false)
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
    public void test01_companyModuleShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call checkModuleWorking checks if Company module working
        // correctly
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.checkModuleWorking();
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test02_saveCompanyWithPermissionShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, save Company with the following call saveCompany
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
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
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNotEquals(0, ((Company) restResponse.getEntity()).getId());
        Assert.assertEquals("ACSoftware", ((Company) restResponse.getEntity()).getBusinessName());
        Assert.assertEquals("Lamezia Terme", ((Company) restResponse.getEntity()).getCity());
        Assert.assertEquals("Lamezia Terme", ((Company) restResponse.getEntity()).getInvoiceAddress());
        Assert.assertEquals("Italy", ((Company) restResponse.getEntity()).getNation());
        Assert.assertEquals("88046", ((Company) restResponse.getEntity()).getPostalCode());
        Assert.assertEquals(company.getVatNumber(), ((Company) restResponse.getEntity()).getVatNumber());
        Assert.assertEquals(huser.getId(), ((Company) restResponse.getEntity()).getHUserCreator().getId());
    }


    @Test
    public void test03_saveCompanyWithoutPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to save Company with the following call saveCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, null);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test04_updateCompanyWithPermissionShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, update Company with the following call updateCompany
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setCity("Bologna");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Bologna", ((Company) restResponse.getEntity()).getCity());
        Assert.assertEquals(company.getEntityVersion() + 1,
                ((Company) restResponse.getEntity()).getEntityVersion());
    }


    @Test
    public void test05_updateCompanyWithoutPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to update Company with the following call updateCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
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


    @Test
    public void test06_findCompanyWithPermissionShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, find Company with the following call findCompany
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findCompany(company.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals(company.getBusinessName(), ((Company) restResponse.getEntity()).getBusinessName());
        Assert.assertEquals(company.getCity(), ((Company) restResponse.getEntity()).getCity());
        Assert.assertEquals(company.getInvoiceAddress(), ((Company) restResponse.getEntity()).getInvoiceAddress());
        Assert.assertEquals(company.getNation(), ((Company) restResponse.getEntity()).getNation());
        Assert.assertEquals(company.getPostalCode(), ((Company) restResponse.getEntity()).getPostalCode());
        Assert.assertEquals(company.getVatNumber(), ((Company) restResponse.getEntity()).getVatNumber());
        Assert.assertEquals(company.getHUserCreator().getId(), ((Company) restResponse.getEntity()).getHUserCreator().getId());
    }


    @Test
    public void test07_findCompanyWithPermissionShouldFailIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to find Company with the following call findCompany,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findCompany(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test08_findCompanyWithoutPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to find Company with the following call findCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findCompany(company.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test09_findCompanyWithoutPermissionShouldFailAndEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to find Company not found with the following call findCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findCompany(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test10_deleteCompanyWithPermissionShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, delete Company with the following call deleteCompany
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(restResponse.getEntity());
    }


    @Test
    public void test11_deleteCompanyWithPermissionShouldFailIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to delete Company with the following call deleteCompany,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.deleteCompany(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test12_deleteCompanyWithoutPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to delete Company with the following call deleteCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test13_deleteCompanyWithoutPermissionShouldFailAndEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to delete Company not found with the following call deleteCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.deleteCompany(0);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test14_findAllCompanyWithPermissionShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, find all Company with the following call findAllCompany
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompany();
        List<Company> listCompanies = restResponse.readEntity(new GenericType<List<Company>>() {
        });
        Assert.assertFalse(listCompanies.isEmpty());
        boolean companyFound = false;
        for (Company companies : listCompanies) {
            if (company.getId() == companies.getId()) {
                companyFound = true;
            }
        }
        Assert.assertTrue(companyFound);
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test15_findAllCompanyWithoutPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to find all Company with the following call findAllCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompany();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test16_saveCompanyWithPermissionShouldFailIfBusinessNameIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName(null);
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
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
                Assert.assertEquals("company-businessname",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-businessname",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }


    @Test
    public void test17_saveCompanyWithPermissionShouldFailIfBusinessNameIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-businessname",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test18_saveCompanyWithPermissionShouldFailIfBusinessNameIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("expression(malicious code)");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-businessname",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getBusinessName(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test19_saveCompanyWithPermissionShouldFailIfCityIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but city is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity(null);
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
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
                Assert.assertEquals("company-city",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-city",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }


    @Test
    public void test20_saveCompanyWithPermissionShouldFailIfCityIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but city is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-city",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test21_saveCompanyWithPermissionShouldFailIfCityIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but city is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("src='malicious code'");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-city",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getCity(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test22_saveCompanyWithPermissionShouldFailIfInvoiceAddressIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but invoice address is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress(null);
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
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
                Assert.assertEquals("company-invoiceaddress",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-invoiceaddress",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }


    @Test
    public void test23_saveCompanyWithPermissionShouldFailIfInvoiceAddressIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but invoice address is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-invoiceaddress",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test24_saveCompanyWithPermissionShouldFailIfInvoiceAddressIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but invoice address is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("<script malicious code>");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-invoiceaddress",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getInvoiceAddress(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test25_saveCompanyWithPermissionShouldFailIfNationIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but nation is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation(null);
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
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
                Assert.assertEquals("company-nation",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-nation",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }


    @Test
    public void test26_saveCompanyWithPermissionShouldFailIfNationIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but nation is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-nation",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test27_saveCompanyWithPermissionShouldFailIfNationIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but nation is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("</script>");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-nation",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getNation(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test28_saveCompanyWithPermissionShouldFailIfPostalCodeIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but postal code is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode(null);
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
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
                Assert.assertEquals("company-postalcode",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-postalcode",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }


    @Test
    public void test29_saveCompanyWithPermissionShouldFailIfPostalCodeIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but postal code is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-postalcode",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test30_saveCompanyWithPermissionShouldFailIfPostalCodeIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but postal code is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("<script>malicious code</script>");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-postalcode",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getPostalCode(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test31_saveCompanyWithPermissionShouldFailIfVatNumberIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but vat number is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber(null);
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
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
                Assert.assertEquals("company-vatnumber",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-vatnumber",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }


    @Test
    public void test32_saveCompanyWithPermissionShouldFailIfVatNumberIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but vat number is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("");
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-vatnumber",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }


    @Test
    public void test33_saveCompanyWithPermissionShouldFailIfVatNumberIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but vat number is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("javascript:");
        company.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-vatnumber",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getVatNumber(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test34_findCompanyShouldFailIfHUserTriesToFindAnotherUserCompanyShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // huser, with permissions, tries to find Company with the following call findCompany,
        // but huser cannot search for another user's company
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FIND);
        HUser huser = createHUser(action);
        HUser huser2 = createHUser(null);
        Company company2 = createCompany(huser2);
        Assert.assertNotEquals(0, company2.getId());
        Assert.assertEquals(huser2.getId(), company2.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findCompany(company2.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test35_updateCompanyWithPermissionShouldFailIfBusinessNameIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but business name is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setBusinessName(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
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
                Assert.assertEquals("company-businessname",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-businessname",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test36_updateCompanyWithPermissionShouldFailIfBusinessNameIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but business name is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setBusinessName("");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-businessname",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test37_updateCompanyWithPermissionShouldFailIfBusinessNameIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but business name is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setBusinessName("onload(malicious code)=");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-businessname",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getBusinessName(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test38_updateCompanyWithPermissionShouldFailIfCityIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but city is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setCity(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
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
                Assert.assertEquals("company-city",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-city",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test39_updateCompanyWithPermissionShouldFailIfCityIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but city is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setCity("");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-city",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test40_updateCompanyWithPermissionShouldFailIfCityIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but city is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setCity("eval(malicious code)");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-city",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getCity(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test41_updateCompanyWithPermissionShouldFailIfInvoiceAddressIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but invoice address is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setInvoiceAddress(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
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
                Assert.assertEquals("company-invoiceaddress",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-invoiceaddress",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test42_updateCompanyWithPermissionShouldFailIfInvoiceAddressIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but invoice address is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setInvoiceAddress("");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-invoiceaddress",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test43_updateCompanyWithPermissionShouldFailIfInvoiceAddressIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but invoice address is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setInvoiceAddress("expression(malicious code)");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-invoiceaddress",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getInvoiceAddress(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test44_updateCompanyWithPermissionShouldFailIfNationIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but nation is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setNation(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
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
                Assert.assertEquals("company-nation",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-nation",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test45_updateCompanyWithPermissionShouldFailIfNationIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but nation is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setNation("");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-nation",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test46_updateCompanyWithPermissionShouldFailIfNationIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but nation is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setNation("javascript:");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-nation",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getNation(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test47_updateCompanyWithPermissionShouldFailIfPostalCodeIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but postal code is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setPostalCode(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
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
                Assert.assertEquals("company-postalcode",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-postalcode",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test48_updateCompanyWithPermissionShouldFailIfPostalCodeIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but postal code is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setPostalCode("");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-postalcode",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test49_updateCompanyWithPermissionShouldFailIfPostalCodeIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but postal code is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setPostalCode("vbscript:");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-postalcode",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getPostalCode(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }

    @Test
    public void test50_updateCompanyWithPermissionShouldFailIfVatNumberIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but vat number is null
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setVatNumber(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
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
                Assert.assertEquals("company-vatnumber",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
            if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
                msgValidationErrorsIsEmpty = true;
                Assert.assertEquals("must not be empty",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
                Assert.assertEquals("company-vatnumber",
                        ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
            }
        }
        Assert.assertTrue(msgValidationErrorsIsNull);
        Assert.assertTrue(msgValidationErrorsIsEmpty);
    }

    @Test
    public void test51_updateCompanyWithPermissionShouldFailIfVatNumberIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but vat number is empty
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setVatNumber("");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("must not be empty",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-vatnumber",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
    }

    @Test
    public void test52_updateCompanyWithPermissionShouldFailIfVatNumberIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but vat number is malicious code
        // response status code '422' HyperIoTValidationException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setVatNumber("</script>");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
        Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
        Assert.assertEquals("company-vatnumber",
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
        Assert.assertEquals(company.getVatNumber(),
                ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
    }


    @Test
    public void test53_saveCompanyWithPermissionShouldFailIfCompanyBelongsToAnotherUser() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but HUser belongs to another Company
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser2 = createHUser(null);
        Company company2 = createCompany(huser2);
        Assert.assertNotEquals(0, company2.getId());
        Assert.assertEquals(huser2.getId(), company2.getHUserCreator().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser2);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(company);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test54_updateCompanyWithPermissionShouldFailIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = new Company();
        company.setCity("entity not found");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test55_updateCompanyWithoutPermissionShouldFailAndIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to update Company not found with
        // the following call updateCompany
        // response status code '404' HyperIoTEntityNotFound
        HUser huser = createHUser(null);
        Company company = new Company();
        company.setCity("Unauthorized");
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test56_saveCompanyWithPermissionShouldFailIfEntityIsDuplicated() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to save Company with the following call saveCompany,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.SAVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());

        Company duplicateCompany = new Company();
        duplicateCompany.setBusinessName("ACSoftware");
        duplicateCompany.setCity("Lamezia Terme");
        duplicateCompany.setInvoiceAddress("Lamezia Terme");
        duplicateCompany.setNation("Italy");
        duplicateCompany.setPostalCode("88046");
        duplicateCompany.setVatNumber(company.getVatNumber());
        duplicateCompany.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(duplicateCompany);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean vatNumberIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("vatNumber")) {
                vatNumberIsDuplicated = true;
                Assert.assertEquals("vatNumber",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertTrue(vatNumberIsDuplicated);
    }


    @Test
    public void test57_saveCompanyWithoutPermissionShouldFailAndEntityIsDuplicated() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to save Company duplicated with the following call saveCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        Company duplicateCompany = new Company();
        duplicateCompany.setBusinessName("ACSoftware");
        duplicateCompany.setCity("Lamezia Terme");
        duplicateCompany.setInvoiceAddress("Lamezia Terme");
        duplicateCompany.setNation("Italy");
        duplicateCompany.setPostalCode("88046");
        duplicateCompany.setVatNumber(company.getVatNumber());
        duplicateCompany.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.saveCompany(duplicateCompany);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test58_updateCompanyWithPermissionShouldFailIfEntityIsDuplicated() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        HUser huser1 = createHUser(null);
        Company company1 = createCompany(huser1);
        Assert.assertNotEquals(0, company1.getId());
        Assert.assertEquals(huser1.getId(), company1.getHUserCreator().getId());

        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser2 = createHUser(action);
        Company duplicateCompany = createCompany(huser2);
        Assert.assertNotEquals(0, duplicateCompany.getId());
        Assert.assertNotEquals(company1.getVatNumber(), duplicateCompany.getVatNumber());
        Assert.assertEquals(huser2.getId(), duplicateCompany.getHUserCreator().getId());

        duplicateCompany.setVatNumber(company1.getVatNumber());
        Assert.assertEquals(company1.getVatNumber(), duplicateCompany.getVatNumber());
        this.impersonateUser(companyRestApi, huser2);
        Response restResponse = companyRestApi.updateCompany(duplicateCompany);
        Assert.assertEquals(422, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
        Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
        boolean vatNumberIsDuplicated = false;
        for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
            if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("vatNumber")) {
                vatNumberIsDuplicated = true;
                Assert.assertEquals("vatNumber",
                        ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
            }
        }
        Assert.assertTrue(vatNumberIsDuplicated);
    }


    @Test
    public void test59_updateCompanyWithoutPermissionShouldFailAndEntityIsDuplicated() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, without permission, tries to update Company duplicated with
        // the following call updateCompany
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser1 = createHUser(null);
        Company company1 = createCompany(huser1);
        Assert.assertNotEquals(0, company1.getId());
        Assert.assertEquals(huser1.getId(), company1.getHUserCreator().getId());

        HUser huser2 = createHUser(null);
        Company duplicateCompany = createCompany(huser2);
        Assert.assertNotEquals(0, duplicateCompany.getId());
        Assert.assertEquals(huser2.getId(), duplicateCompany.getHUserCreator().getId());
        Assert.assertNotEquals(company1.getVatNumber(), duplicateCompany.getVatNumber());

        duplicateCompany.setVatNumber(company1.getVatNumber());
        Assert.assertEquals(company1.getVatNumber(), duplicateCompany.getVatNumber());

        this.impersonateUser(companyRestApi, huser2);
        Response restResponse = companyRestApi.updateCompany(duplicateCompany);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test60_findAllCompanyPaginationWithPermissionShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, with permission,
        // find all Companies with pagination
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
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
        HyperIoTPaginableResult<Company> listCompany = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Company>>() {
                });
        Assert.assertEquals(5, listCompany.getResults().size());
        Assert.assertFalse(listCompany.getResults().isEmpty());
        Assert.assertEquals(2, listCompany.getCurrentPage());
        Assert.assertEquals(5, listCompany.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test61_findAllCompanyPaginationWithoutPermissionShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, without permission,
        // tries to find all Companies with pagination
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser(null);
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

    @Test
    public void test62_findAllCompanyPaginationWithPermissionShouldWorkIfDeltaAndPageAreNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, with permission,
        // find all Companies with pagination if delta and page are null
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = null;
        Integer page = null;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = createCompany(huser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompanyPaginated(delta, page);
        HyperIoTPaginableResult<Company> listCompany = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Company>>() {
                });
        Assert.assertEquals(10, listCompany.getResults().size());
        Assert.assertFalse(listCompany.getResults().isEmpty());
        Assert.assertEquals(1, listCompany.getCurrentPage());
        Assert.assertEquals(10, listCompany.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test63_findAllCompanyPaginationWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, with permission,
        // find all Companies with pagination if delta is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 2;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = createCompany(huser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompanyPaginated(-1, page);
        HyperIoTPaginableResult<Company> listCompany = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Company>>() {
                });
        Assert.assertEquals(10, listCompany.getResults().size());
        Assert.assertFalse(listCompany.getResults().isEmpty());
        Assert.assertEquals(2, listCompany.getCurrentPage());
        Assert.assertEquals(10, listCompany.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test64_findAllCompanyPaginationWithPermissionShouldWorkIfDeltaIsZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, with permission,
        // find all Companies with pagination if delta is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer page = 3;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = createCompany(huser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompanyPaginated(0, page);
        HyperIoTPaginableResult<Company> listCompany = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Company>>() {
                });
        Assert.assertEquals(10, listCompany.getResults().size());
        Assert.assertFalse(listCompany.getResults().isEmpty());
        Assert.assertEquals(3, listCompany.getCurrentPage());
        Assert.assertEquals(10, listCompany.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test65_findAllCompanyPaginationWithPermissionShouldWorkIfPageIsLowerThanZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, with permission,
        // find all Companies with pagination if page is lower than zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        Integer page = -1;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Company company = createCompany(huser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompanyPaginated(delta, page);
        HyperIoTPaginableResult<Company> listCompany = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Company>>() {
                });
        Assert.assertEquals(5, listCompany.getResults().size());
        Assert.assertFalse(listCompany.getResults().isEmpty());
        Assert.assertEquals(1, listCompany.getCurrentPage());
        Assert.assertEquals(5, listCompany.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test66_findAllCompanyPaginationWithPermissionShouldWorkIfPageIsZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, HUser, with permission,
        // find all Companies with pagination if page is zero
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.FINDALL);
        HUser huser = createHUser(action);
        Integer delta = 5;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Company company = createCompany(huser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.findAllCompanyPaginated(delta, 0);
        HyperIoTPaginableResult<Company> listCompany = restResponse
                .readEntity(new GenericType<HyperIoTPaginableResult<Company>>() {
                });
        Assert.assertEquals(5, listCompany.getResults().size());
        Assert.assertFalse(listCompany.getResults().isEmpty());
        Assert.assertEquals(1, listCompany.getCurrentPage());
        Assert.assertEquals(5, listCompany.getNextPage());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test67_triesToDeleteCompanyOfAnotherHUserShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with REMOVE permission, tries to delete Company associated
        // with another HUser, this operation is unauthorized
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        HUser huser2 = createHUser(null);
        Company company2 = createCompany(huser2);
        Assert.assertNotEquals(0, company2.getId());
        Assert.assertEquals(huser2.getId(), company2.getHUserCreator().getId());

        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser.getId(), company2.getHUserCreator().getId());

        this.impersonateUser(companyRestApi, huser);
        Response restResponseDeleteCompany = companyRestApi.deleteCompany(company2.getId());
        Assert.assertEquals(403, restResponseDeleteCompany.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponseDeleteCompany.getEntity()).getType());
    }


    @Test
    public void test68_deleteCompanyWithPermissionNotDeleteInCascadeHUserShouldWord() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with REMOVE permission, deletes his associated Company with
        // the following call deleteCompany, this call not delete in cascade
        // mode HUser
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.REMOVE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, huser);
        Response restResponseDeleteCompany = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(200, restResponseDeleteCompany.getStatus());
        // checks: HUser is still stored in database
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin@hyperiot.com", "admin");
        this.impersonateUser(hUserRestApi, adminUser);
        Response restResponse = hUserRestApi.findHUser(huser.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test69_deleteRelationshipBetweenHUserAndCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with UPDATE permission, setting the hUserCreator field to null
        // and removes the relationship between HUser and Company with the
        // following call updateCompany
        // response status code '200'
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setHUserCreator(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(((Company) restResponse.getEntity()).getHUserCreator());
        Assert.assertEquals(company.getEntityVersion() + 1,
                ((Company) restResponse.getEntity()).getEntityVersion());
    }


    @Test
    public void test70_deleteRelationshipBetweenCompanyAndAnotherHUserShouldFail() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with UPDATE permission, setting the hUserCreator field to null
        // and tries to removes the relationship between HUser and Company with
        // the following call updateCompany
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        HUser huser2 = createHUser(null);
        Company company2 = createCompany(huser2);
        Assert.assertNotEquals(0, company2.getId());
        Assert.assertEquals(huser2.getId(), company2.getHUserCreator().getId());

        company2.setHUserCreator(null);
        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company2);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test71_updateCompanyWithPermissionShouldFailIfCompanyBelongsToAnotherUser() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // HUser, with permission, tries to update Company with the following call updateCompany,
        // but huser2 belongs to another Company
        // response status code '403' HyperIoTUnauthorizedException
        HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(companyResourceName,
                HyperIoTCrudAction.UPDATE);
        HUser huser = createHUser(action);
        Company company1 = createCompany(huser);
        Assert.assertEquals(huser.getId(), company1.getHUserCreator().getId());

        HUser huser2 = createHUser(null);
        Company company2 = createCompany(huser2);

        Assert.assertEquals(huser2.getId(), company2.getHUserCreator().getId());
        Assert.assertNotEquals(huser.getId(), huser2.getId());
        Assert.assertNotEquals(huser2.getId(), company1.getHUserCreator().getId());

        // huser tries to set huser2 (associated with company2) inside company1
        company1.setHUserCreator(huser2);

        this.impersonateUser(companyRestApi, huser);
        Response restResponse = companyRestApi.updateCompany(company1);
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


}
