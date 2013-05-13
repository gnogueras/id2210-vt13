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
public class CheckTimeout extends Timeout{

    public CheckTimeout(ScheduleTimeout request) {
        super(request);
    }
    
}
