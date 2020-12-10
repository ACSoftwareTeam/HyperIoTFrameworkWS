package it.acsoftware.hyperiot.authentication.test;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.authentication.model.JWTLoginResponse;
import it.acsoftware.hyperiot.authentication.service.rest.AuthenticationRestApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.huser.model.HUser;
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

import javax.ws.rs.core.Response;

import static it.acsoftware.hyperiot.authentication.test.HyperIoTAuthenticationConfiguration.getConfiguration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTAuthenticationRestTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
		// starts with HSQL
		// the standard configuration has been moved to the HyperIoTAuthenticationConfiguration class
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
	public void test01_authenticationModuleShouldWork() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// the following call checkModuleWorking checks if Authentication module working
		// correctly
		Response restResponse = authRestService.checkModuleWorking();
		Assert.assertEquals(200, restResponse.getStatus());
	}

	// TO DO: ADD TEST FOR REPOSITORY SYSTEM SERVICE AND SERVICE

	@Test
	public void test02_loginRestServiceShouldFailIfInsertingBadCredential() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// HUser tries to authenticate with the following call login, but credentials
		// are bad
		Response restResponse = authRestService.login("wrongUser", "wrongPassword");
		Assert.assertEquals(401, restResponse.getStatus());
		Assert.assertEquals("Unauthorized", restResponse.getStatusInfo().getReasonPhrase());
	}

	@Test
	public void test03_loginRestServiceShouldFailIfPasswordIsWrong() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// HUser tries to authenticate with the following call login, but password is
		// wrong
		Response restResponse = authRestService.login("hadmin", "wrongPassword");
		Assert.assertEquals(401, restResponse.getStatus());
		Assert.assertEquals("Unauthorized", restResponse.getStatusInfo().getReasonPhrase());
	}

	@Test
	public void test04_loginRestServiceShouldFailIfUserIsWrong() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// HUser tries to authenticate with the following call login, but username is
		// wrong
		Response restResponse = authRestService.login("wrongUser", "admin");
		Assert.assertEquals(401, restResponse.getStatus());
		Assert.assertEquals("Unauthorized", restResponse.getStatusInfo().getReasonPhrase());
	}

	@Test
	public void test05_loginRestServiceShouldSuccess() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// HUser authenticate with the following call login
		Response restResponse = authRestService.login("hadmin", "admin");
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertFalse(((JWTLoginResponse) restResponse.getEntity()).getToken().isEmpty());
        Assert.assertNotNull(((JWTLoginResponse) restResponse.getEntity()).getToken());
	}


	@Test
	public void test06_testWhoAmI() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// Impersonating
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser user = (HUser)authService.login("hadmin", "admin");
		this.impersonateUser(authRestService, user);
		Response restResponse = authRestService.whoAmI();
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test07_loginRestServiceShouldFailIfUserInsertingHashedPassword() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// HUser tries to authenticate with hashed password with the following call login
		Response restResponse = authRestService.login("hadmin", "ISMvKXpXpadDiUoOSoAfww==");
		Assert.assertEquals(401, restResponse.getStatus());
		Assert.assertEquals("Unauthorized", restResponse.getStatusInfo().getReasonPhrase());
	}

	@Test
	public void test08_loginWithEmailAddressShouldSuccess() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// admin authenticates with the following call login
		Response restResponse = authRestService.login("hadmin@hyperiot.com", "admin");
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertFalse(((JWTLoginResponse) restResponse.getEntity()).getToken().isEmpty());
		Assert.assertNotNull(((JWTLoginResponse) restResponse.getEntity()).getToken());
		HyperIoTContext ctx = authRestService.impersonate((HyperIoTUser) ((JWTLoginResponse) restResponse.getEntity()).getAuthenticable());
		Assert.assertEquals("hadmin", ctx.getLoggedUsername());
	}

	@Test
	public void test09_loginWithEmailAddressShouldFailIfPasswordIsWrong() {
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		// admin authenticates with the following call login, but password is wrong
		Response restResponse = authRestService.login("hadmin@hyperiot.com", "wrong");
		Assert.assertEquals(401, restResponse.getStatus());
		Assert.assertEquals("Unauthorized", restResponse.getStatusInfo().getReasonPhrase());
	}


}
