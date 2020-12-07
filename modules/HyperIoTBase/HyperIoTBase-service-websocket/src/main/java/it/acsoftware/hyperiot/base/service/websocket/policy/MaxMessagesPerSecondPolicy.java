package it.acsoftware.hyperiot.base.service.websocket.policy;

import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketAbstractPolicy;
import org.eclipse.jetty.websocket.api.Session;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @Author Aristide Cittadino
 * Max message per second Policy
 */
public class MaxMessagesPerSecondPolicy extends HyperIoTWebSocketAbstractPolicy {
    private static Logger log = Logger.getLogger(MaxMessagesPerSecondPolicy.class.getName());

    public static final long TIME_WINDOW_MS = 1000;
    private long startTimestamp = -1;
    private long count;
    private long maxMessagesPerSecond;
    private Session session;

    public MaxMessagesPerSecondPolicy(Session s, long max) {
        super(s);
        this.count = 1;
        this.maxMessagesPerSecond = max;
    }

    @Override
    public synchronized boolean isSatisfied(Map<String, Object> params, byte[] payload) {
        log.log(Level.FINE, "Policy Max Message Per Second");
        long currentTimeStamp = System.currentTimeMillis();
        if (startTimestamp == -1) {
            startTimestamp = currentTimeStamp;
            return true;
        }
        long diff = currentTimeStamp - startTimestamp;
        if (diff / TIME_WINDOW_MS <= 1) {
            count++;
            log.log(Level.FINE, "Policy Max Message Per Second:Time Window less than 1sec, counter is: {0}", count);
            if (count > maxMessagesPerSecond)
                return false;
            return true;
        } else {
            log.log(Level.FINE, "Policy Max Message Per Second: resetting time winwdow");
            //resetting the time window
            startTimestamp = currentTimeStamp;
            count = 1;
        }
        return true;
    }

    @Override
    public boolean closeWebSocketOnFail() {
        return true;
    }

    @Override
    public boolean printWarningOnFail() {
        return true;
    }

    @Override
    public boolean sendWarningBackToClientOnFail() {
        return false;
    }
}
