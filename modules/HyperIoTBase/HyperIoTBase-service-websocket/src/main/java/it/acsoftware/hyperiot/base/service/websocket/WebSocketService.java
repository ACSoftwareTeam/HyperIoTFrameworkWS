package it.acsoftware.hyperiot.base.service.websocket;

import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketEndPoint;
import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketPolicy;
import it.acsoftware.hyperiot.base.api.HyperIoTWebSocketSession;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessage;
import it.acsoftware.hyperiot.base.model.HyperIoTWebSocketMessageType;
import it.acsoftware.hyperiot.base.util.HyperIoTThreadFactoryBuilder;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.http.HttpService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Author Generoso Martello
 * OSGi component which open a web socket session and keep tracks of all user's sessions
 */
@Component(
    name = "websocket-service",
    immediate = true
)
@WebSocket()
public class WebSocketService {
    public final String WEB_SOCKET_SERVICE_URL = HyperIoTUtil.getHyperIoTBaseRestContext(HyperIoTUtil.getBundleContext(WebSocketService.class)) + "/ws";
    private Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * Managing all websocket sessions in terms of:
     * 1) Classical sessions
     * 2) Bridged Sessions
     * 3) Custom Websocket policies for each session
     */
    private static final Map<Session, HyperIoTWebSocketSession> sessions;
    private static final Map<Session, List<HyperIoTWebSocketPolicy>> webSocketSessionPolicies;

    /**
     * Managing Threads with custom executors in order to avoid greedy clients to monopolize connections
     */
    private static final ThreadFactory onOpenThreadsFactory;
    private static final ThreadFactory onCloseThreadsFactory;
    private static final ThreadFactory onOnMessageThreadsFactory;
    private static final ThreadFactory onErrorThreadsFactory;
    private static final Executor onOpenDispatchThreads;
    private static final Executor onCloseDispatchThreads;
    private static final Executor onMessageDispatchThreads;
    private static final Executor onErrorDispatchThreads;

    static {
        sessions = Collections.synchronizedMap(new HashMap<>());
        webSocketSessionPolicies = Collections.synchronizedMap(new HashMap<>());
        //sharing bridged session object inside the JVM
        onOpenThreadsFactory = HyperIoTThreadFactoryBuilder.build("hyperiot-ws-bridge-thread-%d", false);
        onCloseThreadsFactory = HyperIoTThreadFactoryBuilder.build("hyperiot-ws-bridge-close-thread-%d", false);
        onOnMessageThreadsFactory = HyperIoTThreadFactoryBuilder.build("hyperiot-ws-bridge-message-thread-%d", false);
        onErrorThreadsFactory = HyperIoTThreadFactoryBuilder.build("hyperiot-ws-bridge-error-thread-%d", false);
        //new connections must be throtled 100 is a good chioce
        onOpenDispatchThreads = Executors.newFixedThreadPool(HyperIoTUtil.getWebSocketOnOpenDispatchThreads(200), onOpenThreadsFactory);
        //same as Open
        onCloseDispatchThreads = Executors.newFixedThreadPool(HyperIoTUtil.getWebSocketOnCloseDispatchThreads(200), onCloseThreadsFactory);
        //high used threads, may be this number should be related to jetty max threads
        onMessageDispatchThreads = Executors.newFixedThreadPool(HyperIoTUtil.getWebSocketOnMessageDispatchThreads(500), onOnMessageThreadsFactory);
        //Error threads can be considered as message thread but a very rare
        onErrorDispatchThreads = Executors.newFixedThreadPool(HyperIoTUtil.getWebSocketOnErrorDispatchThreads(20), onErrorThreadsFactory);

    }

    private HttpService httpService;

    @Reference
    protected void setHttpService(HttpService httpService) {
        this.httpService = httpService;
    }

    @Activate
    public void activate() throws Exception {
        httpService.registerServlet(WEB_SOCKET_SERVICE_URL, new WebSocketServiceServlet(), null, null);
    }

    @Deactivate
    public void deactivate() throws Exception {
        sessions.values().forEach(hyperIoTWebSocketSession -> {
            hyperIoTWebSocketSession.dispose();
        });
        httpService.unregister(WEB_SOCKET_SERVICE_URL);
    }

    @OnWebSocketConnect
    public void onOpen(Session session) {
        try {
            log.log(Level.FINE, "Opening web socket...");
            HashMap<String, HyperIoTWebSocketEndPoint> endPointHashMap = findEndWebSocketEndPoints();
            String requestPath = session.getUpgradeRequest().getRequestURI().getPath().replace(WEB_SOCKET_SERVICE_URL, "");
            if (requestPath.startsWith("/")) requestPath = requestPath.substring(1);
            if (endPointHashMap.containsKey(requestPath)) {
                HyperIoTWebSocketEndPoint hyperIotWebSocketEndPoint = endPointHashMap.get(requestPath);
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            HyperIoTWebSocketSession hyperIoTWebSocketSession = hyperIotWebSocketEndPoint.getHandler(session);
                            if (hyperIoTWebSocketSession.isAuthenticationRequired())
                                hyperIoTWebSocketSession.authenticate();
                            //2 minutes idle timeout
                            session.setIdleTimeout(120000);
                            sessions.put(hyperIoTWebSocketSession.getSession(), hyperIoTWebSocketSession);
                            List<HyperIoTWebSocketPolicy> policies = hyperIoTWebSocketSession.getWebScoketPolicies();
                            if (policies != null && policies.size() > 0) {
                                webSocketSessionPolicies.put(session, policies);
                            }
                            hyperIoTWebSocketSession.initialize();
                        } catch (Throwable t) {
                            log.log(Level.SEVERE, t.getMessage(), t);
                        }
                    }
                };
                Executor onOpenCustomExecutor = hyperIotWebSocketEndPoint.getExecutorForOpenConnections(session);
                Executor runner = (onOpenCustomExecutor != null) ? onOpenCustomExecutor : onOpenDispatchThreads;
                runner.execute(r);
            } else {
                HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, "Unknown service requested.".getBytes(), HyperIoTWebSocketMessageType.ERROR);

                session.close(1010, m.toJson());
            }
        } catch (Throwable e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final HyperIoTWebSocketSession hyperIoTWebSocketSession = sessions.get(session);
                    sessions.remove(session);
                    log.log(Level.FINE, "On Close websocket : {0}", reason);
                    if (session.isOpen()) {
                        try {
                            HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, ("Closing websocket: " + reason).getBytes(), HyperIoTWebSocketMessageType.DISCONNECTING);
                            WriteCallback wc = new WriteCallback() {
                                @Override
                                public void writeFailed(Throwable x) {
                                    log.log(Level.WARNING, "Error while sending message: {0}", x);
                                }

                                @Override
                                public void writeSuccess() {
                                    log.log(Level.FINE, "Close message sent!");
                                }
                            };
                            //if websocket is a WebSocketAbstractSession it will use it's own method for sending messages
                            if (hyperIoTWebSocketSession instanceof HyperIoTWebSocketAbstractSession) {
                                ((HyperIoTWebSocketAbstractSession) hyperIoTWebSocketSession).sendRemote(m, wc);
                            } else {
                                session.getRemote().sendString(m.toJson(), wc);
                            }
                        } catch (Throwable e) {
                            log.log(Level.WARNING, "Cannot send closing reason on websocket: {0}", e.getMessage());
                        }
                    }

                    if (hyperIoTWebSocketSession != null) {
                        try {
                            hyperIoTWebSocketSession.dispose();
                        } catch (Throwable e) {
                            log.log(Level.WARNING, "Error closing connection: {0}", e.getMessage());
                        }
                    }
                } catch (Throwable t) {
                    log.log(Level.SEVERE, "Error while closing websocket: {0} - {1}", new Object[]{t.getMessage(), t.getCause()});
                }
            }
        };
        onCloseDispatchThreads.execute(r);
    }


    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    if (session == null || !session.isOpen())
                        return;
                    log.log(Level.FINE, "On Message websocket getting session...");
                    HyperIoTWebSocketSession hyperIoTWebSocketSession = sessions.get(session);
                    //Policy Check
                    if (webSocketSessionPolicies != null && !webSocketSessionPolicies.isEmpty() && webSocketSessionPolicies.containsKey(hyperIoTWebSocketSession.getSession())) {
                        List<HyperIoTWebSocketPolicy> policies = webSocketSessionPolicies.get(hyperIoTWebSocketSession.getSession());
                        for (HyperIoTWebSocketPolicy policy : policies) {
                            if (!policy.isSatisfied(hyperIoTWebSocketSession.getPolicyParams(), message.getBytes())) {
                                if (policy.printWarningOnFail()) {
                                    log.log(Level.SEVERE, "Policy {0} not satisfied! ", policy.getClass().getSimpleName());
                                }

                                if (policy.sendWarningBackToClientOnFail()) {
                                    String policyWarning = "Policy " + policy.getClass().getSimpleName() + " Not satisfied!";
                                    HyperIoTWebSocketMessage m = HyperIoTWebSocketMessage.createMessage(null, policyWarning.getBytes(), HyperIoTWebSocketMessageType.WEBSOCKET_POLICY_WARNING);
                                    hyperIoTWebSocketSession.getSession().getRemote().sendString(m.toJson());
                                }

                                if (policy.closeWebSocketOnFail()) {
                                    hyperIoTWebSocketSession.dispose();
                                    return;
                                }

                                if (policy.ignoreMessageOnFail()) {
                                    return;
                                }
                            }
                        }
                    }

                    //Forwarding message after policy check
                    long sessionFoundTime = System.nanoTime();
                    if (hyperIoTWebSocketSession != null) {
                        hyperIoTWebSocketSession.onMessage(message);
                        log.log(Level.FINE, "Message forwarded to session in {0} seconds", ((double) System.nanoTime() - sessionFoundTime) / 1_000_000_000);
                    }
                } catch (Throwable e) {
                    log.log(Level.SEVERE, "Error while forwarding message to websocket session: {0}", e);
                }
            }
        };
        onMessageDispatchThreads.execute(r);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable cause) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                if (session == null)
                    return;
                log.log(Level.WARNING, "On Web Socket Error: {0} , {1}", new Object[]{cause.getMessage(), cause});
                try {
                    log.log(Level.FINE, "Tring close websocket on error: {0} , {1}", new Object[]{cause.getMessage(), cause});
                    WebSocketService.this.onClose(session, 500, cause.getCause().getMessage());
                } catch (Throwable e) {
                    log.log(Level.SEVERE, e.getMessage(), e);
                }
            }
        };
        onErrorDispatchThreads.execute(r);
    }

    private HashMap<String, HyperIoTWebSocketEndPoint> findEndWebSocketEndPoints() {
        // TODO: in a future version consider using OSGi ServiceTracker to keep track of available WebSocketEndPoints
        //       https://mnlipp.github.io/osgi-getting-started/TrackingAService.html
        HashMap<String, HyperIoTWebSocketEndPoint> endPointHashMap = new HashMap<>();
        try {
            BundleContext bundleContext = HyperIoTUtil.getBundleContext(this);
            ServiceReference[] serviceReferences = bundleContext
                .getAllServiceReferences(HyperIoTWebSocketEndPoint.class.getName(), null);
            if (serviceReferences != null) {
                for (ServiceReference serviceReference : serviceReferences) {
                    HyperIoTWebSocketEndPoint ep = (HyperIoTWebSocketEndPoint) bundleContext
                        .getService(serviceReference);
                    endPointHashMap.put(ep.getPath(), ep);
                }
            }
        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        return endPointHashMap;
    }

    public static Map<Session, HyperIoTWebSocketSession> getSessions() {
        return sessions;
    }
}
