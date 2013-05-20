package search.system.peer;

import java.util.LinkedList;
import java.util.Set;

import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.p2p.bootstrap.BootstrapCompleted;
import se.sics.kompics.p2p.bootstrap.BootstrapRequest;
import se.sics.kompics.p2p.bootstrap.BootstrapResponse;
import se.sics.kompics.p2p.bootstrap.P2pBootstrap;
import se.sics.kompics.p2p.bootstrap.PeerEntry;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClient;
import se.sics.kompics.p2p.bootstrap.client.BootstrapClientInit;
import se.sics.kompics.timer.Timer;

import search.simulator.snapshot.Snapshot;
import search.system.peer.search.Search;
import search.system.peer.search.SearchInit;
import common.configuration.SearchConfiguration;
import common.configuration.CyclonConfiguration;
import common.configuration.TManConfiguration;
import common.peer.PeerAddress;
import cyclon.system.peer.cyclon.*;
import se.sics.kompics.web.Web;
import search.bully.Bully;
import search.bully.BullyInit;
import search.bully.BullyPort;
import search.leaderSelection.LeaderSelection;
import search.leaderSelection.LeaderSelectionInit;
import search.leaderSelection.LeaderSelectionPort;
import search.epfd.EPFDInit;
import search.epfd.EPFDPort;
import search.epfd.EventuallyPerfectFailureDetector;
import tman.system.peer.tman.TMan;
import tman.system.peer.tman.TManInit;
import tman.system.peer.tman.TManSamplePort;

public final class SearchPeer extends ComponentDefinition {

    Positive<IndexPort> indexPort = positive(IndexPort.class);
    Positive<Network> network = positive(Network.class);
    Positive<Timer> timer = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    private Component cyclon, tman, search, bootstrap;
    private Component leaderSelection, bully, epfd;
    private Address self;
    private int bootstrapRequestPeerCount;
    private boolean bootstrapped;
    private SearchConfiguration aggregationConfiguration;
    
    int heartbeatPeriod = 1000;
    int increment = 1000;

//-------------------------------------------------------------------	
    public SearchPeer() {
        cyclon = create(Cyclon.class);
        tman = create(TMan.class);
        search = create(Search.class);
        bootstrap = create(BootstrapClient.class);
        leaderSelection = create(LeaderSelection.class);
        bully = create(Bully.class);
        epfd = create(EventuallyPerfectFailureDetector.class);

        connect(network, search.getNegative(Network.class));
        connect(network, cyclon.getNegative(Network.class));
        connect(network, bootstrap.getNegative(Network.class));
        connect(network, tman.getNegative(Network.class));
        connect(timer, search.getNegative(Timer.class));
        connect(timer, cyclon.getNegative(Timer.class));
        connect(timer, bootstrap.getNegative(Timer.class));
        connect(timer, tman.getNegative(Timer.class));
        connect(timer, leaderSelection.getNegative(Timer.class));
        connect(webPort, search.getPositive(Web.class));
        connect(cyclon.getPositive(CyclonSamplePort.class),
                search.getNegative(CyclonSamplePort.class));
        connect(cyclon.getPositive(CyclonSamplePort.class),
                tman.getNegative(CyclonSamplePort.class));
        connect(tman.getPositive(TManSamplePort.class),
                search.getNegative(TManSamplePort.class));

        connect(indexPort, search.getNegative(IndexPort.class));
        
        connect(bully.getPositive(BullyPort.class),
                leaderSelection.getNegative(BullyPort.class));
        connect(epfd.getPositive(EPFDPort.class),
                leaderSelection.getNegative(EPFDPort.class));
        connect(tman.getPositive(TManSamplePort.class),
                leaderSelection.getNegative(TManSamplePort.class));
        connect(leaderSelection.getPositive(LeaderSelectionPort.class),
                search.getNegative(LeaderSelectionPort.class));
        
        connect(network, bully.getNegative(Network.class));
        connect(network, epfd.getNegative(Network.class));
        connect(timer, bully.getNegative(Timer.class));
        connect(timer, epfd.getNegative(Timer.class));
        

        subscribe(handleInit, control);
        subscribe(handleJoinCompleted, cyclon.getPositive(CyclonPort.class));
        subscribe(handleBootstrapResponse, bootstrap.getPositive(P2pBootstrap.class));
    }
//-------------------------------------------------------------------	
    Handler<SearchPeerInit> handleInit = new Handler<SearchPeerInit>() {
        @Override
        public void handle(SearchPeerInit init) {
            self = init.getPeerSelf();
            CyclonConfiguration cyclonConfiguration = init.getCyclonConfiguration();
            aggregationConfiguration = init.getApplicationConfiguration();
            TManConfiguration tmanConfiguration = init.getTManConfiguration();


            bootstrapRequestPeerCount = cyclonConfiguration.getBootstrapRequestPeerCount();

            //Trigger Init Events for starting components
            trigger(new CyclonInit(cyclonConfiguration), cyclon.getControl());
            trigger(new TManInit(self, tmanConfiguration), tman.getControl());
            trigger(new BootstrapClientInit(self, init.getBootstrapConfiguration()), bootstrap.getControl());
            BootstrapRequest request = new BootstrapRequest("Cyclon", bootstrapRequestPeerCount);
            trigger(request, bootstrap.getPositive(P2pBootstrap.class));
            
            trigger(new LeaderSelectionInit(self), leaderSelection.getControl());
            trigger(new BullyInit(self), bully.getControl());
            trigger(new EPFDInit(self, heartbeatPeriod, increment), epfd.getControl());
        }
    };
//-------------------------------------------------------------------	
    Handler<BootstrapResponse> handleBootstrapResponse = new Handler<BootstrapResponse>() {
        @Override
        public void handle(BootstrapResponse event) {
            if (!bootstrapped) {
                Set<PeerEntry> somePeers = event.getPeers();
                LinkedList<Address> cyclonInsiders = new LinkedList<Address>();

                for (PeerEntry peerEntry : somePeers) {
                    cyclonInsiders.add(
                            peerEntry.getOverlayAddress().getPeerAddress());
                }
                trigger(new CyclonJoin(self, cyclonInsiders),
                        cyclon.getPositive(CyclonPort.class));
                bootstrapped = true;
            }
        }
    };
//-------------------------------------------------------------------	
    Handler<JoinCompleted> handleJoinCompleted = new Handler<JoinCompleted>() {
        @Override
        public void handle(JoinCompleted event) {
            trigger(new BootstrapCompleted("Cyclon", new PeerAddress(self)),
                    bootstrap.getPositive(P2pBootstrap.class));
            trigger(new SearchInit(self, aggregationConfiguration), search.getControl());
        }
    };
}
