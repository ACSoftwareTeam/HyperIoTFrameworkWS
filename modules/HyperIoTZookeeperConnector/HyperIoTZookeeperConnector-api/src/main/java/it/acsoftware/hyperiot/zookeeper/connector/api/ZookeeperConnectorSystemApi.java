package it.acsoftware.hyperiot.zookeeper.connector.api;

import it.acsoftware.hyperiot.base.api.HyperIoTBaseSystemApi;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

/**
 *
 * @author Aristide Cittadino Interface component for %- projectSuffixUC SystemApi. This
 *         interface defines methods for additional operations.
 *
 */
public interface ZookeeperConnectorSystemApi extends HyperIoTBaseSystemApi {

    /**
     * This method adds a LeaderLatchListener
     * @param listener LeaderLatchListener instance
     * @param leadershipPath ZkNode path which bind listener on
     */
    void addListener(LeaderLatchListener listener, String leadershipPath);

    /**
     * This method returns true if current node is leader on given zkNode path, false otherwise
     * @param mutexPath ZkNode path
     * @return True if current node is leader on given zkNode path, false otherwise
     */
    boolean isLeader(String mutexPath);

}
