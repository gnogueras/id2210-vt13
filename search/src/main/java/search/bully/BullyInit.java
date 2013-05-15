package search.bully;

import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public class BullyInit extends Init {

    Address self;
    
    public BullyInit(Address self) {
        super();
        this.self = self;
    }

    public Address getSelf() {
        return self;
    }
    
    
}

