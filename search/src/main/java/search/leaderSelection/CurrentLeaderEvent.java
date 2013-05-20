/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.leaderSelection;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

/**
 *
 * @author Gerard
 */
public class CurrentLeaderEvent extends Event {

    private int instance;
    private Address leader;

    public CurrentLeaderEvent(int instance, Address leader) {
        this.instance = instance;
        this.leader = leader;
    }

    public int getInstance() {
        return instance;
    }

    public Address getLeader() {
        return leader;
    }


}
