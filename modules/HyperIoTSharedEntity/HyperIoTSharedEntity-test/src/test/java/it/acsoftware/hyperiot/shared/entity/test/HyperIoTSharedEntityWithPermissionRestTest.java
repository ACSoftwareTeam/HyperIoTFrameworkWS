package it.acsoftware.hyperiot.shared.entity.test;

import it.acsoftware.hyperiot.area.model.Area;
import it.acsoftware.hyperiot.area.service.rest.AreaRestApi;
import it.acsoftware.hyperiot.authentication.api.AuthenticationApi;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.action.util.HyperIoTCrudAction;
import it.acsoftware.hyperiot.base.action.util.HyperIoTShareAction;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTBaseEntity;
import it.acsoftware.hyperiot.base.api.entity.HyperIoTPaginableResult;
import it.acsoftware.hyperiot.base.model.HyperIoTBaseError;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.hdevice.actions.HyperIoTHDeviceAction;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hdevice.service.rest.HDeviceRestApi;
import it.acsoftware.hyperiot.hpacket.model.*;
import it.acsoftware.hyperiot.hpacket.service.rest.HPacketRestApi;
import it.acsoftware.hyperiot.hproject.actions.HyperIoTHProjectAction;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.hproject.service.rest.HProjectRestApi;
import it.acsoftware.hyperiot.huser.model.HUser;
import it.acsoftware.hyperiot.huser.service.rest.HUserRestApi;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.permission.model.Permission;
import it.acsoftware.hyperiot.permission.service.rest.PermissionRestApi;
import it.acsoftware.hyperiot.role.model.Role;
import it.acsoftware.hyperiot.role.service.rest.RoleRestApi;
import it.acsoftware.hyperiot.shared.entity.model.SharedEntity;
import it.acsoftware.hyperiot.shared.entity.service.rest.SharedEntityRestApi;
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
import java.util.*;

import static it.acsoftware.hyperiot.shared.entity.test.HyperIoTSharedEntityConfiguration.*;

/**
 * 
 * @author Aristide Cittadino Interface component for SharedEntity System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTSharedEntityWithPermissionRestTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
		// starts with HSQL
		// the standard configuration has been moved to the HyperIoTSharedEntityConfiguration class
		return HyperIoTTestConfigurationBuilder.createStandardConfiguration().withHSQL()
//				.withDebug("5010", false)
				.append(getBaseConfiguration()).build();
	}

	public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi,HyperIoTUser user) {
		return restApi.impersonate(user);
	}

	@Test
	public void test000_hyperIoTFrameworkShouldBeInstalled() {
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
		assertContains("HyperIoTDashboard-features ", features);
		assertContains("HyperIoTDashboardWidget-features ", features);
		assertContains("HyperIoTRuleEngine-features ", features);
		assertContains("HyperIoTHadoopManager-features ", features);
		assertContains("HyperIoTHBaseConnector-features", features);
		String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
		assertContains("hyperiot", datasource);
	}


	@Test
	public void test001_sharedEntityModuleShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// the following call checkModuleWorking checks if SharedEntity module working
		// correctly
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(sharedEntityRestApi, adminUser);
		Response restResponse = sharedEntityRestApi.checkModuleWorking();
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test002_findAllSharedEntityWithPermissionShouldWorkIfSharedEntityListIsEmpty() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find all SharedEntity with the following call findAllSharedEntity,
		// there are still no entities saved in the database, this call return an empty list
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntity();
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertTrue(sharedEntityList.isEmpty());
		Assert.assertEquals(0, sharedEntityList.size());
	}


	@Test
	public void test003_findAllSharedEntityWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find all SharedEntity with the following call findAllSharedEntity
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntity();
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertFalse(sharedEntityList.isEmpty());
		boolean sharedEntityFound = false;
		for (SharedEntity sharedEntities : sharedEntityList) {
			if (sharedEntity.getEntityId() == sharedEntities.getEntityId()) {
				Assert.assertEquals(sharedEntity.getEntityId(), sharedEntities.getEntityId());
				Assert.assertEquals(sharedEntity.getEntityResourceName(), sharedEntities.getEntityResourceName());
				Assert.assertEquals(sharedEntity.getUserId(), sharedEntities.getUserId());
				sharedEntityFound = true;
			}
		}
		Assert.assertTrue(sharedEntityFound);
	}


	@Test
	public void test004_findAllSharedEntityWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find all SharedEntity with the following call findAllSharedEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser tries to share hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntity();
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test005_saveSharedEntityWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, save SharedEntity with the following call saveSharedEntity
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName); // "it.acsoftware.hyperiot.hproject.model.HProject"
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(hproject.getId(),
				((SharedEntity) restResponse.getEntity()).getEntityId());
		Assert.assertEquals(huser.getId(),
				((SharedEntity) restResponse.getEntity()).getUserId());
		Assert.assertEquals(hProjectSharedResourceName,
				((SharedEntity) restResponse.getEntity()).getEntityResourceName());
	}


	@Test
	public void test006_saveSharedEntityWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to save SharedEntity with the following call saveSharedEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName); // "it.acsoftware.hyperiot.hproject.model.HProject"
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test007_saveSharedEntityWithPermissionShouldFailIfEntityIsNotShared() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but entity isn't shared
		// response status code '500' HyperIoTRuntimeException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Area area = createArea(hproject);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area.getProject().getUser().getId());

		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(area.getId());
		sharedEntity.setEntityResourceName(areaResourceName); // "it.acsoftware.hyperiot.area.model.Area"
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("Entity " + sharedEntity.getEntityResourceName() + " is not a HyperIoTSharedEntity",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test008_findByPKWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find SharedEntity by primary key with the following call findByPK
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(sharedEntity.getEntityId(),
				((SharedEntity) restResponse.getEntity()).getEntityId());
		Assert.assertEquals(sharedEntity.getUserId(),
				((SharedEntity) restResponse.getEntity()).getUserId());
		Assert.assertEquals(sharedEntity.getEntityResourceName(),
				((SharedEntity) restResponse.getEntity()).getEntityResourceName());
	}


	@Test
	public void test009_findByPKWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by primary key with the following call findByPK,
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser tries to share hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test010_findByPKWithPermissionShouldFailIfSharedEntityNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by primary key with the following call findByPK,
		// but SharedEntity isn't stored in database
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		// SharedEntity isn't stored in database
		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test011_findByPKWithPermissionShouldFailIfPKNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by primary key with the following call findByPK,
		// but primary key not found
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityId(0);
		Assert.assertEquals(0, sharedEntity.getEntityId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test012_findByPKWithPermissionShouldFailIfEntityResourceNameNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by primary key with the following call findByPK,
		// but entityResourceName not found
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityResourceName("entity.resource.name.not.found");
		Assert.assertEquals("entity.resource.name.not.found", sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test013_findByPKWithPermissionShouldFailIfHUserNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by primary key with the following call findByPK,
		// but huser not found
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setUserId(0);
		Assert.assertEquals(0, sharedEntity.getUserId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test014_findByPKWithoutPermissionShouldFailAndSharedEntityNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity (not stored in database) by primary key
		// with the following call findByPK
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		// SharedEntity isn't stored in database
		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test015_findByPKWithoutPermissionShouldFailAndPKNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by primary key (PK not found)
		// with the following call findByPK
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityId(0);
		Assert.assertEquals(0, sharedEntity.getEntityId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test016_findByPKWithoutPermissionShouldFailAndEntityResourceNameNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by primary key (entityResourceName not found)
		// with the following call findByPK,
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityResourceName("entity.resource.name.not.found");
		Assert.assertEquals("entity.resource.name.not.found", sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test017_findByPKWithoutPermissionShouldFailAndHUserNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by primary key (huser not found)
		// with the following call findByPK,
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setUserId(0);
		Assert.assertEquals(0, sharedEntity.getUserId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test018_findByEntityWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find SharedEntity by entity with the following call findByEntity
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.findByEntity(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId());
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertFalse(sharedEntityList.isEmpty());
		Assert.assertEquals(1, sharedEntityList.size());
		Assert.assertEquals(sharedEntity.getEntityId(), sharedEntityList.get(0).getEntityId());
		Assert.assertEquals(sharedEntity.getUserId(), sharedEntityList.get(0).getUserId());
		Assert.assertEquals(sharedEntity.getEntityResourceName(), sharedEntityList.get(0).getEntityResourceName());
	}


	@Test
	public void test019_findByEntityWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by entity with the following call findByEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.findByEntity(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test020_findByEntityWithPermissionShouldWorkIfEntityResourceNameIsNull() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by entity with the following call findByEntity,
		// if entityResourceName is null this call return an empty list
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.findByEntity(null, hproject.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertTrue(sharedEntityList.isEmpty());
		Assert.assertEquals(0, sharedEntityList.size());
	}


	@Test
	public void test021_findByEntityWithPermissionShouldWorkIfEntityIdIsZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by entity with the following call findByEntity,
		// if entityId is zero this call return an empty list
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.findByEntity(sharedEntity.getEntityResourceName(), 0);
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertTrue(sharedEntityList.isEmpty());
		Assert.assertEquals(0, sharedEntityList.size());
	}


	@Test
	public void test022_findByEntityWithoutPermissionShouldFailAndEntityResourceNameIsNull() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by entity (entityResourceName is null)
		// with the following call findByEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.findByEntity(null, hproject.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test023_findByEntityWithoutPermissionShouldFailAndEntityIdIsZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by entity (entityId is zero)
		// with the following call findByEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.findByEntity(sharedEntity.getEntityResourceName(), 0);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test024_findByUserWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find SharedEntity by huser with the following call findByUser
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByUser(huser.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertFalse(sharedEntityList.isEmpty());
		Assert.assertEquals(1, sharedEntityList.size());
		Assert.assertEquals(sharedEntity.getEntityId(), sharedEntityList.get(0).getEntityId());
		Assert.assertEquals(sharedEntity.getUserId(), sharedEntityList.get(0).getUserId());
		Assert.assertEquals(sharedEntity.getEntityResourceName(), sharedEntityList.get(0).getEntityResourceName());
	}


	@Test
	public void test025_findByUserWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by huser with the following call findByUser
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByUser(huser.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test026_findByUserWithPermissionShouldWorkIfUserNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to find SharedEntity by huser with the following call findByUser,
		// if huser not found this call return an empty list
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByUser(0);
		Assert.assertEquals(200, restResponse.getStatus());
		List<SharedEntity> sharedEntityList = restResponse.readEntity(new GenericType<List<SharedEntity>>() {
		});
		Assert.assertTrue(sharedEntityList.isEmpty());
		Assert.assertEquals(0, sharedEntityList.size());
	}


	@Test
	public void test027_findByUserWithoutPermissionShouldFailAndUserNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find SharedEntity by huser (huser not found)
		// with the following call findByUser,
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByUser(0);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test028_getUsersWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find users shared by the entity with the following call getUsers,
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.getUsers(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId());
		Assert.assertEquals(200, restResponse.getStatus());
		List<HUser> userList = restResponse.readEntity(new GenericType<List<HUser>>() {
		});
		Assert.assertFalse(userList.isEmpty());
		Assert.assertEquals(1, userList.size());
		Assert.assertEquals(huser.getId(), userList.get(0).getId());
		Assert.assertEquals(huser.getUsername(), userList.get(0).getUsername());
		Assert.assertEquals(huser.getEmail(), userList.get(0).getEmail());
	}


	@Test
	public void test029_getUsersWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to find users shared by the entity with the following call getUsers
		// response status code '200' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.getUsers(sharedEntity.getEntityResourceName(), sharedEntity.getEntityId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test030_getUsersWithPermissionShouldWorkIfUserIsNotStoredInSharedEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, find users shared by the entity with the following call getUsers,
		// user is not stored in SharedEntity table; this call return an empty list
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.getUsers(hProjectSharedResourceName, hproject.getId());
		Assert.assertEquals(200, restResponse.getStatus());
		List<HUser> userList = restResponse.readEntity(new GenericType<List<HUser>>() {
		});
		Assert.assertTrue(userList.isEmpty());
		Assert.assertEquals(0, userList.size());
	}


	@Test
	public void test031_getUsersWithoutPermissionShouldFailAndUserIsNotStoredInSharedEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, find users shared by the entity (user is not stored in SharedEntity table)
		// with the following call getUsers
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi
				.getUsers(hProjectSharedResourceName, hproject.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test032_deleteSharedEntityWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, delete SharedEntity with the following call deleteSharedEntity
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNull(restResponse.getEntity());
	}


	@Test
	public void test033_deleteSharedEntityWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to delete SharedEntity with the following call deleteSharedEntity
		// response status code '403' HyperIoTUnauthorizedException

		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test034_deleteSharedEntityWithPermissionShouldFailIfSharedEntityNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to delete SharedEntity with the following call deleteSharedEntity
		// but SharedEntity isn't stored in database
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		// SharedEntity isn't stored in database
		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test035_deleteSharedEntityWithPermissionShouldFailIfPKNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to delete SharedEntity with the following call deleteSharedEntity
		// but primary key not found
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityId(0);
		Assert.assertEquals(0, sharedEntity.getEntityId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test036_deleteSharedEntityWithPermissionShouldFailIfEntityResourceNameNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to delete SharedEntity with the following call deleteSharedEntity
		// but entityResourceName not found
		// response status code '500' HyperIoTRuntimeException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityResourceName("entity.resource.name.not.found");
		Assert.assertEquals("entity.resource.name.not.found", sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("Entity " + sharedEntity.getEntityResourceName() + " is not a HyperIoTSharedEntity",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test037_deleteSharedEntityWithPermissionShouldFailIfUserNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to delete SharedEntity with the following call deleteSharedEntity
		// but user not found
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setUserId(0);
		Assert.assertEquals(0, sharedEntity.getUserId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test038_deleteSharedEntityWithPermissionShouldFailIfSharedEntityIsNull() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to delete SharedEntity with the following call deleteSharedEntity
		// but shared entity is null
		// response status code '500' java.lang.NullPointerException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(null);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals("java.lang.NullPointerException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test039_deleteSharedEntityShouldFailIfUser2IsNotUserOwnerOfEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// huser, with permission, tries to remove SharedEntity with the following call deleteSharedEntity,
		// but the huser isn't the user owner of the entity
		// response status code '403' HyperIoTUnauthorizedException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, huser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test040_deleteSharedEntityWithoutPermissionShouldFailAndSharedEntityNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to delete SharedEntity (isn't stored in database)
		// with the following call deleteSharedEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		// SharedEntity isn't stored in database
		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test041_deleteSharedEntityWithoutPermissionShouldFailAndPKNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to delete SharedEntity (primary key not found)
		// with the following call deleteSharedEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityId(0);
		Assert.assertEquals(0, sharedEntity.getEntityId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test042_deleteSharedEntityWithoutPermissionShouldFailAndEntityResourceNameNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to delete SharedEntity (entityResourceName not found)
		// with the following call deleteSharedEntity
		// response status code '500' HyperIoTRuntimeException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setEntityResourceName("entity.resource.name.not.found");
		Assert.assertEquals("entity.resource.name.not.found", sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("Entity " + sharedEntity.getEntityResourceName() + " is not a HyperIoTSharedEntity",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test043_deleteSharedEntityWithoutPermissionShouldFailAndUserNotFound() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to delete SharedEntity (user not found)
		// with the following call deleteSharedEntity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		sharedEntity.setUserId(0);
		Assert.assertEquals(0, sharedEntity.getUserId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test044_saveSharedEntityWithPermissionShouldFailIfHUserIdIsZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but huser id is zero: isn't stored in database
		// response status code '422' HyperIoTValidationException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		//huser isn't stored in database: huser id is zero
		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(0); // huser: "ID: 0 \n User:null"

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be null",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("sharedentity-user",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	@Test
	public void test045_saveSharedEntityWithPermissionShouldFailIfEntityResourceNameIsNull() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but entityResourceName is null
		// response status code '422' HyperIoTValidationException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(null);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
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
				Assert.assertEquals("sharedentity-entityresourcename",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("sharedentity-entityresourcename",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}


	@Test
	public void test046_saveSharedEntityWithPermissionShouldFailIfEntityResourceNameIsEmpty() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but entityResourceName is empty
		// response status code '500' HyperIoTRuntimeException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName("");
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("Entity " + sharedEntity.getEntityResourceName() + " is not a HyperIoTSharedEntity",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test047_saveSharedEntityWithPermissionShouldFailIfEntityResourceNameIsMaliciousCode() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but entityResourceName is malicious code
		// response status code '500' HyperIoTRuntimeException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName("javascript:");
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(500, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTRuntimeException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		Assert.assertEquals("Entity " + sharedEntity.getEntityResourceName() + " is not a HyperIoTSharedEntity",
				((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(0));
	}


	@Test
	public void test048_saveSharedEntityWithPermissionShouldFailIfEntityIdIsZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but entityId is zero
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(0);
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test049_saveSharedEntityWithPermissionShouldFailIfEntityIsNotStoredInDatabase() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but entity isn't stored in database
		// response status code '404' HyperIoTEntityNotFound
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);

		// hproject not stored in database
		HProject hproject = new HProject();

		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test050_saveSharedEntityWithPermissionShouldFailIfUser2IsNotUserOwnerOfEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// huser2, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but the huser2 isn't the user owner of the entity
		// response status code '403' HyperIoTUnauthorizedException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		HUser huser2 = createHUser(action);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser2.getId());

		this.impersonateUser(sharedEntityRestApi, huser2);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test051_saveSharedEntityWithoutPermissionShouldFailIfUser2IsNotUserOwnerOfEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// huser2, without permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but the huser2 isn't the user owner of the entity
		// response status code '403' HyperIoTUnauthorizedException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		HUser huser2 = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName);
		sharedEntity.setUserId(huser2.getId());

		this.impersonateUser(sharedEntityRestApi, huser2);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test052_saveSharedEntityWithPermissionShouldFailIfEntityIsDuplicated() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, tries to save SharedEntity with the following call saveSharedEntity,
		// but SharedEntity is duplicated
		// response status code '422' HyperIoTDuplicateEntityException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		SharedEntity sharedEntityDuplicated = new SharedEntity();
		sharedEntityDuplicated.setEntityId(sharedEntity.getEntityId());
		sharedEntityDuplicated.setEntityResourceName(sharedEntity.getEntityResourceName());
		sharedEntityDuplicated.setUserId(sharedEntity.getUserId());

		Assert.assertEquals(sharedEntity.getEntityId(), sharedEntityDuplicated.getEntityId());
		Assert.assertEquals(sharedEntity.getUserId(), sharedEntityDuplicated.getUserId());
		Assert.assertEquals(sharedEntity.getEntityResourceName(), sharedEntityDuplicated.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntityDuplicated);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTDuplicateEntityException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(3, ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size());
		boolean entityResourceNameIsDuplicated = false;
		boolean entityIdIsDuplicated = false;
		boolean userIdIsDuplicated = false;
		for (int i = 0; i < ((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().size(); i++) {
			if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("entityResourceName")) {
				entityResourceNameIsDuplicated = true;
				Assert.assertEquals("entityResourceName",
						((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("entityId")) {
				entityIdIsDuplicated = true;
				Assert.assertEquals("entityId",
						((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i).contentEquals("userId")) {
				userIdIsDuplicated = true;
				Assert.assertEquals("userId",
						((HyperIoTBaseError) restResponse.getEntity()).getErrorMessages().get(i));
			}
		}
		Assert.assertTrue(entityResourceNameIsDuplicated);
		Assert.assertTrue(entityIdIsDuplicated);
		Assert.assertTrue(userIdIsDuplicated);
	}


	@Test
	public void test053_triesToDuplicateSharedEntityWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, without permission, tries to duplicate SharedEntity with the following call saveSharedEntity,
		// response status code '403' HyperIoTDuplicateEntityException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		SharedEntity sharedEntityDuplicated = new SharedEntity();
		sharedEntityDuplicated.setEntityId(sharedEntity.getEntityId());
		sharedEntityDuplicated.setEntityResourceName(sharedEntity.getEntityResourceName());
		sharedEntityDuplicated.setUserId(sharedEntity.getUserId());

		Assert.assertEquals(sharedEntity.getEntityId(), sharedEntityDuplicated.getEntityId());
		Assert.assertEquals(sharedEntity.getUserId(), sharedEntityDuplicated.getUserId());
		Assert.assertEquals(sharedEntity.getEntityResourceName(), sharedEntityDuplicated.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntityDuplicated);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test054_findAllSharedEntityPaginatedWithPermissionShouldWork() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser find all SharedEntity with pagination
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer delta = 5;
		Integer page = 2;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}

		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(delta, page);
		HyperIoTPaginableResult<SharedEntity> listSharedEntities = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<SharedEntity>>() {
				});
		Assert.assertEquals(5, listSharedEntities.getResults().size());
		Assert.assertFalse(listSharedEntities.getResults().isEmpty());
		Assert.assertEquals(2, listSharedEntities.getCurrentPage());
		Assert.assertEquals(5, listSharedEntities.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test055_findAllSharedEntityPaginatedWithPermissionShouldWorkIfDeltaAndPageAreNull() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser find all SharedEntity with pagination
		// if delta and page are null
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer delta = null;
		Integer page = null;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}
		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(delta, page);
		HyperIoTPaginableResult<SharedEntity> listSharedEntities = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<SharedEntity>>() {
				});
		Assert.assertEquals(10, listSharedEntities.getResults().size());
		Assert.assertFalse(listSharedEntities.getResults().isEmpty());
		Assert.assertEquals(1, listSharedEntities.getCurrentPage());
		Assert.assertEquals(10, listSharedEntities.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test056_findAllSharedEntityPaginatedWithPermissionShouldWorkIfDeltaIsLowerThanZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser find all SharedEntity with pagination
		// if delta is lower than zero
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer page = 2;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}
		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(-1, page);
		HyperIoTPaginableResult<SharedEntity> listSharedEntities = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<SharedEntity>>() {
				});
		Assert.assertEquals(10, listSharedEntities.getResults().size());
		Assert.assertFalse(listSharedEntities.getResults().isEmpty());
		Assert.assertEquals(2, listSharedEntities.getCurrentPage());
		Assert.assertEquals(10, listSharedEntities.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test057_findAllSharedEntityPaginatedWithPermissionShouldWorkIfDeltaIsZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser find all SharedEntity with pagination
		// if delta is zero
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer page = 3;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}
		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(0, page);
		HyperIoTPaginableResult<SharedEntity> listSharedEntities = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<SharedEntity>>() {
				});
		Assert.assertEquals(10, listSharedEntities.getResults().size());
		Assert.assertFalse(listSharedEntities.getResults().isEmpty());
		Assert.assertEquals(3, listSharedEntities.getCurrentPage());
		Assert.assertEquals(10, listSharedEntities.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test058_findAllSharedEntityPaginatedWithPermissionShouldWorkIfPageIsLowerThanZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser find all SharedEntity with pagination
		// if page is lower than zero
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer delta = 5;
		Integer page = -1;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}
		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(delta, page);
		HyperIoTPaginableResult<SharedEntity> listSharedEntities = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<SharedEntity>>() {
				});
		Assert.assertEquals(5, listSharedEntities.getResults().size());
		Assert.assertFalse(listSharedEntities.getResults().isEmpty());
		Assert.assertEquals(1, listSharedEntities.getCurrentPage());
		Assert.assertEquals(5, listSharedEntities.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test059_findAllSharedEntityPaginatedWithPermissionShouldWorkIfPageIsZero() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser find all SharedEntity with pagination
		// if page is zero
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer delta = 5;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}
		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(delta, 0);
		HyperIoTPaginableResult<SharedEntity> listSharedEntities = restResponse
				.readEntity(new GenericType<HyperIoTPaginableResult<SharedEntity>>() {
				});
		Assert.assertEquals(5, listSharedEntities.getResults().size());
		Assert.assertFalse(listSharedEntities.getResults().isEmpty());
		Assert.assertEquals(1, listSharedEntities.getCurrentPage());
		Assert.assertEquals(5, listSharedEntities.getNextPage());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test060_findAllSharedEntityPaginatedWithoutPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// In this following call findAllSharedEntityPaginated, ownerUser, without permission,
		// tries to find all SharedEntity with pagination
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		Integer delta = 5;
		Integer page = 2;

		// ownerUser share his hproject with husers
		List<HUser> husers = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			HUser huser = createHUser(null);
			Assert.assertNotEquals(0, huser.getId());
			husers.add(huser);
		}

		List<SharedEntity> sharedEntities = new ArrayList<>();
		for (int i = 0; i < delta; i++) {
			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
			Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());
			sharedEntities.add(sharedEntity);
		}
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findAllSharedEntityPaginated(delta, page);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test061_huser2WithPermissionTriesToSaveSharedEntityShouldFailIfIsNotOwner() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// huser2WithPermission tries to save SharedEntity with the following call saveSharedEntity,
		// huser2WithPermission has permission (SHARE) but it's unauthorized because isn't owner
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hproject.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName); // "it.acsoftware.hyperiot.hproject.model.HProject"
		sharedEntity.setUserId(huser.getId());

		// huser2WithPermission has permission but it's unauthorized
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser huser2WithPermission = createHUser(action);
		this.impersonateUser(sharedEntityRestApi, huser2WithPermission);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test062_deleteHProjectRemoveInCascadeSharedEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser delete hproject with the following call deleteHProject; this call delete
		// in cascade mode SharedEntity because entityId is equals to hprojectId
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		String sqlSharedEntityId = "select se.entityId from sharedentity se where se.entityId = " + sharedEntity.getEntityId();
		String resultSharedEntityId = executeCommand("jdbc:query hyperiot " + sqlSharedEntityId);
		String[] wrSharedEntityId = resultSharedEntityId.split("\\n");
		System.out.println(resultSharedEntityId);
		Assert.assertEquals(Long.parseLong(wrSharedEntityId[2]), sharedEntity.getEntityId());
		Assert.assertEquals(Long.parseLong(wrSharedEntityId[2]), hproject.getId());

		// this call delete in cascade mode SharedEntity
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.REMOVE);
		addPermission(ownerUser, action);
		this.impersonateUser(hprojectRestService, ownerUser);
		Response responseDeleteHProject = hprojectRestService.deleteHProject(sharedEntity.getEntityId());
		Assert.assertEquals(200, responseDeleteHProject.getStatus());
		Assert.assertNull(responseDeleteHProject.getEntity());

		// checks: HUser is already stored in database
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hUserResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action1);
		this.impersonateUser(hUserRestApi, ownerUser);
		Response responseFindHUser = hUserRestApi.findHUser(sharedEntity.getUserId());
		Assert.assertEquals(200, responseFindHUser.getStatus());
		Assert.assertEquals(huser.getId(), ((HUser) responseFindHUser.getEntity()).getId());

		// checks: SharedEntity isn't stored in database
		HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action2);
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());

		// SharedEntity with entityId has been deleted, in cascade mode, with deleteHProject call
		String sqlSharedEntity = "select * from sharedentity where entityId = " + sharedEntity.getEntityId();;
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);
	}


	@Test
	public void test063_deleteHUserRemoveRecordInsideSharedEntityTable() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser delete huser with call deleteHUser; this call remove in cascade record inside SharedEntity table
		// response status code '200'
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// huser has been removed in SharedEntity table with deleteHUser call
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hUserResourceName,
				HyperIoTCrudAction.REMOVE);
		addPermission(ownerUser, action);
		this.impersonateUser(hUserRestApi, ownerUser);
		Response responseDeleteHUser = hUserRestApi.deleteHUser(huser.getId());
		Assert.assertEquals(200, responseDeleteHUser.getStatus());
		Assert.assertNull(responseDeleteHUser.getEntity());

		// checks: record of SharedEntity has been deleted in database
		HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action1);
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());

		// checks: record in SharedEntity has been deleted with deleteHUser call
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);
	}


	@Test
	public void test064_deleteHProjectRemoveInCascadeSharedEntityAndAllDevicesPacketsAreas() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser delete hproject with the following call deleteHProject;
		// this call delete in cascade mode SharedEntity and all devices, packets, areas associated
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		HDevice hdevice1 = createHDevice(hproject);
		HPacket hpacket1 = createHPacket(hdevice1);
		HPacket hpacket2 = createHPacket(hdevice1);
		Area area1 = createArea(hproject);

		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket1.getDevice().getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket2.getDevice().getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area1.getProject().getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		String sqlSharedEntityId = "select se.entityId from sharedentity se where se.entityId = " + sharedEntity.getEntityId();
		String resultSharedEntityId = executeCommand("jdbc:query hyperiot " + sqlSharedEntityId);
		String[] wrSharedEntityId = resultSharedEntityId.split("\\n");
		System.out.println(resultSharedEntityId);
		Assert.assertEquals(Long.parseLong(wrSharedEntityId[2]), sharedEntity.getEntityId());
		Assert.assertEquals(Long.parseLong(wrSharedEntityId[2]), hproject.getId());

		//checks if Area HProject exists
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTHProjectAction.AREAS_MANAGEMENT);
		addPermission(ownerUser, action1);
		this.impersonateUser(hprojectRestService, ownerUser);
		Response restResponseProjectAreas = hprojectRestService.getHProjectAreaList(hproject.getId());
		Collection<Area> listHProjectAreas = restResponseProjectAreas.readEntity(new GenericType<Collection<Area>>() {
		});
		Assert.assertEquals(1, listHProjectAreas.size());
		Assert.assertFalse(listHProjectAreas.isEmpty());
		boolean area1Found = false;
		for (Area areas : listHProjectAreas) {
			if (area1.getId() == areas.getId()) {
				Assert.assertEquals(hproject.getId(),
						((Area) ((ArrayList) listHProjectAreas).get(0)).getProject().getId());
				Assert.assertEquals(area1.getId(),
						((Area) ((ArrayList) listHProjectAreas).get(0)).getId());
				Assert.assertEquals(ownerUser.getId(),
						((Area) ((ArrayList) listHProjectAreas).get(0)).getProject().getUser().getId());
				area1Found = true;
			}
		}
		Assert.assertTrue(area1Found);
		Assert.assertEquals(200, restResponseProjectAreas.getStatus());


		//checks if device packets exists
		HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
		HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
				HyperIoTHDeviceAction.PACKETS_MANAGEMENT);
		addPermission(ownerUser, action2);
		this.impersonateUser(hPacketRestApi, ownerUser);
		Response restResponseHPacket = hPacketRestApi.getHDevicePacketList(hdevice1.getId());
		Collection<HPacket> listDeviceHPackets = restResponseHPacket.readEntity(new GenericType<Collection<HPacket>>() {
		});
		Assert.assertEquals(2, listDeviceHPackets.size());
		Assert.assertFalse(listDeviceHPackets.isEmpty());
		boolean packet1Found = false;
		boolean packet2Found = false;
		for (HPacket hPackets : listDeviceHPackets) {
			if (hpacket1.getId() == hPackets.getId()) {
				Assert.assertEquals(hproject.getId(),
						((HPacket) ((ArrayList) listDeviceHPackets).get(0)).getDevice().getProject().getId());
				Assert.assertEquals(ownerUser.getId(),
						((HPacket) ((ArrayList) listDeviceHPackets).get(0)).getDevice().getProject().getUser().getId());
				packet1Found = true;
			}
			if (hpacket2.getId() == hPackets.getId()) {
				Assert.assertEquals(hproject.getId(),
						((HPacket) ((ArrayList) listDeviceHPackets).get(1)).getDevice().getProject().getId());
				Assert.assertEquals(ownerUser.getId(),
						((HPacket) ((ArrayList) listDeviceHPackets).get(1)).getDevice().getProject().getUser().getId());
				packet2Found = true;
			}
		}
		Assert.assertTrue(packet1Found);
		Assert.assertTrue(packet2Found);
		Assert.assertEquals(200, restResponseHPacket.getStatus());

		/*
		 *
		 * End: Complete hproject has been created
		 *
		 */

		// this call delete in cascade mode SharedEntity and all entity associated
		HyperIoTAction action3 = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.REMOVE);
		addPermission(ownerUser, action3);
		this.impersonateUser(hprojectRestService, ownerUser);
		Response responseDeleteHProject = hprojectRestService.deleteHProject(sharedEntity.getEntityId());
		Assert.assertEquals(200, responseDeleteHProject.getStatus());
		Assert.assertNull(responseDeleteHProject.getEntity());

		// checks: HUser is already stored in database
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		HyperIoTAction action4 = HyperIoTActionsUtil.getHyperIoTAction(hUserResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action4);
		this.impersonateUser(hUserRestApi, ownerUser);
		Response responseFindHUser = hUserRestApi.findHUser(sharedEntity.getUserId());
		Assert.assertEquals(200, responseFindHUser.getStatus());
		Assert.assertEquals(huser.getId(), ((HUser) responseFindHUser.getEntity()).getId());

		// checks: SharedEntity isn't stored in database
		HyperIoTAction action5 = HyperIoTActionsUtil.getHyperIoTAction(sharedEntityResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action5);
		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponseFindByPK = sharedEntityRestApi.findByPK(sharedEntity);
		Assert.assertEquals(404, restResponseFindByPK.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponseFindByPK.getEntity()).getType());

		// SharedEntity with entityId has been deleted, in cascade mode, with deleteHProject call
		String sqlSharedEntity = "select * from sharedentity where entityId = " + sharedEntity.getEntityId();;
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		// checks: areas has been deleted in cascade mode with call deleteHProject
		AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
		HyperIoTAction action6 = HyperIoTActionsUtil.getHyperIoTAction(areaResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action6);
		this.impersonateUser(areaRestApi, ownerUser);
		Response restResponseFindArea = areaRestApi.findArea(area1.getId());
		Assert.assertEquals(404, restResponseFindArea.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponseFindArea.getEntity()).getType());

		// checks: devices has been deleted in cascade mode with call deleteHProject
		HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
		HyperIoTAction action7 = HyperIoTActionsUtil.getHyperIoTAction(hDeviceResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action7);
		this.impersonateUser(hDeviceRestApi, ownerUser);
		Response restResponseFindHDevice = hDeviceRestApi.findHDevice(hdevice1.getId());
		Assert.assertEquals(404, restResponseFindHDevice.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponseFindHDevice.getEntity()).getType());

		// checks: packets has been deleted in cascade mode with call deleteHProject
		HyperIoTAction action8 = HyperIoTActionsUtil.getHyperIoTAction(hPacketResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action8);
		this.impersonateUser(hPacketRestApi, ownerUser);
		Response restResponseFindHPacket1 = hPacketRestApi.findHPacket(hpacket1.getId());
		Assert.assertEquals(404, restResponseFindHPacket1.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponseFindHPacket1.getEntity()).getType());

		Response restResponseFindHPacket2 = hPacketRestApi.findHPacket(hpacket2.getId());
		Assert.assertEquals(404, restResponseFindHPacket2.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponseFindHPacket2.getEntity()).getType());
	}


	@Test
	public void test065_deleteSharedEntityNotDeleteInCascadeHProjectAndHUser() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// ownerUser, with permission, delete SharedEntity with the following call deleteSharedEntity;
		// this call not delete in cascade mode hproject or huser
		// response status code '200'
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNull(restResponse.getEntity());

		// checks: HUser is already stored in database
		HUserRestApi hUserRestApi = getOsgiService(HUserRestApi.class);
		HyperIoTAction action1 = HyperIoTActionsUtil.getHyperIoTAction(hUserResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action1);
		this.impersonateUser(hUserRestApi, ownerUser);
		Response responseFindHUser = hUserRestApi.findHUser(huser.getId());
		Assert.assertEquals(200, responseFindHUser.getStatus());
		Assert.assertEquals(huser.getId(), ((HUser) responseFindHUser.getEntity()).getId());

		// checks: HProject is already stored in database
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action2 = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.FIND);
		addPermission(ownerUser, action2);
		this.impersonateUser(hprojectRestService, ownerUser);
		Response responseFindHProject = hprojectRestService.findHProject(hproject.getId());
		Assert.assertEquals(200, responseFindHProject.getStatus());
		Assert.assertEquals(hproject.getId(), ((HProject) responseFindHProject.getEntity()).getId());
	}


	@Test
	public void test066_huserFindHProjectSharedAfterSharedEntityOperation() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser find HProject after shared entity operation
		// response status code '200'
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.FIND);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.findHProject(sharedEntity.getEntityId());
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(hproject.getId(),
				((HProject) restResponse.getEntity()).getId());
		Assert.assertEquals(hproject.getDescription(),
				((HProject) restResponse.getEntity()).getDescription());
		Assert.assertEquals(ownerUser.getId(),
				((HProject) restResponse.getEntity()).getUser().getId());
	}


	@Test
	public void test067_huser2TriesToFindHProjectSharedShouldFailIfNotInsertedInSharedEntityTable() {
		// huser2, with permission, tries to find HProject shared with the following call findHProject
		// huser2 has permission (FIND) but it's unauthorized because isn't associated with hproject shared
		// response status code '404' HyperIoTEntityNotFound
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.FIND);
		HUser huser2 = createHUser(action);
		Assert.assertNotEquals(huser2.getId(), ownerUser.getId());
		Assert.assertNotEquals(huser2.getId(), huser.getId());

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.findHProject(hproject.getId());
		Assert.assertEquals(404, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTEntityNotFound",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test068_huserFindAllHProjectSharedAfterSharedEntityOperation() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser find all HProject after shared entity operation
		// response status code '200'
		HUser ownerUser = createHUser(null);
		HProject hproject1 = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject1.getUser().getId());

		HProject hproject2 = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject2.getUser().getId());

		// hproject3 is not shared
		HProject hproject3 = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject3.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity1 = createSharedEntity(hproject1, ownerUser, huser);
		Assert.assertEquals(hproject1.getId(), sharedEntity1.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity1.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity1.getEntityResourceName());

		// second SharedEntity
		SharedEntity sharedEntity2 = createSharedEntity(hproject2, ownerUser, huser);
		Assert.assertEquals(hproject2.getId(), sharedEntity2.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity2.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity2.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity1.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.findAllHProject();
		List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
		});
		Assert.assertFalse(listHProjects.isEmpty());
		Assert.assertEquals(2, listHProjects.size());
		boolean hprojectFound1 = false;
		boolean hprojectFound2 = false;
		for (HProject hprojects : listHProjects) {
			if (hproject1.getId() == hprojects.getId()) {
				Assert.assertEquals(hproject1.getId(), hprojects.getId());
				Assert.assertEquals(hproject1.getDescription(), hprojects.getDescription());
				Assert.assertEquals(hproject1.getUser().getId(), hprojects.getUser().getId());
				hprojectFound1 = true;
			}
			if (hproject2.getId() == hprojects.getId()) {
				Assert.assertEquals(hproject2.getId(), hprojects.getId());
				Assert.assertEquals(hproject2.getDescription(), hprojects.getDescription());
				Assert.assertEquals(hproject2.getUser().getId(), hprojects.getUser().getId());
				hprojectFound2 = true;
			}
		}
		Assert.assertTrue(hprojectFound1);
		Assert.assertTrue(hprojectFound2);
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test069_huser2TriesToFindAllHProjectSharedShouldFail() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to find all shared HProject; this call return an empty list because
		// huser2 isn't associated in shared hproject
		// response status code '200'
		HUser ownerUser = createHUser(null);
		HProject hproject1 = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject1.getUser().getId());

		HProject hproject2 = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject2.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity1 = createSharedEntity(hproject1, ownerUser, huser);
		Assert.assertEquals(hproject1.getId(), sharedEntity1.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity1.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity1.getEntityResourceName());

		// second SharedEntity
		SharedEntity sharedEntity2 = createSharedEntity(hproject2, ownerUser, huser);
		Assert.assertEquals(hproject2.getId(), sharedEntity2.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity2.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity2.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity1.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		// huser2 is not associated with shared hprojects
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.FINDALL);
		HUser huser2 = createHUser(action);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.findAllHProject();
		List<HProject> listHProjects = restResponse.readEntity(new GenericType<List<HProject>>() {
		});
		Assert.assertTrue(listHProjects.isEmpty());
		Assert.assertEquals(0, listHProjects.size());
		Assert.assertEquals(200, restResponse.getStatus());
	}


	@Test
	public void test070_huserUpdateHProjectSharedAfterSharedEntityOperation() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser update HProject after shared entity operation
		// response status code '200'
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		Date date = new Date();
		hproject.setDescription(huser.getUsername() + " edit description in date: " + date);
		this.impersonateUser(hprojectRestService, huser);
		Response restResponseUpdateHProject = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(200, restResponseUpdateHProject.getStatus());
		Assert.assertEquals(hproject.getId(),
				((HProject) restResponseUpdateHProject.getEntity()).getId());
		Assert.assertEquals(huser.getUsername() + " edit description in date: " + date,
				((HProject) restResponseUpdateHProject.getEntity()).getDescription());
		Assert.assertEquals(hproject.getEntityVersion() + 1,
				(((HProject) restResponseUpdateHProject.getEntity()).getEntityVersion()));
		Assert.assertEquals(ownerUser.getId(),
				((HProject) restResponseUpdateHProject.getEntity()).getUser().getId());
	}


	@Test
	public void test071_huser2TriesToUpdateHProjectSharedShouldFail() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject but isn't associated in shared hproject
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		// huser2 tries to update HProject but isn't associated in shared hproject
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		Date date = new Date();
		hproject.setDescription(huser2.getUsername() + " tries to edit description in date: " + date);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponseUpdateHProject = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponseUpdateHProject.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponseUpdateHProject.getEntity()).getType());
	}


	@Test
	public void test072_huserDeleteHProjectSharedAfterSharedEntityOperation() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser delete HProject after shared entity operation
		// response status code '200'
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.REMOVE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		this.impersonateUser(hprojectRestService, huser);
		Response restResponseDeleteHProject = hprojectRestService.deleteHProject(hproject.getId());
		Assert.assertEquals(200, restResponseDeleteHProject.getStatus());
		Assert.assertNull(restResponseDeleteHProject.getEntity());
	}


	@Test
	public void test073_huser2TriesToDeleteSharedEntityShouldFailIfIsNotOwnerOrInsertedInSameSharedEntity() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// huser2 tries to delete SharedEntity with the following call deleteSharedEntity,
		// huser2 has permission (SHARE) but it's unauthorized because isn't owner
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// huser2 has permission but it's unauthorized
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser huser2 = createHUser(action);
		this.impersonateUser(sharedEntityRestApi, huser2);
		Response restResponse = sharedEntityRestApi.deleteSharedEntity(sharedEntity);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test074_huserUpdateHProjectSharedAfterSharedEntityOperationShouldFailIfNameIsNull() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but name is null
		// response status code '422' HyperIoTValidationException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		hproject.setName(null);
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.updateHProject(hproject);
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
				Assert.assertEquals("hproject-name",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
			if (((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage().contentEquals("must not be empty")) {
				msgValidationErrorsIsEmpty = true;
				Assert.assertEquals("must not be empty",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getMessage());
				Assert.assertEquals("hproject-name",
						((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(i).getField());
			}
		}
		Assert.assertTrue(msgValidationErrorsIsNull);
		Assert.assertTrue(msgValidationErrorsIsEmpty);
	}


	@Test
	public void test075_huser2TriesToUpdateHProjectSharedShouldFailIfNameIsNullAndItIsNotInsertedInSameSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject, after shared operation, with the following call updateHProject,
		// huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setName(null);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test076_huserUpdateHProjectSharedAfterSharedEntityOperationShouldFailIfNameIsEmpty() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but name is empty
		// response status code '422' HyperIoTValidationException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		hproject.setName("");
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be empty",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("hproject-name",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals("",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test077_huser2TriesToUpdateHProjectSharedShouldFailIfNameIsEmptyAndItIsNotInsertedInSameSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject, after shared operation, with the following call updateHProject,
		// huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setName("");
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test078_huserUpdateHProjectSharedAfterSharedEntityOperationShouldFailIfNameIsMaliciousCode() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but name is malicious code
		// response status code '422' HyperIoTValidationException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		hproject.setName("javascript:");
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("hproject-name",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(hproject.getName(),
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test079_huser2TriesToUpdateHProjectSharedShouldFailIfNameIsMaliciousCodeAndItIsNotInsertedInSameSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject, after shared operation, with the following call updateHProject,
		// huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setName("javascript:");
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test080_huserUpdateHProjectSharedAfterSharedEntityOperationShouldFailIfDescriptionIsMaliciousCode() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but description is malicious code
		// response status code '422' HyperIoTValidationException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		hproject.setDescription("javascript:");
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("{it.acsoftware.hyperiot.validator.nomalitiuscode.message}",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("hproject-description",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals("javascript:",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue());
	}


	@Test
	public void test081_huser2TriesToUpdateHProjectSharedShouldFailIfDescriptionIsMaliciousCodeAndItIsNotInsertedInSameSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject, after shared operation, with the following call updateHProject,
		// huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setDescription("javascript:");
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test082_huserUpdateHProjectSharedAfterSharedEntityOperationShouldFailIfDescriptionIsOver3000Chars() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but description is over 3000 chars
		// response status code '422' HyperIoTValidationException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		int maxLength = 3001;
		hproject.setDescription(testMaxLength(maxLength));
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("length must be between 0 and 3000",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("hproject-description",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
		Assert.assertEquals(maxLength,
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getInvalidValue().length());
	}


	@Test
	public void test083_huser2TriesToUpdateHProjectSharedShouldFailIfDescriptionIsOver3000CharsAndItIsNotInsertedInSameSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject, after shared operation, with the following call updateHProject,
		// huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		int maxLength = 3001;
		hproject.setDescription(testMaxLength(maxLength));
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test084_huserUpdateHProjectSharedAfterSharedEntityOperationShouldFailIfSetUserNull() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but tries to set userId (of hproject) to null
		// response status code '422' HyperIoTValidationException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		hproject.setUser(null);
		this.impersonateUser(hprojectRestService, huser);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(422, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTValidationException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
		Assert.assertEquals(1, ((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().size());
		Assert.assertEquals("must not be null",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getMessage());
		Assert.assertEquals("hproject-user",
				((HyperIoTBaseError) restResponse.getEntity()).getValidationErrors().get(0).getField());
	}


	@Test
	public void test085_huser2TriesToUpdateHProjectSharedShouldFailIfSetUserNullAndItIsNotInsertedInSameSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject, after shared operation, with the following call updateHProject,
		// huser2 has permission (UPDATE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setUser(null);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test086_huserInsertedInSharedEntityTriesToBecomeNewOwnerHProjectShouldFail() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject, after shared operation, with the following call updateHProject,
		// but tries to be owner of hproject
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Area area1 = createArea(hproject);
		HDevice hdevice1 = createHDevice(hproject);
		HPacket hpacket1 = createHPacket(hdevice1);

		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		Assert.assertNotEquals(huser.getId(), hproject.getUser().getId());
		Assert.assertNotEquals(huser.getId(), area1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		hproject.setUser(huser);
		this.impersonateUser(hprojectRestService, huser);
		// BUG: huser is a new owner of hproject, area, device and packet
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}



	@Test
	public void test087_huser2TriesToBecomeNewOwnerHProjectSharedShouldFail() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject and tries to become the new owner of hproject;
		// huser2 isn't associated with hproject
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Area area1 = createArea(hproject);
		HDevice hdevice1 = createHDevice(hproject);
		HPacket hpacket1 = createHPacket(hdevice1);

		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		Assert.assertNotEquals(huser.getId(), hproject.getUser().getId());
		Assert.assertNotEquals(huser.getId(), area1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setUser(huser2);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test088_huser2TriesToMakeHUserNewOwnerOfHProjectSharedAfterSharedEntityOperationShouldFail() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to update HProject and tries to make huser the new owner of hproject;
		// huser2 isn't associated with hproject
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Area area1 = createArea(hproject);
		HDevice hdevice1 = createHDevice(hproject);
		HPacket hpacket1 = createHPacket(hdevice1);

		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		Assert.assertNotEquals(huser.getId(), hproject.getUser().getId());
		Assert.assertNotEquals(huser.getId(), area1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser2 = createHUser(action);
		hproject.setUser(huser);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}



	@Test
	public void test089_huserTriesToMakeHUser2NewOwnerOfHProjectSharedAfterSharedEntityOperationShouldFail() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser tries to update HProject and tries to make huser2 the new owner of hproject;
		// huser is associated with hproject, but that isn't an allowed operation
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Area area1 = createArea(hproject);
		HDevice hdevice1 = createHDevice(hproject);
		HPacket hpacket1 = createHPacket(hdevice1);

		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// ownerUser share his hproject with huser
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.UPDATE);
		HUser huser = createHUser(action);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		Assert.assertNotEquals(huser.getId(), hproject.getUser().getId());
		Assert.assertNotEquals(huser.getId(), area1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		// huser2 isn't associated in SharedEntity and isn't the owner hproject
		HUser huser2 = createHUser(null);
		hproject.setUser(huser2);
		this.impersonateUser(hprojectRestService, huser);
		// BUG: huser2 is a new owner
		Response restResponse = hprojectRestService.updateHProject(hproject);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test090_huser2TriesToDeleteHProjectSharedShouldFailIfIsNotAssociatedWithSharedEntity() {
		// ownerUser save SharedEntity with the following call saveSharedEntity, and
		// huser2 tries to delete HProject, after shared operation, with the following call deleteHProject,
		// huser2 has permission (REMOVE) but it's unauthorized because isn't associated with shared entity
		// response status code '403' HyperIoTUnauthorizedException
		HUser ownerUser = createHUser(null);
		HProject hproject = createHProject(ownerUser);
		Area area1 = createArea(hproject);
		HDevice hdevice1 = createHDevice(hproject);
		HPacket hpacket1 = createHPacket(hdevice1);

		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
		Assert.assertEquals(ownerUser.getId(), area1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertEquals(ownerUser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		Assert.assertNotEquals(huser.getId(), hproject.getUser().getId());
		Assert.assertNotEquals(huser.getId(), area1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hdevice1.getProject().getUser().getId());
		Assert.assertNotEquals(huser.getId(), hpacket1.getDevice().getProject().getUser().getId());

		// checks: SharedEntity has been stored in database
		String sqlSharedEntity = "select * from sharedentity where userId = " + sharedEntity.getUserId();
		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
		System.out.println(resultSharedEntity);

		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		// huser2 isn't associated in SharedEntity and isn't the owner hproject
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTCrudAction.REMOVE);
		HUser huser2 = createHUser(action);
		this.impersonateUser(hprojectRestService, huser2);
		Response restResponse = hprojectRestService.deleteHProject(hproject.getId());
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}


	@Test
	public void test091_huserTriesToDuplicateSharedEntityWithPermissionShouldFail() {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		// huser, with permission, tries to duplicate SharedEntity with the following call saveSharedEntity
		// response status code '403' HyperIoTUnauthorizedException
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		HUser ownerUser = createHUser(action);
		HProject hproject = createHProject(ownerUser);
		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());

		// ownerUser share his hproject with huser
		HUser huser = createHUser(null);

		SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, huser);
		Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
		Assert.assertEquals(huser.getId(), sharedEntity.getUserId());
		Assert.assertEquals(hProjectSharedResourceName, sharedEntity.getEntityResourceName());

		// huser tries to duplicate SharedEntity
		SharedEntity sharedEntityDuplicated = new SharedEntity();
		sharedEntityDuplicated.setEntityId(sharedEntity.getEntityId());
		sharedEntityDuplicated.setEntityResourceName(sharedEntity.getEntityResourceName());
		sharedEntityDuplicated.setUserId(sharedEntity.getUserId());

		Assert.assertEquals(sharedEntity.getEntityId(), sharedEntityDuplicated.getEntityId());
		Assert.assertEquals(sharedEntity.getUserId(), sharedEntityDuplicated.getUserId());
		Assert.assertEquals(sharedEntity.getEntityResourceName(), sharedEntityDuplicated.getEntityResourceName());

		addPermission(huser, action);
		this.impersonateUser(sharedEntityRestApi, huser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntityDuplicated);
		Assert.assertEquals(403, restResponse.getStatus());
		Assert.assertEquals(hyperIoTException + "HyperIoTUnauthorizedException",
				((HyperIoTBaseError) restResponse.getEntity()).getType());
	}



//	@Test
//	public void test100_stressTestShareEntityWith10000Users() throws ClassNotFoundException {
//		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
//		// This is a stress test: ownerUser will share entity with 10000 husers
//		// response time: 20 minutes around
//		// response status code '200'
//		HUser ownerUser = createHUser(null);
//		HProject hproject = createHProject(ownerUser);
//		Assert.assertEquals(ownerUser.getId(), hproject.getUser().getId());
//
//		Integer delta = 10000;
//
//		// ownerUser share his hproject with husers
//		Class<?> resource = Class.forName(hproject.getResourceName());
//
//		List<HUser> husers = new ArrayList<>();
//		for (int i = 0; i < delta; i++) {
//			HUser huser = createHUser(null);
//			Assert.assertNotEquals(0, huser.getId());
//			husers.add(huser);
//		}
//
//		List<SharedEntity> sharedEntities = new ArrayList<>();
//		for (int i = 0; i < delta; i++) {
//			SharedEntity sharedEntity = createSharedEntity(hproject, ownerUser, husers.get(i));
//			Assert.assertEquals(hproject.getId(), sharedEntity.getEntityId());
//			Assert.assertEquals(husers.get(i).getId(), sharedEntity.getUserId());
//			Assert.assertEquals(resource.getName(), sharedEntity.getEntityResourceName());
//			sharedEntities.add(sharedEntity);
//		}
//
//		// checks: SharedEntity has been stored in database
//		String sqlSharedEntity = "select * from sharedentity where entityId = " + hproject.getId();
//		String resultSharedEntity = executeCommand("jdbc:query hyperiot " + sqlSharedEntity);
//		System.out.println(resultSharedEntity);
//	}


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
		List<Object> roles = new ArrayList<>();
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
		Assert.assertNotEquals(0, ((HUser) restResponse.getEntity()).getId());
		Assert.assertEquals("name", ((HUser) restResponse.getEntity()).getName());
		Assert.assertEquals("lastname", ((HUser) restResponse.getEntity()).getLastname());
		Assert.assertEquals(huser.getUsername(), ((HUser) restResponse.getEntity()).getUsername());
		Assert.assertEquals(huser.getEmail(), ((HUser) restResponse.getEntity()).getEmail());
		Assert.assertFalse(huser.isAdmin());
		Assert.assertTrue(huser.isActive());
		Assert.assertTrue(roles.isEmpty());
		if (action != null) {
			Role role = createRole();
			huser.addRole(role);
			RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
			Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
			Assert.assertEquals(200, restUserRole.getStatus());
			Assert.assertTrue(huser.hasRole(role));
			roles = Arrays.asList(huser.getRoles().toArray());
			Assert.assertFalse(roles.isEmpty());
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
				Assert.assertNotEquals(0, ((Permission) restResponse.getEntity()).getId());
				Assert.assertEquals(testPermission.getName(), ((Permission) restResponse.getEntity()).getName());
				Assert.assertEquals(testPermission.getActionIds(), ((Permission) restResponse.getEntity()).getActionIds());
				Assert.assertEquals(testPermission.getEntityResourceName(), ((Permission) restResponse.getEntity()).getEntityResourceName());
				Assert.assertEquals(testPermission.getRole().getId(), ((Permission) restResponse.getEntity()).getRole().getId());
			} else {
				this.impersonateUser(permissionRestApi, adminUser);
				testPermission.addPermission(action);
				Response restResponseUpdate = permissionRestApi.updatePermission(testPermission);
				Assert.assertEquals(200, restResponseUpdate.getStatus());
				Assert.assertEquals(testPermission.getActionIds(), ((Permission) restResponseUpdate.getEntity()).getActionIds());
				Assert.assertEquals(testPermission.getEntityVersion() + 1,
						((Permission) restResponseUpdate.getEntity()).getEntityVersion());
			}
			Assert.assertTrue(huser.hasRole(role.getId()));
			return testPermission;
		}
	}

	private Role createRole() {
		RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		this.impersonateUser(roleRestApi, adminUser);
		Role role = new Role();
		role.setName("Role" + UUID.randomUUID());
		role.setDescription("Description");
		Response restResponse = roleRestApi.saveRole(role);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0, ((Role) restResponse.getEntity()).getId());
		Assert.assertEquals(role.getName(), ((Role) restResponse.getEntity()).getName());
		Assert.assertEquals("Description", ((Role) restResponse.getEntity()).getDescription());
		return role;
	}


	private SharedEntity createSharedEntity(HyperIoTBaseEntity hyperIoTBaseEntity, HUser ownerUser, HUser huser) {
		SharedEntityRestApi sharedEntityRestApi = getOsgiService(SharedEntityRestApi.class);
		HyperIoTAction action = HyperIoTActionsUtil.getHyperIoTAction(hProjectSharedResourceName,
				HyperIoTShareAction.SHARE);
		addPermission(ownerUser, action);

		SharedEntity sharedEntity = new SharedEntity();
		sharedEntity.setEntityId(hyperIoTBaseEntity.getId());
		sharedEntity.setEntityResourceName(hProjectSharedResourceName); // "it.acsoftware.hyperiot.hproject.model.HProject"
		sharedEntity.setUserId(huser.getId());

		this.impersonateUser(sharedEntityRestApi, ownerUser);
		Response restResponse = sharedEntityRestApi.saveSharedEntity(sharedEntity);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertEquals(hyperIoTBaseEntity.getId(),
				((SharedEntity) restResponse.getEntity()).getEntityId());
		Assert.assertEquals(huser.getId(),
				((SharedEntity) restResponse.getEntity()).getUserId());
		Assert.assertEquals(hProjectSharedResourceName,
				((SharedEntity) restResponse.getEntity()).getEntityResourceName());
		removePermission(ownerUser, action);
		return sharedEntity;
	}

	private Permission addPermission(HUser huser, HyperIoTAction action){
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser user = (HUser) authService.login("hadmin", "admin");
		Role role = createRole();
		huser.addRole(role);
		RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
		this.impersonateUser(roleRestApi, user);
		Response restUserRole = roleRestApi.saveUserRole(role.getId(), huser.getId());
		Assert.assertEquals(200, restUserRole.getStatus());
		Assert.assertTrue(huser.hasRole(role));
		Permission permission = utilGrantPermission(huser, role, action);
		Assert.assertNotEquals(0, permission.getId());
		Assert.assertEquals(action.getActionName(), permission.getName());
		Assert.assertEquals(action.getActionId(), permission.getActionIds());
		Assert.assertEquals(action.getCategory(), permission.getEntityResourceName());
		Assert.assertEquals(role.getId(), permission.getRole().getId());
		return permission;
	}

	private void removePermission(HUser huser, HyperIoTAction action){
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Role role = null;
		List<Object> roles = Arrays.asList(huser.getRoles().toArray());
		if (!roles.isEmpty()) {
			Assert.assertFalse(roles.isEmpty());
			if (action != null) {
				for (int i = 0; i < roles.size(); i++){
					role = ((Role) roles.get(i));
				}
				PermissionSystemApi permissionSystemApi = getOsgiService(PermissionSystemApi.class);
				Permission permission = permissionSystemApi.findByRoleAndResourceName(role, action.getResourceName());
				if (permission != null) {
					PermissionRestApi permissionRestApi = getOsgiService(PermissionRestApi.class);
					this.impersonateUser(permissionRestApi, adminUser);
					permission.removePermission(action);
					Response restResponseUpdate = permissionRestApi.deletePermission(permission.getId());
					Assert.assertEquals(200, restResponseUpdate.getStatus());

					huser.removeRole(permission.getRole());
					RoleRestApi roleRestApi = getOsgiService(RoleRestApi.class);
					this.impersonateUser(roleRestApi, adminUser);
					Response restUserRole = roleRestApi.deleteUserRole(role.getId(), huser.getId());
					Assert.assertEquals(200, restUserRole.getStatus());
					Assert.assertFalse(huser.hasRole(role));
				} else {
					Assert.assertNull(permission);
				}
			} else {
				Assert.assertNull(action);
			}
		}
	}

	private HProject createHProject(HUser huser) {
		HProjectRestApi hprojectRestService = getOsgiService(HProjectRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Assert.assertNotNull(adminUser);
		Assert.assertTrue(adminUser.isAdmin());
		HProject hproject = new HProject();
		hproject.setName("Project " + UUID.randomUUID());
		hproject.setDescription("Description");
		hproject.setUser(huser);
		this.impersonateUser(hprojectRestService, adminUser);
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

	private HDevice createHDevice(HProject hproject) {
		HDeviceRestApi hDeviceRestApi = getOsgiService(HDeviceRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		HDevice hdevice = new HDevice();
		hdevice.setBrand("Brand");
		hdevice.setDescription("Description");
		hdevice.setDeviceName("deviceName" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		hdevice.setFirmwareVersion("1.");
		hdevice.setModel("model");
		hdevice.setPassword("passwordPass&01");
		hdevice.setPasswordConfirm("passwordPass&01");
		hdevice.setSoftwareVersion("1.");
		hdevice.setAdmin(false);
		hdevice.setProject(hproject);
		this.impersonateUser(hDeviceRestApi, adminUser);
		Response restResponse = hDeviceRestApi.saveHDevice(hdevice);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0,
				((HDevice) restResponse.getEntity()).getId());
		Assert.assertEquals("Brand",
				((HDevice) restResponse.getEntity()).getBrand());
		Assert.assertEquals("Description",
				((HDevice) restResponse.getEntity()).getDescription());
		Assert.assertEquals("1.",
				((HDevice) restResponse.getEntity()).getFirmwareVersion());
		Assert.assertEquals("model",
				((HDevice) restResponse.getEntity()).getModel());
		Assert.assertEquals("1.",
				((HDevice) restResponse.getEntity()).getSoftwareVersion());
		Assert.assertFalse(((HDevice) restResponse.getEntity()).isAdmin());
		Assert.assertEquals(hproject.getId(),
				((HDevice) restResponse.getEntity()).getProject().getId());
		Assert.assertEquals(hproject.getUser().getId(),
				((HDevice) restResponse.getEntity()).getProject().getUser().getId());
		return hdevice;
	}

	private HPacket createHPacket(HDevice hdevice) {
		HPacketRestApi hPacketRestApi = getOsgiService(HPacketRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");

		HPacket hpacket = new HPacket();
		hpacket.setName("name" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		hpacket.setDevice(hdevice);
		hpacket.setFormat(HPacketFormat.JSON);
		hpacket.setSerialization(HPacketSerialization.AVRO);
		hpacket.setType(HPacketType.IO);
		hpacket.setVersion("version" + java.util.UUID.randomUUID().toString().replaceAll("-", ""));

		hpacket.setTrafficPlan(HPacketTrafficPlan.LOW);
		Date timestamp = new Date();
		hpacket.setTimestampField(String.valueOf(timestamp));
		hpacket.setTimestampFormat("String");

		this.impersonateUser(hPacketRestApi, adminUser);
		Response restResponse = hPacketRestApi.saveHPacket(hpacket);
		Assert.assertEquals(200, restResponse.getStatus());
		Assert.assertNotEquals(0,
				((HPacket) restResponse.getEntity()).getId());
		Assert.assertEquals(hdevice.getId(),
				((HPacket) restResponse.getEntity()).getDevice().getId());
		Assert.assertEquals(hdevice.getProject().getId(),
				((HPacket) restResponse.getEntity()).getDevice().getProject().getId());
		return hpacket;
	}

	private Area createArea(HProject hproject) {
		AreaRestApi areaRestApi = getOsgiService(AreaRestApi.class);
		AuthenticationApi authService = getOsgiService(AuthenticationApi.class);
		HyperIoTUser adminUser = (HUser) authService.login("hadmin", "admin");
		Area area = new Area();
		area.setName("Area " + java.util.UUID.randomUUID());
		area.setDescription("Description");
		area.setProject(hproject);
		this.impersonateUser(areaRestApi, adminUser);
		Response restResponseArea = areaRestApi.saveArea(area);
		Assert.assertEquals(200, restResponseArea.getStatus());
		Assert.assertNotEquals(0, ((Area) restResponseArea.getEntity()).getId());
		Assert.assertEquals(area.getName(), ((Area) restResponseArea.getEntity()).getName());
		Assert.assertEquals("Description", ((Area) restResponseArea.getEntity()).getDescription());
		Assert.assertEquals(hproject.getId(), ((Area) restResponseArea.getEntity()).getProject().getId());
		return area;
	}

	private String testMaxLength(int maxLength) {
		String symbol = "a";
		String descriptionLength = String.format("%" + maxLength + "s", " ").replaceAll(" ", symbol);
		Assert.assertEquals(maxLength, descriptionLength.length());
		return descriptionLength;
	}

}