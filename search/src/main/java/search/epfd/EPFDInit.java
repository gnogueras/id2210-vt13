/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import java.util.List;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

/**
 *
 * @author Gerard
 */
public class EPFDInit extends Init{
    
    Address self;
    int heartbeatPeriod, increment;

    public EPFDInit(Address self, int heartbeatPeriod, int increment) {
        this.self = self;
        this.heartbeatPeriod = heartbeatPeriod;
        this.increment = increment;
    }

    public Address getSelf() {
        return self;
    }

    public int getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public int getIncrement() {
        return increment;
    }
    
}
