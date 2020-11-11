package it.acsoftware.hyperiot.base.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HyperIoTWebSocketMessage {
    @JsonIgnore
    private static Logger log = Logger.getLogger("it.acsofware.hyperiot");
    @JsonIgnore
    private static ObjectMapper mapper = new ObjectMapper();

    private String cmd;
    private byte[] payload;
    private String contentType;
    private Date timestamp;
    private HyperIoTWebSocketMessageType type;
    private HashMap<String, String> params;

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public HyperIoTWebSocketMessage() {
        this.params = new HashMap<>();
        this.contentType = "text/plain";
    }

    public HashMap<String, String> getParams() {
        return params;
    }

    public void setParams(HashMap<String, String> params) {
        this.params = params;
    }

    public String getCmd() {
        return cmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public HyperIoTWebSocketMessageType getType() {
        return type;
    }

    public void setType(HyperIoTWebSocketMessageType type) {
        this.type = type;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public static HyperIoTWebSocketMessage createMessage(String cmd, byte[] payload, HyperIoTWebSocketMessageType type) {
        HyperIoTWebSocketMessage m = new HyperIoTWebSocketMessage();
        m.setTimestamp(new Date());
        m.setCmd(cmd);
        m.setPayload(payload);
        m.setType(type);
        return m;
    }

    public static HyperIoTWebSocketMessage fromString(String message) {
        try {
            return mapper.readValue(message, HyperIoTWebSocketMessage.class);
        } catch (Throwable t) {
            log.log(Level.FINEST, "Error while parsing websocket message: {0}", new Object[]{t.getMessage()});
        }
        return null;
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Throwable t) {
            log.log(Level.SEVERE, t.getMessage(), t);
        }
        return "{}";
    }
}
