package edu.usc.softarch.arcade.clustering;

public class ClusterGainStoppingCriterion implements StoppingCriterion {
	private boolean secondRun;// Whether we've already determined the optimal
	// number of
	// clusters;
	private int optimalNumClusters;

	public ClusterGainStoppingCriterion() {
		secondRun = false;
		optimalNumClusters = 0;
	}

	public boolean isSecondRun() {
		return secondRun;
	}

	public void setSecondRun(boolean secondRun) {
		this.secondRun = secondRun;
	}

	public int getOptimalNumClusters() {
		return optimalNumClusters;
	}

	public void setOptimalNumClusters(int optimalNumClusters) {
		this.optimalNumClusters = optimalNumClusters;
	}

	@Override
	public boolean notReadyToStop() {
		if (secondRun)
			return ClusteringAlgoRunner.getFastClusters().size() != optimalNumClusters;
		return ClusteringAlgoRunner.getFastClusters().size() != 1 && ClusteringAlgoRunner.getFastClusters().size() != ClusteringAlgoRunner.getNumClustersAtMaxClusterGain();
	}
}