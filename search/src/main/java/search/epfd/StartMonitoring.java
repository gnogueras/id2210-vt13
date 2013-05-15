/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import java.util.ArrayList;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

/**
 *
 * @author Gerard
 */
public class StartMonitoring extends Event{
    
    ArrayList<Address> neighbours;

    public StartMonitoring(ArrayList<Address> neighbours) {
        this.neighbours = neighbours;
    }

    public ArrayList<Address> getNeighbours() {
        return neighbours;
    }
    
    
}
