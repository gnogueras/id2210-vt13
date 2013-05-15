package search.bully;

import java.util.Set;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public class LeaderSelectionInit extends Init {
    
    private Address self;

    public LeaderSelectionInit(Address self) {
        this.self = self;
    }

    public Address getSelf() {
        return self;
    }

    
}
