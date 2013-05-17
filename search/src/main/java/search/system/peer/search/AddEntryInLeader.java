/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import se.sics.kompics.address.Address;
import se.sics.kompics.Event;

/**
 *
 * @author Nicolae
 */
public class AddEntryInLeader extends Event {

    String textEntry;
    Address entryPeer;

    public AddEntryInLeader(String textEntry, Address entryPeer) {
        super();
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
