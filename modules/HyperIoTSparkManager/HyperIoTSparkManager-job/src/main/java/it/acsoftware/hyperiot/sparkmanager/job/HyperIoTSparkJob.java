package it.acsoftware.hyperiot.sparkmanager.job;

import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerSystemApi;
import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerUtil;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiResponse;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiSubmissionRequest;
import org.osgi.framework.FrameworkUtil;
import org.quartz.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class HyperIoTSparkJob implements Job {

    protected static final Logger LOGGER = Logger.getLogger("it.acsoftware.hyperiot");
    private static final String WARNING_MESSAGE = "Job {0} is not going to be fired because of empty value for argument {1}";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // TODO check if job is ready to accept algorithmId and projectId outside of json config
        JobDetail jobDetail = context.getJobDetail();
        JobKey jobKey = jobDetail.getKey();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        LOGGER.log(Level.INFO, "Preparing job {0} for its execution", jobKey);
        // Create request body which will be sent to Spark hidden REST api
        SparkRestApiSubmissionRequest request = new SparkRestApiSubmissionRequest();
        // get some mandatory parameters
        String appResource = getJobArg(jobKey, jobDataMap, "appResource");
        if (appResource == null)
            return;
        String mainClass = getJobArg(jobKey, jobDataMap, "mainClass");
        if (mainClass == null)
            return;
        String sparkJarsProperty = getJobArg(jobKey, jobDataMap, "spark.jars");
        if (sparkJarsProperty == null)
            return;
        // get Spark job arguments
        String[] appArgs = getAppArgs(jobDetail);
        // get env variables
        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPARK_ENV_LOADED", "1");
        // get Spark properties
        Map<String, String> sparkProps = getSparkProperties(jobKey.toString(), sparkJarsProperty);

        request.setAction("CreateSubmissionRequest");
        request.setAppResource(appResource);
        request.setMainClass(mainClass);
        request.setClientSparkVersion("2.4.5");
        request.setAppArgs(appArgs);
        request.setEnvironmentVariables(envVars);
        request.setSparkProperties(sparkProps);

        SparkRestApiResponse response = submitJob(request);
        LOGGER.log(Level.INFO, "Job submission request sent. Response: {0}", response);
    }

    public abstract String[] getAppArgs(JobDetail jobDetail);

    private String getJobArg(JobKey jobKey, JobDataMap jobDataMap, String key) {
        if (jobDataMap.containsKey("appResource"))
            return jobDataMap.getString("appResource");
        else {
            LOGGER.log(Level.WARNING, WARNING_MESSAGE, new Object[]{jobKey, "projectId"});
            return null;
        }
    }

    /**
     * It returns Spark properties. Parameters appName and sparkJarsProperty are related to job which will be executed
     * @param appName Job app name
     * @param sparkJarsProperty Jars which job needs
     * @return Map containing Spark properties
     */
    private Map<String, String> getSparkProperties(String appName, String sparkJarsProperty) {
        SparkManagerUtil sparkManagerUtil = (SparkManagerUtil) FrameworkUtil.getBundle(SparkManagerUtil.class)
                .getBundleContext().getServiceReference(SparkManagerUtil.class);
        Map<String, String> sparkProps = new HashMap<>();
        sparkProps.put("spark.jars", sparkJarsProperty);
        sparkProps.put("spark.driver.supervise", "false");
        sparkProps.put("spark.app.name", appName);
        sparkProps.put("spark.submit.deployMode", "cluster");
        sparkProps.put("spark.master", sparkManagerUtil.getSparkMasterHostname() + ":"
                + sparkManagerUtil.getSparkRestApiPort());
        sparkProps.put("packages", "org.apache.spark:spark-avro_2.11:2.4.5"); //TODO check if this new property works (when tested, I did not have it)
        return sparkProps;
    }

    /**
     * It submit Spark job asynchronously
     * @param request Request to be sent to Spark hidden REST api
     * @return SparkRestApiResponse
     */
    private SparkRestApiResponse submitJob(SparkRestApiSubmissionRequest request) {
        SparkManagerSystemApi sparkManagerSystemApi =
                (SparkManagerSystemApi) FrameworkUtil.getBundle(SparkManagerSystemApi.class).getBundleContext().getServiceReference(SparkManagerSystemApi.class);
        return sparkManagerSystemApi.submitJob(request);
    }

}
