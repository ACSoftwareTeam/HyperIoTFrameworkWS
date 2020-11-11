package it.acsoftware.hyperiot.hbase.connector.model;

/**
 * This class contains information about an HPacket event number,
 * which was registered between a start time and an end time.
 */
@SuppressWarnings("unused")
public class HBaseConnectorHPacketCountSlot {

    private long start, end, count;

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public void addCount(long count) {
        this.count += count;
    }

}
