package search.bully;

import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;

public class BullyInit extends Init {

    private final ArrayList<Address> neighbors;
    private final Address self;

    public BullyInit(List<Address> neighbors, Address self) {
        this.neighbors = (ArrayList<Address>) neighbors;
        this.self = self;
    }

    public ArrayList<Address> getNeighbors() {
        return neighbors;
    }

    public Address getSelf() {
        return self;
    }

   
}

