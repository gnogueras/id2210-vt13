/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.epfd;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 *
 * @author Gerard
 */
public class HeartbeatTimeout extends Timeout{

    public HeartbeatTimeout(ScheduleTimeout request) {
        super(request);
    }
    
}
