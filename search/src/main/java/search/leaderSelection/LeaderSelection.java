/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.leaderSelection;

import common.peer.PeerAddress;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
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
import search.system.peer.search.Search;
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
    final int CONVERGENCE_THRESHOLD = 20;
    final int SAFETY_SET = 2;
    int convergenceCounter, absolutCounter;
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
            absolutCounter = 0;
            tmanPartners = new ArrayList<Address>();
            previousPartners = new ArrayList<Address>();
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
            //System.out.println("Leader Selection component started.");
        }
    };
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        public void handle(TManSample event) {
            previousPartners.clear();
            previousPartners.addAll(tmanPartners);
            tmanPartners.clear();
            tmanPartners.addAll(event.getSample());
            //TMan converged?
            NUMBER_OF_COMPARED_PEERS = Math.min(previousPartners.size(), tmanPartners.size()) - 1;
            //logger.info("\n****** " + self.getId() + " - TMANPARTNERS MONITORING = {} \n NUMBER OF PEERS={} \n", tmanPartners, NUMBER_OF_COMPARED_PEERS);
            if (compareFirstNElements(tmanPartners, previousPartners, NUMBER_OF_COMPARED_PEERS)) {
                convergenceCounter++;
            } else {
                convergenceCounter = 0;
            }
            absolutCounter++;
            //logger.info("\n****** " + self.getId() + " - COUNTER = {}  \n InstanceRunning={} \n", convergenceCounter, instanceRunning);
            if (convergenceCounter == CONVERGENCE_THRESHOLD && leader == null && instanceRunning == 0) {
                logger.info("$$$$$ - " + self.getId() + " - ConvergenceCounter = {}  AbsoulteCounter={} $$$$$\n", convergenceCounter, absolutCounter);
                ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(tmanPartners);
                if (higherIdNeighbors.isEmpty()) {
                    trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                    instanceRunning = 1;
                    instance++;
                    logger.info("****** " + self.getId() + " - INSTANCENUMBER = {} \n", instance);
                }
                trigger(new StartMonitoring(tmanPartners), epfdPort);
            }
        }
    };
    Handler<NewLeaderFromBully> handleNewLeaderFromBully = new Handler<NewLeaderFromBully>() {
        public void handle(NewLeaderFromBully event) {
            leader = event.getLeader();
            logger.info("****** " + self.getId() + " - REPORTED LEADER = {} \n", leader);
            instanceRunning = 0;
            trigger(new CurrentLeaderEvent(event.getInstance(), leader), leaderSelectionPort);
            /*if (leader.getId() == self.getId()) {
                //trigger timeout for crash simulation of the leader
                ScheduleTimeout stCrash = new ScheduleTimeout(5000);
                stCrash.setTimeoutEvent(new CrashSimulationTimeout(stCrash));
                trigger(stCrash, timerPort);
            }*/
        }
    };
    Handler<CrashSimulationTimeout> handleCrashSimulationTimeout = new Handler<CrashSimulationTimeout>() {
        public void handle(CrashSimulationTimeout event) {
            logger.info("****** " + self.getId() + " - I AM GOING TO FAIL");
            trigger(new CrashSimulation(), epfdPort);
        }
    };
    Handler<Suspect> handleSuspect = new Handler<Suspect>() {
        public void handle(Suspect event) {
            if (leader != null) {
                if (event.getSuspected().getId() == leader.getId()) {
                    logger.info("****** " + self.getId() + " - SUSPECTING LEADER FAILED: "+leader.getId());
                    //Remove suspected peer (leader) from tmanPartners
                    tmanPartners.remove(leader);
                    leader=null;
                    ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(tmanPartners);
                    if (higherIdNeighbors.size() <= SAFETY_SET) {
                        //trigger(new UpdatePartnersMonitoring(tmanPartners), epfdPort);
                        trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                        instanceRunning = 1;
                        instance++;
                        logger.info("****** " + self.getId() + " - INSTANCENUMBER = {} \n", instance);
                    }
                    trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                }
            }
        }
    };

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
