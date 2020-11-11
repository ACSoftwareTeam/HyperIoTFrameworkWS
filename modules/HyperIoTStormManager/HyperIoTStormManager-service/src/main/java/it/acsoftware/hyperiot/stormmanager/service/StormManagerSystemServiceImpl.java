/*
 * Copyright (C) AC Software, S.r.l. - All Rights Reserved
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * Written by Gene <generoso.martello@acsoftware.it>, March 2019
 *
 */
package it.acsoftware.hyperiot.stormmanager.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.service.HyperIoTBaseSystemServiceImpl;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.hdevice.api.HDeviceSystemApi;
import it.acsoftware.hyperiot.hdevice.model.HDevice;
import it.acsoftware.hyperiot.hpacket.api.HPacketSystemApi;
import it.acsoftware.hyperiot.hpacket.model.HPacket;
import it.acsoftware.hyperiot.hpacket.model.HPacketFieldType;
import it.acsoftware.hyperiot.hproject.model.HProject;
import it.acsoftware.hyperiot.rule.api.RuleEngineSystemApi;
import it.acsoftware.hyperiot.rule.model.RuleType;
import it.acsoftware.hyperiot.stormmanager.api.StormClient;
import it.acsoftware.hyperiot.stormmanager.api.StormManagerSystemApi;
import it.acsoftware.hyperiot.stormmanager.model.StormManager;
import it.acsoftware.hyperiot.stormmanager.model.TopologyConfig;
import it.acsoftware.hyperiot.stormmanager.model.TopologyConfigParts;
import it.acsoftware.hyperiot.stormmanager.model.TopologyInfo;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.generated.TopologySummary;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author Aristide Cittadino Implementation class of the StormManagerSystemApi
 * interface. This class is used to implements all additional methods to
 * interact with the persistence layer.
 * @author Gene (generoso.martello@acsoftware.it)
 * @version 2019-03-19 Initial release
 */
@Component(service = StormManagerSystemApi.class, immediate = true)
public final class StormManagerSystemServiceImpl extends HyperIoTBaseSystemServiceImpl
        implements StormManagerSystemApi {

    //private static String topologyServiceBaseUrl = "http://localhost:9001/topology/";

    /**
     * Injecting the HDeviceSystemApi
     */
    private HDeviceSystemApi deviceSystemService;

    /**
     * Injecting HPacketSystemApi
     */
    private HPacketSystemApi hPacketSystemApi;

    /**
     * Injecting the RuleEngineSystemApi
     */
    private RuleEngineSystemApi ruleEngineSystemApi;

    /**
     * Injecting Storm Client
     */
    private StormClient stormClient;

    /**
     * @param deviceSystemService Injecting via OSGi DS current HDeviceSystemService
     */
    @Reference(service = HDeviceSystemApi.class)
    protected void setDeviceSystemService(HDeviceSystemApi deviceSystemService) {
        log.log(Level.FINE, "invoking setDeviceSystemService, setting: {}", deviceSystemService);
        this.deviceSystemService = deviceSystemService;
    }

    /**
     * @param ruleEngineSystemApi Injecting via OSGi DS current
     *                            RuleEngineSystemService
     */
    @Reference(service = RuleEngineSystemApi.class)
    protected void setRuleEngineSystemService(RuleEngineSystemApi ruleEngineSystemApi) {
        log.log(Level.FINE, "invoking setDeviceSystemService, setting: {}", ruleEngineSystemApi);
        this.ruleEngineSystemApi = ruleEngineSystemApi;
    }

    /**
     * @param hPacketSystemApi
     */
    @Reference
    public void sethPacketSystemApi(HPacketSystemApi hPacketSystemApi) {
        this.hPacketSystemApi = hPacketSystemApi;
    }

    /**
     * @param stormClient
     */
    @Reference
    public void setStormClient(StormClient stormClient) {
        this.stormClient = stormClient;
    }

    @Override
    public String getTopologyName(long projectId) {
        return "topology-" + projectId;
    }


    public String getTopologyName(HDevice device) {
        return "topology-" + device.getProject().getId() + "-" + device.getId();
    }

    public boolean mustResubmitTopology(long projectId, int currentTopologyConfigHashcode) throws IOException {
        TopologyConfig config = this.getTopologyConfig(projectId);
        if (config.hashCode() == currentTopologyConfigHashcode) {
            return false;
        }
        return true;
    }

    public TopologyConfig getTopologyConfig(long projectId) throws IOException {
        TopologyConfig topologyConfig = new TopologyConfig();
        topologyConfig.name = getTopologyName(projectId);
        TopologyConfigParts topologyConfigParts = new TopologyConfigParts();
        Collection<HDevice> devices = deviceSystemService.getProjectDevicesList(projectId);
        devices.forEach((d) -> {
            try {
                TopologyConfigParts deviceTopologyParts = getDeviceTopologyParts(d.getId());
                topologyConfigParts.packetConfig.append(deviceTopologyParts.packetConfig);
                topologyConfigParts.properties.append(deviceTopologyParts.properties);
                topologyConfigParts.spoutConfig.append(deviceTopologyParts.spoutConfig);
                topologyConfigParts.boltKafkaRealtimeConfig.append(deviceTopologyParts.boltKafkaRealtimeConfig);
                topologyConfigParts.boltKafkaRealtimeDeclaration.append(deviceTopologyParts.boltKafkaRealtimeDeclaration);
                topologyConfigParts.spoutStreams.append(deviceTopologyParts.spoutStreams);
                topologyConfigParts.boltKafkaRealtimeStreams.append(deviceTopologyParts.boltKafkaRealtimeStreams);
                topologyConfigParts.spoutDeclaration.append(deviceTopologyParts.spoutDeclaration);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        // read templates
        String spoutConfigTemplate = readBundleResource("spout-config.yaml");
        String boltRealtimeConfigTemplate = readBundleResource("bolt-kafka-realtime-config.yaml");
        String spoutDeclarationTemplate = readBundleResource("spout-declaration.yaml");
        String boltRealtimeDeclarationTemplate = readBundleResource("bolt-kafka-realtime-declaration.yaml");
        String boltHBaseDeclarationTemplate = readBundleResource("bolt-hbase-declaration.yaml");
        String spoutStreamsTemplate = readBundleResource("spout-streams.yaml");
        String boltRealtimeStreamsTemplate = readBundleResource("bolt-kafka-realtime-streams.yaml");
        String boltHBaseErrorStreamsTemplate = readBundleResource("bolt-hbase-error-streams.yaml");
        String boltHBaseStreamsTemplate = readBundleResource("bolt-hbase-streams.yaml");
        String boltHBaseKafkaErrorDeclarationTemplate = readBundleResource("bolt-hbase-kafka-error-declaration.yaml");
        String boltHBaseKafkaErrorStreamsTemplate = readBundleResource("bolt-hbase-kafka-error-streams.yaml");
        // generate project spouts/bolts/streams
        topologyConfigParts.spoutConfig.append(spoutConfigTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.spoutDeclaration.append(
                spoutDeclarationTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.spoutStreams
                .append(spoutStreamsTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        // generate topology parts for this project
        topologyConfigParts.boltKafkaRealtimeConfig.append(
                boltRealtimeConfigTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltKafkaRealtimeDeclaration.append(
                boltRealtimeDeclarationTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltHBaseDeclaration.append(
                boltHBaseDeclarationTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltKafkaRealtimeStreams.append(
                boltRealtimeStreamsTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltHBaseErrorStrems.append(
                boltHBaseErrorStreamsTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltHBaseStreams.append(
                boltHBaseStreamsTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltHBaseKafkaErrorDeclaration.append(
                boltHBaseKafkaErrorDeclarationTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        topologyConfigParts.boltHBaseKafkaErrorStreams.append(
                boltHBaseKafkaErrorStreamsTemplate.replaceAll("%projectId%", String.valueOf(projectId)));
        // generate topology.properties
        String topologyProperties = readBundleResource("topology.properties");
        topologyProperties = topologyProperties.replace("%packets%", topologyConfigParts.properties);
        topologyProperties = topologyProperties.replace("%drools-enrichment%",
                getDroolsCode(projectId, RuleType.ENRICHMENT));
        topologyProperties = topologyProperties.replace("%drools-event%",
                getDroolsCode(projectId, RuleType.EVENT));
        topologyProperties = topologyProperties.replace("%kafka-bootstrap-servers%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.kafka.bootstrap.servers"));
        topologyProperties = topologyProperties.replace("%hdfs-namenode-hosts%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hdfs.namenode.hosts"));
        topologyProperties = topologyProperties.replace("%hfds-write-dir%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hdfs.write.dir"));
        topologyProperties = topologyProperties.replace("%hbase-root-dir%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hbase.root.dir"));
        topologyProperties = topologyProperties.replace("%zookeeper-hosts%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hbase.zookeeper.quorum"));
        topologyProperties = topologyProperties.replace("%zookeeper-client-port%", (String) HyperIoTUtil.getHyperIoTProperty("it.acsoftware.hyperiot.stormmanager.topology.hbase.zookeeper.client.port"));

        // generate topology.yaml
        String topologyYaml = readBundleResource("topology.yaml");
        topologyYaml = topologyYaml.replace("%topology-name%", topologyConfig.name);
        topologyYaml = topologyYaml.replace(
                "%hproject-id%", "\"" + projectId + "\""); // DeserializationBolt and SelectionBolt needs projectId to send to HBase tables
        topologyYaml = topologyYaml.replace("%packets-config%", topologyConfigParts.packetConfig);
        topologyYaml = topologyYaml.replace("%bolt-kafka-realtime-config%", topologyConfigParts.boltKafkaRealtimeConfig);
        topologyYaml = topologyYaml.replace("%spouts-config%", topologyConfigParts.spoutConfig);
        topologyYaml = topologyYaml.replace("%spouts-declaration%", topologyConfigParts.spoutDeclaration);
        topologyYaml = topologyYaml.replace("%bolt-kafka-realtime-declaration%", topologyConfigParts.boltKafkaRealtimeDeclaration);
        topologyYaml = topologyYaml.replace("%bolt-hbase-declaration%", topologyConfigParts.boltHBaseDeclaration);
        topologyYaml = topologyYaml.replace("%bolt-hbase-kafka-error-declaration%", topologyConfigParts.boltHBaseKafkaErrorDeclaration);
        topologyYaml = topologyYaml.replace("%spouts-streams%", topologyConfigParts.spoutStreams);
        topologyYaml = topologyYaml.replace("%bolt-hbase-error-streams%", topologyConfigParts.boltHBaseErrorStrems);
        topologyYaml = topologyYaml.replace("%bolt-kafka-realtime-streams%", topologyConfigParts.boltKafkaRealtimeStreams);
        topologyYaml = topologyYaml.replace("%bolt-hbase-streams%", topologyConfigParts.boltHBaseStreams);
        topologyYaml = topologyYaml.replace("%bolt-hbase-kafka-error-streams%", topologyConfigParts.boltHBaseKafkaErrorStreams);
        // add generated configs for current device
        topologyConfig.properties = topologyProperties;
        topologyConfig.yaml = topologyYaml;
        return topologyConfig;
    }

    @Override
    public void submitProjectTopology(long projectId)
            throws IOException {
        TopologyConfig topologyConfig = this.getTopologyConfig(projectId);
        log.log(Level.FINE, "Submitting topology with props: \n {0} and yaml: {1}", new Object[]{topologyConfig.properties, topologyConfig.yaml});
        topologyServiceSubmit(topologyConfig);
    }

    @Override
    public String getTopologyList() throws IOException {
        try {
            return this.stormClient.getTopologyList().toString();
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public TopologyInfo getTopologyStatus(long projectId)
            throws IOException {
        String topologyName = this.getTopologyName(projectId);
        TopologyInfo info = new TopologyInfo();
        try {
            TopologySummary topologySummary = this.stormClient.getTopologyList().stream().filter(topology -> topology.get_name().equalsIgnoreCase(topologyName)).findFirst().get();
            if (topologyName != null) {
                String topologyId = topologySummary.get_id();
                info.setStatus(topologySummary.get_status());
                info.setUptimeSecs(topologySummary.get_uptime_secs());
                info.setMustResubmit(this.mustResubmitTopology(projectId, this.stormClient.getTopologyConfigHashCode(topologySummary)));
                StormTopology topology = this.stormClient.getTopology(topologyId);
                if (topology != null) {
                    info.setSpoutsCount(topology.get_spouts().size());
                    info.setBoltsCount(topology.get_bolts().size());
                }
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
            info.setStatus("NOT FOUND");
        }
        return info;
    }

    @Override
    public void activateTopology(String topologyName) throws IOException {
        try {
            this.stormClient.activate(topologyName);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public void deactivateTopology(String topologyName) throws IOException {
        try {
            this.stormClient.deactivate(topologyName);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    @Override
    public void killTopology(String topologyName)
            throws IOException {
        try {
            this.stormClient.killTopology(topologyName);
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    private TopologyConfigParts getDeviceTopologyParts(long deviceId) throws IOException {
        HDevice device = deviceSystemService.find(deviceId, null, null);
        HProject project = device.getProject();
        // load properties file template
        String packetConfigTemplate = readBundleResource("packet-config.yaml");
        // topology config parts
        TopologyConfigParts topologyConfigParts = new TopologyConfigParts();
        Collection<HPacket> packets = hPacketSystemApi.getPacketsList(device.getId());
        topologyConfigParts.project = project;
        topologyConfigParts.device = device;
        topologyConfigParts.packets = packets;
        packets.forEach((p) -> {
            // build JSON schema for deserialization bolt
            HashMap<String, Object> packetData = new HashMap<>();
            packetData.put("name", p.getName());
            packetData.put("type", p.getType().getName());
            HashMap<String, Object> schema = new HashMap<>();
            packetData.put("schema", schema);
            schema.put("type", p.getFormat().getName());
            HashMap<String, Object> fields = new HashMap<>();
            schema.put("fields", fields);
            p.getFieldsMap().forEach((k, f) -> {
                HashMap<String, Object> innerFieldData = new HashMap<>();
                if (f.getType() != HPacketFieldType.OBJECT) {
                    innerFieldData.put("id", f.getId());
                    innerFieldData.put("type", f.getType().toString().toLowerCase());
                    fields.put(k, innerFieldData);
                }
            });
            //add timestamp information
            HashMap<String, Object> timestampInformation = new HashMap<>();
            timestampInformation.put("format", p.getTimestampFormat());
            timestampInformation.put("field", p.getTimestampField());
            packetData.put("timestamp", timestampInformation);
            //add traffic plan information
            packetData.put("trafficPlan", p.getTrafficPlan().getName().toLowerCase());
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                String jsonSchema = objectMapper.writeValueAsString(packetData);
                String tp = "packet." + p.getId() + "='" + jsonSchema + "'" + "\n";
                topologyConfigParts.properties.append(tp);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            // apply templates
            topologyConfigParts.packetConfig
                    .append(packetConfigTemplate.replaceAll("%pid%", String.valueOf(p.getId())));
        });
        return topologyConfigParts;
    }

    private void topologyServiceSubmit(TopologyConfig topologyConfig) {
        try {
            this.stormClient.submitTopology(topologyConfig.properties, topologyConfig.yaml, topologyConfig.hashCode());
        } catch (Exception e) {
            throw new HyperIoTRuntimeException(e.getMessage());
        }
    }

    private String readBundleResource(String path) throws IOException {
        URL url = FrameworkUtil.getBundle(StormManager.class).getResource(path);
        BufferedReader br = new BufferedReader(
                new InputStreamReader(url.openConnection().getInputStream()));
        StringBuilder buffer = new StringBuilder();
        while (br.ready()) {
            buffer.append(br.readLine()).append("\n");
        }
        br.close();
        return buffer.toString();
    }

    private String getDroolsCode(long projectId, RuleType ruleType) {
        StringBuilder droolsCode = new StringBuilder();
        String[] droolsLines = ruleEngineSystemApi.getDroolsForProject(projectId, ruleType)
                .split("\n");
        int lines = droolsLines.length;
        for (String l : droolsLines) {
            droolsCode.append(l);
            if (--lines == 0) {
                droolsCode.append("\n");
            } else if (!l.trim().isEmpty()) {
                droolsCode.append(" \\\n    ");
            }
        }
        return droolsCode.toString();
    }
}
