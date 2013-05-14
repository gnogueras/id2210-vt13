/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import common.peer.PeerAddress;
import java.util.ArrayList;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;
import search.epfd.EPFDPort;
import search.epfd.Suspect;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

/**
 * @author Gerard
 */
public class LeaderSelection extends ComponentDefinition {

    Positive<TManSamplePort> tmanPort = requires(TManSamplePort.class);
    Positive<BullyPort> bullyPort = requires(BullyPort.class);
    Positive<EPFDPort> epfdPort = requires(EPFDPort.class);
    Negative<LeaderSelectionPort> leaderSelectionPort = negative(LeaderSelectionPort.class);
    Address self, leader;
    ArrayList<Address> tmanPartners, previousPartners;
    static final int CONVERGENCE_THRESHOLD = 10;
    int convergenceCounter;
    static int instance=0;

    public LeaderSelection() {
        super();

        subscribe(initHandler, control);
        subscribe(handleStart, control);

    }
    Handler<LeaderSelectionInit> initHandler = new Handler<LeaderSelectionInit>() {
        public void handle(LeaderSelectionInit event) {
            self = event.getSelf();
            leader = null;
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
    /*
     Handler<StartLeaderSelectionEvent> handleLeaderSelectedEvent = new Handler<StartLeaderSelectionEvent>() {
     public void handle(StartLeaderSelectionEvent event) {
     trigger(new NewInstance(self, event.getInstance(), tmanPartners), bullyPort);
     }
     };*/
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        public void handle(TManSample event) {

            previousPartners.clear();
            previousPartners.addAll(tmanPartners);
            tmanPartners = event.getSample();
            //TMan converged?
            if (tmanPartners.containsAll(previousPartners)) {
                convergenceCounter++;
            } else {
                convergenceCounter = 0;
            }

            if (convergenceCounter == CONVERGENCE_THRESHOLD && leader == null) {
                trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
            }
        }
    };
    Handler<NewLeaderFromBully> handleNewLeaderFromBully = new Handler<NewLeaderFromBully>() {
        public void handle(NewLeaderFromBully event) {
            leader = event.getLeader();
            instance++;
            trigger(new CurrentLeaderEvent(event.getInstance(), leader), leaderSelectionPort);
        }
    };
    Handler<Suspect> handleSuspect = new Handler<Suspect>() {
        public void handle(Suspect event) {
            if (leader != null) {
                if (event.getSuspected().getId() == leader.getId()) {
                    trigger(new NewInstance(self, instance, tmanPartners), bullyPort);
                }
            }
        }
    };
}
