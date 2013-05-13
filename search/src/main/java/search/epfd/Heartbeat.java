/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;


/**
 *
 * @author Gerard
 */
public class Heartbeat extends Message{

    public Heartbeat(Address source, Address destination) {
        super(source, destination);
    }
    
}
