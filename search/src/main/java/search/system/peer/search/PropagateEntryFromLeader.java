/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.system.peer.search;

import se.sics.kompics.Event;

/**
 *
 * @author Nicolae
 */
public class PropagateEntryFromLeader extends Event {

    String textEntry;
    int id;

    public PropagateEntryFromLeader(String textEntry, int id) {
        super();
        this.textEntry = textEntry;
        this.id = id;
    }

    public String getTextEntry() {
        return textEntry;
    }

    public int getId() {
        return id;
    }
}
