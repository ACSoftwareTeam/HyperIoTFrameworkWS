package it.acsoftware.hyperiot.hbase.connector.api;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.TreeMap;

import it.acsoftware.hyperiot.hbase.connector.model.HBaseTimelineColumnFamily;

public interface HBaseConnectorTimelineUtil extends Serializable {

    /**
     * Build JSON output for timeline
     * @param events Map containing timestamp and event number
     * @return JSON like this: [{"timestamp": 1579705026419, "value": 8}, ...]
     */
    String buildJsonOutput(TreeMap<Long, Long> events);

    /**
     * Get column value inside Day column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getDayColumnFamily(Instant instant);

    /**
     * Get column value inside hour column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getHourColumnFamily(Instant instant);

    /**
     * Retrieve step from HBase row key.
     * Step is value which will be inserted in output, while a retrieved HBase row key can contain
     * one or more granularity deeper levels.
     * @param key HBase row key
     * @param step Step value
     * @return HBase row key first part, depending on step
     * @throws ParseException ParseException
     */
    String getKeyFromStep(String key, HBaseTimelineColumnFamily step) throws ParseException;

    /**
     * Get column value inside millisecond column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getMillisecondColumnFamily(Instant instant);

    /**
     * Get column value inside minute column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getMinuteColumnFamily(Instant instant);

    /**
     * Get column value inside month column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getMonthColumnFamily(Instant instant);

    /**
     * This method build a String, which is a HBase row key prefix
     * @param hPacketId HPacket id which retrieves event number for
     * @param step Timeline step
     * @param timestamp Timestamp which retrieves event number from
     * @return HBase row key prefix
     * @throws IOException IOException
     */
    String getRowKeyPrefix(String hPacketId, HBaseTimelineColumnFamily step, long timestamp) throws IOException;

    /**
     * Get column value inside second column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getSecondColumnFamily(Instant instant);

    /**
     * Return column bound in String format
     * @param instant Date representing bound
     * @param step Step
     * @return String representing column bound, depending on step value
     * @throws IOException IOException
     */
    String getStringColumnBound(Instant instant, HBaseTimelineColumnFamily step) throws IOException;

    /**
     * TODO
     * @param stringTimestamp
     * @param column
     * @param step
     * @return
     * @throws ParseException
     */
    long getTimestamp(String stringTimestamp, byte[] column, HBaseTimelineColumnFamily step, SimpleDateFormat format) throws ParseException ;

    /**
     * Get column value inside year column family
     * @param instant Date which derive column from
     * @return A String representing HBase column value
     */
    String getYearColumnFamily(Instant instant);

    /**
     * It initializes output of timeline queries
     * @param events Output, containing key-value pairs: key are timestamps, values are HPacket events
     * @param step Scanning step
     * @param startTime Scanning start time
     * @param endTime Scanning end time
     * @param timezone Timezone of client which has invoked the method, i.e. Europe/Rome
     * @throws IOException IOException
     */
    void initializeEventMap(TreeMap<Long, Long> events, HBaseTimelineColumnFamily step, long startTime, long endTime,
                            String timezone) throws IOException;

}
