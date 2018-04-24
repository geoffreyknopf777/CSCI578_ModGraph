package edu.usc.softarch.arcade.clustering;

public class SingleClusterStoppingCriterion implements StoppingCriterion {
	@Override
	public boolean notReadyToStop() {
		return ClusteringAlgoRunner.getFastClusters().size() != 1;
	}
}