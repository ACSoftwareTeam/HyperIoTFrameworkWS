package it.acsoftware.hyperiot.base.action;

import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import org.osgi.framework.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @param <T> parameter that indicates a generic class
 * @author Aristide Cittadino Model class that define a bundle activator for a
 * generic entity of HyperIoT platform.
 */
public abstract class HyperIoTPermissionActivator<T> implements BundleActivator {
    protected Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * Generic class for HyperIoT platform
     */
    private Class<T> type;

    /**
     * Constructor for HyperIoTPermissionActivator
     *
     * @param type parameter that indicates a generic entity
     */
    public HyperIoTPermissionActivator(Class<T> type) {
        this.type = type;
    }

    /**
     * Registers a list of actions that have to be registered as OSGi components
     */
    public void registerActions() {
        log.log(Level.FINE, "Invoking registerActions of {0}", this.getClass().getSimpleName());
        BundleContext bc = this.getBundleContext();
        HyperIoTActionList list = this.getActions();
        List<HyperIoTAction> actions = list.getList();
        if (list != null && actions.size() > 0) {
            //registering also the actionList in order to let oder moudles to add permission to same entity
            Dictionary<String, String> dictionary = new Hashtable<String, String>();
            dictionary.put(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, type.getName());
            bc.registerService(HyperIoTActionList.class, list, dictionary);
            for (int i = 0; i < actions.size(); i++) {
                HyperIoTAction action = actions.get(i);
                if (!action.isRegistered()) {
                    dictionary = new Hashtable<String, String>();
                    dictionary.put(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, type.getName());
                    dictionary.put(HyperIoTConstants.OSGI_ACTION_RESOURCE_CATEGORY, action.getCategory());
                    dictionary.put(HyperIoTConstants.OSGI_ACTION_NAME, action.getActionName());
                    action.setRegistered(true);
                    bc.registerService(HyperIoTAction.class, action, dictionary);
                    log.log(Level.FINE, "Action Registered : {0} - {1} - {2}", new Object[]{action.getActionName(), action.getResourceName(), action.getCategory()});
                } else {
                    log.log(Level.FINE, "Action Already Registered : {0} - {1} - {2}", new Object[]{action.getActionName(), action.getResourceName(), action.getCategory()});
                }
            }
        }
    }

    /**
     * Releases an action that have to be registered as OSGi components
     */
    public void unregisterActions() {
        log.log(Level.FINE, "Invoking unregisterActions of {0}", this.getClass().getSimpleName());
        BundleContext bc = this.getBundleContext();
        try {
            ServiceReference<?>[] actions;
            actions = bc.getAllServiceReferences(HyperIoTAction.class.getName(),
                    "(" + HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME + "=" + type.getName() + ")");
            for (int i = 0; i < actions.length; i++) {
                bc.ungetService(actions[i]);
                log.log(Level.FINE, "Action Unregistered : {0}", actions[i]);
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

    }

    /**
     * Returns the BundleContext of the bundle which contains this component.
     *
     * @return The BundleContext of the bundle containing this component.
     */
    protected BundleContext getBundleContext() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        return bundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        log.log(Level.FINE, "Invoking start bundle of {0}", this.getClass().getSimpleName());
        this.registerActions();

    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.log(Level.FINE, "Invoking stop bundle of {0}", this.getClass().getSimpleName());
        this.unregisterActions();

    }

    /**
     * Gets list of actions
     *
     * @return List of actions
     */
    public abstract HyperIoTActionList getActions();
}
