package it.acsoftware.hyperiot.sparkmanager.service;

import java.io.IOException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerUtil;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiResponse;
import it.acsoftware.hyperiot.sparkmanager.model.SparkRestApiSubmissionRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

import it.acsoftware.hyperiot.sparkmanager.api.SparkManagerSystemApi;

import  it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl ;
import org.osgi.service.component.annotations.Reference;

/**
 * 
 * @author Aristide Cittadino Implementation class of the SparkManagerSystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = SparkManagerSystemApi.class, immediate = true)
public final class SparkManagerSystemServiceImpl extends HyperIoTBaseSystemServiceImpl
        implements SparkManagerSystemApi {

    private ObjectMapper objectMapper;
    private CloseableHttpClient sparkJobserverRestClient;
    private SparkManagerUtil sparkManagerUtil;
    private String sparkManagerUrl;

    @Activate
    public void activate() {
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        sparkJobserverRestClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        sparkManagerUrl = sparkManagerUtil.getSparkMasterHostname() + ":"
                + sparkManagerUtil.getSparkRestApiPort();
    }

    @Override
    public SparkRestApiResponse getStatus(String driverId) {
        try {
            String sparkManagerUrl = sparkManagerUtil.getSparkMasterHostname() + ":"
                    + sparkManagerUtil.getSparkRestApiPort();
            HttpGet httpGet = new HttpGet(sparkManagerUrl + "/v1/submissions/status/" + driverId);
            CloseableHttpResponse response = sparkJobserverRestClient.execute(httpGet);
            HttpEntity responseEntity = response.getEntity();
            String res = EntityUtils.toString(responseEntity);
            // map json to Object
            EntityUtils.consume(responseEntity);
            return objectMapper.readValue(res, SparkRestApiResponse.class);
        } catch (IOException e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public SparkRestApiResponse kill(String driverId) {
        try {
            String sparkManagerUrl = sparkManagerUtil.getSparkMasterHostname() + ":"
                    + sparkManagerUtil.getSparkRestApiPort();
            HttpPost httpPost = new HttpPost(sparkManagerUrl + "/v1/submissions/kill/" + driverId);
            CloseableHttpResponse response = sparkJobserverRestClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String res = EntityUtils.toString(responseEntity);
            // map json to Object
            EntityUtils.consume(responseEntity);
            return objectMapper.readValue(res, SparkRestApiResponse.class);
        } catch (IOException e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public SparkRestApiResponse submitJob(SparkRestApiSubmissionRequest data) {
        try {
            HttpPost httpPost = new HttpPost(sparkManagerUrl + "/v1/submissions/create");
            JSONObject payload = new JSONObject(data);
            httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
            CloseableHttpResponse response = sparkJobserverRestClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String res = EntityUtils.toString(responseEntity);
            // map json to Object
            EntityUtils.consume(responseEntity);
            return objectMapper.readValue(res, SparkRestApiResponse.class);
        } catch (IOException e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Reference
    public void setSparkManagerUtil(SparkManagerUtil sparkManagerUtil) {
        this.sparkManagerUtil = sparkManagerUtil;
    }

}
