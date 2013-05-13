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
public class Election extends Message{
    /*
     * Election: message sent as a request for being the new Leader
     * Peer sends Election message to all higher ID nodes. 
     * If no Answer from any node, it will becomes new leader.
     * Otherwise, wait some time until receive a Coordinator message.
     * (expiration timeout, resend Election)
     */
    
    private int instance;

    public Election(Address source, Address destination,int instance) {
        super(source, destination);
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }

    
}
