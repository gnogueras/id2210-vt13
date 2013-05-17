/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.system.peer.search;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author Nicolae
 */
public class PropagateEntryFromLeader extends Message {

    String textEntry;
    int id;
    Address entryPeer;

    public PropagateEntryFromLeader(Address source, Address destination, String textEntry, int id, Address entryPeer) {
        super(source, destination);
        this.textEntry = textEntry;
        this.id = id;
        this.entryPeer = entryPeer;
    }

    public String getTextEntry() {
        return textEntry;
    }

    public int getId() {
        return id;
    }
    
    public Address getEntryPeer() {
        return entryPeer;
    }
}
