package it.acsoftware.hyperiot.hbase.connector.service;

import com.google.protobuf.ServiceException;
import it.acsoftware.hyperiot.base.action.util.HyperIoTActionsUtil;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.hbase.connector.actions.HBaseConnectorAction;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorSystemApi;
import it.acsoftware.hyperiot.hbase.connector.api.HBaseConnectorUtil;
import it.acsoftware.hyperiot.hbase.connector.model.*;
import it.acsoftware.hyperiot.permission.api.PermissionSystemApi;
import it.acsoftware.hyperiot.role.util.HyperIoTRoleConstants;
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
    private Admin admin;
    private Connection connection;
    private int maxScanPageSize;       // max number of retrieved rows from a single scan
    private PermissionSystemApi permissionSystemApi;

    @Activate
    public void activate() throws IOException {
        // set HBase configurations
        setHBaseConfiguration();
        // create a connection to the database
        connection = ConnectionFactory.createConnection(configuration);
        // get admin object, which manipulate database structure
        admin = connection.getAdmin();
        checkRegisteredUserRoleExists();
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

    @Override
    public void createTable(String tableName, List<String> columnFamilies) throws IOException {
        TableName table = TableName.valueOf(tableName);
        HTableDescriptor descriptor = new HTableDescriptor(table);
        // add column families to table descriptor
        columnFamilies.forEach((columnFamily) -> descriptor.addFamily(new HColumnDescriptor(columnFamily)));
        admin.createTable(descriptor);
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
    public Configuration getConfiguration() {
        return configuration;
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

    @Override
    public ResultScanner getScanner(String tableName, byte[] columnFamily, byte[] column,
                                    byte[] rowKeyLowerBound, byte[] rowKeyUpperBound, boolean limitScan)
            throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Filter rowFilterLowerBound =
                new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(rowKeyLowerBound));
        Filter rowFilterUpperBound =
                new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL, new BinaryComparator(rowKeyUpperBound));
        PageFilter pageFilter = new PageFilter(maxScanPageSize);
        List<Filter> rowFilterList = limitScan ?
                Arrays.asList(rowFilterLowerBound, rowFilterUpperBound, pageFilter) :
                Arrays.asList(rowFilterLowerBound, rowFilterUpperBound) ;
        Scan scan = new Scan();
        if (column == null)
            scan.addFamily(columnFamily);
        else
            scan.addColumn(columnFamily, column);
        scan.setFilter(new FilterList(FilterList.Operator.MUST_PASS_ALL, rowFilterList));
        return table.getScanner(scan);
    }

    // TODO use this method instead of the above one, because it is more generic
    // however, the previous method supports scan without limit, which timeline needs to count HPacket event
    private ResultScanner getScanner(String tableName, Map<byte[], List<byte[]>> columns,
                                    byte[] rowKeyLowerBound, byte[] rowKeyUpperBound, int limit)
            throws IOException {
        Table table = connection.getTable(TableName.valueOf(tableName));
        Filter rowFilterLowerBound =
                new RowFilter(CompareFilter.CompareOp.GREATER_OR_EQUAL, new BinaryComparator(rowKeyLowerBound));
        Filter rowFilterUpperBound =
                new RowFilter(CompareFilter.CompareOp.LESS_OR_EQUAL, new BinaryComparator(rowKeyUpperBound));
        // if limit is not equal to 0 and not greater than maxScanPageSize, set it
        PageFilter pageFilter = new PageFilter(limit != 0 && limit <= maxScanPageSize ? limit : maxScanPageSize);
        List<Filter> rowFilterList = Arrays.asList(rowFilterLowerBound, rowFilterUpperBound, pageFilter);
        Scan scan = new Scan();
        for (byte[] columnFamily : columns.keySet()) {
            for (byte[] column : columns.get(columnFamily))
                scan.addColumn(columnFamily, column);
        }
        scan.setFilter(new FilterList(FilterList.Operator.MUST_PASS_ALL, rowFilterList));
        return table.getScanner(scan);
    }

    @Override
    public List<byte[]> scan(String tableName, byte[] columnFamily, byte[] column,
                             byte[] rowKeyLowerBound, byte[] rowKeyUpperBound)
            throws IOException {
        ResultScanner scanner =
                getScanner(tableName, columnFamily, column, rowKeyLowerBound, rowKeyUpperBound, true);
        List<byte[]> cells = new ArrayList<>();
        for (Result result : scanner)
            cells.add(result.getValue(columnFamily, column));
        return cells;
    }

    @Override
    public Map<byte[], Map<byte[], Map<byte[], byte[]>>> scan(String tableName, Map<byte[], List<byte[]>> columns, byte[] rowKeyLowerBound,
                       byte[] rowKeyUpperBound, int limit) throws IOException {
        Map<byte[], Map<byte[], Map<byte[], byte[]>>> output = new HashMap<>();
        ResultScanner scanner = getScanner(tableName, columns, rowKeyLowerBound, rowKeyUpperBound, limit);
        for (Result result : scanner) {
            output.put(result.getRow(), new HashMap<>());
            for (byte[] columnFamily : columns.keySet()) {
                output.get(result.getRow()).put(columnFamily, new HashMap<>());
                for (byte[] column : columns.get(columnFamily)) {
                    byte[] value = result.getValue(columnFamily, column);
                    if (value != null)
                        output.get(result.getRow()).get(columnFamily).put(column, result.getValue(columnFamily, column));
                }
            }
        }
        return output;
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

    @Override
    public boolean tableExists(String tableName) throws IOException {
        return admin.tableExists(TableName.valueOf(tableName));
    }

    @Reference
    public void setHBaseConnectorUtil(HBaseConnectorUtil hBaseConnectorUtil) {
        this.hBaseConnectorUtil = hBaseConnectorUtil;
    }

    @Reference
    public void setPermissionSystemApi(PermissionSystemApi permissionSystemApi) {
        this.permissionSystemApi = permissionSystemApi;
    }

}
