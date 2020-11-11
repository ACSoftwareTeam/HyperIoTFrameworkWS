package it.acsoftware.hyperiot.base.service.websocket;

import it.acsoftware.hyperiot.base.api.HyperIoTJwtContext;
import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketSession;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.base.security.rest.HyperIoTAuthenticationFilter;
import it.acsoftware.hyperiot.base.service.websocket.compression.HyperIoTWebSocketCompressionPolicy;
import it.acsoftware.hyperiot.base.service.websocket.encryption.policy.HyperIoTWebSocketEncryptionPolicy;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import java.net.HttpCookie;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Generoso Martello
 * This class implements the concept of a Web Socket Session
 */
public abstract class HyperIoTWebSocketAbstractSession implements HyperIoTWebSocketSession {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    private Session session;
    private HyperIoTJwtContext context;
    private BundleContext bundleContext;
    private boolean authenticationRequired;
    private HyperIoTWebSocketMessageBroker messageBroker;

    private HyperIoTAuthenticationFilter hyperIoTAuthenticationFilter;

    /**
     * @param session
     */
    public HyperIoTWebSocketAbstractSession(Session session, boolean authenticationRequired) {
        log.log(Level.FINE, "Creating websocket session....");
        this.session = session;
        bundleContext = HyperIoTUtil.getBundleContext(this);
        this.authenticationRequired = authenticationRequired;
        this.initMessageBroker(session, null, null);
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     */
    public HyperIoTWebSocketAbstractSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy) {
        this(session, authenticated);
        initMessageBroker(session, encryptionPolicy, null);
    }

    /**
     * @param session
     * @param authenticated
     * @param compressionPolicy
     */
    public HyperIoTWebSocketAbstractSession(Session session, boolean authenticated, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        this(session, authenticated);
        initMessageBroker(session, null, compressionPolicy);
    }

    /**
     * @param session
     * @param authenticated
     * @param encryptionPolicy
     * @param compressionPolicy
     */
    public HyperIoTWebSocketAbstractSession(Session session, boolean authenticated, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        this(session, authenticated);
        initMessageBroker(session, encryptionPolicy, compressionPolicy);
    }

    /**
     * @param s
     * @param encryptionPolicy
     * @param compressionPolicy
     */
    private void initMessageBroker(Session s, HyperIoTWebSocketEncryptionPolicy encryptionPolicy, HyperIoTWebSocketCompressionPolicy compressionPolicy) {
        this.messageBroker = new HyperIoTWebSocketMessageBroker(s);
        this.messageBroker.setEncryptionPolicy(encryptionPolicy);
        this.messageBroker.setCompressionPolicy(compressionPolicy);

        if (encryptionPolicy != null)
            this.messageBroker.setEncryptionPolicy(encryptionPolicy);

        if (compressionPolicy != null)
            this.messageBroker.setCompressionPolicy(compressionPolicy);

        this.messageBroker.onOpenSession(s);
    }

    /**
     * @return
     */
    public Session getSession() {
        return session;
    }

    /**
     * @return
     */
    protected HyperIoTWebSocketMessageBroker getMessageBroker() {
        return messageBroker;
    }

    /**
     * @return
     */
    protected BundleContext getBundleContext() {
        return bundleContext;
    }

    /**
     * @return
     */
    protected HyperIoTJwtContext getContext() {
        return context;
    }

    /**
     *
     */
    public void dispose() {
        this.getMessageBroker().onCloseSession(this.getSession());
        try {
            session.close();
        } catch (Throwable t) {
            log.log(Level.SEVERE, t.getMessage(), t);
        }
    }

    /**
     * @return
     */
    public boolean isAuthenticated() {
        return this.authenticationRequired && this.context != null && context.getLoggedUsername() != null && !context.getLoggedUsername().isEmpty();
    }

    /**
     * @return
     */
    public boolean isAuthenticationRequired() {
        return authenticationRequired;
    }

    /**
     * @param m
     */
    public void sendRemote(HyperIoTWebSocketMessage m) {
        this.sendRemote(m, true, null);
    }

    /**
     * @param m
     * @param callback
     */
    public void sendRemote(HyperIoTWebSocketMessage m, WriteCallback callback) {
        this.sendRemote(m, true, callback);
    }

    /**
     * @param message
     * @param callback
     */
    public void sendRemote(String message, WriteCallback callback) {
        this.sendRemote(message, true, callback);
    }

    /**
     * @param message
     */
    public void sendRemote(String message) {
        this.sendRemote(message, true);
    }

    /**
     * @param m
     */
    public void sendRemote(HyperIoTWebSocketMessage m, boolean encodeBase64) {
        this.messageBroker.sendAsync(m, encodeBase64, null);
    }

    /**
     * @param m
     * @param callback
     */
    public void sendRemote(HyperIoTWebSocketMessage m, boolean encodeBase64, WriteCallback callback) {
        this.messageBroker.sendAsync(m, encodeBase64, callback);
    }

    /**
     * @param message
     * @param callback
     */
    public void sendRemote(String message, boolean encodeBase64, WriteCallback callback) {
        this.messageBroker.sendAsync(message.getBytes(), encodeBase64, callback);
    }

    /**
     * @param message
     */
    public void sendRemote(String message, boolean encodeBase64) {
        this.messageBroker.sendAsync(message.getBytes(), encodeBase64, null);
    }

    /**
     *
     */
    public void authenticate() {
        try {
            if (this.bundleContext == null)
                bundleContext = HyperIoTUtil.getBundleContext(this);
            ServiceReference serviceReference = bundleContext
                .getAllServiceReferences(ContainerRequestFilter.class.getName(), "(org.apache.cxf.dosgi.IntentName=jwtAuthFilter)")[0];
            hyperIoTAuthenticationFilter = (HyperIoTAuthenticationFilter) bundleContext
                .getService(serviceReference);
        } catch (Exception e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }

        String jwtToken = null;

        log.log(Level.FINE, "Checking Auth token in cookies or headers");
        if (session.getUpgradeRequest()
            .getCookies() != null && session.getUpgradeRequest()
            .getCookies().size() > 0) {
            log.log(Level.FINE, "Token found in cookies");
            HttpCookie cookie = session.getUpgradeRequest()
                .getCookies()
                .stream()
                .filter((c) -> c.getName().equals(HyperIoTConstants.HYPERIOT_AUTHORIZATION_COOKIE_NAME))
                .findAny().orElse(null);

            if (cookie != null) {
                log.log(Level.FINE, "Cookie found, checking authentication...");
                jwtToken = cookie.getValue();

            }
        } else if (session.getUpgradeRequest().getHeader(HttpHeaders.AUTHORIZATION) != null) {
            log.log(Level.FINE, "token found in header");
            jwtToken = session.getUpgradeRequest().getHeader(HttpHeaders.AUTHORIZATION).replace("JWT ", "");
        }

        if (jwtToken != null) {
            HyperIoTJwtContext hyperIoTContext = hyperIoTAuthenticationFilter.doApplicationFilter(jwtToken);
            if (hyperIoTContext != null) {
                context = hyperIoTContext;
            }
        }

        if (this.context == null) {
            log.log(Level.FINE, "User not authorized to connect to websocket");
            //Closes the connection if the client is not authenticated and authentication is required
            if (this.isAuthenticationRequired()) {
                try {
                    session.getRemote().sendString("Client not authenticated!", new WriteCallback() {
                        @Override
                        public void writeFailed(Throwable x) {
                            log.log(Level.WARNING, "Error while sending message: {}", new Object[]{x});
                        }

                        @Override
                        public void writeSuccess() {
                            log.log(Level.FINE, "Send message success!");
                        }
                    });
                    session.close(1008, "Client not authenticated!");
                } catch (Exception e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public abstract void initialize();

    /**
     * @param message
     */
    @Override
    public abstract void onMessage(String message);
}
