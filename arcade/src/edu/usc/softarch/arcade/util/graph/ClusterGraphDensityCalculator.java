package edu.usc.softarch.arcade.util.graph;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.util.FileUtil;

public class ClusterGraphDensityCalculator {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final File depsFile = FileUtil.checkFile(args[0], false, false);
		final File clustersFile = FileUtil.checkFile(args[1], false, false);

		RsfReader.loadRsfDataFromFile(depsFile);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile(clustersFile);
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;

		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);

		final Set<List<String>> edges = ClusterUtil.buildClusterEdges(clusterMap, depFacts);

		final int numEdges = edges.size();
		final int numVertices = clusterMap.keySet().size();

		final double graphDensity = (double) numEdges / (double) (numVertices * (numVertices - 1));

		System.out.println(Joiner.on("\n").join(edges));
		System.out.println("no. of edges: " + numEdges);
		System.out.println("no. of vertices: " + numVertices);
		System.out.println("graph density: " + graphDensity);

	}

}
