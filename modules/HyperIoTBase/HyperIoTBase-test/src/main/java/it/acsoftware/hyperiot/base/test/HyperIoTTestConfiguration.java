/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 */
package it.acsoftware.hyperiot.base.test;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.debugConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;

import org.apache.karaf.itests.KarafTestSupport;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.ConfigurationPointer;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileExtendOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionConfigurationFileReplacementOption;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

/**
 * Author Aristide Cittadino
 * Helper class for tests
 */
public class HyperIoTTestConfiguration {
    public static final String MIN_RMI_SERVER_PORT = "44444";
    public static final String MAX_RMI_SERVER_PORT = "65534";
    public static final String MIN_HTTP_PORT = "9080";
    public static final String MAX_HTTP_PORT = "9999";
    public static final String MIN_RMI_REG_PORT = "1099";
    public static final String MAX_RMI_REG_PORT = "9999";
    public static final String MIN_SSH_PORT = "8101";
    public static final String MAX_SSH_PORT = "8888";

    private String httpPort;
    private String rmiRegistryPort;
    private String rmiServerPort;
    private String sshPort;
    private String karafVersion;
    private String hyperiotVersion;
    private String awaitilityVersion;

    private Option[] options;

    public HyperIoTTestConfiguration(String karafVersion, String hyperIoTVersion,
                                     String awaitilityVersion) {
        httpPort = Integer.toString(KarafTestSupport.getAvailablePort(
                Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        rmiRegistryPort = Integer.toString(KarafTestSupport.getAvailablePort(
                Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        rmiServerPort = Integer.toString(KarafTestSupport.getAvailablePort(
                Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        sshPort = Integer.toString(KarafTestSupport.getAvailablePort(Integer.parseInt(MIN_SSH_PORT),
                Integer.parseInt(MAX_SSH_PORT)));

        this.karafVersion = karafVersion;
        this.hyperiotVersion = hyperIoTVersion;
        this.awaitilityVersion = awaitilityVersion;
        this.options = getBaseConfig();
    }

    public HyperIoTTestConfiguration withPostgresSQL() {
        Option[] postgresSQL = {
                // Datasource configuration with Postgres
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "databaseName",
                        "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "portNumber",
                        "5432"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "serverName",
                        "localhost"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "user",
                        "postgres"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "password", ""),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "dataSourceName",
                        "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg",
                        "osgi.jdbc.driver.class", "org.postgresql.Driver"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "xa", "true"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "pool",
                        "dbcp2")};
        return append(postgresSQL);
    }

    public HyperIoTTestConfiguration withDebug(String port, boolean hold) {
        Option[] debugConfig = {debugConfiguration(port, hold)};
        return append(debugConfig);
    }

    public HyperIoTTestConfiguration withHSQL() {
        Option[] hsqlConfig = {
                // Datasource configuration with HSQL
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "databaseName",
                        "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "user", "sa"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "password", ""),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "dataSourceName",
                        "hyperiot"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg",
                        "osgi.jdbc.driver.name", "H2"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "xa", "true"),
                editConfigurationFilePut("etc/org.ops4j.datasource-hyperiot.cfg", "pool",
                        "dbcp2")};
        return append(hsqlConfig);
    }

    public HyperIoTTestConfiguration append(Option[] customOptions) {
        Option[] config = new Option[this.options.length + customOptions.length];
        System.arraycopy(this.options, 0, config, 0, this.options.length);
        System.arraycopy(customOptions, 0, config, options.length, customOptions.length);
        this.options = config;
        return this;
    }

    public Option[] build() {
        return this.options;
    }

    /**
     * @deprecated Replaced by {@link #append(Option[])}
     */
    @Deprecated
    public HyperIoTTestConfiguration addCustomTestConfiguration(Option[] customOptions) {
        return append(customOptions);
    }

    private Option[] getBaseConfig() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf")
                .artifactId("apache-karaf").version(this.karafVersion).type("tar.gz");
        File keyStoreFile = new File("src/main/resources/karaf-keystore");
        if (!keyStoreFile.exists())
            throw new RuntimeException(
                    "Keystore file needed inside src/main/resources/karaf-keystore for JWT authentication");

        String localRepository = System.getProperty("org.ops4j.pax.url.mvn.localRepository");
        if (localRepository == null) {
            localRepository = "";
        }

        return new Option[]{
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf")
                        .unpackDirectory(new File("target/exam")).useDeployFolder(false),
                KarafDistributionOption.keepRuntimeFolder(),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                configureConsole().startLocalConsole().ignoreRemoteShell(),
                mavenBundle().groupId("org.apache.karaf.shell")
                        .artifactId("org.apache.karaf.shell.core").version(this.karafVersion),
                mavenBundle().groupId("org.awaitility").artifactId("awaitility")
                        .version(this.awaitilityVersion),
                mavenBundle().groupId("org.apache.karaf.itests").artifactId("common")
                        .version(this.karafVersion),
                // JWT Configuration
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg",
                        "rs.security.keystore.type", "jks"),
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg",
                        "rs.security.keystore.password", "hyperiot"),
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg",
                        "rs.security.keystore.alias", "karaf"),
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg",
                        "rs.security.keystore.file", "${karaf.etc}/karaf-keystore"),
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg",
                        "rs.security.key.password", "hyperiot"),
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.jwt.cfg",
                        "rs.security.signature.algorithm", "RS256"),
                // Setting test mode ON
                editConfigurationFilePut("etc/it.acsoftware.hyperiot.cfg",
                        "it.acsoftware.hyperiot.testMode", "true"),
                new KarafDistributionConfigurationFileReplacementOption("etc/karaf-keystore",
                        keyStoreFile),
                // Adding hyperiot repositories
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.base/HyperIoTBase-features/"
                                + this.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.permission/HyperIoTPermission-features/"
                                + this.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.role/HyperIoTRole-features/"
                                + this.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.huser/HyperIoTHUser-features/"
                                + this.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.authentication/HyperIoTAuthentication-features/"
                                + this.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.mail/HyperIoTMail-features/"
                                + this.hyperiotVersion + "/xml/features"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresRepositories"),
                        ",mvn:it.acsoftware.hyperiot.kafka.connector/HyperIoTKafkaConnector-features/"
                                + "1.0.0" + "/xml/features"),
                // bootstraping components
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-base"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresBoot"),
                        ",hyperiot-permission"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-role"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-huser"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresBoot"),
                        ",hyperiot-authentication"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",hyperiot-mail"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.apache.karaf.features.cfg",
                                "featuresBoot"),
                        ",hyperiot-kafkaconnector"),
                new KarafDistributionConfigurationFileExtendOption(new ConfigurationPointer(
                        "etc/org.apache.karaf.features.cfg", "featuresBoot"), ",pax-jdbc-h2"),
                new KarafDistributionConfigurationFileExtendOption(
                        new ConfigurationPointer("etc/org.ops4j.pax.url.mvn.cfg",
                                "org.ops4j.pax.url.mvn.repositories"),
                        ",https://hyperiot-developer-user:18t7FJ3J3@nexus.acsoftware.it/nexus/repository/maven/"),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port",
                        httpPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort",
                        rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort",
                        rmiServerPort),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg",
                        "org.ops4j.pax.url.mvn.localRepository", localRepository)};

    }
}
