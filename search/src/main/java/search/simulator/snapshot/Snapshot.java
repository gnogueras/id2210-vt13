package search.simulator.snapshot;

import common.simulation.scenarios.Scenario1;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
    static TreeSet timerSet;
    static TreeSet idSet;
    static int timerSetCounter = 0;
    static int bullyMessageCounter = 0;
    static int findLeaderCounter=0;
    static int leaderSearchCounter = 0;
    static HashMap<Integer, TreeSet> indexEntry = new HashMap<Integer, TreeSet>();
    static HashMap<Integer, TreeSet> indexTimer = new HashMap<Integer, TreeSet>();
    static HashMap<String, Integer> leaderSearchKV = new HashMap<String, Integer>();
    static Scenario1 scenario = new Scenario1();
    static ArrayList<Long> latencyList = new ArrayList<Long>();
    //static Map Map.Entry<Long,TreeSet> entry; // = new AbstractMap.SimpleEntry<Long, TreeSet>(id, idSet);

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
    public static void updateByllyCounter() {
        bullyMessageCounter++;
    }

    //-------------------------------------------------------------------
    public static void updateLeaderSearchCounter(String textEntry) {
        String str = new String();
        if(!leaderSearchKV.containsKey(textEntry)){
             str += "Creating leader search counter\n";
            findLeaderCounter = 0;
            leaderSearchKV.put(textEntry, findLeaderCounter++);
        }else{
            findLeaderCounter = leaderSearchKV.get(textEntry);
            str += "Leader Search counter exists = " + findLeaderCounter +"\n";
            leaderSearchKV.put(textEntry, findLeaderCounter+1);
        }
              System.out.println(str);
    }

//-------------------------------------------------------------------
    public static void updateTimerSet(int indexUpdate, long id, long timestamp) {
        String str = new String();

        if(!indexEntry.containsKey(indexUpdate)){
            idSet = new TreeSet();
        }else{
            idSet = indexEntry.get(indexUpdate);
        }
        idSet.add(id);
        indexEntry.put(indexUpdate, idSet);

        if(!indexTimer.containsKey(indexUpdate)){
            timerSet = new TreeSet();
        }else{
            timerSet = indexTimer.get(indexUpdate);
        }
        timerSet.add(timestamp);
        indexTimer.put(indexUpdate, timerSet);

        str += "";
        if (idSet.size() == scenario.PeersAdded()){
            str += "---------------------------------------------------------------------\n";
            str += "LATENCY STATISTICS\n";
            str += "PropagationLatency for id: "+ indexUpdate +" is "+ ((Long)timerSet.last()-(Long)timerSet.first()) + "\n";
            latencyList.add(((Long)timerSet.last()-(Long)timerSet.first()));
            str += "Latency List size " + latencyList.size() + "\n;";
            str += "---------------------------------------------------------------------\n";
            str += "Total propagation latency----------------------- \n";
            str += latencyList + "\n";
            str += "Average propagation latency--------------------- \n";
            long m=0;
            for(long i:latencyList){
                m+=i;
                }
            str += m/latencyList.size() + ";";
            idSet.clear();
        }else{
            str += "IdSetSize: " + idSet.size() + "\n";
        }

         if (latencyList.size() == scenario.IndexEntriesAdded()){
            str += "Total propagation latency----------------------- \n";
            str += latencyList + "\n";
            str += "Average propagation latency--------------------- \n";
            long m=0;
            for(long i:latencyList){
                m+=i;
                }
            str += m/latencyList.size() + ";";
         }
        System.out.println(str);
    }
    




   //-------------------------------------------------------------------
    public static void printLeaderSearchCounterStats(String textEntry) {
        String str = new String();
        str += "---------------------------------------------------------------------\n";
        str += "FIND LEADER STATISTICS\n";
        int leaderCount = 0;
        if(leaderSearchKV.containsKey(textEntry)){
            leaderCount = leaderSearchKV.get(textEntry);
        }
        leaderCount++;
        str += "Messages to find leader: " + leaderCount + "\n";
        str += "---------------------------------------------------------------------\n";
        System.out.println(str);
    }

    //-------------------------------------------------------------------
    public static void reportBullyCounter(long id, long leaderId) {
        String str = new String();
        str += "---------------------------------------------------------------------\n";
        str += "LEADER SELECTION STATISTICS\n";
        str += "Messages to select leader: " + bullyMessageCounter + "\n";
        str += "Selected leader: " + leaderId + "\n";
        str += "Reporting node: " + id + "\n";
        str += "---------------------------------------------------------------------\n";
        System.out.println(str);
    }



    //-------------------------------------------------------------------
    public static void reportConvergenceNumber(int messages) {
        String str = new String();
        int totalNumOfPeers = peers.size();
        str += "---------------------------------------------------------------------\n";
        str += "Convergence STATISTICS\n";
        str += "MESSAGES TO CONVERGE: " + messages + "\n";
        str += "total number of peers: " + totalNumOfPeers + "\n";
        str += "Peers: " + getOrderPeers() + "\n";
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
        //int totalNumOfPeers = peers.size();
        //str += "total number of peers: " + totalNumOfPeers + "\n";
        //str += "Peers: " + getOrderPeers() + "\n";
        //str += "STATISTICS\n";
        
        return str;
    }


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
        //str += "Peer with max num of index entries: " + maxNumIndexEntries + "\n";
        //str += "Peer with min num of index entries: " + minNumIndexEntries + "\n";

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
