package it.acsoftware.hyperiot.authentication.service.rest.test;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileReplacementOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import it.acsoftware.hyperiot.authentication.service.rest.AuthenticationRestApi;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class AuthenticationServiceRestTest extends KarafTestSupport {

	@Override
	@Configuration
	public Option[] config() {
		MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
				.artifactId("apache-karaf").version("4.2.3").type("tar.gz");

		String httpPort = "9091";
		String rmiRegistryPort = "2000";
		String rmiServerPort = "44445";
		String sshPort = "8102";
		File keyStoreFile = new File("src/main/resources/karaf-keystore");
		String localRepository = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
		if (localRepository == null) {
			localRepository = "";
		}
		
		return new Option[] {
				karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
				
				// enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
				configureSecurity().disableKarafMBeanServerBuilder(),
				keepRuntimeFolder(), logLevel(LogLevelOption.LogLevel.INFO),
				mavenBundle().groupId("org.apache.karaf.shell").artifactId("org.apache.karaf.shell.core").version("4.2.3"),
				mavenBundle().groupId("org.awaitility").artifactId("awaitility").version("3.1.6"),
				mavenBundle().groupId("org.apache.karaf.itests").artifactId("common").version("4.2.3"),
				//Datasource configuration with HSQL
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "databaseName","hyperiot"),
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "user","sa"),
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "password",""),
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "dataSourceName","hyperiot"),
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "osgi.jdbc.driver.class","H2"),
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "xa","true"),
				editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "pool","dbcp2"),
				//JWT Configuration
				editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg", "rs.security.keystore.type","jks"),
				editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg", "rs.security.keystore.password","hyperiot"),
				editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg", "rs.security.keystore.alias","karaf"),
				editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg", "rs.security.keystore.file","${karaf.etc}/karaf-keystore"),
				editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg", "rs.security.key.password","hyperiot"),
				editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg", "rs.security.signature.algorithm","RS256"),
				new KarafDistributionConfigurationFileReplacementOption("etc/karaf-keystore", keyStoreFile),
				//Adding hyperiot repositories
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"), ",mvn:it.acsoftware.hyperiot.base/HyperIoTBase-features/1.0.0/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"), ",mvn:it.acsoftware.hyperiot.permission/HyperIoTPermission-features/1.0.0/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"), ",mvn:it.acsoftware.hyperiot.role/HyperIoTRole-features/1.0.0/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"), ",mvn:it.acsoftware.hyperiot.huser/HyperIoTHUser-features/1.0.0/xml/features"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresRepositories"), ",mvn:it.acsoftware.hyperiot.authentication/HyperIoTAuthentication-features/1.0.0/xml/features"),
				//bootstraping components
				//new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"), ",pax-jdbc-h2 "),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-base"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-permission"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-role"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-huser"),
				new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer("etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-authentication"),
				
				editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port",httpPort),
				editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort",rmiRegistryPort),
				editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort",rmiServerPort),
				editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
				editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg","org.ops4j.pax.url.mvn.localRepository", localRepository) 
			};
	}

	@Test
	public void installAuthenticationBundle() throws Exception {
		// assert on an available service
		assertServiceAvailable(FeaturesService.class);
		String features = executeCommand("feature:list -i");
		assertContains("HyperIoTBase-features ", features);
		assertContains("HyperIoTPermission-features ", features);
		assertContains("HyperIoTRole-features ", features);
		assertContains("HyperIoTHUser-features ", features);
		assertContains("HyperIoTAuthentication-features ", features);
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		javax.ws.rs.core.Response restResponse = authRestService.checkModuleWorking();
		Assert.assertNotNull(authRestService);
		Assert.assertTrue(restResponse.getStatus() == 200);
		System.out.println(restResponse.getEntity().toString());
	}
	
	@Test
	public void loginFailTest() throws Exception{
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		javax.ws.rs.core.Response restResponse = authRestService.login("wrongUser", "wrongPassword");
		System.out.println(restResponse.toString());
		Assert.assertNotNull(authRestService);
		Assert.assertTrue(restResponse.getStatus() == 401);
	}
	
	@Test
	public void loginSuccessTest() throws Exception{
		AuthenticationRestApi authRestService = getOsgiService(AuthenticationRestApi.class);
		javax.ws.rs.core.Response restResponse = authRestService.login("hadmin", "admin");
		System.out.println(restResponse.toString());
		Assert.assertNotNull(authRestService);
		Assert.assertTrue(restResponse.getStatus() == 200);
	}

}
