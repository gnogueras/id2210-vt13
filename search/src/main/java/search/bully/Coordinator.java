/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author Gerard
 */
public class Coordinator extends Message {
    /*
     * Coordinator: message sent to announce the identity of the peer elected as leader.
     * It sends this message when a peer has broadcast Answer message to all its higer ID peers
     * and it does not get any answer. 
     */

    private int instance;

    public Coordinator(Address source, Address destination, int instance) {
        super(source, destination);
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }
}
