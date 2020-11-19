package it.acsoftware.hyperiot.hbase.connector.api;

import com.google.protobuf.ServiceException;
import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;
import org.apache.hadoop.hbase.client.ResultScanner;

import java.io.IOException;
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
     * It returns an HBase scanner, setting on it three filters:
     *  - row keys greater or equal than a lower bound
     *  - row keys less or equal than an upper bound
     *  - client max scanning
     * @param tableName HBase table name which derive a scanner from
     * @param columnFamily  HBase table column family which extract columns from
     * @param column  HBase table column which extract cells from
     * @param rowKeyLowerBound Row key lower bound
     * @param rowKeyUpperBound Row key upper bound
     * @param limitScan boolean value, which indicates if scan is limited or not
     * @return A ResultScanner on table
     * @throws IOException IOException
     */
    ResultScanner getScanner(String tableName, byte[] columnFamily, byte[] column,
                             byte[] rowKeyLowerBound, byte[] rowKeyUpperBound, boolean limitScan)
            throws IOException;

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
     * It scans HBase table
     * @param tableName table name
     * @param columnFamily column family
     * @param column column
     * @param rowKeyLowerBound rowKeyLowerBound
     * @param rowKeyUpperBound rowKeyUpperBound
     * @return List<byte[]>
     * @throws IOException IOException
     */
    @SuppressWarnings("unused")
    List<byte[]> scan(String tableName, byte[] columnFamily, byte[] column,
                      byte[] rowKeyLowerBound, byte[] rowKeyUpperBound) throws IOException;

    /**
     * It checks if table exists
     * @param tableName Table name
     * @return True if it exists, false otherwise
     * @throws IOException IOException
     */
    @SuppressWarnings("unused")
    boolean tableExists(String tableName) throws IOException;

}