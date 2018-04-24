package edu.usc.softarch.arcade.decay;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.beust.jcommander.JCommander;
import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.driver.ConcernClusterRsf;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.LogUtil;

public class ReflexionMethod {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ReflexionMethod.class);

	public static void main(String[] args) throws FileNotFoundException {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		LogUtil.printLogFiles();

		final VCOptions vco = new VCOptions();
		new JCommander(vco, args);

		final File clustersFile = FileUtil.checkFile(vco.clustersFilename, false, false);
		final File depsFile = FileUtil.checkFile(vco.depsFilename, false, false);
		final File expectedDepsFile = FileUtil.checkFile(vco.expectedDepsFilename, false, false);
		// String ignoreClustersFilename = null;
		// if (vco.ignoredClustersFilename != null) {
		// ignoreClustersFilename = FileUtil
		// .tildeExpandPath(vco.ignoredClustersFilename);
		// }
		final File outputFile = FileUtil.checkFile(vco.outputFilename, false, false);

		final Set<ConcernCluster> clusters = ConcernClusterRsf.extractConcernClustersFromRsfFile(clustersFile);

		final boolean showBuiltClusters = true;
		if (showBuiltClusters) {
			logger.debug("Found and built clusters:");
			for (final ConcernCluster cluster : clusters) {
				logger.debug(cluster.getName());
			}
		}

		final SimpleDirectedGraph<String, DefaultEdge> actualGraph = ClusterUtil.buildSimpleDirectedGraph(depsFile, clusters);

		final SimpleDirectedGraph<String, DefaultEdge> expectedGraph = ClusterUtil.buildConcernClustersDiGraph(clusters, expectedDepsFile);

		final Set<Pair<String, String>> convergenceEdges = new LinkedHashSet<Pair<String, String>>();
		final Set<Pair<String, String>> divergenceEdges = new LinkedHashSet<Pair<String, String>>();
		final Set<Pair<String, String>> absentEdges = new LinkedHashSet<Pair<String, String>>();

		// divergence edges start with all actual edges
		for (final DefaultEdge actualEdge : actualGraph.edgeSet()) {
			final String actualSrc = actualGraph.getEdgeSource(actualEdge);
			final String actualTgt = actualGraph.getEdgeTarget(actualEdge);
			final Pair<String, String> actualPair = new ImmutablePair<String, String>(actualSrc, actualTgt);
			divergenceEdges.add(actualPair);
		}

		for (final DefaultEdge actualEdge : actualGraph.edgeSet()) {
			final String actualSrc = actualGraph.getEdgeSource(actualEdge);
			final String actualTgt = actualGraph.getEdgeTarget(actualEdge);
			final Pair<String, String> actualPair = new ImmutablePair<String, String>(actualSrc, actualTgt);
			boolean foundMatchingEdge = false;
			Pair<String, String> expectedPair = null;
			for (final DefaultEdge expectedEdge : expectedGraph.edgeSet()) {
				final String expectedSrc = expectedGraph.getEdgeSource(expectedEdge);
				final String expectedTgt = expectedGraph.getEdgeTarget(expectedEdge);
				expectedPair = new ImmutablePair<String, String>(expectedSrc, expectedTgt);
				if (actualSrc.equals(expectedSrc) && actualTgt.equals(expectedTgt)) {
					convergenceEdges.add(actualPair);
					foundMatchingEdge = true;
				}
			}
			// no matching eges of an expected pair is an absence
			if (!foundMatchingEdge && expectedPair != null) {
				absentEdges.add(expectedPair);
			}
		}

		// all actual edges - converge edges = divergence edges
		divergenceEdges.removeAll(convergenceEdges);

		final PrintWriter writer = new PrintWriter(outputFile);
		writer.println("Convergence edges:");
		writer.println(Joiner.on("\n").join(convergenceEdges));
		writer.println();
		writer.println("Divergence edges:");
		writer.println(Joiner.on("\n").join(divergenceEdges));
		writer.println();
		writer.println("Absent edges:");
		writer.println(Joiner.on("\n").join(absentEdges));
		writer.close();

	}

}
