/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import java.util.ArrayList;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

/**
 *
 * @author Gerard
 */
public class NewInstance extends Event {

    private int instance;
    private Address self;
    ArrayList<Address> neighbors;

    public NewInstance(Address self, int instance, ArrayList<Address> neighbors) {
        super();
        this.instance = instance;
        this.self = self;
        this.neighbors = neighbors;
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

    public ArrayList<Address> getNeighbors() {
        return neighbors;
    }

}

