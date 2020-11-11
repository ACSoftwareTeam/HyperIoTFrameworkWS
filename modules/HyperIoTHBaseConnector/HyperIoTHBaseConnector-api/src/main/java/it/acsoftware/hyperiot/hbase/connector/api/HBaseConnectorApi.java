package it.acsoftware.hyperiot.hbase.connector.api;

import com.google.protobuf.ServiceException;
import it.acsoftware.hyperiot.base.api.HyperIoTBaseApi;
import it.acsoftware.hyperiot.base.api.HyperIoTContext;
import it.acsoftware.hyperiot.base.exception.HyperIoTUnauthorizedException;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnectorHPacketCount;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnectorHProjectScan;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseConnectorTimelineScan;
import it.acsoftware.hyperiot.hbase.connector.model.HBaseTimelineColumnFamily;
import it.acsoftware.hyperiot.hpacket.model.HPacket;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * 
 * @author Aristide Cittadino Interface component for HBaseConnectorApi. This interface
 *         defines methods for additional operations.
 *
 */
public interface HBaseConnectorApi extends HyperIoTBaseApi {

    /**
     * It checks if there's an active connection
     * @param context HyperIoTContext
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     * @throws ServiceException ServiceException
     */
    void checkConnection(HyperIoTContext context) throws IOException, HyperIoTUnauthorizedException, ServiceException;

    /**
     * It creates HBase Table
     * @param context HyperIoTContext
     * @param tableName Table name
     * @param columnFamilies HBase table column families
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    void createTable(HyperIoTContext context, String tableName, List<String> columnFamilies) throws IOException, HyperIoTUnauthorizedException;

    /**
     * It deletes data
     * @param context HyperIoTContext
     * @param tableName Table name
     * @param rowKey Row key
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    void deleteData(HyperIoTContext context, String tableName, String rowKey)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * It disables HBase table
     * @param context HyperIoTContext
     * @param tableName Table name
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    void disableTable(HyperIoTContext context, String tableName) throws IOException, HyperIoTUnauthorizedException;

    /**
     * It drops HBase table
     * @param context HyperIoTContext
     * @param tableName Table name
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    void dropTable(HyperIoTContext context, String tableName) throws IOException, HyperIoTUnauthorizedException;

    /**
     * It enables HBase table
     * @param context HyperIoTContext
     * @param tableName Table name
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    void enableTable(HyperIoTContext context, String tableName) throws IOException, HyperIoTUnauthorizedException;

    /**
     * It inserts data
     * @param context HyperIoTContext
     * @param tableName Table name
     * @param rowKey Row key
     * @param columnFamily Column Family
     * @param column Column
     * @param cellValue Value to insert
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    void insertData(HyperIoTContext context, String tableName, String rowKey, String columnFamily, String column, String cellValue)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * It scans and returns rows
     * @param context HyperIoTContext
     * @param tableName Table from which retrieve data
     * @param columnFamily Column family
     * @param column Column, i.e. HPacket ID
     * @param rowKeyLowerBound Row key lower bound
     * @param rowKeyUpperBound Rok key upper bound
     * @return HPacket list
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException Unauthorized operation
     */
    List<HPacket> scanAvroHPackets(HyperIoTContext context, String tableName, String columnFamily, long column, long rowKeyLowerBound, long rowKeyUpperBound)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * Given an HProject ID and a list of HPacket IDs inside it, this method scan Avro HPackets between a start time
     * and an end time, stored in an HBase table.
     * @param context HyperIoTContext
     * @param hProjectId HProject ID
     * @param hPacketIds HPacket list
     * @param rowKeyLowerBound Scanning start time (i.e. an HBase row key)
     * @param rowKeyUpperBound Scanning end time (i.e. an HBase row key)
     * @return HBaseConnectorHProjectScan list
     * @throws IOException IOException
     * @throws HyperIoTUnauthorizedException HyperIoTUnauthorizedException
     */
    List<HBaseConnectorHProjectScan> scanHProject(HyperIoTContext context, long hProjectId, List<String> hPacketIds, long rowKeyLowerBound, long rowKeyUpperBound)
            throws IOException, HyperIoTUnauthorizedException;

    /**
     * It counts HPacket event number between start time and end time,
     * dividing into slots (depending on HBase max scan page size)
     * @param context HyperIoTContext
     * @param tableName HBase table, related to a particular HProject
     * @param packetIds List of HPacket IDs, which count event numer for
     * @param startTime Scanning start time
     * @param endTime Scanning end time
     * @return A list of HBaseConnectorHPacketCount
     * @throws IOException IOException
     * @throws ParseException ParseException
     * @throws HyperIoTUnauthorizedException HyperIoTUnauthorizedException
     */
    List<HBaseConnectorHPacketCount> timelineEventCount(HyperIoTContext context, String tableName, List<String> packetIds,
            long startTime, long endTime) throws IOException, ParseException, HyperIoTUnauthorizedException;

    /**
     * It scans HBase table for timeline queries and return hpacket event number, depending on step value
     * @param context HyperIoTContext
     * @param tableName HBase table name
     * @param packetIds HPacket ID list
     * @param step Scanning step
     * @param granularity Minimum scanning value from step, startTime and endTime
     * @param startTime Scanning start time
     * @param endTime Scanning end time
     * @param timezone Timezone of client which has invoked the method, i.e. Europe/Rome
     * @return HBaseConnectorTimelineScan list
     * @throws IOException IOException
     * @throws ParseException ParseException
     * @throws HyperIoTUnauthorizedException HyperIoTUnauthorizedException
     */
    List<HBaseConnectorTimelineScan> timelineScan(HyperIoTContext context, String tableName, List<String> packetIds,
                                                  HBaseTimelineColumnFamily step, HBaseTimelineColumnFamily granularity,
                                                  long startTime, long endTime, String timezone)
            throws IOException, ParseException, HyperIoTUnauthorizedException;

}