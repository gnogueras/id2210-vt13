/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import se.sics.kompics.PortType;


public class LeaderSelectionPort extends PortType{
	{
		indication(LeaderSelected.class);
		request(SelectNewLeader.class);
	}

}
