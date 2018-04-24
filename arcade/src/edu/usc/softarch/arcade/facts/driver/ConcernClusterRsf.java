package edu.usc.softarch.arcade.facts.driver;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.usc.softarch.arcade.facts.ConcernCluster;

public class ConcernClusterRsf {
	private static boolean containsClusterWithName(Set<ConcernCluster> clusters, String clusterName) {
		for (final ConcernCluster cluster : clusters) {
			if (cluster.getName().equals(clusterName)) {
				return true;
			}
		}
		return false;
	}

	public static Set<ConcernCluster> extractConcernClustersFromRsfFile(File rsfFile) {
		RsfReader.loadRsfDataFromFile(rsfFile);
		final Iterable<List<String>> clusterFacts = RsfReader.filteredRoutineFacts;
		final Set<ConcernCluster> clusters = new HashSet<ConcernCluster>();
		for (final List<String> fact : clusterFacts) {
			final String clusterName = fact.get(1).trim();
			final String element = fact.get(2).trim();
			if (containsClusterWithName(clusters, clusterName)) {
				for (final ConcernCluster cluster : clusters) {
					if (cluster.getName().equals(clusterName)) {
						cluster.addEntity(element);
					}
				}
			} else {
				final ConcernCluster newCluster = new ConcernCluster();
				newCluster.setName(clusterName);
				newCluster.addEntity(element);
				clusters.add(newCluster);
			}
		}
		return clusters;
	}
}
