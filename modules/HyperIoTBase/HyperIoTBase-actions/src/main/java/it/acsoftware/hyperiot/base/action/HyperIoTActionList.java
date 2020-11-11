package it.acsoftware.hyperiot.base.action;

import it.acsoftware.hyperiot.base.api.HyperIoTAction;

import java.util.*;

/**
 * @author Aristide Cittadino Model class that defines method to create a list
 * of actions.
 */
public class HyperIoTActionList {
    /**
     * List of actions for HyperIoTAction
     */
    private List<HyperIoTAction> actions;
    private int currentActionId;

    /**
     * Constructor of HyperIoTActionList
     */
    public HyperIoTActionList() {
        actions = new ArrayList<>();
        currentActionId = 1;
    }

    /**
     * Add items to the list of actions
     *
     * @param action
     */
    public void addAction(HyperIoTAction action) {
        action.setActionId(this.currentActionId);
        this.actions.add(action);
        currentActionId *= 2;
    }

    /**
     * Gets a actions list
     *
     * @return actions list
     */
    public List<HyperIoTAction> getList() {
        //Sorting based on actionIds
        Collections.sort(this.actions, new Comparator<HyperIoTAction>() {
            @Override
            public int compare(HyperIoTAction o1, HyperIoTAction o2) {
                if (o1.getActionId() > o2.getActionId())
                    return 1;
                else if (o1.getActionId() < o2.getActionId())
                    return -1;

                return 0;
            }
        });
        return this.actions;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < actions.size(); i++) {
            sb.append("Action " + actions.get(i).getActionName() + " - " + actions.get(i).getActionId() + "\n");
        }
        return sb.toString();
    }

}
