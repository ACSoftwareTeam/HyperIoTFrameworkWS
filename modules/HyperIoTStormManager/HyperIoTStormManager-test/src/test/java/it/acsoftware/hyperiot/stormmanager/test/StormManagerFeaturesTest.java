/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * Written by Gene <generoso.martello@acsoftware.it>, March 2019
 *
 */
package it.acsoftware.hyperiot.stormmanager.test;

import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.stormmanager.service.rest.StormManagerRestApi;
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
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import javax.ws.rs.core.Response;

/**
 *
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-11 Initial release
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class StormManagerFeaturesTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
		//starts with HSQL
		Option[] options = {
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
						",mvn:it.acsoftware.hyperiot.hproject/HyperIoTHProject-features/" + Defs.hyperiotVersion
								+ "/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
						",mvn:it.acsoftware.hyperiot.hdevice/HyperIoTHDevice-features/" + Defs.hyperiotVersion
								+ "/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
						",mvn:it.acsoftware.hyperiot.hpacket/HyperIoTHPacket-features/" + Defs.hyperiotVersion
								+ "/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
						",mvn:it.acsoftware.hyperiot.stormmanager/HyperIoTStormManager-features/" + Defs.hyperiotVersion
								+ "/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"),
						",mvn:it.acsoftware.hyperiot.rule/HyperIoTRuleEngine-features/" + Defs.hyperiotVersion
								+ "/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
						",hyperiot-hproject,hyperiot-hdevice,hyperiot-hpacket,hyperiot-stormmanager,hyperiot-ruleengine"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
								"featuresRepositories"),
						",mvn:it.acsoftware.hyperiot.stormmanager/HyperIoTStormManager-features/"
								+ Defs.hyperiotVersion + "/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
						"etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-stormmanager")
		};
		return HyperIoTTestConfigurationBuilder
                .createStandardConfiguration()
                // Starts with HSQL
                .withHSQL()
                // Custom options
                .append(options)
                .build();
	}

	@Test
	public void test00_hyperIoTFrameworkShouldBeInstalled() throws Exception {
		// assert on an available service
		assertServiceAvailable(FeaturesService.class);
		String features = executeCommand("feature:list -i");
		System.out.println(features);
		assertContains("HyperIoTBase-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTRole-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTAuthentication-features ", features);
		assertContains("HyperIoTStormManager-features ", features);
		String datasource = executeCommand("jdbc:ds-list");
		System.out.println(datasource);
		assertContains("hyperiot", datasource);
	}

	@Test
	public void test01_stormManagerRestModuleShouldWork() {
		StormManagerRestApi stormManagerRestApi = getOsgiService(StormManagerRestApi.class);
		// Test StormManagerRestApi module
		Response response = stormManagerRestApi.checkModuleWorking();
		Assert.assertEquals(200, response.getStatus());
	}

	// TODO: implement test for ensuring that Storm folder and binaries are set

}
