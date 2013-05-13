/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

/**
 *
 * @author Gerard
 */
public class Restore extends Event{
    
    Address restored;

    public Restore(Address restored) {
        this.restored = restored;
    }

    public Address getRestored() {
        return restored;
    }

        
    
}
