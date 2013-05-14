/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import se.sics.kompics.PortType;

/**
 *
 * @author Gerard
 */
public class BullyPort extends PortType {
    {
        indication(NewLeaderFromBully.class);       
        request(NewInstance.class);
    }
}
