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
public class Suspect extends Event{
    
    Address suspected;

    public Suspect(Address suspected) {
        this.suspected = suspected;
    }

    public Address getSuspected() {
        return suspected;
    }

        
    
}
