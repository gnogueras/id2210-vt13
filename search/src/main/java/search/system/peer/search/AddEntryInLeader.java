/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.system.peer.search;

import se.sics.kompics.address.Address;
import se.sics.kompics.Event;
import se.sics.kompics.network.Message;

/**
 *
 * @author Nicolae
 */
public class AddEntryInLeader extends Message {

    String textEntry;
    Address entryPeer;

    public AddEntryInLeader(Address source, Address destination, String textEntry, Address entryPeer) {
        super(source, destination);
        this.textEntry = textEntry;
        this.entryPeer = entryPeer;
    }

    public String getTextEntry() {
        return textEntry;
    }

    public Address getEntryPeer() {
        return entryPeer;
    }
}
