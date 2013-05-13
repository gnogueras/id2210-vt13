/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import java.util.ArrayList;
import java.util.List;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
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
    List<Address> neighbours, alive, suspected;
    int heartbeatPeriod, checkPeriod, increment;

    public EventuallyPerfectFailureDetector() {
        subscribe(handleInit, control);
        subscribe(handleStart, control);
        subscribe(handleHeartbeatTimeout, timerPort);
        subscribe(handleCheckTimeout, timerPort);
        subscribe(handleHeartbeat, networkPort);
    }
    
    Handler<EPFDInit> handleInit = new Handler<EPFDInit>() {
        public void handle(EPFDInit event) {
            self = event.getSelf();
            neighbours = event.getNeighbours();
            heartbeatPeriod = event.getHeartbeatPeriod();
            checkPeriod = event.getHeartbeatPeriod();
            increment = event.getIncrement();
            alive = new ArrayList<Address>();
            suspected = new ArrayList<Address>();
            
            //Trigger heartbeatTimeout event
            startHeartbeatTimeout(heartbeatPeriod);

            //Trigger checkTimeout event
            startCheckTimeout(checkPeriod);
        }
    };
    
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
        }
    };
    
    Handler<CheckTimeout> handleCheckTimeout = new Handler<CheckTimeout>() {
        public void handle(CheckTimeout event) {
            if(intersection(alive, suspected)){
                checkPeriod+=increment;
            }
            for(Address peer : neighbours){
                if(!alive.contains(peer) && !suspected.contains(peer)){
                    suspected.add(peer);
                    trigger(new Suspect(peer), epfdPort);
                }
                else if(alive.contains(peer) && suspected.contains(peer)){
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
            for(Address peer : neighbours){
                trigger(new Heartbeat(self, peer), networkPort);
            }
            startHeartbeatTimeout(heartbeatPeriod);
        }
    };
    
    Handler<Heartbeat> handleHeartbeat = new Handler<Heartbeat>() {
        public void handle(Heartbeat event) {
            alive.add(event.getSource());
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
     * @param l1
     * @param l2
     * @return true if intersection exists, false otherwise
     */
    private boolean intersection (List l1, List l2){
        List intersection = l1;
        intersection.retainAll(l2);
        return !intersection.isEmpty();
    }
}
