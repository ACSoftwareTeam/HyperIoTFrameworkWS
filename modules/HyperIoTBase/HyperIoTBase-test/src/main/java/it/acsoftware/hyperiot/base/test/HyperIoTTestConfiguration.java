/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 */
package it.acsoftware.hyperiot.base.test;

import org.apache.karaf.itests.KarafTestSupport;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

/**
 * Author Aristide Cittadino
 * Helper class for tests
 */
public class HyperIoTTestConfiguration {
    private static Logger log = Logger.getLogger(HyperIoTTestConfiguration.class.getName());
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
    private String hyperiotVersion;
    private String karafVersion;

    private Option[] options;

    public HyperIoTTestConfiguration(String karafVersion, String hyperIoTVersion) {
        httpPort = Integer.toString(KarafTestSupport.getAvailablePort(
            Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        rmiRegistryPort = Integer.toString(KarafTestSupport.getAvailablePort(
            Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        rmiServerPort = Integer.toString(KarafTestSupport.getAvailablePort(
            Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        sshPort = Integer.toString(KarafTestSupport.getAvailablePort(Integer.parseInt(MIN_SSH_PORT),
            Integer.parseInt(MAX_SSH_PORT)));
        this.hyperiotVersion = hyperIoTVersion;
        this.karafVersion = karafVersion;
        this.options = getBaseConfig();
        log.log(Level.INFO, "SSH PORT: {}", new Object[]{sshPort});
    }

    public HyperIoTTestConfiguration withDebug(String port, boolean hold) {
        Option[] debugConfig = {debugConfiguration(port, hold)};
        return append(debugConfig);
    }

    public HyperIoTTestConfiguration keepRuntime() {
        Option opt = KarafDistributionOption.keepRuntimeFolder();
        return append(new Option[]{opt});
    }

    public HyperIoTTestConfiguration withLogLevel(LogLevelOption.LogLevel level) {
        Option opt = KarafDistributionOption.logLevel(level);
        return append(new Option[]{opt});
    }

    @Deprecated
    public HyperIoTTestConfiguration withHSQL() {
        //default distribution for test is with HSQL
        Option[] hsqlConfig = {};
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
        MavenArtifactUrlReference karafUrl = maven().groupId("it.acsoftware.hyperiot.container")
            .artifactId("hyperiot-karaf-distribution-test").version(this.hyperiotVersion).type("tar.gz");
        return new Option[]{
            karafDistributionConfiguration().frameworkUrl(karafUrl).name("HyperIoT Karaf Distribution")
                .unpackDirectory(new File("target/exam")).useDeployFolder(false),
            // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
            configureSecurity().disableKarafMBeanServerBuilder(),
            // Setting test mode ON
            mavenBundle().groupId("org.apache.karaf.itests").artifactId("common")
                .version(this.karafVersion),
            editConfigurationFilePut("etc/it.acsoftware.hyperiot.cfg",
                "it.acsoftware.hyperiot.testMode", "true"),
            editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port",
                httpPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort",
                rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort",
                rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort)};

    }
}
