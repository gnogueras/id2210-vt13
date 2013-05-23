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
    private ArrayList<PeerDescriptor> tmanPartnersDescriptors;
    private TManConfiguration tmanConfiguration;
    private Random r;
    private int MAX_AGE = 10;
    private int PSI = 7;
    private static final int PEER_TRUNCATE_VALUE = 6;

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
        tmanPartnersDescriptors = new ArrayList<PeerDescriptor>();

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
    // merge cyclonPartners into TManPartners
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {

            List<Address> cyclonPartners = event.getSample();
            ArrayList<PeerDescriptor> cyclonPartnersDescriptors = addressToPeerDescriptor(cyclonPartners);

            //merge
            //tmanPartners = merge(tmanPartners, cyclonPartners);
            tmanPartnersDescriptors = merge_pd(removeOldPeers(MAX_AGE, tmanPartnersDescriptors), cyclonPartnersDescriptors);
            tmanPartners = peerDescriptorToAddress(tmanPartnersDescriptors);
            //rank
            tmanPartners = rank(tmanPartners);

            //ACTIVE THREAD
            //from 2 to 6 from the active 
            if (tmanPartners.isEmpty()) {
                return;
            }

            //select peer: p
            Address selectedPeer = getSoftMaxAddress(tmanPartners);
            
            ArrayList<PeerDescriptor> bufferToSend = getBufferToSend(tmanPartners);
            DescriptorBuffer descriptorBuffer = new DescriptorBuffer(self, bufferToSend);
            //send m entries to p = trigger <ExchangeMsg.Request>
            trigger(new ExchangeMsg.Request(UUID.randomUUID(), descriptorBuffer, self, selectedPeer), networkPort);

            //At the end of the Active Thread
        }

    };
//-------------------------------------------------------------------	
    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {
        @Override
        public void handle(ExchangeMsg.Request event) {
            //PASSIVE THREAD

            DescriptorBuffer descriptorBuffer = new DescriptorBuffer(self, tmanPartnersDescriptors);

            //send m entries to p = trigger <ExchangeMsg.Respone>
            trigger(new ExchangeMsg.Response(UUID.randomUUID(), descriptorBuffer, self, event.getSource()), networkPort);

            //Extract bufferq of PeerAddress from PeerDescriptor
            //Get the PeerDescriptor buffer (ArrayList<PeerDescriptor>)
            ArrayList<PeerDescriptor> receivedDescriptors = new ArrayList<PeerDescriptor>(event.getRandomBuffer().getDescriptors());
            receivedDescriptors.add(new PeerDescriptor(event.getSource()));

            //view <-- merge(bufferq, view) (View = tmanpartners)
            tmanPartnersDescriptors = merge_pd(tmanPartnersDescriptors, receivedDescriptors);
            tmanPartners = peerDescriptorToAddress(tmanPartnersDescriptors);
            tmanPartners = rank(tmanPartners);

        }
    };
    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {
        @Override
        public void handle(ExchangeMsg.Response event) {
            //ACTIVE THREAD
            //from 7 to 8

            //Extract bufferq of PeerAddress from PeerDescriptor
            //Get the PeerDescriptor buffer (ArrayList<PeerDescriptor>)
            ArrayList<PeerDescriptor> receivedDescriptors = event.getSelectedBuffer().getDescriptors();
            receivedDescriptors.add(new PeerDescriptor(event.getSource()));

            //view <-- merge(bufferq, view) (View = tmanpartners)
            //tmanPartners = merge(tmanPartners, bufferq);
            tmanPartnersDescriptors = merge_pd(tmanPartnersDescriptors, receivedDescriptors);
            tmanPartners = peerDescriptorToAddress(tmanPartnersDescriptors);
            tmanPartners = rank(tmanPartners);
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

        ArrayList<Address> aux = new ArrayList<Address>(buffer);
        Collections.sort(aux, new ComparatorById(self));

       if (aux.size()> PEER_TRUNCATE_VALUE){
            Random r = new Random();
            int randomTailPeer=r.nextInt(aux.size()-PEER_TRUNCATE_VALUE) + PEER_TRUNCATE_VALUE;
            ArrayList<Address> truncatedAux = new ArrayList<Address>(aux.subList(0, PEER_TRUNCATE_VALUE));
            truncatedAux.add(aux.get(randomTailPeer));
            return truncatedAux;
        }
        else {
            return aux;
       }
    }

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
    /*
     * Function that receives to lists of PeerDescriptors. It remove the expired peers from the oldPeers 
     * (peers with age>max_age are expired) and merge the remaining peers with the newPeers
     * (newPeers are fresh, have age 0)
     */

    //Merge function using peer descriptors
    private ArrayList<PeerDescriptor> merge_pd(ArrayList<PeerDescriptor> oldPeers, List<PeerDescriptor> newPeers) {
        ArrayList<PeerDescriptor> merged = oldPeers;
        for (PeerDescriptor peer : newPeers) {
            if (peer.getAddress().getId() != self.getId()) {
                merged = tryToAdd(peer, merged);
            }
        }
        return merged;
    }

    //Remove peers with age grater than max_age and return the updated list
    private ArrayList<PeerDescriptor> removeOldPeers(int max_age, List<PeerDescriptor> oldPeers) {
        ArrayList<PeerDescriptor> notExpiredPeers = new ArrayList();
        for (PeerDescriptor peer : oldPeers) {
            if (peer.incrementAndGetAge() <= max_age) {
                notExpiredPeers.add(peer);
            }
        }
        return notExpiredPeers;
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

    //Function to get a PeerDescriptor List from an Address List
    private ArrayList<PeerDescriptor> addressToPeerDescriptor(List<Address> addressList) {
        ArrayList<PeerDescriptor> peerDescriptorList = new ArrayList<PeerDescriptor>();
        for (Address p : addressList) {
            peerDescriptorList.add(new PeerDescriptor(p));
        }
        return peerDescriptorList;
    }

    //Function to get an Address List from a PeerDescriptor list
    private ArrayList<Address> peerDescriptorToAddress(ArrayList<PeerDescriptor> peerDescriptorList) {
        ArrayList<Address> addressList = new ArrayList<Address>();
        for (PeerDescriptor pd : peerDescriptorList) {
            addressList.add(pd.getAddress());
        }
        return addressList;
    }

    //Check if the PeerDescriptor is contained in the list (based on id, ignoring the age)
    private boolean isPeerContained(PeerDescriptor peer, ArrayList<PeerDescriptor> list) {
        for (PeerDescriptor p : list) {
            if (p.getAddress().equals(peer.getAddress())) {
                return true;
            }
        }
        return false;
    }

    //Get the PeerDescriptor identified by the Address peer, from the peerDescrpitor list
    private PeerDescriptor getPeerInList(Address peer, ArrayList<PeerDescriptor> list) {
        for (PeerDescriptor p : list) {
            if (p.getAddress().equals(peer)) {
                return p;
            }
        }
        return null;
    }

    //Function that tries to add the PeerDescriptor to the list. If it contained, update the age
    private ArrayList<PeerDescriptor> tryToAdd(PeerDescriptor peer, ArrayList<PeerDescriptor> peerList) {
        ArrayList<PeerDescriptor> list = new ArrayList<PeerDescriptor>(peerList);
        if (!isPeerContained(peer, list)) {
            list.add(peer);
        } else {
            for (PeerDescriptor p : list) {
                if (p.getAddress().equals(peer.getAddress()) && p.getAge() > peer.getAge()) {
                    list.remove(p);
                    list.add(peer);
                    break;
                }
            }
        }
        return list;
    }
    
    //Get the Buffer to send. PeerDescriptor list of maximum size of PSI 
    private ArrayList<PeerDescriptor> getBufferToSend(ArrayList<Address> partners) {
        ArrayList<PeerDescriptor> buffer = addressToPeerDescriptor(partners);
        if(buffer.size()<=PSI){
            return buffer;
        }else{
            return (new ArrayList<PeerDescriptor>(buffer.subList(0, PSI))); 
        }
    }

    //Printing functions for debuging
    private void printDescriptorList(List<PeerDescriptor> list) {
        System.out.print("{");
        for (PeerDescriptor partner : list) {
            System.out.print("(Id:" + partner.getAddress().getId() + " - Age:" + partner.getAge() + "), ");
        }
        System.out.println("}");
    }
    private void printAddressList(List<Address> list) {
        System.out.print("{");
        for (Address partner : list) {
            System.out.print("(Id:" + partner.getId() + "), ");
        }
        System.out.println("}");
    }
    
}