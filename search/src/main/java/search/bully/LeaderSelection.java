/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import common.peer.PeerAddress;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import search.epfd.EPFDPort;
import search.epfd.StartMonitoring;
import search.epfd.Suspect;
import search.system.peer.search.Search;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * @author Gerard
 */
public class LeaderSelection extends ComponentDefinition {
    private static final Logger logger = LoggerFactory.getLogger(Search.class);
    Positive<TManSamplePort> tmanPort = requires(TManSamplePort.class);
    Positive<BullyPort> bullyPort = requires(BullyPort.class);
    Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
    Negative<LeaderSelectionPort> leaderSelectionPort = negative(LeaderSelectionPort.class);
    Address self;
    static Address leader = null;
    ArrayList<Address> tmanPartners, previousPartners;
    final int CONVERGENCE_THRESHOLD = 15;
    int convergenceCounter;
    int instance=0;
    int numberOfComparedPeers;
    int instanceRunning = 0;

    public LeaderSelection() {
        super();
        subscribe(initHandler, control);
        subscribe(handleStart, control);
        subscribe(handleTManSample, tmanPort);
        subscribe(handleNewLeaderFromBully, bullyPort);
        subscribe(handleSuspect, epfdPort);       
    }
    Handler<LeaderSelectionInit> initHandler = new Handler<LeaderSelectionInit>() {
        public void handle(LeaderSelectionInit event) {
            self = event.getSelf();
            convergenceCounter = 0;
            tmanPartners = new ArrayList<Address>();
            previousPartners = new ArrayList<Address>();
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
            System.out.println("Leader Selection component started.");
        }
    };

    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        public void handle(TManSample event) {
            previousPartners.clear();
            previousPartners.addAll(tmanPartners);
            tmanPartners = event.getSample();
            //TMan converged?
            numberOfComparedPeers = Math.min(previousPartners.size(), tmanPartners.size())-1;
            logger.info("\n****** "+self.getId() + " - TMANPARTNERS MONITORING = {} \n NUMBER OF PEERS={} \n", tmanPartners, numberOfComparedPeers);
            if (compareFirstNElements(tmanPartners, previousPartners, numberOfComparedPeers)) {
                convergenceCounter++;
            } else {
                convergenceCounter = 0;
            }
            logger.info("\n****** "+self.getId() + " - COUNTER = {}  \n InstanceRunning={} \n", convergenceCounter, instanceRunning);
            if (convergenceCounter == CONVERGENCE_THRESHOLD && leader == null && instanceRunning==0) {
                ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(tmanPartners);
                if (higherIdNeighbors.isEmpty()) {
                    trigger(new StartMonitoring(tmanPartners), epfdPort);
                    trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                    instanceRunning = 1;
                    instance++;
                    logger.info("\n****** "+self.getId() + " - INSTANCENUMBER = {} \n", instance);
                }
            }
        }
    };
    Handler<NewLeaderFromBully> handleNewLeaderFromBully = new Handler<NewLeaderFromBully>() {
        public void handle(NewLeaderFromBully event) {
            leader = event.getLeader();
            logger.info("\n****** "+self.getId() + " - REPORTED LEADER = {} \n", leader);
            instanceRunning = 0;
            trigger(new CurrentLeaderEvent(event.getInstance(), leader), leaderSelectionPort);
        }
    };
    Handler<Suspect> handleSuspect = new Handler<Suspect>() {
        public void handle(Suspect event) {
            logger.info("\n****** "+self.getId() + " - SUSPECTING LEADER FAILED \n", leader);
            if (leader != null) {
                if (event.getSuspected().getId() == leader.getId()) {
                    trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                }
            }
        }
    };
    
    private boolean compareFirstNElements(ArrayList<Address> list1, ArrayList<Address> list2, int n){     
        if(list1.isEmpty() && list2.size()>0){
            return false;
        }
        if(list1.size()>0 && list2.isEmpty()){
            return false;
        }
        
        for(int i=0; i<n; i++){
            if(list1.get(i).getId()!=list2.get(i).getId()){
                return false;
            }
        }
        return true;
    }

        //Select peers from the neighbor list with higher id
    //CHANGES: change the name to selectHigherIdNeighbors
    // -1 instead of 1
    // For consistency, when comparing: first self.getPeerId, second BigInteger to compare with
    private ArrayList<Address> selectHigherIdNeighbors(ArrayList<Address> neighbors) {
        ArrayList<Address> higherIdNeighbors = new ArrayList<Address>();
        for (Address peer : neighbors) {
            // Add peer to higherIdNeighbors list if self.getId > peer.getId
            if (self.getId() < peer.getId()) {
                higherIdNeighbors.add(peer);
            }
        }
        return higherIdNeighbors;
    }


}
