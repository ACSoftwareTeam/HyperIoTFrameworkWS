package it.acsoftware.hyperiot.hbase.connector.model;

/**
 * This class supports timeline queries.
 * timestamp property contains a time value when there were HPacket events.
 * Event number is storing to count variable.
 */
@SuppressWarnings("unused")
public class HBaseConnectorTimelineScan {

    private long timestamp, count;

    public HBaseConnectorTimelineScan(long timestamp, long count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

}
