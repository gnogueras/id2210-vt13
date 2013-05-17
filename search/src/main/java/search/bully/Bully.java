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

/**
 *
 * @author Gerard
 */
public class Bully extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(Bully.class);
    Negative<BullyPort> bullyPort = negative(BullyPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    ArrayList<Address> neighbors;
    Address self;
    int delay;
    boolean gotAnswer;
    UUID coordinatorTimeoutId, answerTimeoutId;

    public Bully() {
        //subscribe
        subscribe(handleInit, control);
        subscribe(handleStart, control);
        subscribe(handleNewInstance, bullyPort);
        subscribe(handleElection, networkPort);
        subscribe(handleAnswer, networkPort);
        subscribe(handleCoordinator, networkPort);
        subscribe(handleNoCoordinationTimeout, timerPort);
        subscribe(handleNoAnswerTimeout, timerPort);

        //subscribe(handleCrash, pfdPort);
    }
    Handler<BullyInit> handleInit = new Handler<BullyInit>() {
        public void handle(BullyInit event) {
            //Inicialization of the Delay
            //Delay: timeout for NoCoordination 
            self = event.getSelf();
            delay = 5000;
            gotAnswer = false;
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
        }
    };
    Handler<NewInstance> handleNewInstance = new Handler<NewInstance>() {
        @Override
        public void handle(NewInstance event) {

            neighbors = event.getNeighbors();
            logger.info("Node {} got a select new leader request from instance {}",
                    self.getId(), event.getInstance());
            ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(neighbors);
            if (higherIdNeighbors.isEmpty()) {
                //trigger COORDINATOR
                broadcastCoordinator(neighbors, event.getInstance());
                trigger(new NewLeaderFromBully(event.getInstance(), self), bullyPort);
            } else {
                broadcastElection(higherIdNeighbors, event.getInstance());
                //trigger timeout
                ScheduleTimeout stAnswer = new ScheduleTimeout(delay);
                stAnswer.setTimeoutEvent(new NoAnswerTimeout(stAnswer, event.getInstance()));
                answerTimeoutId = stAnswer.getTimeoutEvent().getTimeoutId();
                trigger(stAnswer, timerPort);
            }
        }
    };
    Handler<Election> handleElection = new Handler<Election>() {
        @Override
        public void handle(Election event) {
            // For consistency, when comparing: first self.getPeerId, second BigInteger to compare with
            // Send reply if self has higher id than proposed leader
            logger.info("ELECTION: Node {} got a Election from node {}",
                    self.getId(), event.getSource().getId());
            if (self.getId() > event.getSource().getId()) {
                trigger(new Answer(self, event.getSource(), event.getInstance()), networkPort);
            }
        }
    };
    Handler<Answer> handleAnswer = new Handler<Answer>() {
        public void handle(Answer event) {
            //Wait until a coordinator message is received.
            //If no coordination message before timeout expiration,
            //resent Election message.
            logger.info("ANSWER: Node {} got a Answer from node {}",
                    self.getId(), event.getSource().getId());
            if (gotAnswer == false) {
                gotAnswer = true;
                trigger(new CancelTimeout(answerTimeoutId), timerPort);
                ScheduleTimeout stCoordination = new ScheduleTimeout(delay);
                stCoordination.setTimeoutEvent(new NoCoordinationTimeout(stCoordination, event.getInstance()));
                coordinatorTimeoutId = stCoordination.getTimeoutEvent().getTimeoutId();
                trigger(stCoordination, timerPort);
            }
        }
    };
    Handler<NoCoordinationTimeout> handleNoCoordinationTimeout = new Handler<NoCoordinationTimeout>() {
        public void handle(NoCoordinationTimeout event) {
            //If no Coordinator message, resend the Election message again
            logger.info("TIMEOUT COORDINATION EXPIRATION: Node {}",
                    self.getId());
            ArrayList<Address> higherIdNeighbors = selectHigherIdNeighbors(neighbors);
            broadcastElection(higherIdNeighbors, event.getInstance());
            //trigger timeout
            ScheduleTimeout stAnswer = new ScheduleTimeout(delay);
            stAnswer.setTimeoutEvent(new NoAnswerTimeout(stAnswer, event.getInstance()));
            answerTimeoutId = stAnswer.getTimeoutEvent().getTimeoutId();
            trigger(stAnswer, timerPort);
        }
    };
    Handler<NoAnswerTimeout> handleNoAnswerTimeout = new Handler<NoAnswerTimeout>() {
        public void handle(NoAnswerTimeout event) {
            //If no Coordinator message, resend the Election message again
            logger.info("TIMEOUT ANSWER EXPIRATION: Node {}", self.getId());
            broadcastCoordinator(neighbors, event.getInstance());
            // inform own instance that it's the leader
            trigger(new NewLeaderFromBully(event.getInstance(), self), bullyPort);
        }
    };
    Handler<Coordinator> handleCoordinator = new Handler<Coordinator>() {
        public void handle(Coordinator event) {
            //Abort other processing:sending election or answer..
            logger.info("COORDINATOR: Node {} got a Coordinator from node {}",
                    self.getId(), event.getSource().getId());
            //Coordinator received. Cancel the timeout
            trigger(new CancelTimeout(coordinatorTimeoutId), timerPort);
            //Trigger new NewLeaderFromBully event
            trigger(new NewLeaderFromBully(event.getInstance(), event.getSource()), bullyPort);
        }
    };

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

    //Broadcast message m to the set of peers
    private void broadcastElection(ArrayList<Address> peers, int instance) {
        for (Address peer : peers) {
            trigger(new Election(self, peer, instance), networkPort);
        }
    }
    //Broadcast message m to the set of peers

    private void broadcastCoordinator(ArrayList<Address> peers, int instance) {
        for (Address peer : peers) {
            trigger(new Coordinator(self, peer, instance), networkPort);
        }
    }
}
