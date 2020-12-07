package it.acsoftware.hyperiot.sparkmanager.api;

public interface SparkManagerUtil {

    String getSparkClientVersion();
    boolean getSparkDriverSupervise();
    int getSparkEnvLoaded();
    String getSparkMasterHostname();
    int getSparkRestApiPort();
    String getSparkRestApiUrl();
    String getSparkSubmitDeployMode();

}
