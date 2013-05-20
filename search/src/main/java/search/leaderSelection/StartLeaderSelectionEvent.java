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
public class StartLeaderSelectionEvent extends Event {

    private int instance;


    public StartLeaderSelectionEvent(int instance) {
        super();
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }

}
