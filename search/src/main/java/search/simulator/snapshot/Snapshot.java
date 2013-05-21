package search.simulator.snapshot;

import java.util.ArrayList;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import se.sics.kompics.address.Address;

public class Snapshot {

    private static ConcurrentHashMap<Address, PeerInfo> peers = 
            new ConcurrentHashMap<Address, PeerInfo>();
    private static int counter = 0;
    private static String FILENAME = "search.out";
    static TreeSet timerSet = new TreeSet();
    static TreeSet idSet = new TreeSet();
    static int timerSetCounter = 0;
//-------------------------------------------------------------------
    public static void init(int numOfStripes) {
        FileIO.write("", FILENAME);
    }

//-------------------------------------------------------------------
    public static void addPeer(Address address) {
        peers.put(address, new PeerInfo());
    }

//-------------------------------------------------------------------
    public static void removePeer(Address address) {
        peers.remove(address);
    }

//-------------------------------------------------------------------
    public static void incNumIndexEntries(Address address) {
        PeerInfo peerInfo = peers.get(address);

        if (peerInfo == null) {
            return;
        }

        peerInfo.incNumIndexEntries();
    }

//-------------------------------------------------------------------
    public static void updateNeighbours(Address address, ArrayList<Address> partners) {
        PeerInfo peerInfo = peers.get(address);

        if (peerInfo == null) {
            return;
        }

        peerInfo.setNeighbours(partners);
    }


//-------------------------------------------------------------------
    public static void updateTimerSet(long id, long timestamp) {
        idSet.add(id);
        timerSet.add(timestamp);
        String str = new String();
        str += "";
        if (idSet.size() == 50){
            str += "---------------------------------------------------------------------\n";
            str += "PropagationLatency: " + ((Long)timerSet.last()-(Long)timerSet.first()) + "\n";
            idSet.clear();
        }else{
            str += "IdSetSize: " + idSet.size() + "\n";
        }
        str += "LATENCY STATISTICS\n";
        str += "---------------------------------------------------------------------\n";
        System.out.println(str);
    }



//-------------------------------------------------------------------
    public static void report() {
        String str = new String();
        str += "current time: " + counter++ + "\n";
        str += reportNetworkState();
        str += reportDetails();
        str += "###\n";

        System.out.println(str);
        FileIO.append(str, FILENAME);
    }

//-------------------------------------------------------------------
    private static String reportNetworkState() {
        String str = "---\n";
        int totalNumOfPeers = peers.size();
        str += "total number of peers: " + totalNumOfPeers + "\n";
        str += "Peers: " + getOrderPeers() + "\n";
        str += "STATISTICS\n";
        
        return str;
    }



    //-------------------------------------------------------------------
    private static String reportPropagationLatency() {

        timerSet.add(System.currentTimeMillis());
        timerSetCounter++;
        if (timerSetCounter == 50){
        timerSetCounter =0;
        timerSet.clear();
        }
        String str = "---\n";
        int totalNumOfPeers = peers.size();
        str += "total number of peers: " + totalNumOfPeers + "\n";
        str += "Peers: " + getOrderPeers() + "\n";
        str += "STATISTICS\n";

        return str;
    }

    //logger.info(self.getId() + " - updating the index entry timetSetCounter={} at TIME={}", timerSetCounter, System.currentTimeMillis());
    //logger.info(self.getId() + " - First to last latency={}", ((Long)timerSet.last() - (Long)timerSet.first()));
        

//-------------------------------------------------------------------
    private static String reportDetails() {
        String str = "---\n";
        int maxNumIndexEntries = 0;
        int minNumIndexEntries = Integer.MAX_VALUE;
        for (PeerInfo p : peers.values()) {
            if (p.getNumIndexEntries() < minNumIndexEntries) {
                minNumIndexEntries = p.getNumIndexEntries();
            }
            if (p.getNumIndexEntries() > maxNumIndexEntries) {
                maxNumIndexEntries = p.getNumIndexEntries();
            }
        }
        str += "Peer with max num of index entries: " + maxNumIndexEntries + "\n";
        str += "Peer with min num of index entries: " + minNumIndexEntries + "\n";

        return str;
    }
    
    private static Set<Integer> getOrderPeers(){
        SortedSet<Integer> set = new TreeSet<Integer>(); 
        for(Address p : peers.keySet()){
            set.add(p.getId());
        }
        return set;
    }
    
}
