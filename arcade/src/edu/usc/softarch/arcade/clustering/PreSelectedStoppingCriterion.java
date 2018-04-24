package edu.usc.softarch.arcade.clustering;

import edu.usc.softarch.arcade.config.Config;

/**
 *
 * @author daniellink
 *
 *         Stopping criterion in case we use a preselected one, i.e. not
 *         clusterGain
 *
 */
public class PreSelectedStoppingCriterion implements StoppingCriterion {
	@Override
	public boolean notReadyToStop() {
		return ClusteringAlgoRunner.getFastClusters().size() != 1 && ClusteringAlgoRunner.getFastClusters().size() != Config.getNumClusters();
	}
}