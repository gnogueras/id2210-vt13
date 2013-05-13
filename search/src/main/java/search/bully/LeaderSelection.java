/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import common.peer.PeerAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.address.Address;


/**
 * @author Gerard
 */
public class LeaderSelection extends ComponentDefinition {

    Positive<BullyPort> bullyPort = requires(BullyPort.class);
    Negative<LeaderSelectionPort> leaderSelectionPort = negative(LeaderSelectionPort.class);

    Address self;
    
    
    public LeaderSelection() {
        super();
   
        subscribe(initHandler, control);
        subscribe(handleStart, control);
        
    }
    Handler<LeaderSelectionInit> initHandler = new Handler<LeaderSelectionInit>() {
        public void handle(LeaderSelectionInit event) {
            self = event.getSelf();
            
        }
    };
    Handler<Start> handleStart = new Handler<Start>() {
        public void handle(Start event) {
            System.out.println("Leader Selection component started.");
            proposeLeader(0);
        }
    };
   

    private final void proposeLeader(int instance) {
        /*
         * Start Bully instance (create component)
         * trigger new SelectLeaderRequest in Bully
         */
        trigger(new SelectNewLeader(self, instance), bullyPort);
    }

    
}
