/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import java.util.ArrayList;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import search.epfd.Suspect;
import search.epfd.EPFDPort;


/**
 *
 * @author Gerard
 */
public class Bully extends ComponentDefinition {
    private static final Logger logger = LoggerFactory.getLogger(Bully.class);
    Negative<BullyPort> bullyPort = negative(BullyPort.class);
    Positive<EPFDPort> epfdPort = positive(EPFDPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    ArrayList<Address> neighbors;
    Address self;
    int delay;
    UUID timeoutId;


    public Bully() {
        //subscribe
        subscribe(handleInit, control);
        subscribe(handleStart, control);
        subscribe(handleNewInstance, bullyPort);
        subscribe(handleElection, networkPort);
        subscribe(handleAnswer, networkPort);
        subscribe(handleCoordinator, networkPort);        
        //subscribe(handleCrash, pfdPort);
    }
    Handler<BullyInit> handleInit = new Handler<BullyInit>() {
        public void handle(BullyInit event) {
            //Inicialization of the Delay
            //Delay: timeout for NoCoordination 
            delay = 5000;
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {           
        }
    };
    
    Handler<NewInstance> handleNewInstance = new Handler<NewInstance>() {
        @Override
        public void handle(NewInstance event) {
            logger.info("Node {} got a select new leader request from instance {}",
                    self.getId(), event.getInstance());
            ArrayList<Address> lowerIdNeighbors = selectLowerIdNeighbors(neighbors);
            broadcastElection(lowerIdNeighbors, event.getInstance());
        }
    };
    Handler<Election> handleElection = new Handler<Election>() {
        @Override
        public void handle(Election event) {
        // For consistency, when comparing: first self.getPeerId, second BigInteger to compare with
        // Send reply if self has lower id than proposed leader
            if (self.getId() < event.getSource().getId()) {
                trigger(new Answer(self, event.getSource(), event.getInstance()), networkPort);
            }
        }
    };
    Handler<Answer> handleAnswer = new Handler<Answer>() {
        public void handle(Answer event) {
            //Wait until a coordinator message is received.
            //If no coordination message before timeout expiration,
            //resent Election message.
            ScheduleTimeout stCoordination = new ScheduleTimeout(delay);
            stCoordination.setTimeoutEvent(new NoCoordinationTimeout(stCoordination, event.getInstance()));
            timeoutId = stCoordination.getTimeoutEvent().getTimeoutId();
            trigger(stCoordination, timerPort);
        }
    };
    Handler<NoCoordinationTimeout> handleNoCoordinationTimeout = new Handler<NoCoordinationTimeout>() {
        public void handle(NoCoordinationTimeout event) {
            //If no Coordinator message, resend the Election message again
            ArrayList<Address> lowerIdNeighbors = selectLowerIdNeighbors(neighbors);
            broadcastElection(lowerIdNeighbors, event.getInstance());
        }
    };
    
    Handler<Coordinator> handleCoordinator = new Handler<Coordinator>() {
        public void handle(Coordinator event) {
            //Abort other processing:sending election or answer..
            //Coordinator received. Cancel the timeout
            trigger(new CancelTimeout(timeoutId), timerPort);
            //Trigger new StartLeaderSelectionEvent event
            trigger(new NewLeaderFromBully(event.getInstance(), event.getSource()), bullyPort);
        }
    };

    //Select peers from the neighbor list with lower id
    //CHANGES: change the name to selectLowerIdNeighbors
    // -1 instead of 1 
    // For consistency, when comparing: first self.getPeerId, second BigInteger to compare with
    private ArrayList<Address> selectLowerIdNeighbors(ArrayList<Address> neighbors) {
        ArrayList<Address> lowerIdNeighbors = new ArrayList<Address>();
        for (Address peer : neighbors) {
        // Add peer to lowerIdNeighbors list if self.getId > peer.getId
            if (self.getId() > peer.getId()) {
                lowerIdNeighbors.add(peer);
            }
        }
        return lowerIdNeighbors;
    }

    //Broadcast message m to the set of peers
    private void broadcastElection(ArrayList<Address> peers, int instance) {
        for (Address peer : peers) {
            trigger(new Election(self, peer, instance), networkPort);
        }
    }
}
