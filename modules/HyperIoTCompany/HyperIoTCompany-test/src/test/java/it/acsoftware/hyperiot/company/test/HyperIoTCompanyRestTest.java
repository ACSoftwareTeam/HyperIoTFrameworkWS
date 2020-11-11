package it.acsoftware.hyperiot.company.test;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
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

import static it.acsoftware.hyperiot.company.test.HyperIoTCompanyConfiguration.hyperIoTException;
import static it.acsoftware.hyperiot.company.test.HyperIoTCompanyConfiguration.getBaseConfiguration;

/**
 * @author Aristide Cittadino Interface component for Company System Service.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTCompanyRestTest extends KarafTestSupport {

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
        assertContains("HyperIoTHadoopManager-features ", features);
        assertContains("HyperIoTHBaseConnector-features", features);
        String datasource = executeCommand("jdbc:ds-list");
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
    public void test02_saveCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin save Company with the following call saveCompany
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
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
    }


    @Test
    public void test03_saveCompanyShouldFailIfNotLogged() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call tries to save Company, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser();
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
    public void test04_updateCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin update Company with the following call updateCompany
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setCity("Bologna");
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertEquals("Bologna", ((Company) restResponse.getEntity()).getCity());
        Assert.assertEquals(company.getEntityVersion() + 1,
                ((Company) restResponse.getEntity()).getEntityVersion());
    }


    @Test
    public void test05_updateCompanyShouldFailIfNotLogged() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call tries to update Company, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser();
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        company.setCity("Bologna");
        this.impersonateUser(companyRestApi, null);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test06_findCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin find Company with the following call findCompany
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test07_findCompanyShouldFailIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to find Company with the following call findCompany,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.findCompany(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test08_findCompanyShouldFailIfNotLogged() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call tries to find Company, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser();
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, null);
        Response restResponse = companyRestApi.findCompany(company.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test09_findAllCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin find all Company with the following call findAllCompany
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test10_findAllCompanyShouldFailIfNotLogged() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call tries to find all Company, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser();
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, null);
        Response restResponse = companyRestApi.findAllCompany();
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test11_deleteCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin delete Company with the following call deleteCompany
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }

    @Test
    public void test12_deleteCompanyShouldFailIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to delete Company with the following call deleteCompany,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.deleteCompany(0);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }

    @Test
    public void test13_deleteCompanyShouldFailIfNotLogged() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call tries to find all Company, but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        HUser huser = createHUser();
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, null);
        Response restResponse = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test14_saveCompanyShouldFailIfBusinessNameIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but business name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName(null);
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test15_saveCompanyShouldFailIfBusinessNameIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but business name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test16_saveCompanyShouldFailIfBusinessNameIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but business name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("expression(malicious code)");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test17_saveCompanyShouldFailIfCityIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but city is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity(null);
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test18_saveCompanyShouldFailIfCityIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but city is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test19_saveCompanyShouldFailIfCityIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but city is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("src='malicious code'");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test20_saveCompanyShouldFailIfInvoiceAddressIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but invoice address is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress(null);
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test21_saveCompanyShouldFailIfInvoiceAddressIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but invoice address is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test22_saveCompanyShouldFailIfInvoiceAddressIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but invoice address is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("<script malicious code>");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test23_saveCompanyShouldFailIfNationIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but nation is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation(null);
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test24_saveCompanyShouldFailIfNationIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but nation is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test25_saveCompanyShouldFailIfNationIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but nation is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("</script>");
        company.setPostalCode("88046");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test26_saveCompanyShouldFailIfPostalCodeIsNull() {
        // hadmin tries to save Company with the following call saveCompany,
        // but postal code is null
        // response status code '422' HyperIoTValidationException
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode(null);
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test27_saveCompanyShouldFailIfPostalCodeIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but postal code is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test28_saveCompanyShouldFailIfPostalCodeIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but postal code is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("<script>malicious code</script>");
        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        company.setHUserCreator(huser);
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
    public void test29_saveCompanyShouldFailIfVatNumberIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but vat number is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber(null);
        company.setHUserCreator(huser);
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
    public void test30_saveCompanyShouldFailIfVatNumberIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but vat number is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("");
        company.setHUserCreator(huser);
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
    public void test31_saveCompanyShouldFailIfVatNumberIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but vat number is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        this.impersonateUser(companyRestApi, adminUser);
        HUser huser = createHUser();
        Company company = new Company();
        company.setBusinessName("ACSoftware");
        company.setCity("Lamezia Terme");
        company.setInvoiceAddress("Lamezia Terme");
        company.setNation("Italy");
        company.setPostalCode("88046");
        company.setVatNumber("eval(malicious code)");
        company.setHUserCreator(huser);
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
    public void test32_findCompanyShouldFailIfAdminTriesToFindAnotherUserCompany() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to find Company with the following call findCompany,
        // but hadmin cannot search for another user's company
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        HUser huser = createHUser();
        Company company = createCompany(huser);
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.findCompany(company.getId());
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test33_updateCompanyShouldFailIfBusinessNameIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but business name is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setBusinessName(null);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test34_updateCompanyShouldFailIfBusinessNameIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but business name is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setBusinessName("");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test35_updateCompanyShouldFailIfBusinessNameIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but business name is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setBusinessName("<script malicious code>");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test36_updateCompanyShouldFailIfCityIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but city is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setCity(null);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test37_updateCompanyShouldFailIfCityIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but city is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setCity("");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test38_updateCompanyShouldFailIfCityIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but city is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setCity("</script>");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test39_updateCompanyShouldFailIfInvoiceAddressIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but invoice address is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setInvoiceAddress(null);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test40_updateCompanyShouldFailIfInvoiceAddressIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but invoice address is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setInvoiceAddress("");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test41_updateCompanyShouldFailIfInvoiceAddressIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but invoice address is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setInvoiceAddress("vbscript:");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test42_updateCompanyShouldFailIfNationIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but nation is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setNation(null);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test43_updateCompanyShouldFailIfNationIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but nation is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setNation("");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test44_updateCompanyShouldFailIfNationIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but nation is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setNation("onload(malicious code)=");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test45_updateCompanyShouldFailIfPostalCodeIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but postal code is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setPostalCode(null);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test46_updateCompanyShouldFailIfPostalCodeIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but postal code is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setPostalCode("");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test47_updateCompanyShouldFailIfPostalCodeIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but postal code is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setPostalCode("expression(malicious code)");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test48_updateCompanyShouldFailIfVatNumberIsNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but vat number is null
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setVatNumber(null);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test49_updateCompanyShouldFailIfVatNumberIsEmpty() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but vat number is empty
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setVatNumber("");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test50_updateCompanyShouldFailIfVatNumberIsMaliciousCode() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but vat number is malicious code
        // response status code '422' HyperIoTValidationException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        company.setVatNumber("eval(malicious code)");
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test51_updateCompanyShouldFailIfEntityNotFound() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but entity not found
        // response status code '404' HyperIoTEntityNotFound
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = new Company();
        company.setCity("entity not found");
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(404, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test52_saveCompanyShouldFailIfEntityIsDuplicated() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to save Company with the following call saveCompany,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
        HUser huser = createHUser();
        Company duplicateCompany = new Company();
        duplicateCompany.setBusinessName("ACSoftware");
        duplicateCompany.setCity("Lamezia Terme");
        duplicateCompany.setInvoiceAddress("Lamezia Terme");
        duplicateCompany.setNation("Italy");
        duplicateCompany.setPostalCode("88046");
        duplicateCompany.setVatNumber(company.getVatNumber());
        duplicateCompany.setHUserCreator(huser);
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test53_updateCompanyShouldFailIfEntityIsDuplicated() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin tries to update Company with the following call updateCompany,
        // but entity is duplicated
        // response status code '422' HyperIoTDuplicateEntityException
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Company company1 = createCompany((HUser) adminUser);
        Assert.assertNotEquals(0, company1.getId());
        Assert.assertEquals(adminUser.getId(), company1.getHUserCreator().getId());

        HUser huser = createHUser();
        Company duplicateCompany = createCompany(huser);
        Assert.assertNotEquals(0, duplicateCompany.getId());
        Assert.assertEquals(huser.getId(), duplicateCompany.getHUserCreator().getId());
        Assert.assertNotEquals(company1.getVatNumber(), duplicateCompany.getVatNumber());

        duplicateCompany.setVatNumber(company1.getVatNumber());
        Assert.assertEquals(company1.getVatNumber(), duplicateCompany.getVatNumber());

        this.impersonateUser(companyRestApi, adminUser);
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
    public void test54_findAllCompanyPaginationShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, hadmin find all Companies with pagination
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = 5;
        Integer page = 2;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Company company = createCompany((HUser) adminUser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test55_findAllCompanyPaginationShouldFailIfNotLogged() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // the following call tries to find all Companies with pagination,
        // but HUser is not logged
        // response status code '403' HyperIoTUnauthorizedException
        this.impersonateUser(companyRestApi, null);
        Response restResponse = companyRestApi.findAllCompanyPaginated(null, null);
        Assert.assertEquals(403, restResponse.getStatus());
        Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
                ((HyperIoTBaseError) restResponse.getEntity()).getType());
    }


    @Test
    public void test56_findAllCompanyPaginationShouldWorkIfDeltaAndPageAreNull() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, hadmin find all Companies with pagination
        // if delta and page are null
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = null;
        Integer page = null;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = createCompany((HUser) adminUser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test57_findAllCompanyPaginationShouldWorkIfDeltaIsLowerThanZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, hadmin find all Companies with pagination
        // if delta is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer page = 2;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = createCompany((HUser) adminUser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test58_findAllCompanyPaginationShouldWorkIfDeltaIsZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, hadmin find all Companies with pagination
        // if delta is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer page = 3;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Company company = createCompany((HUser) adminUser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test59_findAllCompanyPaginationShouldWorkIfPageIsLowerThanZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, hadmin find all Companies with pagination
        // if page is lower than zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = 5;
        Integer page = -1;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Company company = createCompany((HUser) adminUser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test60_findAllCompanyPaginationShouldWorkIfPageIsZero() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // In this following call findAllCompany, hadmin find all Companies with pagination
        // if page is zero
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
        Integer delta = 5;
        List<Company> companies = new ArrayList<>();
        for (int i = 0; i < delta; i++) {
            Company company = createCompany((HUser) adminUser);
            Assert.assertNotEquals(0, company.getId());
            Assert.assertEquals(adminUser.getId(), company.getHUserCreator().getId());
            companies.add(company);
        }
        this.impersonateUser(companyRestApi, adminUser);
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
    public void test61_deleteCompanyNotDeleteInCascadeHUserShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin delete Company associated with HUser with the following
        // call deleteCompany this call not delete HUser in cascade mode
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin@hyperiot.com", "admin");
        HUser huser = createHUser();
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, adminUser);
        Response restResponseDeleteCompany = companyRestApi.deleteCompany(company.getId());
        Assert.assertEquals(200, restResponseDeleteCompany.getStatus());

        // checks: HUser is still stored in database
        HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
        this.impersonateUser(hUserRestApi, adminUser);
        Response restResponse = hUserRestApi.findHUser(huser.getId());
        Assert.assertEquals(200, restResponse.getStatus());
    }


    @Test
    public void test62_removeRelationshipBetweenHUserAndCompanyShouldWork() {
        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
        // hadmin setting the hUserCreator field to null and removes the relationship
        // between HUser and Company with the following call updateCompany
        // response status code '200'
        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
        HyperIoTUser adminUser = (HUser) authService.login("hadmin@hyperiot.com", "admin");
        HUser huser = createHUser();
        Company company = createCompany(huser);
        Assert.assertNotEquals(0, company.getId());
        Assert.assertEquals(huser.getId(), company.getHUserCreator().getId());
        this.impersonateUser(companyRestApi, adminUser);
        company.setHUserCreator(null);
        Response restResponse = companyRestApi.updateCompany(company);
        Assert.assertEquals(200, restResponse.getStatus());
        Assert.assertNull(((Company) restResponse.getEntity()).getHUserCreator());
        Assert.assertEquals(company.getEntityVersion() + 1,
                ((Company) restResponse.getEntity()).getEntityVersion());
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
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.isAdmin());
        HUser huser = new HUser();
        huser.setName("name");
        huser.setLastname("lastname");
        huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
        huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
        huser.setPassword("passwordPass&01");
        huser.setPasswordConfirm("passwordPass&01");
        huser.setAdmin(false);
        Response restResponse = hUserRestApi.saveHUser(huser);
        Assert.assertEquals(200, restResponse.getStatus());
        return huser;
    }


//    private Company createCompany() {
//        CompanyRestApi companyRestApi = getOsgiService(CompanyRestApi.class);
//        AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
//        HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
//        this.impersonateUser(companyRestApi, adminUser);
//        Company company = new Company();
//        company.setBusinessName("ACSoftware");
//        company.setCity("Lamezia Terme");
//        company.setInvoiceAddress("Lamezia Terme");
//        company.setNation("Italy");
//        company.setPostalCode("88046");
//        company.setVatNumber("01234567890" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
//        company.setHUserCreator((HUser) adminUser);
//        Response restResponse = companyRestApi.saveCompany(company);
//        Assert.assertEquals(200, restResponse.getStatus());
//        return company;
//    }


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