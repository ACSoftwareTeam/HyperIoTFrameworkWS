package it.acsoftware.hyperiot.stormmanager.model;

import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hproject.model.HProject;

import java.util.Collection;
import java.util.LinkedList;

public class TopologyConfigParts {
    public HProject project;
    public HDevice device;
    public Collection<HPacket> packets = new LinkedList<>();
    public StringBuilder packetConfig = new StringBuilder();
    public StringBuilder spoutConfig = new StringBuilder();
    public StringBuilder boltKafkaRealtimeConfig = new StringBuilder();
    public StringBuilder spoutDeclaration = new StringBuilder();
    public StringBuilder boltKafkaRealtimeDeclaration = new StringBuilder();
    public StringBuilder boltHBaseDeclaration = new StringBuilder();
    public StringBuilder boltHBaseKafkaErrorDeclaration= new StringBuilder();
    public StringBuilder spoutStreams = new StringBuilder();
    public StringBuilder boltKafkaRealtimeStreams = new StringBuilder();
    public StringBuilder boltHBaseStreams = new StringBuilder();
    public StringBuilder boltHBaseErrorStrems = new StringBuilder();
    public StringBuilder boltHBaseKafkaErrorStreams = new StringBuilder();
    public StringBuilder properties = new StringBuilder();
}
