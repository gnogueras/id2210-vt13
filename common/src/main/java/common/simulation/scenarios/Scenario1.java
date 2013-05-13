package common.simulation.scenarios;

import se.sics.kompics.p2p.experiment.dsl.SimulationScenario;

@SuppressWarnings("serial")
public class Scenario1 extends Scenario {
	private static SimulationScenario scenario = new SimulationScenario() {{
		StochasticProcess process1 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(3, Operations.peerJoin(4), 
                                uniform(0, Integer.MAX_VALUE)
                             );
		}};
		
		StochasticProcess process2 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(2, Operations.peerJoin(6), 
                                uniform(0, Integer.MAX_VALUE)
                                );
		}};
		
		StochasticProcess process3 = new StochasticProcess() {{
			eventInterArrivalTime(constant(100));
			raise(8, Operations.addIndexEntry(), 
                                uniform(0, Integer.MAX_VALUE)
                                );
		}};

		process1.start();
		process3.startAfterTerminationOf(2000, process1);
                process2.startAfterTerminationOf(3000, process3);
	}};

	// -------------------------------------------------------------------
	public Scenario1() {
		super(scenario);
	}
}
