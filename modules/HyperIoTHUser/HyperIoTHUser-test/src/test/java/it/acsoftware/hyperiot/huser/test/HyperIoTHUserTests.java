package it.acsoftware.hyperiot.huser.test;

import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.authentication.service.rest.AuthenticationRestApi;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.exception.HyperIoTDuplicateEntityException;
import it.acsoftware.hyperiot.base.exception.HyperIoTScreenNameAlreadyExistsException;
import it.acsoftware.hyperiot.base.exception.HyperIoTUserNotActivated;
import it.acsoftware.hyperiot.base.exception.HyperIoTValidationException;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.huser.api.HUserApi;
import it.acsoftware.hyperiot.huser.api.HUserSystemApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.model.HUserPasswordReset;
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

import javax.validation.ConstraintViolation;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.acsoftware.hyperiot.huser.test.HyperIoTHUserConfiguration.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHUserTests extends KarafTestSupport {

	@Configuration
	public Option[] config() {
		// starts with HSQL
		// the standard configuration has been moved to the HyperIoTHUserConfiguration class
		return HyperIoTTestConfigurationBuilder.createStandardConfiguration()
//				.withDebug("5010", false)
				.append(getBaseConfiguration()).build();
	}

	public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi, HyperIoTUser user) {
		return restApi.impersonate(user);
	}

	/*
	 *
	 *
	 * REST API TESTS
	 *
	 *
	 */

	@Test
	public void test00_hyperIoTFrameworkShouldBeInstalled() {
		// assert on an available service
		assertServiceAvailable(FeaturesService.class);
		String features = executeCommand("feature:list -i");
		assertContains("HyperIoTBase-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTRole-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTMail", features);
		assertContains("HyperIoTAuthentication-features ", features);
		assertContains("HyperIoTHBaseConnector-features", features);
		String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
		// checks that datasource is installed correctly
		assertContains("hyperiot", datasource);
	}

	@Test
	public void test01_huserModuleShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.checkModuleWorking();
		Assert.assertNotNull(huserRestService);
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test02_huserModuleShouldFailIfNotLogged() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		this.impersonateUser(huserRestService, null);
		Response restResponse = huserRestService.checkModuleWorking();
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test03_registerHUserShouldSuccess() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call register a new HUser
		// response status code '200'
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Assert.assertNull(huser.getActivateCode());
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertFalse(((HUser) restResponse.getEntity()).isActive());
		Assert.assertEquals(huser.getActivateCode(), ((HUser) restResponse.getEntity()).getActivateCode());
		Assert.assertFalse(((HUser) restResponse.getEntity()).getActivateCode().isEmpty());
	}

	@Test
	public void test04_registerHUserShouldFailIfUsernameMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but username is malformed
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername("username&&&&");
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-username",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test05_registerHUserShouldFailIfUsernameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but username is null
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(null);
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test06_registerHUserShouldFailIfUsernameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but username is empty
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername("");
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsEmpty = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Allowed characters are letters (lower and upper cases) and numbers")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());

		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test07_registerHUserShouldFailIfUsernameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but username is malicious code
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername("<script>console.log()</script>");
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Allowed characters are letters (lower and upper cases) and numbers")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test08_registerHUserShouldFailIfEmailIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but email is malformed
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("wrongEmail");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must be a well-formed email address",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-email",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test09_registerHUserShouldFailIfEmailIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but email is malicious code
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("<script>console.log()</script>");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must be a well-formed email address")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("must be a well-formed email address",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-email",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test10_registerHUserShouldFailIfEmailIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but email is null
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(null);
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-email",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test11_registerHUserShouldFailIfEmailIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but email is empty
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-email",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test12_registerHUserShouldFailIfPasswordIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but password is malformed
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("wrong");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test13_registerHUserShouldFailIfPasswordIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but password is malicious code
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("<script>console.log()</script>");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(5, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test14_registerHUserShouldFailIfPasswordIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but password is null
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword(null);
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(5, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		boolean msgValidationErrorsIsNull = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be null")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsNull = true;
				Assert.assertEquals("must not be null",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
		Assert.assertTrue(msgValidationErrorsIsNull);
	}

	@Test
	public void test15_registerHUserShouldFailIfPasswordConfirmIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but password is null
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm(null);
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test16_registerHUserShouldFailIfPasswordIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but password is empty
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test17_registerHUserShouldFailIfPasswordConfirmIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but passwordConfirm is malformed
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("wrong_ Password___");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test18_registerHUserShouldFailIfPasswordConfirmIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but passwordConfirm is malicious code
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("<script>console.log()</script>");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test19_registerHUserShouldFailIfPasswordConfirmIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// a new user tries to register with the platform,
		// but passwordConfirm is empty
		// response status code '422' HyperIoTValidationException
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("");
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test20_saveHUserShouldWork() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin save HUser with the following call saveHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
		Assert.assertFalse(((HUser) restResponse.getEntity()).isActive());
		Assert.assertNull(((HUser) restResponse.getEntity()).getActivateCode());
	}

	@Test
	public void test21_saveHUserShouldFailIfNotLogged() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// the following call tries to save HUser, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		this.impersonateUser(hUserRestApi, null);
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test22_saveHUserShouldFailIfNameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// name is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName(null);
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-name",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test23_saveHUserShouldFailIfNameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// name is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-name",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test24_saveHUserShouldFailIfNameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// name is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("</script>");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-name",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test25_saveHUserShouldFailIfLastnameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// lastname is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname(null);
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-lastname",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test26_saveHUserShouldFailIfLastnameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// lastname is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-lastname",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test27_saveHUserShouldFailIfLastnameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// lastname is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("javascript:");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-lastname",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test28_saveHUserShouldFailIfUsernameIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// username is malformed
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("&%/&%/");
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-username",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test29_saveHUserShouldFailIfUsernameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// username is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("eval(test malicious code)");
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Allowed characters are letters (lower and upper cases) and numbers")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test30_saveHUserShouldFailIfUsernameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// username is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername(null);
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test31_saveHUserShouldFailIfUsernameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// username is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("");
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsEmpty = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Allowed characters are letters (lower and upper cases) and numbers")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());

		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test32_saveHUserShouldFailIfEmailIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// email is malformed
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("wrongEmail");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must be a well-formed email address",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-email",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test33_saveHUserShouldFailIfEmailIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// email is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("vbscript:");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must be a well-formed email address")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("must be a well-formed email address",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-email",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test34_saveHUserShouldFailIfEmailIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// email is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(null);
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-email",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test35_saveHUserShouldFailIfEmailIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// email is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-email",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test36_saveHUserShouldFailIfPasswordIsMalformed() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// password is malformed
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordMalformed");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test37_saveHUserShouldFailIfPasswordIsMaliciousCode() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// password is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("expression(malitius code)");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(5, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test38_saveHUserShouldFailIfPasswordIsNull() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// password is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword(null);
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(5, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		boolean msgValidationErrorsIsNull = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be null")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsNull = true;
				Assert.assertEquals("must not be null",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
		Assert.assertTrue(msgValidationErrorsIsNull);
	}

	@Test
	public void test39_saveHUserShouldFailIfPasswordIsEmpty() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// password is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("");
		huser.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test40_saveHUserShouldFailIfPasswordConfirmIsMalformed() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// passwordConfirm is malformed
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("FirstUser09&");
		huser.setPasswordConfirm("passwordMalformed");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test41_saveHUserShouldFailIfPasswordConfirmIsMaliciousCode() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// passwordConfirm is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername("TestUser" + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("FirstUser09&");
		huser.setPasswordConfirm("<script>console.log()</script>");
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test42_findHUserShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with the following call findHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findHUser(huser.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(huser.getId(), ((HUser) restResponse.getEntity()).getId());
	}

	@Test
	public void test43_findHUserShouldFailIfNotLogged() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call tries to find HUser, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, null);
		Response restResponse = huserRestService.findHUser(huser.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test44_findHUserShouldFailIfEntityNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to find HUser, but entity not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findHUser(0);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test45_findAllHUserShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with the following call findAllHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findAllHUser();
		List<HUser> listHUsers = restResponse.readEntity(new GenericType<List<HUser>>() {
		});
		Assert.assertNotNull(listHUsers);
		boolean huserFound = false;
		for (HUser husers : listHUsers) {
			if (huser.getId() == husers.getId())
				huserFound = true;
		}
		Assert.assertTrue(huserFound);
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test46_findAllHUserShouldFailIfNotLogged() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call tries to find all HUser, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		createHUser();
		this.impersonateUser(huserRestService, null);
		Response restResponse = huserRestService.findAllHUser();
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test47_updateHUserShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin updates HUser name with the following call updateHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setName("edited");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(huser.getId(), ((HUser) restResponse.getEntity()).getId());
		Assert.assertEquals("edited", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test48_updateHUserShouldFailIfNotLogged() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call tries to update HUser, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		HUser huser = createHUser();
		huser.setName("edited failed");
		this.impersonateUser(huserRestService, null);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test49_updateHUserShouldFailIfNameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser name with the following call updateHUser,
		// but name is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setName(null);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-name",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test50_updateHUserShouldFailIfNameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser name with the following call updateHUser,
		// but name is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setName("");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-name",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test51_updateHUserShouldFailIfNameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser name with the following call updateHUser,
		// but name is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setName("</script>");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-name",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test52_updateHUserShouldFailIfLastnameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser lastname with the following call updateHUser,
		// but lastname is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setLastname(null);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
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
			Assert.assertEquals("huser-lastname",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test53_updateHUserShouldFailIfLastnameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser lastname with the following call updateHUser,
		// but lastname is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setLastname("");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-lastname",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test54_updateHUserShouldFailIfLastnameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser lastname with the following call updateHUser,
		// but lastname is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setLastname("javascript:");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-lastname",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test55_updateHUserShouldFailIfUsernameIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser username with the following call updateHUser,
		// but username is malformed
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setUsername("username&&&&&");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-username",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test56_updateHUserShouldFailIfUsernameIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser username with the following call updateHUser,
		// but username is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setUsername("vbscript:");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Allowed characters are letters (lower and upper cases) and numbers")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test57_updateHUserShouldFailIfUsernameIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser username with the following call updateHUser,
		// but username is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setUsername(null);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test58_updateHUserShouldFailIfUsernameIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser username with the following call updateHUser,
		// but username is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setUsername("");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsEmpty = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Allowed characters are letters (lower and upper cases) and numbers")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-username",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test59_updateHUserShouldFailIfEmailIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser email with the following call updateHUser,
		// but email is malformed
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setEmail("malformedEmail");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must be a well-formed email address",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-email",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test60_updateHUserShouldFailIfEmailIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser email with the following call updateHUser,
		// but email is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setEmail("</script>");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(2, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsCharsAndNumbers = false;
		boolean msgValidationErrorsIsMaliciousCode = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must be a well-formed email address")) {
				msgValidationErrorsIsCharsAndNumbers = true;
				Assert.assertEquals("must be a well-formed email address",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
				msgValidationErrorsIsMaliciousCode = true;
				Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-email",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsCharsAndNumbers);
		Assert.assertTrue(msgValidationErrorsIsMaliciousCode);
	}

	@Test
	public void test61_updateHUserShouldFailIfEmailIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser email with the following call updateHUser,
		// but email is null
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setEmail(null);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
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
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
			}
			Assert.assertEquals("huser-email",
					((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}

	@Test
	public void test62_updateHUserShouldFailIfEmailIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser email with the following call updateHUser,
		// but email is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setEmail("");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("huser-email",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test63_updateHUserShouldFailIfTryToChangePassword() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to change the HUser password but fails. Password can be changed
		// with changePassword method
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		huser.setPassword("passwordPass/01");
		huser.setPasswordConfirm("passwordPass/01");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test64_updateHUserShouldFailIfEntityNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to update HUser, but entity not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser user = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, user);
		HUser huser = new HUser();
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test65_removeHUserShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// in this call hadmin delete HUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.deleteHUser(huser.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNull(restResponse.getEntity());
	}

	@Test
	public void test66_removeHUserShouldFailIfNotLogged() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// this call tries to delete HUser, but HUser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, null);
		Response restResponse = huserRestService.deleteHUser(huser.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test67_removeHUserShouldFailIfEntityNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin tries to delete HUser, but entity not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.deleteHUser(0);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test68_loginHUserFailedIfUserNotActivated() {
		// HUser tries to log in but fails because he is not active
		// response status code '403' HyperIoTUserNotActivated
		HUser huser = registerHUser();
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		Response restResponse = authRestService.login(huser.getUsername(), huser.getPassword());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUserNotActivated",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.authentication.error.user.not.active",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test69_activateNewHUserShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call activateUser with his email and reset code
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser huser = registerHUser();

		//checks: huser is not active
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		Response restResponse = authRestService.login(huser.getUsername(), huser.getPassword());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUserNotActivated",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.authentication.error.user.not.active",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));

		// Activate  huser
		Assert.assertFalse(huser.isActive());
		String activationCode = huser.getActivateCode();
		Assert.assertNotNull(activationCode);
		Response restResponseActivateUser = huserRestService.activate(huser.getEmail(), activationCode);
		Assert.assertEquals(200, restResponseActivateUser.getStatus());
		boolean userActivated;
		try {
			huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
			userActivated = true;
		} catch (HyperIoTUserNotActivated ex) {
			userActivated = false;
			Assert.assertFalse(huser.isActive());
		}
		Assert.assertTrue(userActivated);
		Assert.assertTrue(huser.isActive());
	}

	@Test
	public void test70_activateUserShouldFailIfPasswordResetCodeIsWrong() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call activateUser, but it fail because reset code is wrong
		// response status code '422' HyperIoTWrongUserActivationCode
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser huser = registerHUser();
		Assert.assertFalse(huser.isActive());
		String activationCode = huser.getActivateCode();
		Assert.assertNotNull(activationCode);
		Response restResponse = huserRestService.activate(huser.getEmail(), "wrongActivationCode");
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTWrongUserActivationCode",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.activation.failed",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
		boolean userActivated = true;
		try {
			huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
		} catch (HyperIoTUserNotActivated ex) {
			userActivated = false;
		}
		Assert.assertFalse(userActivated);
		Assert.assertFalse(huser.isActive());
	}

	@Test
	public void test71_activateUserShouldFailIfEntityNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call activateUser, but it fail because user not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HUser huser = registerHUser();
		Assert.assertFalse(huser.isActive());
		String activationCode = huser.getActivateCode();
		Assert.assertNotNull(activationCode);
		Response restResponse = huserRestService.activate("wrongEmail", activationCode);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		boolean userActivated = true;
		try {
			huser = (HUser) authService.login(huser.getUsername(), "passwordPass&01");
		} catch (HyperIoTUserNotActivated ex) {
			userActivated = false;
		}
		Assert.assertFalse(userActivated);
		Assert.assertFalse(huser.isActive());
	}

	@Test
	public void test72_activateUserShouldFailIfUserAlreadyActivated() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call activateUser, but it fail because user is already
		// activated
		// response status code '422' HyperIoTUserAlreadyActivated
		HUser huser = createHUser(); //huser is already activated
		String activationCode = huser.getActivateCode();
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.activate(huser.getEmail(), activationCode);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUserAlreadyActivated",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.already.activated",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test73_resetPasswordRequestShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPasswordRequest
		// response status code '200'
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.resetPasswordRequest(huser.getEmail());
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test74_resetPasswordRequestShouldFailIfMailNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPasswordRequest, but email
		// not found in db
		// response status code '404' HyperIoTEntityNotFound
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.resetPasswordRequest("wrongEmail");
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test75_resetPasswordShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword
		// response status code '200'
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test76_resetPasswordShouldFailIfResetCodeIsWrong() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// resetCode is wrong
		// response status code '422' HyperIoTWrongUserPasswordResetCode
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		pwdReset.setResetCode("wrongResetCode");
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTWrongUserPasswordResetCode",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.password.reset.failed",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test77_resetPasswordShouldFailIfResetCodeIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// resetCode is null
		// response status code '422' HyperIoTWrongUserPasswordResetCode
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		pwdReset.setResetCode(null);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTWrongUserPasswordResetCode",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.password.reset.failed",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test78_resetPasswordShouldFailIfResetCodeIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// resetCode is empty
		// response status code '422' HyperIoTWrongUserPasswordResetCode
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		pwdReset.setResetCode("");
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTWrongUserPasswordResetCode",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.password.reset.failed",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test79_resetPasswordShouldFailIfResetCodeIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// resetCode is malicious code
		// response status code '422' HyperIoTWrongUserPasswordResetCode
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		pwdReset.setResetCode("eval(malicious code)");
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTWrongUserPasswordResetCode",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.password.reset.failed",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test80_resetPasswordShouldFailIfMailNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// user email not found
		// response status code '404' HyperIoTEntityNotFound
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail("mailNotFound");
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test81_resetPasswordShouldFailIfPasswordIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// password is null
		// response status code '500' HyperIoTRuntimeException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword(null);
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.error.huser.password.reset.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test82_resetPasswordShouldFailIfPasswordIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// password is empty
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test83_resetPasswordShouldFailIfPasswordIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// password is malicious code
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("vbscript:");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test84_resetPasswordShouldFailIfPasswordConfirmIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// passwordConfirm is null
		// response status code '500' HyperIoTRuntimeException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm(null);
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.error.huser.password.reset.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test85_resetPasswordShouldFailIfPasswordConfirmIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// passwordConfirm is empty
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test86_resetPasswordShouldFailIfPasswordConfirmIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// passwordConfirm is malicious code
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("vbscript:");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test87_resetPasswordShouldFailIfPasswordAndPasswordConfirmAreDifferent() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// password and passwordConfirm are different
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass10/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test88_resetPasswordShouldFailIfPasswordIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// password is malformed
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("new");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test89_resetPasswordShouldFailIfPasswordConfirmIsMalformed() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser reset password with the following call resetPassword, but
		// passwordConfirm is malformed
		// response status code '422' HyperIoTValidationException
		HUser huser = requestResetPassword();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("new");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test90_resetPasswordShouldFailIfResetCodeIsNullInDatabase() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser tries to reset password with the following call resetPassword, but
		// resetCode is null in database
		// response status code '422' HyperIoTWrongUserPasswordResetCode
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, huser);
		HUserPasswordReset pwdReset = new HUserPasswordReset();
		pwdReset.setPassword("newPass01/");
		pwdReset.setPasswordConfirm("newPass01/");
		pwdReset.setEmail(huser.getEmail());
		String resetCode = huser.getPasswordResetCode();
		pwdReset.setResetCode(resetCode);
		Response restResponse = huserRestService.resetPassword(pwdReset);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTWrongUserPasswordResetCode",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.acsoftware.hyperiot.huser.error.password.reset.failed",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test91_findAllHUserPaginationShouldWorkIfDeltaAndPageIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with pagination with the following call findAllHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		List<HUser> husers;
		husers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			HUser huser = createHUser();
			husers.add(huser);
		}
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findAllHUserPaginated(null, null);
		HyperIoTPaginableResult<HUser> listHUsers = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<HUser>>() {
				});
		Assert.assertEquals(10, listHUsers.getResults().size());
		Assert.assertFalse(listHUsers.getResults().isEmpty());
		Assert.assertEquals(1, listHUsers.getCurrentPage());
		Assert.assertEquals(10, listHUsers.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test92_findAllHUserPaginationShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with pagination with the following call findAllHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Integer delta = 5;
		Integer page = 2;
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser();
			husers.add(huser);
		}
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findAllHUserPaginated(delta, page);
		HyperIoTPaginableResult<HUser> listHUsers = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<HUser>>() {
				});
		Assert.assertEquals(5, listHUsers.getResults().size());
		Assert.assertFalse(listHUsers.getResults().isEmpty());
		Assert.assertEquals(2, listHUsers.getCurrentPage());
		Assert.assertEquals(5, listHUsers.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test93_findAllHUserPaginationShouldWorkIfDeltaIsLowerThanZero() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with pagination with the following call findAllHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Integer delta = 5;
		Integer page = 1;
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser();
			husers.add(huser);
		}
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findAllHUserPaginated(-1, page);
		HyperIoTPaginableResult<HUser> listHUsers = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<HUser>>() {
				});
		Assert.assertEquals(10, listHUsers.getResults().size());
		Assert.assertFalse(listHUsers.getResults().isEmpty());
		Assert.assertEquals(1, listHUsers.getCurrentPage());
		Assert.assertEquals(10, listHUsers.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test94_findAllHUserPaginationShouldWorkIfDeltaIsZero() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with pagination with the following call findAllHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Integer page = 1;
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			HUser huser = createHUser();
			husers.add(huser);
		}
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findAllHUserPaginated(0, page);
		HyperIoTPaginableResult<HUser> listHUsers = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<HUser>>() {
				});
		Assert.assertEquals(10, listHUsers.getResults().size());
		Assert.assertFalse(listHUsers.getResults().isEmpty());
		Assert.assertEquals(1, listHUsers.getCurrentPage());
		Assert.assertEquals(10, listHUsers.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test95_findAllHUserPaginationShouldWorkIfPageIsLowerThanZero() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin find all HUser with pagination with the following call findAllHUser
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Integer delta = 5;
		Integer page = -1;
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser();
			husers.add(huser);
		}
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.findAllHUserPaginated(delta, page);
		HyperIoTPaginableResult<HUser> listHUsers = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<HUser>>() {
				});
		Assert.assertEquals(5, listHUsers.getResults().size());
		Assert.assertFalse(listHUsers.getResults().isEmpty());
		Assert.assertEquals(1, listHUsers.getCurrentPage());
		Assert.assertEquals(5, listHUsers.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}

	@Test
	public void test96_changePasswordShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser changes password of HUser the following call
		// changePassword
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Assert.assertTrue(HyperIoTUtil.getPasswordHash(oldPassword).equals(huser.getPassword()));
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(HyperIoTUtil.getPasswordHash(newPassword), ((HUser) restResponse.getEntity()).getPassword());
		Assert.assertEquals(HyperIoTUtil.getPasswordHash(passwordConfirm), ((HUser) restResponse.getEntity()).getPasswordConfirm());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test97_changePasswordShouldFailIfUserNotFound() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but HUser not found
		// response status code '404' HyperIoTEntityNotFound
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(0, oldPassword, newPassword, passwordConfirm);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test98_changePasswordShouldFailIfOldPasswordIsWrong() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but oldPassword is wrong
		// response status code '500' HyperIoTRuntimeException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "wrongPass";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.match",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test99_changePasswordShouldFailIfOldPasswordIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but oldPassword is wrong
		// response status code '500' HyperIoTRuntimeException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), null, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test100_changePasswordShouldFailIfOldPasswordIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but oldPassword is empty
		// response status code '500' HyperIoTRuntimeException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.match",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test101_changePasswordShouldFailIfOldPasswordIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but oldPassword is malicious code
		// response status code '500' HyperIoTRuntimeException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "javascript:";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.match",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test102_changePasswordShouldFailIfNewPasswordIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but newPassword is null
		// response status code '500' HyperIoTRuntimeException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, null,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test103_changePasswordShouldFailIfNewPasswordIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but newPassword is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String newPassword = "";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test104_changePasswordShouldFailIfNewPasswordIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but newPassword is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String newPassword = "eval(malicious code)";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test105_changePasswordShouldFailIfPasswordConfirmIsNull() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but passwordConfirm is null
		// response status code '500' HyperIoTRuntimeException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				null);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test106_changePasswordShouldFailIfPasswordConfirmIsEmpty() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but passwordConfirm is empty
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test107_changePasswordShouldFailIfPasswordConfirmIsMaliciousCode() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// adminUser tries to change password of HUser, with the following
		// call, but passwordConfirm is malicious code
		// response status code '422' HyperIoTValidationException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "javascript:";
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test108_changePasswordWithPermissionShouldWork() {
		// HUser, with permission, changes his password with
		// the following call changePassword
		// response status code '200'
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Assert.assertTrue(HyperIoTUtil.getPasswordHash(oldPassword).equals(huser.getPassword()));
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(HyperIoTUtil.getPasswordHash(newPassword), ((HUser) restResponse.getEntity()).getPassword());
		Assert.assertEquals(HyperIoTUtil.getPasswordHash(passwordConfirm), ((HUser) restResponse.getEntity()).getPasswordConfirm());
		Assert.assertNotEquals(huser.getEntityVersion(),
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test109_changePasswordWithPermissionShouldFailIfOldPasswordIsNull() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but oldPassword is null
		// response status code '500' HyperIoTRuntimeException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), null, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test110_changePasswordWithPermissionShouldFailIfOldPasswordIsEmpty() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but oldPassword is empty
		// response status code '500' HyperIoTRuntimeException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.match",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test111_changePasswordWithPermissionShouldFailIfOldPasswordIsMaliciousCode() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but oldPassword is malicious code
		// response status code '500' HyperIoTRuntimeException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "javascript:";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.match",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test112_changePasswordWithPermissionShouldFailIfNewPasswordIsNull() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but newPassword is null
		// response status code '500' HyperIoTRuntimeException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, null,
				passwordConfirm);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test113_changePasswordWithPermissionShouldFailIfNewPasswordIsEmpty() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but newPassword is empty
		// response status code '422' HyperIoTValidationException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test114_changePasswordWithPermissionShouldFailIfNewPasswordIsMaliciousCode() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but newPassword is malicious code
		// response status code '422' HyperIoTValidationException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "javascript:";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test115_changePasswordWithPermissionShouldFailIfPasswordConfirmIsNull() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but passwordConfirm is null
		// response status code '500' HyperIoTRuntimeException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				null);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("it.ascoftware.hyperiot.error.password.not.null",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test116_changePasswordWithPermissionShouldFailIfPasswordConfirmIsEmpty() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but passwordConfirm is empty
		// response status code '422' HyperIoTValidationException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test117_changePasswordWithPermissionShouldFailIfPasswordConfirmIsMaliciousCode() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but passwordConfirm is malicious code
		// response status code '422' HyperIoTValidationException
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "vbscript:";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(4, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		boolean msgValidationErrorsIsPasswordMustMatch = false;
		boolean msgValidationErrorsIsInvalidPassword = false;
		boolean msgValidationErrorsIsPasswordConfirmMustMatch = false;
		boolean msgValidationErrorsIsInvalidPasswordConfirm = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsPasswordMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Passwords must match")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsPasswordConfirmMustMatch = true;
				Assert.assertEquals("Passwords must match",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-password")) {
				msgValidationErrorsIsInvalidPassword = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("Invalid password")
					&& ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField().contentEquals("huser-passwordConfirm")) {
				msgValidationErrorsIsInvalidPasswordConfirm = true;
				Assert.assertEquals("Invalid password",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("huser-passwordConfirm",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsPasswordMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPassword);
		Assert.assertTrue(msgValidationErrorsIsPasswordConfirmMustMatch);
		Assert.assertTrue(msgValidationErrorsIsInvalidPasswordConfirm);
	}

	@Test
	public void test118_changePasswordWithPermissionShouldFailIfEntityNotFound() {
		// HUser, with permission, tries to change his password with the
		// following call changePassword, but HUser id is zero
		// response status code '404' HyperIoTEntityNotFound
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(0, oldPassword, newPassword, passwordConfirm);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test119_changePasswordWithoutPermissionShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser, without permission, changes his password with the following call changePassword
		// response status code '200'
		HUser huser = createHUser(null);
		String oldPassword = "passwordPass&01";
		String newPassword = "testPass01/";
		String passwordConfirm = "testPass01/";
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.changeHUserPassword(huser.getId(), oldPassword, newPassword,
				passwordConfirm);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(HyperIoTUtil.getPasswordHash(newPassword), ((HUser) restResponse.getEntity()).getPassword());
		Assert.assertEquals(HyperIoTUtil.getPasswordHash(passwordConfirm), ((HUser) restResponse.getEntity()).getPasswordConfirm());
		Assert.assertNotEquals(HyperIoTUtil.getPasswordHash(oldPassword), ((HUser) restResponse.getEntity()).getPassword());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test120_changeAccountInfoShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser changes his account info with the following call changeAccountInfo
		// response status code '200'
		HUser huser = createHUser();
		this.impersonateUser(huserRestService, huser);
		huser.setName("temporaryNameUpdateAccount");
		huser.setLastname("temporaryLastnameUpdateAccount");
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals("temporaryNameUpdateAccount", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals("temporaryLastnameUpdateAccount", ((HUser) restResponse.getEntity()).getLastname());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test121_changeAccountInfoShouldFailIfNotLogged() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call tries to change account info of HUser,
		// but huser is not logged
		// response status code '403' HyperIoTUnauthorizedException
		HUser huser = createHUser();
		huser.setName("temporaryNameUpdateAccount");
		huser.setLastname("temporaryLastnameUpdateAccount");
		this.impersonateUser(huserRestService, null);
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test122_changeAccountInfoShouldFailIfUsernameIsDuplicated() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to change his account info with the following call updateAccountInfo,
		// but username is duplicated
		// response HyperIoTScreenNameAlreadyExistsException
		HUser huser = createHUser();
		HyperIoTContext ctx = huserRestService.impersonate(huser);
		huser.setName("name edited");
		huser.setLastname("lastname edited");
		huser.setUsername("hadmin");
		boolean usernameIsDuplicated = false;
		try {
			huserApi.updateAccountInfo(ctx, huser);
		} catch (HyperIoTScreenNameAlreadyExistsException ex) {
			Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException: Screen name already exists", ex.toString());
			Assert.assertEquals("Screen name already exists", ex.getMessage());
			Assert.assertEquals("username", ex.getFieldName());
			usernameIsDuplicated = true;
		}
		Assert.assertTrue(usernameIsDuplicated);
	}


	@Test
	public void test123_changeAccountInfoShouldFailIfEmailIsDuplicated() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// the following call tries to change account info of HUser,
		// but email is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		HUser huser = createHUser();
		huser.setEmail("hadmin@hyperiot.com");
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("email",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}

	@Test
	public void test124_changeAccountInfoShouldFailIfUserTryToChangeInfoOfAnotherUser() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser 1 try to change account info of HUser with the following call
		// changeAccountInfo
		// response status code '403' HyperIoTUnauthorizedException
		HUser huser = createHUser();
		HUser huser1 = createHUser();
		this.impersonateUser(huserRestService, huser1);
		huser.setName("temporaryNameUpdateAccount");
		huser.setLastname("temporaryLastnameUpdateAccount");
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}

	@Test
	public void test125_changeAccountInfoWithPermissionShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser changes his account info with Permission with the following call
		// changeAccountInfo
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		huser.setName("updateNameAccount");
		huser.setLastname("updateLastnameAccount");
		huser.setEmail("updateemail@hyperiot.com");
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals("updateNameAccount", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals("updateLastnameAccount", ((HUser) restResponse.getEntity()).getLastname());
		Assert.assertEquals("updateemail@hyperiot.com", ((HUser) restResponse.getEntity()).getEmail());
		Assert.assertNotEquals(huser.getEntityVersion(),
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test126_changeAccountInfoWithPermissionShouldFailIfEmailIsDuplicated() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser tries to change his account info with Permission with the following
		// call changeAccountInfo, but email is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		huser.setEmail("hadmin@hyperiot.com");
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("email",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test127_changeAccountInfoWithPermissionShouldFailIfUsernameIsDuplicated() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to change his account info with Permission with the following
		// call changeAccountInfo, but username is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(huserResourceName, HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);
		HyperIoTContext ctx = huserRestService.impersonate(huser);
		huser.setUsername("hadmin");
		boolean usernameIsDuplicated = false;
		try {
			huserApi.updateAccountInfo(ctx, huser);
		} catch (HyperIoTScreenNameAlreadyExistsException ex) {
			Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException: Screen name already exists", ex.toString());
			Assert.assertEquals("Screen name already exists", ex.getMessage());
			Assert.assertEquals("username", ex.getFieldName());
			usernameIsDuplicated = true;
		}
		Assert.assertTrue(usernameIsDuplicated);
	}

	@Test
	public void test128_changeAccountInfoWithoutPermissionShouldWork() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// HUser, without Permission, changes his account info with
		// the following call changeAccountInfo
		// response status code '200'
		HUser huser = createHUser(null);
		huser.setName("updateNameAccount");
		huser.setLastname("updateLastnameAccount");
		this.impersonateUser(huserRestService, huser);
		Response restResponse = huserRestService.updateAccountInfo(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals("updateNameAccount", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals("updateLastnameAccount", ((HUser) restResponse.getEntity()).getLastname());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test129_saveHUserShouldFailIfUsernameIsDuplicated() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// username is duplicated
		// response status code '422' HyperIoTScreenNameAlreadyExistsException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huserDuplicated = new HUser();
		huserDuplicated.setName("name");
		huserDuplicated.setLastname("lastname");
		huserDuplicated.setUsername(huser.getUsername());
		huserDuplicated.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huserDuplicated.setPassword("passwordPass&01");
		huserDuplicated.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huserDuplicated);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("Screen name already exists",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("username",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}

	@Test
	public void test130_saveHUserShouldFailIfEmailIsDuplicated() {
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		// hadmin tries to save HUser with the following call saveHUser, but
		// email is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		this.impersonateUser(hUserRestApi, adminUser);
		HUser huserDuplicated = new HUser();
		huserDuplicated.setName("name");
		huserDuplicated.setLastname("lastname");
		huserDuplicated.setUsername("testusername" + UUID.randomUUID().toString().replaceAll("-", ""));
		huserDuplicated.setEmail(huser.getEmail());
		huserDuplicated.setPassword("passwordPass&01");
		huserDuplicated.setPasswordConfirm("passwordPass&01");
		Response restResponse = hUserRestApi.saveHUser(huserDuplicated);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("email", ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test131_hadminActivateHUserAccountManually() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin activate HUser with the following call updateHUser.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = registerHUser();
		Assert.assertFalse(huser.isActive());
		huser.setActive(true);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertTrue(huser.isActive());
		Assert.assertEquals(huser.isActive(), ((HUser) restResponse.getEntity()).isActive());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}

	@Test
	public void test132_hadminDeactivateHUserAccountManually() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		// hadmin deactivate HUser with the following call updateHUser.
		// response status code '200'
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HUser huser = createHUser();
		Assert.assertTrue(huser.isActive());
		huser.setActive(false);
		this.impersonateUser(huserRestService, adminUser);
		Response restResponse = huserRestService.updateHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertFalse(huser.isActive());
		Assert.assertEquals(huser.isActive(), ((HUser) restResponse.getEntity()).isActive());
		Assert.assertEquals(huser.getEntityVersion() + 1,
				((HUser) restResponse.getEntity()).getEntityVersion());
	}



	/*
	 *
	 * Utility methods: create new HUser and assigns permissions
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
		huser.setActive(true);
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
				testPermission = permission;
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
		role.setDescription("Description" + java.util.UUID.randomUUID());
		Response restResponse = roleRestApi.saveRole(role);
		Assert.assertEquals(200, restResponse.getStatus());
		return role;
	}

	private HUser requestResetPassword() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(huserRestService, adminUser);
		String username = "TestUser";
		HUser huser = new HUser();
		huser.setName("name");
		huser.setLastname("lastname");
		huser.setUsername(username + UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail("testusername" + UUID.randomUUID().toString() + "@hyperiot.com");
		huser.setPassword("passwordPass01/");
		huser.setPasswordConfirm("passwordPass01/");
		huser.setActive(true);
		huser.setAdmin(false);
		huser.setPasswordResetCode(java.util.UUID.randomUUID().toString());
		Response restResponse = huserRestService.saveHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		return huser;
	}

	private HUser registerHUser() {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		this.impersonateUser(huserRestService, null);
		HUser huser = new HUser();
		huser.setName("Mark");
		huser.setLastname("Norris");
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setActive(false);
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		Assert.assertNull(huser.getActivateCode());
		Response restResponse = huserRestService.register(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		return huser;
	}

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
		huser.setActive(true);
		huser.setAdmin(false);
		Response restResponse = hUserRestApi.saveHUser(huser);
		Assert.assertEquals(200, restResponse.getStatus());
		return huser;
	}

	/*
	 *
	 *
	 * SERVICE LAYER
	 *
	 *
	 */

	@Test
	public void test01_findUserByUsernameWork() {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser find user by username with the following call
		// findUserByUsername
		HyperIoTUser adminUser = huserApi.findUserByUsername("hadmin");
		Assert.assertEquals("hadmin", adminUser.getUsername());
		Assert.assertNotNull(adminUser);
	}

	@Test
	public void test02_findUserByUsernameFailIfUsernameNotFound() {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to find user by username with the following call
		// findUserByUsername, but user not found
		HyperIoTUser adminUser = huserApi.findUserByUsername("wrongUsername");
		Assert.assertNull(adminUser);
	}

	@Test
	public void test03_registerUserWork() {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// the following call register a new HUser
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		huserApi.registerUser(huser, null);
		HyperIoTContext ctx = huserRestApi.impersonate(huser);
		Assert.assertNotNull(ctx);
		Assert.assertEquals(ctx.getLoggedUsername(), huser.getUsername());
		Assert.assertNotEquals(0, huser.getId());
	}

	@Test
	public void test04_registerUserFailIfNameIsEmpty() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but name is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("");
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test05_registerUserFailIfNameIsNull() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but name is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsMustNotBeNull = false;
		try {
			HUser huser = new HUser();
			huser.setName(null);
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test06_registerUserFailIfNameIsMaliciousCode() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but name is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		try {
			HUser huser = new HUser();
			huser.setName("javascript:");
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
	}

	@Test
	public void test07_registerUserFailIfLastnameIsEmpty() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but lastname is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("");
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test08_registerUserFailIfLastnameIsNull() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but lastname is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsMustNotBeNull = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname(null);
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
	}

	@Test
	public void test09_registerUserFailIfLastnameIsMaliciousCode() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but lastname is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("vbscript:");
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
	}

	@Test
	public void test10_registerUserFailIfEmailIsMalformed() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but email is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsEmailMustBeWellFormed = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail("wrongMail");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.Email.message}")) {
					validationExceptionIsEmailMustBeWellFormed = true;
					Assert.assertEquals("must be a well-formed email address",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsEmailMustBeWellFormed);
	}

	@Test
	public void test11_registerUserFailIfEmailIsNull() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but email is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsMustNotBeNull = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(null);
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
	}

	@Test
	public void test12_registerUserFailIfEmailIsEmpty() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but email is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail("");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test13_registerUserFailIfEmailIsMaliciousCode() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but email is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		boolean validationExceptionIsMustBeWellFormed = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail("eval(malicious code)");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.Email.message}")) {
					validationExceptionIsMustBeWellFormed = true;
					Assert.assertEquals("must be a well-formed email address",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
		Assert.assertTrue(validationExceptionIsMustBeWellFormed);
	}

	@Test
	public void test14_registerUserFailIfUsernameIsMalformed() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but username is malformed
		// response HyperIoTValidationException
		boolean validationException = false;
		boolean validationExceptionIsAllowedLettersNumbers = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername("%&/%&%&/");
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			huserApi.registerUser(huser, null);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Allowed characters are letters (lower and upper cases) and numbers")) {
					validationExceptionIsAllowedLettersNumbers = true;
					Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsAllowedLettersNumbers);
	}

	@Test
	public void test15_registerUserFailIfUsernameIsNull() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but username is null
		// response HyperIoTValidationException
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsMustNotBeNull = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(null);
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			huserApi.registerUser(huser, null);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
	}

	@Test
	public void test16_registerUserFailIfUsernameIsEmpty() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but username is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsAllowedLettersNumbers = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername("");
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Allowed characters are letters (lower and upper cases) and numbers")) {
					validationExceptionIsAllowedLettersNumbers = true;
					Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsAllowedLettersNumbers);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test17_registerUserFailIfUsernameIsMaliciousCode() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but username is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsCharsAndNumbers = false;
		boolean validationExceptionIsMaliciousCode = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername("</script>");
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Allowed characters are letters (lower and upper cases) and numbers")) {
					validationExceptionIsCharsAndNumbers = true;
					Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsCharsAndNumbers);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
	}

	@Test
	public void test18_registerUserFailIfPasswordIsMalformed() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but password is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("malformed");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test19_registerUserFailIfPasswordIsNull() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but password is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsMustNotBeNull = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + UUID.randomUUID());
			huser.setLastname("lastname" + UUID.randomUUID());
			huser.setUsername(UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword(null);
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(3, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test20_registerUserFailIfPasswordIsEmpty() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but password is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test21_registerUserFailIfPasswordIsMaliciousCode() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but password is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsMaliciousCode = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("eval(malicious code)");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(3, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessageTemplate());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test22_registerUserFailIfPasswordConfirmIsMalformed() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but passwordConfirm is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("malformed");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test23_registerUserFailIfPasswordConfirmIsNull() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but passwordConfirm is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm(null);
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test24_registerUserFailIfPasswordConfirmIsEmpty() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but passwordConfirm is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test25_registerUserFailIfPasswordConfirmIsMaliciousCode() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform, but passwordConfirm is malicious
		// code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("</script>");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test26_registerUserFailIfPasswordIsNotEqualsPasswordConfirm() throws HyperIoTValidationException {
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to register with the platform,
		// but password is not equals to passwordConfirm
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("pAssw0rdPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test27_changeAccountInfoShouldFailIfUsernameIsDuplicated() throws HyperIoTScreenNameAlreadyExistsException {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to change his account info with the following call updateAccountInfo,
		// but username is duplicated
		// response HyperIoTScreenNameAlreadyExistsException
		HUser huser = createHUser();
		HyperIoTContext ctx = huserRestService.impersonate(huser);
		huser.setName("name edited");
		huser.setLastname("lastname edited");
		huser.setUsername("hadmin");
		boolean usernameIsDuplicated = false;
		try {
			huserApi.updateAccountInfo(ctx, huser);
		} catch (HyperIoTScreenNameAlreadyExistsException ex) {
			Assert.assertEquals(hyperIoTException + "HyperIoTScreenNameAlreadyExistsException: Screen name already exists", ex.toString());
			Assert.assertEquals("Screen name already exists", ex.getMessage());
			Assert.assertEquals("username", ex.getFieldName());
			usernameIsDuplicated = true;
		}
		Assert.assertTrue(usernameIsDuplicated);
	}

	@Test
	public void test28_changeAccountInfoShouldFailIfEmailIsDuplicated() throws HyperIoTDuplicateEntityException {
		HUserRestApi huserRestService = getOsgiService(HUserRestApi.class);
		HUserApi huserApi = getOsgiService(HUserApi.class);
		// HUser tries to change his account info with the following call updateAccountInfo,
		// but email is duplicated
		// response HyperIoTDuplicateEntityException
		HUser huser = createHUser();
		HyperIoTContext ctx = huserRestService.impersonate(huser);
		huser.setName("name edited");
		huser.setLastname("lastname edited");
		huser.setEmail("hadmin@hyperiot.com");
		boolean emailIsDuplicated = false;
		try {
			huserApi.updateAccountInfo(ctx, huser);
		} catch (HyperIoTDuplicateEntityException ex) {
			Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException: email", ex.toString());
			Assert.assertEquals("email", ex.getMessage());
			emailIsDuplicated = true;
		}
		Assert.assertTrue(emailIsDuplicated);
	}

	/*
	 *
	 *
	 * SYSTEM SERVICE TESTS
	 *
	 *
	 */

	@Test
	public void test01s_findUserByUsernameWork() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser find user by username with the following call
		// findUserByUsername
		HyperIoTUser adminUser = huserSystemApi.findUserByUsername("hadmin");
		Assert.assertEquals("hadmin", adminUser.getUsername());
		Assert.assertNotNull(adminUser);
	}

	@Test
	public void test02s_findUserByUsernameFailIfUsernameNotFound() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to find user by username with the following call
		// findUserByUsername, but user not found
		HyperIoTUser user = huserSystemApi.findUserByUsername("wrongUsername");
		Assert.assertNull(user);
	}

	@Test
	public void test03s_registerUserWork() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// the following call register a new HUser
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		HUser huser = new HUser();
		huser.setName("name" + java.util.UUID.randomUUID());
		huser.setLastname("lastname" + java.util.UUID.randomUUID());
		huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
		huser.setPassword("passwordPass&01");
		huser.setPasswordConfirm("passwordPass&01");
		HyperIoTContext ctx = huserRestApi.impersonate(huser);
		huserSystemApi.registerUser(huser, ctx);
		Assert.assertEquals(ctx.getLoggedUsername(), huser.getUsername());
		Assert.assertNotNull(ctx);
		Assert.assertNotEquals(0, huser.getId());
	}

	@Test
	public void test04s_registerUserFailIfNameIsEmpty() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but name is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("");
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test05s_registerUserFailIfNameIsNull() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but name is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsMustNotBeNull = false;
		try {
			HUser huser = new HUser();
			huser.setName(null);
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
	}

	@Test
	public void test06s_registerUserFailIfNameIsMaliciousCode() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but name is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		try {
			HUser huser = new HUser();
			huser.setName("eval(malicious code)");
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
	}

	@Test
	public void test07s_registerUserFailIfLastnameIsEmpty() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but lastname is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("");
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test08s_registerUserFailIfLastnameIsNull() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but lastname is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeNull = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname(null);
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test09s_registerUserFailIfLastnameIsMaliciousCode() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but lastname is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("javascript:");
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
	}

	@Test
	public void test10s_registerUserFailIfEmailIsMalformed() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but email is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsEmailMustBeWellFormed = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail("malformed");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.Email.message}")) {
					validationExceptionIsEmailMustBeWellFormed = true;
					Assert.assertEquals("must be a well-formed email address",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsEmailMustBeWellFormed);
	}

	@Test
	public void test11s_registerUserFailIfEmailIsNull() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but email is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeNull = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(null);
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test12s_registerUserFailIfEmailIsEmpty() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but email is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail("");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
	}

	@Test
	public void test13s_registerUserFailIfEmailIsMaliciousCode() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but email is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		boolean validationExceptionIsEmailMustBeWellFormed = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail("vbscript:");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.Email.message}")) {
					validationExceptionIsEmailMustBeWellFormed = true;
					Assert.assertEquals("must be a well-formed email address",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
		Assert.assertTrue(validationExceptionIsEmailMustBeWellFormed);
	}

	@Test
	public void test14s_registerUserFailIfUsernameIsMalformed() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but username is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsAllowedLettersNumbers = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername("%&/%&%&/");
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(1, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Allowed characters are letters (lower and upper cases) and numbers")) {
					validationExceptionIsAllowedLettersNumbers = true;
					Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsAllowedLettersNumbers);
	}

	@Test
	public void test15s_registerUserFailIfUsernameIsNull() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but username is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsMustNotBeNull = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(null);
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")) {
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
	}

	@Test
	public void test16s_registerUserFailIfUsernameIsEmpty() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but username is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMustNotBeEmpty = false;
		boolean validationExceptionIsAllowedLettersNumbers = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername("");
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotEmpty.message}")) {
					validationExceptionIsMustNotBeEmpty = true;
					Assert.assertEquals("must not be empty",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("Allowed characters are letters (lower and upper cases) and numbers")) {
					validationExceptionIsAllowedLettersNumbers = true;
					Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMustNotBeEmpty);
		Assert.assertTrue(validationExceptionIsAllowedLettersNumbers);
	}

	@Test
	public void test17s_registerUserFailIfUsernameIsMaliciousCode() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but username is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsMaliciousCode = false;
		boolean validationExceptionIsAllowedLettersNumbers = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername("</script>");
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")) {
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("Allowed characters are letters (lower and upper cases) and numbers")) {
					validationExceptionIsAllowedLettersNumbers = true;
					Assert.assertEquals("Allowed characters are letters (lower and upper cases) and numbers",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
		Assert.assertTrue(validationExceptionIsAllowedLettersNumbers);
	}

	@Test
	public void test18s_registerUserFailIfPasswordIsMalformed() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but password is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("malformed");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()) {
				if (violation.getMessageTemplate().contains("Invalid password")) {
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")) {
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test19s_registerUserFailIfPasswordIsNull() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but password is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsMustNotBeNull = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword(null);
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(3, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{javax.validation.constraints.NotNull.message}")){
					validationExceptionIsMustNotBeNull = true;
					Assert.assertEquals("must not be null",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsMustNotBeNull);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test20s_registerUserFailIfPasswordIsEmpty() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but password is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test21s_registerUserFailIfPasswordIsMaliciousCode() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but password is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		boolean validationExceptionIsMaliciousCode = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("eval(malicious code)");
			huser.setPasswordConfirm("passwordPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(3, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}")){
					validationExceptionIsMaliciousCode = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
		Assert.assertTrue(validationExceptionIsMaliciousCode);
	}

	@Test
	public void test22s_registerUserFailIfPasswordConfirmIsMalformed() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but passwordConfirm is malformed
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("malformed");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test23s_registerUserFailIfPasswordConfirmIsNull() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but passwordConfirm is null
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm(null);
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test24s_registerUserFailIfPasswordConfirmIsEmpty() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform, but passwordConfirm is empty
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsInvalidPassword = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test25s_registerUserFailIfPasswordConfirmIsMaliciousCode() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform,
		// but passwordConfirm is malicious code
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		boolean validationExceptionIsInvalidPassword = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("javascript:");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
	}

	@Test
	public void test26s_registerUserFailIfPasswordIsNotEqualsPasswordConfirm() throws HyperIoTValidationException {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to register with the platform,
		// but password is not equals to passwordConfirm
		// response HyperIoTValidationException
		HUserRestApi huserRestApi = getOsgiService(HUserRestApi.class);
		boolean validationException = false;
		boolean validationExceptionIsPasswordMustMatch = false;
		boolean validationExceptionIsInvalidPassword = false;
		try {
			HUser huser = new HUser();
			huser.setName("name" + java.util.UUID.randomUUID());
			huser.setLastname("lastname" + java.util.UUID.randomUUID());
			huser.setUsername(java.util.UUID.randomUUID().toString().replaceAll("-", ""));
			huser.setEmail(java.util.UUID.randomUUID() + "@hyperiot.com");
			huser.setPassword("passwordPass&01");
			huser.setPasswordConfirm("pAssw0rdPass&01");
			HyperIoTContext ctx = huserRestApi.impersonate(huser);
			Assert.assertNotNull(ctx);
			huserSystemApi.registerUser(huser, ctx);
		} catch (HyperIoTValidationException ex) {
			validationException = true;
			Assert.assertEquals(2, ex.getViolations().size());
			for (ConstraintViolation violation : ex.getViolations()){
				if (violation.getMessageTemplate().contains("Invalid password")){
					validationExceptionIsInvalidPassword = true;
					Assert.assertEquals("Invalid password",
							violation.getMessage());
				}
				if (violation.getMessageTemplate().contains("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}")){
					validationExceptionIsPasswordMustMatch = true;
					Assert.assertEquals("{it.acsoftware.hyperiot.huser.validator.passwordMustMatch.message}",
							violation.getMessage());
				}
			}
		}
		Assert.assertTrue(validationException);
		Assert.assertTrue(validationExceptionIsInvalidPassword);
		Assert.assertTrue(validationExceptionIsPasswordMustMatch);
	}

	@Test
	public void test27s_findByEmailWork() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser find user by email with the following call
		// findUserByEmail
		HyperIoTUser adminUser = huserSystemApi.findUserByEmail("hadmin@hyperiot.com");
		Assert.assertEquals("hadmin@hyperiot.com", adminUser.getEmail());
		Assert.assertNotNull(adminUser);
	}

	@Test
	public void test28s_findByEmailFailIfMailNotFound() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// HUser tries to find user by email with the following call
		// findUserByEmail, but email not found
		HyperIoTUser user = huserSystemApi.findUserByEmail("wrongMail");
		Assert.assertNull(user);
	}

	@Test
	public void test29s_loginWithEmailAddressShouldWork() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// hadmin authenticate with his email address and password
		// with the following call login
		HUser huser = huserSystemApi.login("hadmin@hyperiot.com", "admin");
		Assert.assertNotNull(huser);
		Assert.assertTrue(huser.isActive());
		Assert.assertNotEquals(0, huser.getId());
	}

	@Test
	public void test30s_loginWithEmailAddressShouldFailIfEmailIsNotFound() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// hadmin tries to authenticates with his email address and password
		// with the following call login, but email is wrong
		HUser huser = huserSystemApi.login("wrongEmail", "admin");
		Assert.assertNull(huser);
	}

	@Test
	public void test31s_loginWithEmailAddressShouldFailIfUserIsNotActivated() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// hadmin tries to authenticates with his email address and password
		// with the following call login, but huser is not activated
		HUser huser = registerHUser();
		boolean userIsActivated = true;
		try {
			huser = huserSystemApi.login(huser.getEmail(), "passwordPass&01");
		} catch (HyperIoTUserNotActivated ex) {
			userIsActivated = false;
			Assert.assertFalse(huser.isActive());
			Assert.assertNotEquals(0, huser.getId());
		}
		Assert.assertFalse(userIsActivated);
	}


	@Test
	public void test32s_loginWithEmailAddressShouldFailIfPasswordIsWrong() {
		HUserSystemApi huserSystemApi = getOsgiService(HUserSystemApi.class);
		// hadmin tries to authenticates with his email address and password
		// with the following call login, but password is wrong
		HUser huser = createHUser();
		Assert.assertTrue(huser.isActive());
		huser = huserSystemApi.login(huser.getEmail(), "wrong");
		Assert.assertNull(huser);
	}


}
