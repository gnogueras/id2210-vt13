/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.leaderSelection;

import se.sics.kompics.PortType;


public class LeaderSelectionPort extends PortType{
	{
		request(StartLeaderSelectionEvent.class);
		indication(CurrentLeaderEvent.class);
	}

}
