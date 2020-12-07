package it.acsoftware.hyperiot.bundle.listener.service;

import it.acsoftware.hyperiot.bundle.listener.model.BundleTrackerItem;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import java.util.ArrayList;
import java.util.logging.Logger;

public class BundleTracker {
    protected static Logger log = Logger.getLogger(BundleTracker.class.getName());

    private MyBundleTracker bundleTracker;

    public void start(BundleContext context) {
        log.info("Starting Bundle Tracker");
        int trackStates = Bundle.STARTING | Bundle.STOPPING | Bundle.RESOLVED | Bundle.INSTALLED | Bundle.UNINSTALLED;
        bundleTracker = new MyBundleTracker(context, trackStates, null);
        bundleTracker.open();
    }

    public void stop(BundleContext context) {
        log.info("Stopping Bundle Tracker");
        bundleTracker.close();
        bundleTracker = null;
    }

    public ArrayList<BundleTrackerItem> list() {
        return bundleTracker.bundleList;
    }
    public BundleTrackerItem get(String symbolicName) {
        return bundleTracker.get(symbolicName);
    }

    private static final class MyBundleTracker extends org.osgi.util.tracker.BundleTracker {
        private ArrayList<BundleTrackerItem> bundleList = new ArrayList();
        public MyBundleTracker(BundleContext context, int stateMask,
                               BundleTrackerCustomizer customizer) {
            super(context, stateMask, customizer);
            // TODO: should get the list of bundles
        }

        public BundleTrackerItem get(String symbolicName) {
            return bundleList.stream()
                    .filter((b) -> b.getBundle().getSymbolicName().equals(symbolicName))
                    .findFirst().orElse(null);
        }

        public Object addingBundle(Bundle bundle, BundleEvent event) {
            update(bundle, event);
            return bundle;
        }

        public void removedBundle(Bundle bundle, BundleEvent event,
                                  Object object) {
            update(bundle, event);
        }

        public void modifiedBundle(Bundle bundle, BundleEvent event,
                                   Object object) {
            update(bundle, event);
        }

        private void update(Bundle bundle, BundleEvent event) {
            BundleTrackerItem item = get(bundle.getSymbolicName());
            if (item == null) {
                item = new BundleTrackerItem(bundle);
                bundleList.add(item);
            }
            item.update(event);
            log.info(item.getName() + ", state: " + item.getState() + ", event.type: " + item.getType() + " COUNT = " + bundleList.size());
        }
    }

}
