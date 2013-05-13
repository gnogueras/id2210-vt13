package tman.system.peer.tman;

import common.configuration.TManConfiguration;
import java.util.ArrayList;

import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.DescriptorBuffer;
import cyclon.system.peer.cyclon.PeerDescriptor;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TMan.class);
    Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    private long period;
    private Address self;
    private ArrayList<Address> tmanPartners;
    private TManConfiguration tmanConfiguration;
    private Random r;

    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

        public TManSchedule(ScheduleTimeout request) {
            super(request);
        }
    }

//-------------------------------------------------------------------	
    public TMan() {
        tmanPartners = new ArrayList<Address>();

        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManPartnersResponse, networkPort);
        subscribe(handleTManPartnersRequest, networkPort);
    }
//-------------------------------------------------------------------	
    Handler<TManInit> handleInit = new Handler<TManInit>() {
        @Override
        public void handle(TManInit init) {
            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            period = tmanConfiguration.getPeriod();
            r = new Random(tmanConfiguration.getSeed());
            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);

        }
    };
//-------------------------------------------------------------------	
    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {
        @Override
        public void handle(TManSchedule event) {
            Snapshot.updateTManPartners(self, tmanPartners);

            // Publish sample to connected components
            trigger(new TManSample(tmanPartners), tmanPort);
        }
    };
//-------------------------------------------------------------------	
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {
            List<Address> cyclonPartners = event.getSample();
            int psi = 4; //PSI. What should we do with this variable? Parameter configuration, constant...

            // merge cyclonPartners into TManPartners
            System.out.print("ACTIVE. Self=" + self.getId() + " CyclonPartners={");
            for (Address partner : cyclonPartners) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");
            // merge cyclonPartners into TManPartners
            System.out.print("ACTIVE. Self=" + self.getId() + " TmanPartners (last view)={");
            for (Address partner : tmanPartners) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");

            //merge
            tmanPartners = merge(tmanPartners, cyclonPartners);
            //rank
            tmanPartners = rank(tmanPartners);

            System.out.print("ACTIVE. Self=" + self.getId() + " Merged (new view)={");
            for (Address partner : tmanPartners) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");
            
            //ACTIVE THREAD
            //from 2 to 6 from the active 
            if(tmanPartners.isEmpty()){
                return;
            }

            //select peer: p
            //PeerAddress selectedPeer = selectPeer(psi, rank(self, tmanPartners));
            Address selectedPeer = getSoftMaxAddress(tmanPartners);

            //buffer <-- merge
            ArrayList<Address> myDescriptor = new ArrayList<Address>();
            myDescriptor.add(self);
            ArrayList<Address> buffer = append(tmanPartners, myDescriptor);

            //buffer <-- rank
            //ORDER BASED ON THE SELECTED PEER!!!
            buffer = rank(buffer);

            //Transfrom buffer into DescriptorBuffer 
            ArrayList<PeerDescriptor> pd = new ArrayList<PeerDescriptor>();
            for (Address p : buffer) {
                pd.add(new PeerDescriptor(p));
            }
            DescriptorBuffer descriptorBuffer = new DescriptorBuffer(self, pd);

            //send m entries to p = trigger <ExchangeMsg.Request>
            trigger(new ExchangeMsg.Request(UUID.randomUUID(), descriptorBuffer, self, selectedPeer), networkPort);

            //At the end of the Active Thread
            //trigger timer (wait(delta))

        }
    };
//-------------------------------------------------------------------	
    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
            //PASSIVE THREAD

            System.out.print("PASSIVE. Self=" + self.getId() + " last view={");
            for (Address partner : tmanPartners) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");

            //buffer <-- merge
            ArrayList<Address> myDescriptor = new ArrayList<Address>();
            myDescriptor.add(self);

            ArrayList<Address> buffer = append(tmanPartners, myDescriptor);
            /*System.out.print("PASSIVE. Self="+self+" buffer to send={");
             for( PeerAddress partner : buffer ){
             System.out.print(partner.getPeerId()+",");
             }
             System.out.println("}");*/

            //buffer <-- rank
            //RANK ORDER BASED ON THE ID OF THE RECEIVER NODE WE ARE SENDING
            buffer = rank(buffer);
            System.out.print("PASSIVE. Self=" + self.getId() + " buffer to send ranked={");
            for (Address partner : buffer) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");


            //Transfrom buffer into DescriptorBuffer 
            ArrayList<PeerDescriptor> pd = new ArrayList<PeerDescriptor>();
            for (Address p : buffer) {
                pd.add(new PeerDescriptor(p));
            }
            DescriptorBuffer descriptorBuffer = new DescriptorBuffer(self, pd);

            //send m entries to p = trigger <ExchangeMsg.Respone>
            trigger(new ExchangeMsg.Response(UUID.randomUUID(), descriptorBuffer, self, event.getSource()), networkPort);

            //Extract bufferq of PeerAddress from PeerDescriptor
            pd = event.getRandomBuffer().getDescriptors();
            ArrayList<Address> bufferq = new ArrayList<Address>();
            for (PeerDescriptor d : pd) {
                bufferq.add(d.getAddress());
            }
            //view <-- merge(bufferq, view) (View = tmanpartners)
            tmanPartners = merge(tmanPartners, bufferq);
            tmanPartners = rank(tmanPartners);
            System.out.print("PASSIVE. Self=" + self.getId() + " View updated={");
            for (Address partner : tmanPartners) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");
        }
    };
    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {
            //ACTIVE THREAD
            //from 7 to 8

            //Extract bufferq of PeerAddress from PeerDescriptor
            ArrayList<PeerDescriptor> pd = event.getSelectedBuffer().getDescriptors();
            ArrayList<Address> bufferp = new ArrayList<Address>();
            for (PeerDescriptor d : pd) {
                bufferp.add(d.getAddress());
            }
            //view <-- merge(bufferq, view) (View = tmanpartners)
            tmanPartners = merge(tmanPartners, bufferp);
            tmanPartners = rank(tmanPartners);
            System.out.print("ACTIVE. Self=" + self.getId() + " View updated={");
            for (Address partner : tmanPartners) {
                System.out.print(partner.getId() + ",");
            }
            System.out.println("}");

        }
    };
// TODO - if you call this method with a list of entries, it will
// return a single node, weighted towards the 'best' node (as defined by
// ComparatorById) with the temperature controlling the weighting.
// A temperature of '1.0' will be greedy and always return the best node.
// A temperature of '0.000001' will return a random node.
// A temperature of '0.0' will throw a divide by zero exception :)
// Reference:
// http://webdocs.cs.ualberta.ca/~sutton/book/2/node4.html

    public Address getSoftMaxAddress(List<Address> entries) {
        Collections.sort(entries, new ComparatorById(self));

        double rnd = r.nextDouble();
        double total = 0.0d;
        double[] values = new double[entries.size()];
        int j = entries.size() + 1;
        for (int i = 0; i < entries.size(); i++) {
            // get inverse of values - lowest have highest value.
            double val = j;
            j--;
            values[i] = Math.exp(val / tmanConfiguration.getTemperature());
            total += values[i];
        }

        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                values[i] += values[i - 1];
            }
            // normalise the probability for this entry
            double normalisedUtility = values[i] / total;
            if (normalisedUtility >= rnd) {
                return entries.get(i);
            }
        }
        return entries.get(entries.size() - 1);
    }

    //Implement Rank function
    private ArrayList<Address> rank(List<Address> buffer) {
        /*System.out.print("RANK. base=" + base.getPeerId() + " buffer={");
         for (PeerAddress partner : buffer) {
         System.out.print(partner.getPeerId() + ",");
         }
         System.out.println("}");*/
        ArrayList<Address> aux = new ArrayList<Address>(buffer);

        Collections.sort(aux, new ComparatorById(self));
        /*System.out.print("RANK after SORT. base=" + base.getPeerId() + " aux={");
         for (PeerAddress partner : aux) {
         System.out.print(partner.getPeerId() + ",");
         }
         System.out.println("}");*/

        // Select the nodes with LOWER ID
        //ArrayList<Address> sorted = new ArrayList<Address>(aux.subList(0, aux.indexOf(base)));
        //Collections.reverse(sorted);
        /*System.out.print("RANK. base=" + base.getPeerId() + " sorted={");
         for (PeerAddress partner : sorted) {
         System.out.print(partner.getPeerId() + ",");
         }
         System.out.println("}");*/


        //ArrayList<Address> tail = new ArrayList<Address>(aux.subList(aux.indexOf(base) + 1, aux.size()));
        /*System.out.print("RANK. base=" + base.getPeerId() + " tail={");
         for (PeerAddress partner : tail) {
         System.out.print(partner.getPeerId() + ",");
         }
         System.out.println("}");*/
        /*for (int i = 1; i < aux.indexOf(base); i++) {
         sorted.add(aux.get(aux.indexOf(base) - i));
         System.out.println("Element "+(aux.indexOf(base)-i)+ ": "+ aux.get(aux.indexOf(base) - i));
         }*/

        //aux.remove(base);
        /*System.out.print("RANK after FOR. base=" + base.getPeerId() + " aux={");
         for (PeerAddress partner : aux) {
         System.out.print(partner.getPeerId() + ",");
         }
         System.out.println("}");*/
        //sorted.addAll(tail);
        /*System.out.print("RANK. base=" + base.getPeerId() + " return={");
         for (PeerAddress partner : sorted) {
         System.out.print(partner.getPeerId() + ",");
         }
         System.out.println("}");*/
        return aux;
    }

    //Comparator for sort the Peers
    /*class PeersComparator implements Comparator<PeerAddress> {

     @Override
     public int compare(PeerAddress a, PeerAddress b) {
     return a.compareTo(b);
     }
     }*/
    //Implement Merge function
    private ArrayList<Address> merge(List<Address> list1, List<Address> list2) {
        ArrayList<Address> merged = new ArrayList<Address>(list1);
        for (Address p : list2) {
            if (!merged.contains(p) && p.getId() != self.getId()) {
                merged.add(p);
            }
        }
        return merged;
    }

    //Implementd Append function
    private ArrayList<Address> append(ArrayList<Address> list1, ArrayList<Address> list2) {
        ArrayList<Address> merged = new ArrayList<Address>(list1);
        for (Address p : list2) {
            if (!merged.contains(p)) {
                merged.add(p);
            }
        }
        return merged;
    }
    //Implement Select Peer
    /*private PeerAddress selectPeer(int psi, ArrayList<PeerAddress> view) {
     int randomIndex = (new Random()).nextInt(psi);
     if (randomIndex >= view.size()) {
     randomIndex = (new Random()).nextInt(view.size());
     }
     return view.get(randomIndex);
     }*/
}
