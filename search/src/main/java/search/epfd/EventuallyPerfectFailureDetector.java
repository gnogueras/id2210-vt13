/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;

/**
 *
 * @author Gerard
 */
public class EventuallyPerfectFailureDetector extends ComponentDefinition {

    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<EPFDPort> epfdPort = negative(EPFDPort.class);
    Address self;
    Set<Address> neighbours, alive, suspected;
    int heartbeatPeriod, checkPeriod, increment;
    boolean crashSimulation;

    public EventuallyPerfectFailureDetector() {
        subscribe(handleInit, control);
        subscribe(handleStart, control);
        subscribe(handleHeartbeatTimeout, timerPort);
        subscribe(handleCheckTimeout, timerPort);
        subscribe(handleHeartbeat, networkPort);
        subscribe(handleStartMonitoring, epfdPort);
        subscribe(handleCrashSimulation, epfdPort);
    }
    Handler<EPFDInit> handleInit = new Handler<EPFDInit>() {
        public void handle(EPFDInit event) {
            self = event.getSelf();
            heartbeatPeriod = event.getHeartbeatPeriod();
            checkPeriod = event.getHeartbeatPeriod() + increment;
            increment = event.getIncrement();
            alive = new HashSet<Address>();
            suspected = new HashSet<Address>();
            neighbours = new HashSet<Address>();
            crashSimulation = false;
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {

        }
    };
    Handler<StartMonitoring> handleStartMonitoring = new Handler<StartMonitoring>() {
        public void handle(StartMonitoring event) {
            neighbours.addAll(event.getNeighbours());

            System.out.println("\n****** " + self.getId() + " - EPFD Started monitoring: " + neighbours
                    + " \n alive: " + alive);
            //Broadcast heartbeat and trigger heartbeatTimeout event
            for (Address peer : neighbours) {
                trigger(new Heartbeat(self, peer), networkPort);
            }
            startHeartbeatTimeout(heartbeatPeriod);
            
            //Trigger checkTimeout event
            startCheckTimeout(checkPeriod);
        }
    };
    Handler<CheckTimeout> handleCheckTimeout = new Handler<CheckTimeout>() {
        public void handle(CheckTimeout event) {
            if (intersection(alive, suspected)) {
                checkPeriod += increment;
            }
            System.out.println("\n****** " + self.getId() + " - ALIVE: " + alive + "\nSUSPECT: " + suspected);
            for (Address peer : neighbours) {
                if (!alive.contains(peer) && !suspected.contains(peer)) {
                    suspected.add(peer);
                    trigger(new Suspect(peer), epfdPort);
                } else if (alive.contains(peer) && suspected.contains(peer)) {
                    suspected.remove(peer);
                    trigger(new Restore(peer), epfdPort);
                }
            }
            alive.clear();
            startCheckTimeout(checkPeriod);
        }
    };
    Handler<HeartbeatTimeout> handleHeartbeatTimeout = new Handler<HeartbeatTimeout>() {
        public void handle(HeartbeatTimeout event) {
            if (!crashSimulation) {
                for (Address peer : neighbours) {
                    trigger(new Heartbeat(self, peer), networkPort);
                }
                startHeartbeatTimeout(heartbeatPeriod);
            }
        }
    };
    Handler<Heartbeat> handleHeartbeat = new Handler<Heartbeat>() {
        public void handle(Heartbeat event) {
            alive.add(event.getSource());
            System.out.println("\n-- " + self.getId() + " - HEARTBEAT RECEIVED from "+event.getSource().getId()+" alive: "+alive);
            /*if(!crashSimulation){
                trigger(new Heartbeat(self, event.getSource()), networkPort);
            }*/
        }
    };
    Handler<CrashSimulation> handleCrashSimulation = new Handler<CrashSimulation>() {
        public void handle(CrashSimulation event) {
            System.out.println("\n****** " + self.getId() + " - CRASH SIMULATION SET TO TRUE");
            crashSimulation = true;
        }
    };

    private void startHeartbeatTimeout(int delay) {
        //Trigger event HeartbeaTimeout
        ScheduleTimeout stHeartbeat = new ScheduleTimeout(delay);
        stHeartbeat.setTimeoutEvent(new HeartbeatTimeout(stHeartbeat));
        trigger(stHeartbeat, timerPort);
    }

    private void startCheckTimeout(int delay) {
        //Trigger event CheckTimeout
        ScheduleTimeout stCheck = new ScheduleTimeout(delay);
        stCheck.setTimeoutEvent(new CheckTimeout(stCheck));
        trigger(stCheck, timerPort);
    }

    /**
     * Check if there exists intersection of two list
     *
     * @param s1
     * @param s2
     * @return true if intersection exists, false otherwise
     */
    private boolean intersection(Set s1, Set s2) {
        Set<Address> intersection = new HashSet<Address>(s1); 
        intersection.retainAll(s2);
        return !intersection.isEmpty();
    }
}
