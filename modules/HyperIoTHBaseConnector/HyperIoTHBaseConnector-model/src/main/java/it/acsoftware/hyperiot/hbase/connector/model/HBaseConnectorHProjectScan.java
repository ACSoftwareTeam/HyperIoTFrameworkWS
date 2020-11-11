package it.acsoftware.hyperiot.hbase.connector.model;

import it.acsoftware.hyperiot.hpacket.model.HPacket;

import java.util.ArrayList;
import java.util.List;

/**
 * This class contains information about HPacket.
 * Property hPacketId is HPacket ID, while values contains HPacket instances.
 */
@SuppressWarnings("unused")
public class HBaseConnectorHProjectScan {

    private long hPacketId;
    private List<HPacket> values;

    public HBaseConnectorHProjectScan(long hPacketId) {
        this.hPacketId = hPacketId;
        values = new ArrayList<>();
    }

    public long gethPacketId() {
        return hPacketId;
    }

    public void sethPacketId(long hPacketId) {
        this.hPacketId = hPacketId;
    }

    public List<HPacket> getValues() {
        return values;
    }

    public void setValues(List<HPacket> values) {
        this.values = values;
    }

    public void addValue(HPacket hPacket) {
        values.add(hPacket);
    }

}
