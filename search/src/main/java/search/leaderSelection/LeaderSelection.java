/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.leaderSelection;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import search.bully.BullyPort;
import search.bully.NewInstance;
import search.bully.NewLeaderFromBully;
import search.epfd.CrashSimulation;
import search.epfd.EPFDPort;
import search.epfd.StartMonitoring;
import search.epfd.Suspect;
import search.simulator.snapshot.Snapshot;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * @author Gerard
 */
public class LeaderSelection extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(LeaderSelection.class);
    Positive<TManSamplePort> tmanPort = requires(TManSamplePort.class);
    Positive<BullyPort> bullyPort = requires(BullyPort.class);
    Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<LeaderSelectionPort> leaderSelectionPort = negative(LeaderSelectionPort.class);
    Address self;
    static Address leader = null;
    ArrayList<Address> tmanPartners, previousPartners;
    final int CONVERGENCE_THRESHOLD = 10;
    final int SAFETY_SET = 2;
    int convergenceCounter, absoluteCounter;
    int instance = 0;
    int NUMBER_OF_COMPARED_PEERS;
    int instanceRunning = 0;

    public LeaderSelection() {
        super();
        subscribe(initHandler, control);
        subscribe(handleStart, control);
        subscribe(handleTManSample, tmanPort);
        subscribe(handleNewLeaderFromBully, bullyPort);
        subscribe(handleSuspect, epfdPort);
        subscribe(handleCrashSimulationTimeout, timerPort);
    }
    Handler<LeaderSelectionInit> initHandler = new Handler<LeaderSelectionInit>() {
        public void handle(LeaderSelectionInit event) {
            self = event.getSelf();
            convergenceCounter = 0;
            absoluteCounter = 0;
            tmanPartners = new ArrayList<Address>();
            previousPartners = new ArrayList<Address>();
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
        }
    };
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        public void handle(TManSample event) {
            //Keep a previous list of partners to check Tman convergence
            previousPartners.clear();
            previousPartners.addAll(tmanPartners);
            //Update current list of partners
            tmanPartners.clear();
            tmanPartners.addAll(event.getSample());
            //TMan converged?
            //Get the minimum size between the previous list and the current list
            NUMBER_OF_COMPARED_PEERS = Math.min(previousPartners.size(), tmanPartners.size()) - 1;
            
            if (compareFirstNElements(tmanPartners, previousPartners, NUMBER_OF_COMPARED_PEERS)) {
                convergenceCounter++;
            } else {
                convergenceCounter = 0;
            }
            absoluteCounter++;
            
            if (convergenceCounter == CONVERGENCE_THRESHOLD && leader == null && instanceRunning == 0) {
                //If the Gradient is converged
                ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(tmanPartners);
                //logger.info("$$$$$ - " + self.getId() + "  CONVERGED.\n");    
                
                //If we are converged and we do not have higher id neighbors, we are probably the leader
                if (higherIdNeighbors.isEmpty()) {
                    Snapshot.reportConvergenceNumber(absoluteCounter);
                    //Trigger new instance of Bully
                    trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                    instanceRunning = 1;
                    instance++;
                    
                }/*else{
                    convergenceCounter=convergenceCounter/2;
                }*/
                //Start the EPFD to monitor my neighbors
                trigger(new StartMonitoring(tmanPartners), epfdPort);
            }
        }
    };
    
    //The Bully algorithm has elected a new leader
    Handler<NewLeaderFromBully> handleNewLeaderFromBully = new Handler<NewLeaderFromBully>() {
        public void handle(NewLeaderFromBully event) {
            leader = event.getLeader();
            //logger.info("****** " + self.getId() + " - REPORTED LEADER = {} \n", leader);
            instanceRunning = 0;
            trigger(new CurrentLeaderEvent(event.getInstance(), leader), leaderSelectionPort);
            
            //In case we are checking the leader crashed, we trigger a timeout to start a crash simulation of the leader
            //The leader will stop sending Heartbeat messages, so it will be detect as crashed
            /*if (leader.getId() == self.getId()) {
                //trigger timeout for crash simulation of the leader
                ScheduleTimeout stCrash = new ScheduleTimeout(10000);
                stCrash.setTimeoutEvent(new CrashSimulationTimeout(stCrash));
                trigger(stCrash, timerPort);
            }*/
        }
    };
    
    //Leader is going to crash (Simulation)
    Handler<CrashSimulationTimeout> handleCrashSimulationTimeout = new Handler<CrashSimulationTimeout>() {
        public void handle(CrashSimulationTimeout event) {
            //logger.info("****** " + self.getId() + " - I AM GOING TO FAIL");
            Snapshot.resetByllyCounter();
            //Trigger the crash simulation event to the EPFD port
            trigger(new CrashSimulation(), epfdPort);
        }
    };
    
    //Suspect event from the EPFD
    Handler<Suspect> handleSuspect = new Handler<Suspect>() {
        public void handle(Suspect event) {
            if (leader != null) {
                //If the suspected node is the leader, start a new Bully instance to elect a new leader
                if (event.getSuspected().getId() == leader.getId()) {
                    //logger.info("****** " + self.getId() + " - SUSPECTING LEADER FAILED: "+leader.getId());
                    
                    leader=null;
                    ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(tmanPartners);
                    
                    //If the number of higher id neighbors is less than SAFETY_SET, I wil start a new Bully instance
                    if (higherIdNeighbors.size() <= SAFETY_SET) {
                        //trigger(new UpdatePartnersMonitoring(tmanPartners), epfdPort);
                        trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                        instanceRunning = 1;
                        instance++;
                    }
                    //trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                }
            }
        }
    };

    //Function to compare the first N elements of the two lists
    private boolean compareFirstNElements(ArrayList<Address> list1, ArrayList<Address> list2, int n) {
        if (list1.isEmpty() && list2.size() > 0) {
            return false;
        }
        if (list1.size() > 0 && list2.isEmpty()) {
            return false;
        }

        for (int i = 0; i < n; i++) {
            if (list1.get(i).getId() != list2.get(i).getId()) {
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
