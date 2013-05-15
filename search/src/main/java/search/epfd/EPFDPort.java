/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import se.sics.kompics.PortType;

/**
 *
 * @author Gerard
 */
public class EPFDPort extends PortType{
    {
        request(StartMonitoring.class);
        indication(Suspect.class);
        indication(Restore.class);
    }
}
