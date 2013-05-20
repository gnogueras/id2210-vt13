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
public class AddEntryInLeaderSimulation extends Message {

    String textEntry;
    Address entryPeer;
    int counter;

    public AddEntryInLeaderSimulation(Address source, Address destination, String textEntry, Address entryPeer, int counter) {
        super(source, destination);
        this.textEntry = textEntry;
        this.entryPeer = entryPeer;
        this.counter = counter;
    }

    public String getTextEntry() {
        return textEntry;
    }

    public Address getEntryPeer() {
        return entryPeer;
    }
    
    public int getCounter(){
        return counter;
    }
}
