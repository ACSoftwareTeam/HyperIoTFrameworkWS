package it.acsoftware.hyperiot.zookeeper.connector.test;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
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

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.api.HyperIoTUser;
import it.acsoftware.hyperiot.base.service.rest.HyperIoTBaseRestApi;
import it.acsoftware.hyperiot.base.test.HyperIoTTestConfigurationBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;

/**
 *
 * @author Aristide Cittadino Interface component for ZookeeperConnector System Service.
 *
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ZookeeperConnectorTest extends KarafTestSupport {

	@Configuration
	public Option[] config() {
        //starts with HSQL
		Option[] customOptions = { new KarafDistributionConfigurationFileExtendOption(
				new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
						"featuresRepositories"),
				",mvn:it.acsoftware.hyperiot.zookeeper.connector/ZookeeperConnector-features/1.0.0" +
						"/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(
						new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"),
						",hyperiot-zookeeperconnector") };
		return HyperIoTTestConfigurationBuilder.createStandardConfiguration().append(customOptions).build();
	}

	public HyperIoTContext impersonateUser(HyperIoTBaseRestApi restApi,HyperIoTUser user) {
		return restApi.impersonate(user);
	}

	private HyperIoTAction getHyperIoTAction(String resourceName,
			HyperIoTActionName action, long timeout) {
		String actionFilter = OSGiFilterBuilder
				.createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, resourceName)
				.and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
		return getOsgiService(HyperIoTAction.class, actionFilter, timeout);
	}

	@Test
	public void hyperIoTFrameworkShouldBeInstalled() throws Exception {
		// assert on an available service
		assertServiceAvailable(FeaturesService.class);
		String features = executeCommand("feature:list -i");
		assertContains("HyperIoTBase-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTRole-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTAuthentication-features ", features);
		String datasource = executeCommand("jdbc:ds-list");
		assertContains("hyperiot", datasource);
	}

}
