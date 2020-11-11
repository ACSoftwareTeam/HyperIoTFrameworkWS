package it.acsoftware.hyperiot.hbase.connector.service.postactions;

import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;

import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

@Component(service = HProjectPostActionThreadPool.class, immediate = true)
public class HProjectPostActionThreadPool {

    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");
    private ExecutorService executorService;
    /**
     * Injecting the HBaseConnectorSystemApi
     */
    private HBaseConnectorSystemApi hBaseConnectorSystemService;
    /**
     * Injecting the HProjectUtil
     */
    private HBaseConnectorUtil hBaseConnectorUtil;

    @Activate
    public void activate() {
        executorService = new ThreadPoolExecutor(hBaseConnectorUtil.getCorePoolSize(), hBaseConnectorUtil.getMaximumPoolSize(),
                hBaseConnectorUtil.getKeepAliveTime(), TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    }

    /**
     * Remove HBase tables related to this HProject (see asynchronousPostSaveAction for knowledge about HBase tables)
     * @param hProjectId HProject ID
     */
    private void asynchronousPostRemoveAction(long hProjectId) {
        String avroTableName = "avro_hproject_" + hProjectId;
        String timelineTableName = "timeline_hproject_" + hProjectId;
        String errorTableName = "hproject_error_" + hProjectId;
        String kafkaErrortableName = "kafka_error_" + hProjectId;
        try {
            log.log(Level.FINE, "Disabling table {0}", avroTableName);
            hBaseConnectorSystemService.disableTable(avroTableName);
            log.log(Level.FINE, "Disabling table {0}", timelineTableName);
            hBaseConnectorSystemService.disableTable(timelineTableName);
            log.log(Level.FINE, "Disabling table {0}", errorTableName);
            hBaseConnectorSystemService.disableTable(errorTableName);
            log.log(Level.FINE, "Disabling table {0}", kafkaErrortableName);
            hBaseConnectorSystemService.disableTable(kafkaErrortableName);
            log.log(Level.FINE, "Dropping table {0}", avroTableName);
            hBaseConnectorSystemService.dropTable(avroTableName);
            log.log(Level.FINE, "Dropping table {0}", timelineTableName);
            hBaseConnectorSystemService.dropTable(timelineTableName);
            log.log(Level.FINE, "Dropping table {0}", errorTableName);
            hBaseConnectorSystemService.dropTable(errorTableName);
            log.log(Level.FINE, "Dropping table {0}", kafkaErrortableName);
            hBaseConnectorSystemService.dropTable(kafkaErrortableName);
            log.log(Level.FINE, "HBase tables of HProject with id {0} removed", hProjectId);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error during HBase table removing", e);
        }
    }

    /**
     * For each hproject, there are four tables:
     *  - avro_hproject_<hprojectID> contains HPackets related to this project, in Avro format
     *  - timeline_hproject_<hprojectID> contains HPacket counting, for timeline queries
     *  - hproject_error_<hprojectID> contains errors (why we could not save hpacket instances)
     *  - kafka_error_<hprojectID> contains wrong input from Kafka
     * @param hProjectId HProject ID
     */
    private void asynchronousPostSaveAction(long hProjectId) {
        String avroTableName = "avro_hproject_" + hProjectId;
        String timelineTableName = "timeline_hproject_" + hProjectId;
        String errorTableName = "hproject_error_" + hProjectId;
        String kafkaErrorTableName = "kafka_error_" + hProjectId;
        List<String> avroColumnFamilies = new ArrayList<>();
        avroColumnFamilies.add("hpacket");
        List<String> timelineColumnFamilies = new ArrayList<>();
        timelineColumnFamilies.add("year"); timelineColumnFamilies.add("month"); timelineColumnFamilies.add("day");
        timelineColumnFamilies.add("hour"); timelineColumnFamilies.add("minute"); timelineColumnFamilies.add("second");
        timelineColumnFamilies.add("millisecond");
        List<String> tableErrorColumnFamilies = new ArrayList<>();
        tableErrorColumnFamilies.add("error");
        try {
            log.log(Level.FINE, "Creating table {0}", avroTableName);
            hBaseConnectorSystemService.createTable(avroTableName, avroColumnFamilies);
            log.log(Level.FINE, "Creating table {0}", timelineTableName);
            hBaseConnectorSystemService.createTable(timelineTableName, timelineColumnFamilies);
            log.log(Level.FINE, "Creating table {0}", errorTableName);
            hBaseConnectorSystemService.createTable(errorTableName, tableErrorColumnFamilies);
            log.log(Level.FINE, "Creating table {0}", kafkaErrorTableName);
            hBaseConnectorSystemService.createTable(kafkaErrorTableName, tableErrorColumnFamilies);
            log.log(Level.FINE, "HBase tables of HProject with id {0} created", hProjectId);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error during HBase table creations", e);
        }
    }

    /**
     * Create HBase tables, if they do not exist, related to this HProject (see asynchronousPostSaveAction for knowledge about HBase tables)
     * @param hProjectId HProject ID
     */
    private void asynchronousPostUpdateAction(long hProjectId) {
        String avroTableName = "avro_hproject_" + hProjectId;
        String timelineTableName = "timeline_hproject_" + hProjectId;
        String errorTableName = "hproject_error_" + hProjectId;
        String kafkaErrorTableName = "kafka_error_" + hProjectId;
        try {
            createTables(avroTableName, timelineTableName, errorTableName, kafkaErrorTableName);
            log.log(Level.FINE, "Post update actions of HProject with id {0} executed", hProjectId);
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error during HBase table creations", e);
        }
    }

    private void createTables(String avroTableName, String timelineTableName, String errorTableName, String kafkaErrorTableName)
            throws IOException {
        if(!hBaseConnectorSystemService.tableExist(avroTableName)) {
            log.log(Level.FINE, "Creating table {0}", avroTableName);
            List<String> avroColumnFamilies = new ArrayList<>();
            avroColumnFamilies.add("hpacket");
            hBaseConnectorSystemService.createTable(avroTableName, avroColumnFamilies);
        }
        if(!hBaseConnectorSystemService.tableExist(timelineTableName)) {
            log.log(Level.FINE, "Creating table {0}", timelineTableName);
            List<String> timelineColumnFamilies = new ArrayList<>();
            timelineColumnFamilies.add("year"); timelineColumnFamilies.add("month"); timelineColumnFamilies.add("day");
            timelineColumnFamilies.add("hour"); timelineColumnFamilies.add("minute"); timelineColumnFamilies.add("second");
            timelineColumnFamilies.add("millisecond");
            hBaseConnectorSystemService.createTable(timelineTableName, timelineColumnFamilies);
        }
        if(!hBaseConnectorSystemService.tableExist(errorTableName)) {
            log.log(Level.FINE, "Creating table {0}", errorTableName);
            List<String> tableErrorColumnFamilies = new ArrayList<>();
            tableErrorColumnFamilies.add("error");
            hBaseConnectorSystemService.createTable(errorTableName, tableErrorColumnFamilies);
        }
        if(!hBaseConnectorSystemService.tableExist(kafkaErrorTableName)) {
            log.log(Level.FINE, "Creating table {0}", kafkaErrorTableName);
            List<String> tableErrorColumnFamilies = new ArrayList<>();
            tableErrorColumnFamilies.add("error");
            hBaseConnectorSystemService.createTable(kafkaErrorTableName, tableErrorColumnFamilies);
        }
    }

    /**
     * Shutdown executor service after stopping application
     */
    @Deactivate
    public void deactivate() {
        // stop taking new tasks
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(hBaseConnectorUtil.getAwaitTermination(), TimeUnit.MILLISECONDS)) {
                // wait up to 1000 ms for all tasks to be completed
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            // if time expires, stop execution immediately
            executorService.shutdownNow();
        }
    }

    /**
     * Execute HProject post remove action in asynchronous way
     * @param hProjectId HProject ID
     */
    public void runPostRemoveAction(long hProjectId) {
        Runnable runnableTask = () -> asynchronousPostRemoveAction(hProjectId);
        executorService.execute(runnableTask); // At the moment, we don't need to get the result of task's execution
    }

    /**
     * Execute HProject post save action in asynchronous way
     * @param hProjectId HProject ID
     */
    public void runPostSaveAction(long hProjectId) {
        Runnable runnableTask = () -> asynchronousPostSaveAction(hProjectId);
        executorService.execute(runnableTask); // At the moment, we don't need to get the result of task's execution
    }

    /**
     * Execute HProject post update action in asynchronous way
     * @param hProjectId HProject ID
     */
    public void runPostUpdateAction(long hProjectId) {
        Runnable runnableTask = () -> asynchronousPostUpdateAction(hProjectId);
        executorService.execute(runnableTask); // At the moment, we don't need to get the result of task's execution
    }

    /**
     *
     * @param hBaseConnectorSystemService HBaseConnectorSystemApi service
     */
    @Reference
    public void setHBaseConnectorSystemService(HBaseConnectorSystemApi hBaseConnectorSystemService) {
        this.hBaseConnectorSystemService = hBaseConnectorSystemService;
    }

    /**
     *
     * @param hBaseConnectorUtil HBaseConnectorUtil service
     */
    @Reference
    public void sethBaseConnectorUtil(HBaseConnectorUtil hBaseConnectorUtil) {
        this.hBaseConnectorUtil = hBaseConnectorUtil;
    }

}
