package it.acsoftware.hyperiot.hbase.connector.model;

import java.util.ArrayList;
import java.util.List;

/**
 * This class supports queries for timeline.
 * It contains information about an HPacket event number,
 * which was registered between a start time and an end time.
 * Counters are divided into slots, depending on HBase max scan page size.
 * A slot is an instance of HBaseConnectorHPacketCountSlot object
 */
@SuppressWarnings("unused")
public class HBaseConnectorHPacketCount {

    private long hPacketId;
    private long totalCount;
    private List<HBaseConnectorHPacketCountSlot> slots;

    public HBaseConnectorHPacketCount(long hPacketId) {
        this.hPacketId = hPacketId;
        totalCount = 0L;
        slots = new ArrayList<>();
    }

    public long getHPacketId() {
        return hPacketId;
    }

    public void setHPacketId(long hPacketId) {
        this.hPacketId = hPacketId;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public List<HBaseConnectorHPacketCountSlot> getSlots() {
        return slots;
    }

    public void setSlots(List<HBaseConnectorHPacketCountSlot> slots) {
        this.slots = slots;
    }

    public void addSlot(HBaseConnectorHPacketCountSlot slot) {
        slots.add(slot);
    }

}
