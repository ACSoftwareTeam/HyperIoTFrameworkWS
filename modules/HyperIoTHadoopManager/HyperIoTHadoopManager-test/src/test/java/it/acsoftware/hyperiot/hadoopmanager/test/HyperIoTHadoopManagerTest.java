package it.acsoftware.hyperiot.hadoopmanager.test;

import it.acsoftware.hyperiot.hadoopmanager.api.HadoopManagerSystemApi;
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

import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;

import java.io.File;
import java.io.IOException;

import static it.acsoftware.hyperiot.hadoopmanager.test.HyperIoTHadoopManagerConfiguration.*;

/**
 * 
 * @author Aristide Cittadino Interface component for HadoopManager System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HyperIoTHadoopManagerTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
		// starts with HSQL
		// the standard configuration has been moved to the HyperIoTHadoopManagerConfiguration class
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
		assertContains("HyperIoTAuthentication-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTCompany-features ", features);
		assertContains("HyperIoTRole-features ", features);
		assertContains("HyperIoTAssetCategory-features", features);
		assertContains("HyperIoTAssetTag-features", features);
		assertContains("HyperIoTSharedEntity-features", features);
		assertContains("HyperIoTHadoopManager-features ", features);
		String datasource = executeCommand("jdbc:ds-list");
//		System.out.println(executeCommand("bundle:list | grep HyperIoT"));
		assertContains("hyperiot", datasource);
	}


	@Test
	public void test001_checksIfFileHadoopManagerCfgExists() {
		// Test will be runs if docker image has been launched.
		// Please runs "docker-compose -f docker-compose-svil-hdfs-only.yml up"
		// checks if it.acsoftware.hyperiot.hadoopmanager.cfg exists.
		// If file not found HyperIoTHadoopManager-service bundle is in Waiting state
		String hyperIoTHadoopManagerService = executeCommand("bundle:list | grep HyperIoTHadoopManager-service");
		boolean fileCfgHadoopManagerFound = false;
		String fileConfigHadoopManager = executeCommand("config:list | grep it.acsoftware.hyperiot.hadoopmanager.cfg");
		if (hyperIoTHadoopManagerService.contains("│ Active  │")) {
			Assert.assertTrue(hyperIoTHadoopManagerService.contains("│ Active  │"));
			if (fileConfigHadoopManager.contains("it.acsoftware.hyperiot.hadoopmanager.cfg")) {
				Assert.assertTrue(fileConfigHadoopManager.contains("it.acsoftware.hyperiot.hadoopmanager.cfg"));
				fileCfgHadoopManagerFound = true;
			}
		}
		if (hyperIoTHadoopManagerService.contains("│ Waiting │")) {
			Assert.assertTrue(hyperIoTHadoopManagerService.contains("│ Waiting │"));
			if (fileConfigHadoopManager.isEmpty()) {
				Assert.assertTrue(fileConfigHadoopManager.isEmpty());
				Assert.assertFalse(fileCfgHadoopManagerFound);
				System.out.println("file etc/it.acsoftware.hyperiot.hadoopmanager.cfg not found...");
			}
		}
		Assert.assertTrue(fileCfgHadoopManagerFound);
	}


	@Test (expected = IOException.class)
	public void test002_copyFileShouldWork() throws IOException {
		// Test will be runs if docker image has been launched.
		// Please runs "docker-compose -f docker-compose-svil-hdfs-only.yml up"
		HadoopManagerSystemApi hadoopManagerSystemApi = getOsgiService(HadoopManagerSystemApi.class);
		File algorithmFile = new File(jarPath + jarName);
		hadoopManagerSystemApi.copyFile(algorithmFile, String.valueOf(algorithmFile), true);
	}


	@Test
	public void test003_copyFileShouldFailIfPathAlreadyExistsAsADirectory() {
		// Test will be runs if docker image has been launched.
		// Please runs "docker-compose -f docker-compose-svil-hdfs-only.yml up"
		HadoopManagerSystemApi hadoopManagerSystemApi = getOsgiService(HadoopManagerSystemApi.class);
		File algorithmFile = new File(jarPath + jarName);
		try {
			hadoopManagerSystemApi.copyFile(algorithmFile, jarPath, true);
		} catch (IOException e) {
			String msg = e.getMessage();
			Assert.assertTrue(msg.contains("/spark/jobs already exists as a directory"));
		}
	}


	@Test
	public void test004_deleteFileShouldWork() throws IOException {
		// Test will be runs if docker image has been launched.
		// Please runs "docker-compose -f docker-compose-svil-hdfs-only.yml up"
		HadoopManagerSystemApi hadoopManagerSystemApi = getOsgiService(HadoopManagerSystemApi.class);
		hadoopManagerSystemApi.deleteFile(jarPath + jarName);
	}


}

