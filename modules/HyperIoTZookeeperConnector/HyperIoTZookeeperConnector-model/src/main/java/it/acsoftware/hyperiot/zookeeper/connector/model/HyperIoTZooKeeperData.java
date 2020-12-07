package it.acsoftware.hyperiot.zookeeper.connector.model;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HyperIoTZooKeeperData {
    private static Logger log = Logger.getLogger(HyperIoTZooKeeperData.class.getName());
    private static ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> data;

    public HyperIoTZooKeeperData() {
        this.data = new HashMap<>();
    }

    public void addinfo(String key, Object o) {
        this.data.put(key, o);
    }

    public void removeInfo(String key) {
        this.data.remove(key);
    }

    public byte[] getBytes() {
        try {
            return mapper.writeValueAsBytes(data);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return new byte[]{};
    }
}
