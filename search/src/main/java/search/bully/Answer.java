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
public class Answer extends Message {
    /*
     * Answer: response to Election message. 
     * Peer answers an Election message received from a lower ID node.
     * It can be seen as an "I am alive" message.
     */

    private int instance;

    public Answer(Address source, Address destination, int instane) {
        super(source, destination);
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }
}
