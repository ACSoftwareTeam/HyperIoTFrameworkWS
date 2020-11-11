package it.acsoftware.hyperiot.hbase.connector.service;

import com.google.protobuf.ServiceException;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hbase.connector.actions.HBaseConnectorAction;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorTimelineUtil;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorUtil;
import it.acsoftware.hyperiot.hbase.connector.model.*;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.JsonDecoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;


/**
 * 
 * @author Aristide Cittadino Implementation class of the HBaseConnectorSystemApi
 *         interface. This  class is used to implements all additional
 *         methods to interact with the persistence layer.
 */
@Component(service = HBaseConnectorSystemApi.class, immediate = true)
public final class HBaseConnectorSystemServiceImpl extends HyperIoTBaseSystemServiceImpl implements HBaseConnectorSystemApi {

    private Configuration configuration;
    private HBaseConnectorUtil hBaseConnectorUtil;
    private HBaseConnectorTimelineUtil hBaseConnectorTimelineUtil;
    private Admin admin;
    private Connection connection;
    private int maxScanPageSize;       // max number of retrieved rows from a single scan
    /**
     * This map will contain one date formatter for each timeline step (year, month, day, hour, minute, second, millisecond).
     * See HBase row keys for further information
     */
    private static Map<HBaseTimelineColumnFamily, SimpleDateFormat> formats;
    private final static String HBASE_AVRO_TABLE_PREFIX = "avro_hproject_";     // Prefix of HBase tables containing HPackets in Avro format
    private final static String HBASE_AVRO_TABLE_COLUMN_FAMILY = "hpacket";     // Column family of HBase tables containing HPackets in Avro format

    private PermissionSystemApi permissionSystemApi;

    @Activate
    public void activate() throws IOException {
        // set HBase configurations
        setHBaseConfiguration();
        // create a connection to the database
        connection = ConnectionFactory.createConnection(configuration);
        // get admin object, which manipulate database structure
        admin = connection.getAdmin();
        //set formats for timeline timestamp
        setFormats();
        checkRegisteredUserRoleExists();
    }

    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

    @Override
    public void checkConnection() throws IOException, ServiceException {
        HBaseAdmin.checkHBaseAvailable(configuration);
    }

    private void checkRegisteredUserRoleExists() {
        String resourceName = HBaseConnector.class.getName();
        List<HyperIoTAction> actions = new ArrayList<>();
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HBaseConnectorAction.CREATE_TABLE));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HBaseConnectorAction.DISABLE_TABLE));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HBaseConnectorAction.DROP_TABLE));
        actions.add(HyperIoTActionsUtil.getHyperIoTAction(resourceName, HBaseConnectorAction.READ_DATA));
        this.permissionSystemApi
                .checkOrCreateRoleWithPermissions(HyperIoTRoleConstants.ROLE_NAME_REGISTERED_USER, actions);
    }

    private void countHPacketEvents(Table table, List<HBaseConnectorHPacketCount> countList, String hPacketId,
            long startTime, long endTime) throws IOException, ParseException {
        // get HBase scanner and its iterator
        // NB: scanning step is always set to HBaseTimelineColumnFamily.SECOND when counting
        String startRowKeyPrefix = hBaseConnectorTimelineUtil.getRowKeyPrefix(hPacketId, HBaseTimelineColumnFamily.MILLISECOND, startTime); // first possible row key
        String endRowKeyPrefix = hBaseConnectorTimelineUtil.getRowKeyPrefix(hPacketId, HBaseTimelineColumnFamily.MILLISECOND, endTime);     // last possible row key
        ResultScanner scanner = getTimelineScanner(table, HBaseTimelineColumnFamily.MILLISECOND, startRowKeyPrefix, endRowKeyPrefix, true);
        Iterator<Result> iterator = scanner.iterator();
        // get lower bound from startTime
        Instant instant = Instant.ofEpochMilli(startTime);
        String startBoundColumn = hBaseConnectorTimelineUtil.getStringColumnBound(instant, HBaseTimelineColumnFamily.MILLISECOND);         // first possible column
        // Remember on the previous variable: if we receive a request with step second,
        // a permitted startBoundColumn value can be 15: it is the first second which we retrieve elements from

        // get upper bound from endTime
        instant = Instant.ofEpochMilli(endTime);
        String endBoundColumn = hBaseConnectorTimelineUtil.getStringColumnBound(instant, HBaseTimelineColumnFamily.MILLISECOND);           // last possible column
        // extract HPacket's rows saved on HBase
        countOnHBase(countList, Long.parseLong(hPacketId), iterator, startRowKeyPrefix, endRowKeyPrefix, startBoundColumn, endBoundColumn);
    }

    private Map<byte[], byte[]> getColumnMap(Result result, String startRowKeyPrefix,
                  String endRowKeyPrefix, HBaseTimelineColumnFamily granularity, String startBoundColumn, String endBoundColumn, Iterator<Result> iterator) {
        Map<byte[], byte[]> columnMap;
        if(Bytes.toString(result.getRow()).equals(startRowKeyPrefix)) {
            // Check on the first value: if its row key is equal to received start time
            // and some of its columns are before that, exclude them
            columnMap = startRowKeyPrefix.equals(endRowKeyPrefix) ?
                    result.getFamilyMap(Bytes.toBytes(granularity.getName())).subMap(startBoundColumn.getBytes(),
                            true, endBoundColumn.getBytes(), false) :
                    result.getFamilyMap(Bytes.toBytes(granularity.getName())).tailMap(startBoundColumn.getBytes());
        }
        else {
            // Check on the last value and received end time:
            //  - it is not the last value => get all columns;
            //  - it is the last value and its row key is not equal to end time => get all columns
            //  - it is the last value and its row key is equal to end time => some of is columns
            //    can be after end time, exclude them
            //  - it is not the last value and its row key is equal to end time => it can't be exist,
            //    because row key contains all value between start and end times (both of them included)
            columnMap = iterator.hasNext() || !Bytes.toString(result.getRow()).equals(endRowKeyPrefix)?
                    result.getFamilyMap(Bytes.toBytes(granularity.getName())) :
                    result.getFamilyMap(Bytes.toBytes(granularity.getName())).headMap(endBoundColumn.getBytes(), false);
        }
        return columnMap;
    }

    private void countOnHBase(List<HBaseConnectorHPacketCount> countList, long hPacketId, Iterator<Result> iterator,
            String startRowKeyPrefix, String endRowKeyPrefix, String startBoundColumn, String endBoundColumn) throws ParseException {
        HBaseConnectorHPacketCount hBaseConnectorHPacketCount = new HBaseConnectorHPacketCount(hPacketId);
        HBaseConnectorHPacketCountSlot slot= new HBaseConnectorHPacketCountSlot();
        long currentTimestamp;     // variable containing current timestamp
        long lastTimestamp = 0L;        // variable containing end time for last incomplete slot
        long totalCount = 0L;
        while(iterator.hasNext()) {
            Result result = iterator.next();
            Map<byte[], byte[]> columnMap = getColumnMap(result, startRowKeyPrefix, endRowKeyPrefix,
                    HBaseTimelineColumnFamily.MILLISECOND, startBoundColumn, endBoundColumn, iterator);
            for (byte[] column : columnMap.keySet()) {
                String stringTimestamp = Bytes.toString(result.getRow()) + "_" + Bytes.toString(column);
                currentTimestamp = hBaseConnectorTimelineUtil.getTimestamp(stringTimestamp, column,
                        HBaseTimelineColumnFamily.MILLISECOND, formats.get(HBaseTimelineColumnFamily.MILLISECOND));
                if(slot.getStart() == 0L) {
                    // create first slot
                    slot.setStart(currentTimestamp);
                }
                long currentCount = Bytes.toLong(columnMap.get(column));
                if (slot.getCount() + currentCount > maxScanPageSize ) {
                    // slot has reached its limit, create a new one
                    long remainingCount = currentCount - (maxScanPageSize - slot.getCount());
                    slot.addCount(maxScanPageSize - slot.getCount());
                    slot.setEnd(lastTimestamp);
                    hBaseConnectorHPacketCount.addSlot(slot);
                    totalCount += slot.getCount();
                    slot = new HBaseConnectorHPacketCountSlot();    // create next slot
                    slot.setStart(currentTimestamp);
                    slot.addCount(remainingCount);                  // add remaining count from previous slot
                }
                else {
                    slot.addCount(currentCount);
                    lastTimestamp = currentTimestamp;
                }
            }
        }
        slot.setEnd(lastTimestamp);         // slot has not reached its limit: set end time
        totalCount += slot.getCount();
        hBaseConnectorHPacketCount.addSlot(slot);
        hBaseConnectorHPacketCount.setTotalCount(totalCount);
        countList.add(hBaseConnectorHPacketCount);
    }

    @Override
    public void createTable(String tableName, List<String> columnFamilies) throws IOException {
        TableName table = TableName.valueOf(tableName);
        HTableDescriptor descriptor = new HTableDescriptor(table);
        // add column families to table descriptor
        columnFamilies.forEach((columnFamily) -> descriptor.addFamily(new HColumnDescriptor(columnFamily)));
        admin.createTable(descriptor);
    }

    private HPacket decodeAvroHPacket(String avroHPacket) throws IOException {
        HPacket hPacket = new HPacket();
        DatumReader<HPacket> reader = new SpecificDatumReader<>(hPacket.getSchema());
        JsonDecoder decoder = DecoderFactory.get().jsonDecoder(hPacket.getSchema(), avroHPacket);
        hPacket = reader.read(null, decoder);
        return hPacket;
    }

    @Override
    public void deleteData(String tableName, String rowKey)
            throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Delete delete = new Delete(Bytes.toBytes(rowKey));
        table.delete(delete);
        table.close();
    }

    @Override
    public void disableTable(String tableName) throws IOException {
        admin.disableTable(TableName.valueOf(tableName));
    }

    @Override
    public void dropTable(String tableName) throws IOException {
        admin.deleteTable(TableName.valueOf(tableName));
    }

    @Override
    public void enableTable(String tableName) throws IOException {
        admin.enableTable(TableName.valueOf(tableName));
    }

    @Override
    public void insertData(String tableName, String rowKey, String columnFamily, String column, String cellValue)
            throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Put put = new Put(rowKey.getBytes());
        put.addImmutable(columnFamily.getBytes(), column.getBytes(), cellValue.getBytes());
        table.put(put);
        table.close();
    }

    /**
     * It returns an HBase scanner, setting on it three filters:
     *  - row keys greater or equal than a lower bound
     *  - row keys less or equal than an upper bound
     *  - client max scanning if scanner scans HPacket whole records, otherwise on counting there is no limit
     * @param table HBase table which derive a scanner from
     * @param granularity  HBase table column family which extract columns from
     * @param lowerBound Row key lower bound
     * @param upperBound Row key upper bound
     * @param isCounting If service count or scan records
     * @return A ResultScanner on table
     * @throws IOException IOException
     */
    private ResultScanner getTimelineScanner(Table table, HBaseTimelineColumnFamily granularity, String lowerBound,
            String upperBound, boolean isCounting) throws IOException{
        Filter rowFilterLowerBound = new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(Bytes.toBytes(lowerBound)));
        Filter rowFilterUpperBound = new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL, new BinaryComparator(Bytes.toBytes(upperBound)));
        PageFilter pageFilter = new PageFilter(maxScanPageSize);
        List<Filter> rowFilterList = isCounting ?
                // Set limit on scanning only
                Arrays.asList(rowFilterLowerBound, rowFilterUpperBound) : Arrays.asList(rowFilterLowerBound, rowFilterUpperBound, pageFilter);
        Scan scan = new Scan();
        scan.addFamily(Bytes.toBytes(granularity.getName()));
        scan.setFilter(new FilterList(FilterList.Operator.MUST_PASS_ALL, rowFilterList));
        return table.getScanner(scan);
    }

    /**
     * Extract rows from HBase
     * @param events Output which will be returned to the caller
     * @param iterator Iterator on HBase table scanner
     * @param startRowKeyPrefix First possible row key
     * @param endRowKeyPrefix Last possible row key
     * @param step Timeline step
     * @param granularity Search granularity
     * @param startBoundColumn First possible column
     * @param endBoundColumn Last possible column
     * @throws ParseException ParseException
     */
    private void extractRowsFromHBase(TreeMap<Long, Long> events, Iterator<Result> iterator, String startRowKeyPrefix, String endRowKeyPrefix,
          HBaseTimelineColumnFamily step, HBaseTimelineColumnFamily granularity, String startBoundColumn, String endBoundColumn) throws ParseException {
        while(iterator.hasNext()) {
            Result result = iterator.next();
            Map<byte[], byte[]> columnMap = getColumnMap(result, startRowKeyPrefix, endRowKeyPrefix, granularity, startBoundColumn, endBoundColumn, iterator);
            for (byte[] column : columnMap.keySet())
                putInsideMap(events, result, column, columnMap, step);
        }
    }

    private void putInsideMap(TreeMap<Long, Long> events, Result result, byte[] column, Map<byte[], byte[]> columnMap,
            HBaseTimelineColumnFamily step) throws ParseException{
        String stringTimestamp = Bytes.toString(result.getRow()) + "_" + Bytes.toString(column);
        long key = hBaseConnectorTimelineUtil.getTimestamp(stringTimestamp, column, step, formats.get(step));   // millis in UnixEpochTime
        key = events.floorEntry(key).getKey();           // bind to entry of event map
        events.put(key, events.get(key) + Bytes.toLong(columnMap.get(column)));
    }

    @Override
    public List<HPacket> scanAvroHPackets(String tableName, String columnFamily, long column, long rowKeyLowerBound, long rowKeyUpperBound)
            throws IOException {
        List<HPacket> hPacketList = new ArrayList<>();
        Table table = connection.getTable(TableName.valueOf(tableName));
        Filter rowFilterLowerBound = new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL,  new BinaryComparator(Bytes.toBytes(rowKeyLowerBound)));
        Filter rowFilterUpperBound = new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,  new BinaryComparator(Bytes.toBytes(rowKeyUpperBound)));
        PageFilter pageFilter = new PageFilter(maxScanPageSize);
        List<Filter> rowFilterList = Arrays.asList(rowFilterLowerBound, rowFilterUpperBound, pageFilter);
        Scan scan = new Scan();
        scan.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(column));
        scan.setFilter(new FilterList(FilterList.Operator.MUST_PASS_ALL, rowFilterList));
        ResultScanner scanner = table.getScanner(scan);
        for (Result result : scanner) {
            String avroHPacket = Bytes.toString(result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(column)));
            hPacketList.add(decodeAvroHPacket(avroHPacket));
        }
        return hPacketList;
    }

    @Override
    public List<HBaseConnectorHProjectScan> scanHProject(long hProjectId, List<String> hPacketIds, long rowKeyLowerBound, long rowKeyUpperBound)
            throws IOException {
        String tableName = HBASE_AVRO_TABLE_PREFIX + hProjectId;
        List<HBaseConnectorHProjectScan> hBaseConnectorHProjectScanList = new ArrayList<>();
        Table table = connection.getTable(TableName.valueOf(tableName));
        Filter rowFilterLowerBound = new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL,  new BinaryComparator(Bytes.toBytes(rowKeyLowerBound)));
        Filter rowFilterUpperBound = new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL,  new BinaryComparator(Bytes.toBytes(rowKeyUpperBound)));
        PageFilter pageFilter = new PageFilter(maxScanPageSize);
        List<Filter> rowFilterList = Arrays.asList(rowFilterLowerBound, rowFilterUpperBound, pageFilter);
        for (String packetId : hPacketIds) {
            long hPacketId = Long.parseLong(packetId);
            HBaseConnectorHProjectScan hBaseConnectorHProjectScan = new HBaseConnectorHProjectScan(hPacketId);
            Scan scan = new Scan();
            scan.addColumn(Bytes.toBytes(HBASE_AVRO_TABLE_COLUMN_FAMILY), Bytes.toBytes(hPacketId));
            scan.setFilter(new FilterList(FilterList.Operator.MUST_PASS_ALL, rowFilterList));
            ResultScanner scanner = table.getScanner(scan);
            for (Result result : scanner) {
                String avroHPacket = Bytes.toString(result.getValue(Bytes.toBytes(HBASE_AVRO_TABLE_COLUMN_FAMILY),
                        Bytes.toBytes(hPacketId)));
                hBaseConnectorHProjectScan.addValue(decodeAvroHPacket(avroHPacket));
            }
            hBaseConnectorHProjectScanList.add(hBaseConnectorHProjectScan);
        }
        return hBaseConnectorHProjectScanList;
    }

    /**
     * Given an HPacket ID, this method scan its event number on HBase table
     * @param table HProject's HBase table
     * @param events Map containing caller output
     * @param hPacketId HPacket ID
     * @param step Timeline step
     * @param startTime Timeline start time
     * @param endTime Timeline end time
     * @throws IOException IOException
     * @throws ParseException ParseException
     */
    private void scanHPacketEvents(Table table, TreeMap<Long, Long> events, String hPacketId, HBaseTimelineColumnFamily step,
            HBaseTimelineColumnFamily granularity, long startTime, long endTime) throws IOException, ParseException {
        // get HBase scanner and its iterator
        String startRowKeyPrefix = hBaseConnectorTimelineUtil.getRowKeyPrefix(hPacketId, granularity, startTime); // first possible row key
        String endRowKeyPrefix = hBaseConnectorTimelineUtil.getRowKeyPrefix(hPacketId, granularity, endTime);     // last possible row key
        ResultScanner scanner = getTimelineScanner(table, granularity, startRowKeyPrefix, endRowKeyPrefix, false);
        Iterator<Result> iterator = scanner.iterator();
        // get lower bound from startTime
        Instant instant = Instant.ofEpochMilli(startTime);
        String startBoundColumn = hBaseConnectorTimelineUtil.getStringColumnBound(instant, granularity);         // first possible column
        // Remember on the previous variable: if we receive a request with step second,
        // a permitted startBoundColumn value can be 15: it is the first second which we retrieve elements from

        // get upper bound from endTime
        instant = Instant.ofEpochMilli(endTime);
        String endBoundColumn = hBaseConnectorTimelineUtil.getStringColumnBound(instant, granularity);           // last possible column
        // extract HPacket's rows saved on HBase
        extractRowsFromHBase(events, iterator, startRowKeyPrefix, endRowKeyPrefix, step, granularity, startBoundColumn, endBoundColumn);
    }

    /**
     * Initialization method: for each step, set one date format.
     * As a matter of fact, caller will receive timestamp in millis unix epoch time,
     * so we need conversion between HBase row keys and millis
     */
    private void setFormats() {
        formats = new HashMap<>();
        formats.put(HBaseTimelineColumnFamily.YEAR, new SimpleDateFormat("yyyy"));
        formats.put(HBaseTimelineColumnFamily.MONTH, new SimpleDateFormat("yyyy_MM"));
        formats.put(HBaseTimelineColumnFamily.DAY, new SimpleDateFormat("yyyy_MM_dd"));
        formats.put(HBaseTimelineColumnFamily.HOUR, new SimpleDateFormat("yyyy_MM_dd_HH"));
        formats.put(HBaseTimelineColumnFamily.MINUTE, new SimpleDateFormat("yyyy_MM_dd_HH_mm"));
        formats.put(HBaseTimelineColumnFamily.SECOND, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss"));
        formats.put(HBaseTimelineColumnFamily.MILLISECOND, new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS"));
        // set UTC TimeZone
        for(HBaseTimelineColumnFamily key : formats.keySet())
            formats.get(key).setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Initialization method: HBase client configuration
     */
    private void setHBaseConfiguration() {
        configuration = HBaseConfiguration.create();
        configuration.setBoolean("hbase.cluster.distributed", hBaseConnectorUtil.getClusterDistributed());
        configuration.set("hbase.master", hBaseConnectorUtil.getMaster());
        configuration.set("hbase.master.hostname", hBaseConnectorUtil.getMasterHostname());
        configuration.setInt("hbase.master.info.port", hBaseConnectorUtil.getMasterInfoPort());
        configuration.setInt("hbase.master.port", hBaseConnectorUtil.getMasterPort());
        configuration.setInt("hbase.regionserver.info.port", hBaseConnectorUtil.getRegionserverInfoPort());
        configuration.setInt("hbase.regionserver.port", hBaseConnectorUtil.getRegionserverPort());
        configuration.set("hbase.rootdir", hBaseConnectorUtil.getRootdir());
        configuration.set("hbase.zookeeper.quorum", hBaseConnectorUtil.getZookeeperQuorum());
        maxScanPageSize = hBaseConnectorUtil.getMaxScanPageSize();
    }

    @Reference
    public void setHBaseConnectorUtil(HBaseConnectorUtil hBaseConnectorUtil) {
        this.hBaseConnectorUtil = hBaseConnectorUtil;
    }

    @Reference
    public void setHBaseConnectorTimelineUtil(HBaseConnectorTimelineUtil hBaseConnectorTimelineUtil) {
        this.hBaseConnectorTimelineUtil = hBaseConnectorTimelineUtil;
    }

    @Override
    public List<HBaseConnectorHPacketCount> timelineEventCount(String tableName, List<String> packetIds, long startTime, long endTime) throws IOException, ParseException {
        List<HBaseConnectorHPacketCount> countList= new ArrayList<>();
        Table table = connection.getTable(TableName.valueOf(tableName));
        for(String hPacketId : packetIds)
            countHPacketEvents(table, countList, hPacketId, startTime, endTime);
        return countList;
    }

    @Override
    public List<HBaseConnectorTimelineScan> timelineScan(String tableName, List<String> packetIds, HBaseTimelineColumnFamily step,
            HBaseTimelineColumnFamily granularity, long startTime, long endTime, String timezone) throws IOException, ParseException {
        TreeMap<Long, Long> events = new TreeMap<>();
        hBaseConnectorTimelineUtil.initializeEventMap(events, step, startTime, endTime, timezone);
        List<HBaseConnectorTimelineScan> hBaseConnectorTimelineScanList = new ArrayList<>();
        Table table = connection.getTable(TableName.valueOf(tableName));
        for(String hPacketId : packetIds)
            scanHPacketEvents(table, events, hPacketId, step, granularity, startTime, endTime);
        for(Long timestamp : events.keySet())
            hBaseConnectorTimelineScanList.add(new HBaseConnectorTimelineScan(timestamp, events.get(timestamp)));
        return hBaseConnectorTimelineScanList;
    }

    @Override
    public boolean tableExist(String tableName) throws IOException {
        return admin.tableExists(TableName.valueOf(tableName));
    }

}
