package search.bully;

import java.util.Set;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public class LeaderSelectionInit extends Init {
    
    private Address self;
    private Set<Address> neighborSet;

    public LeaderSelectionInit(Address self, Set<Address> neighborSet) {
        this.neighborSet = neighborSet;
        this.self = self;
    }

    public Address getSelf() {
        return self;
    }

    public Set<Address> getNeighborSet() {
        return neighborSet;
    }
    
}
