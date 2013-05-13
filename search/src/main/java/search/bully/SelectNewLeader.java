/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import common.peer.PeerAddress;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

/**
 *
 * @author Gerard
 */
public class SelectNewLeader extends Event {

    private int instance;
    private Address self;

    public SelectNewLeader(Address self, int instance) {
        super();
        this.instance = instance;
        this.self = self;
    }

    public int getInstance() {
        return instance;
    }

    public Address getSelf() {
        return self;
    }

    public void setInstance(int instance) {
        this.instance = instance;
    }

    public void setSelf(Address self) {
        this.self = self;
    }

}

