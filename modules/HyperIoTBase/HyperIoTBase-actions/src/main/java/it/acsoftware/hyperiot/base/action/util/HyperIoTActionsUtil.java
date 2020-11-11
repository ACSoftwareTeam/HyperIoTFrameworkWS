package it.acsoftware.hyperiot.base.action.util;

import it.acsoftware.hyperiot.base.action.HyperIoTActionName;
import it.acsoftware.hyperiot.base.api.HyperIoTAction;
import it.acsoftware.hyperiot.base.exception.HyperIoTRuntimeException;
import it.acsoftware.hyperiot.base.util.HyperIoTConstants;
import it.acsoftware.hyperiot.base.util.HyperIoTUtil;
import it.acsoftware.hyperiot.osgi.util.filter.OSGiFilterBuilder;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Aristide Cittadino Class with some helper methods about Actions
 */
public class HyperIoTActionsUtil {
    private static Logger log = Logger.getLogger("it.acsoftware.hyperiot");

    /**
     * @param className className related to the Action
     * @param action    ActionName
     * @return the OSGi registered action
     */
    public static HyperIoTAction getHyperIoTAction(String className, HyperIoTActionName action) {
        log.log(Level.FINE,
                "Service getAction for {0}" , new Object[]{className,action.getName()});
        Collection<ServiceReference<HyperIoTAction>> serviceReferences;
        try {
            String actionFilter = OSGiFilterBuilder
                    .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, className)
                    .and(HyperIoTConstants.OSGI_ACTION_NAME, action.getName()).getFilter();
            log.log(Level.FINE,
                    "Searching for OSGi registered action with filter: {0}" , actionFilter);
            serviceReferences = HyperIoTUtil.getBundleContext(HyperIoTActionName.class)
                    .getServiceReferences(HyperIoTAction.class, actionFilter);
            if (serviceReferences.size() > 1) {
                log.log(Level.FINE, "No OSGi action found for filter: " + actionFilter);
                throw new HyperIoTRuntimeException();
            }
            HyperIoTAction act = (HyperIoTAction) HyperIoTUtil
                    .getBundleContext(HyperIoTActionName.class)
                    .getService(serviceReferences.iterator().next());
            log.log(Level.FINE, "OSGi action found {0}" , act);
            return act;
        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, "Invalid OSGi Syntax", e);
            throw new HyperIoTRuntimeException();
        }
    }

    /**
     * @param className className related to the Action
     * @return the OSGi registered action
     */
    public static List<HyperIoTAction> getHyperIoTCrudActions(String className) {
        log.log(Level.FINE,
                "Service getAction for {0}",className);
        List<HyperIoTAction> actions = new ArrayList<>();
        for (int i = 0; i < HyperIoTCrudAction.values().length; i++) {
            actions.add(getHyperIoTAction(className, HyperIoTCrudAction.values()[i]));
        }
        return actions;
    }

    public static List<HyperIoTAction> getHyperIoTActions() {
        log.log(Level.FINE, "Service getActions ");
        Collection<ServiceReference<HyperIoTAction>> serviceReferences;
        try {

            log.log(Level.FINE, "Searching for OSGi registered actions");
            serviceReferences = HyperIoTUtil.getBundleContext(HyperIoTActionName.class)
                    .getServiceReferences(HyperIoTAction.class, null);
            List<HyperIoTAction> actions = serviceReferences.stream()
                    .map(serviceReference -> (HyperIoTAction) HyperIoTUtil
                            .getBundleContext(HyperIoTActionName.class)
                            .getService(serviceReference))
                    .collect(Collectors.toList());
            return actions;

        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, "Invalid OSGi Syntax", e);
            throw new HyperIoTRuntimeException();
        }
    }

    /**
     * @param className className related to the Actions
     * @return the OSGi registered actions
     */
    public static List<HyperIoTAction> getHyperIoTActions(String className) {
        log.log(Level.FINE,
                "Service getActions for {0}" , className);
        Collection<ServiceReference<HyperIoTAction>> serviceReferences;
        try {
            String actionFilter = OSGiFilterBuilder
                    .createFilter(HyperIoTConstants.OSGI_ACTION_RESOURCE_NAME, className).getFilter();
            log.log(Level.FINE, "Searching for OSGi registered action with filter: {0}" , actionFilter);
            serviceReferences = HyperIoTUtil.getBundleContext(HyperIoTActionName.class)
                    .getServiceReferences(HyperIoTAction.class, actionFilter);
            if (serviceReferences.isEmpty()) {
                log.log(Level.FINE, "No OSGi action found for filter: " + actionFilter);
                throw new HyperIoTRuntimeException();
            }
            List<HyperIoTAction> hyperIoTActionList = new ArrayList<>();
            for(ServiceReference<HyperIoTAction> hyperIoTAction : serviceReferences) {
                hyperIoTActionList.add(HyperIoTUtil.getBundleContext(HyperIoTActionName.class)
                        .getService(hyperIoTAction));
            }
            return hyperIoTActionList;
        } catch (InvalidSyntaxException e) {
            log.log(Level.SEVERE, "Invalid OSGi Syntax", e);
            throw new HyperIoTRuntimeException();
        }
    }

}
