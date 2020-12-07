package it.acsoftware.hyperiot.sparkmanager.job;

import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerSystemApi;
import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerUtil;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiResponse;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiSubmissionRequest;
import org.quartz.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public abstract class HyperIoTSparkJob implements Job {

    protected static final Logger LOGGER = Logger.getLogger("it.acsoftware.hyperiot");
    private static final String ERROR_MESSAGE = "Job {0} is not going to be fired because of empty value for argument {1}";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDetail jobDetail = context.getJobDetail();
        JobKey jobKey = jobDetail.getKey();
        JobDataMap jobDataMap = jobDetail.getJobDataMap();
        try {
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
            // get SparkManagerUtil OSGi service
            SparkManagerUtil sparkManagerUtil =
                    (SparkManagerUtil) HyperIoTUtil.getService(SparkManagerUtil.class);
            // get Spark job arguments
            String[] appArgs = getAppArgs(jobDetail);
            // set env variables
            Map<String, String> envVars = new HashMap<>();
            envVars.put("SPARK_ENV_LOADED", String.valueOf(sparkManagerUtil.getSparkEnvLoaded()));
            // get Spark properties
            Map<String, String> sparkProps = getSparkProperties(sparkManagerUtil, jobKey.toString(), sparkJarsProperty);

            request.setAction("CreateSubmissionRequest");
            request.setAppResource(appResource);
            request.setMainClass(mainClass);
            request.setClientSparkVersion(sparkManagerUtil.getSparkClientVersion());
            request.setAppArgs(appArgs);
            request.setEnvironmentVariables(envVars);
            request.setSparkProperties(sparkProps);

            SparkRestApiResponse response = submitJob(request);
            LOGGER.log(Level.INFO, "Job submission request sent. Response: {0}", response);
        } catch (Throwable e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw new JobExecutionException(e);
        }
    }

    /**
     * This method get specific job arguments
     * @param jobDetail jobDetail
     * @return String array containing job arguments
     */
    public abstract String[] getAppArgs(JobDetail jobDetail);

    private String getJobArg(JobKey jobKey, JobDataMap jobDataMap, String key) {
        if (jobDataMap.containsKey(key))
            return jobDataMap.getString(key);
        else {
            LOGGER.log(Level.SEVERE, ERROR_MESSAGE, new Object[]{jobKey, key});
            return null;
        }
    }

    /**
     * It returns Spark properties. Parameters appName and sparkJarsProperty are related to job which will be executed
     * @param sparkManagerUtil SparkManagerUtil OSGi service
     * @param appName Job app name
     * @param sparkJarsProperty Jars which job needs
     * @return Map containing Spark properties
     */
    private Map<String, String> getSparkProperties(SparkManagerUtil sparkManagerUtil, String appName,
                                                   String sparkJarsProperty) {

        Map<String, String> sparkProps = new HashMap<>();
        sparkProps.put("spark.jars", sparkJarsProperty);
        sparkProps.put("spark.driver.supervise", String.valueOf(sparkManagerUtil.getSparkDriverSupervise()));
        sparkProps.put("spark.app.name", appName); // This property is mandatory as the others, however if job define its name, the latter will override the former
        sparkProps.put("spark.submit.deployMode", sparkManagerUtil.getSparkSubmitDeployMode());
        sparkProps.put("spark.master", sparkManagerUtil.getSparkRestApiUrl());
        sparkProps.put("spark.jars.packages", "org.apache.spark:spark-avro_2.11:2.4.5");    // HyperIoT Spark jobs need avro dependency to read data on which they do computation
        return sparkProps;
    }

    /**
     * It submits Spark job asynchronously
     * @param request Request to be sent to Spark hidden REST api
     * @return SparkRestApiResponse
     */
    private SparkRestApiResponse submitJob(SparkRestApiSubmissionRequest request) {
        SparkManagerSystemApi sparkManagerSystemApi =
                (SparkManagerSystemApi) HyperIoTUtil.getService(SparkManagerSystemApi.class);
        return sparkManagerSystemApi.submitJob(request);
    }

}
