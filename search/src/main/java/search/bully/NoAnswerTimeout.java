/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package search.bully;

import java.util.UUID;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 *
 * @author Gerard
 */
public class NoAnswerTimeout extends Timeout {

    int instance;

    public NoAnswerTimeout(ScheduleTimeout request, int instance) {
        super(request);
        this.instance = instance;
    }

    public int getInstance() {
        return instance;
    }
 
}
