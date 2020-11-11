package it.acsoftware.hyperiot.hbase.connector.api;

import com.google.protobuf.ServiceException;
import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnectorHPacketCount;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnectorHProjectScan;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseTimelineColumnFamily;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnectorTimelineScan;
import it.acsoftware.hyperiot.hpacket.model.HPacket;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * 
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface HBaseConnectorSystemApi extends HyperIoTBaseSystemApi {

    /**
     * It checks if there's an active connection
     * @throws IOException IOException
     */
    void checkConnection() throws IOException, ServiceException;

    /**
     * It creates HBase Table
     * @param tableName Table name
     * @param columnFamilies HBase table column families
     * @throws IOException IOException
     */
    void createTable(String tableName, List<String> columnFamilies) throws IOException;

    /**
     * It deletes data
     * @param tableName Table name
     * @param rowKey Row key
     * @throws IOException IOException
     */
    void deleteData(String tableName, String rowKey) throws IOException;

    /**
     * It disables HBase table
     * @param tableName Table name
     * @throws IOException IOException
     */
    void disableTable(String tableName) throws IOException;

    /**
     * It drops HBase table
     * @param tableName Table name
     * @throws IOException IOException
     */
    void dropTable(String tableName) throws IOException;

    /**
     * It enables HBase table
     * @param tableName Table name
     * @throws IOException IOException
     */
    void enableTable(String tableName) throws IOException;

    /**
     * It inserts data
     * @param tableName Table name
     * @param rowKey Row key
     * @param columnFamily Column Family
     * @param column Column
     * @param cellValue Value to insert
     * @throws IOException IOException
     */
    void insertData(String tableName, String rowKey, String columnFamily, String column, String cellValue)
            throws IOException;

    /**
     * It scans and returns rows
     * @param tableName Table from which retrieve data
     * @param columnFamily Column family
     * @param column Column, i.e. HPacket ID
     * @param rowKeyLowerBound Row key lower bound
     * @param rowKeyUpperBound Rok key upper bound
     * @return HPacket list
     * @throws IOException IOException
     */
    List<HPacket> scanAvroHPackets(String tableName, String columnFamily, long column, long rowKeyLowerBound, long rowKeyUpperBound)
            throws IOException;

    /**
     * Given an HProject ID and a list of HPacket IDs inside it, this method scan Avro HPackets between a start time
     * and an end time, stored in an HBase table.
     * @param hProjectId HProject ID
     * @param hPacketIds HPacket list
     * @param rowKeyLowerBound Scanning start time (i.e. an HBase row key)
     * @param rowKeyUpperBound Scanning end time (i.e. an HBase row key)
     * @return HBaseConnectorHProjectScan list
     * @throws IOException IOException
     */
    List<HBaseConnectorHProjectScan> scanHProject(long hProjectId, List<String> hPacketIds, long rowKeyLowerBound, long rowKeyUpperBound)
            throws IOException;

    /**
     * Service counts HPacket event number between start time and end time,
     * dividing into slots (depending on HBase max scan page size)
     * @param tableName HBase table, related to a particular HProject
     * @param packetIds List of HPacket IDs, which count event numer for
     * @param startTime Scanning start time
     * @param endTime Scanning end time
     * @return A list of HBaseConnectorHPacketCount
     * @throws IOException IOException
     * @throws ParseException ParseException
     */
    List<HBaseConnectorHPacketCount> timelineEventCount(String tableName, List<String> packetIds,
            long startTime, long endTime) throws IOException, ParseException;

    /**
     * Service scans and returns data from timeline table
     * @param tableName Table name
     * @param packetIds Packet IDs
     * @param step Step
     * @param granularity Scanning granularity
     * @param startTime Timeline start time
     * @param endTime Timeline end time
     * @param timezone Timezone of client which has invoked the method, i.e. Europe/Rome
     * @return HBaseConnectorTimelineScan list
     * @throws IOException IOException
     * @throws ParseException ParseException
     */
    List<HBaseConnectorTimelineScan> timelineScan(String tableName, List<String> packetIds, HBaseTimelineColumnFamily step,
            HBaseTimelineColumnFamily granularity, long startTime, long endTime, String timezone)
            throws IOException, ParseException;

    /**
     * It checks if table exists
     * @param tableName Table name
     * @return True if it exists, false otherwise
     * @throws IOException IOException
     */
    @SuppressWarnings("unused")
    boolean tableExist(String tableName) throws IOException;

}