package edu.usc.softarch.arcade.decay;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.logging.log4j.Logger;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import edu.usc.softarch.arcade.clustering.StringGraph;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.driver.ConcernClusterRsf;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.LogUtil;

public class DecayMetricAnalyzer {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(DecayMetricAnalyzer.class);
	public static Double rciVal;
	public static double twoWayPairRatio;
	public static double avgStability;
	public static double mqRatio;

	public static void main(String[] args) {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		rciVal = null;
		twoWayPairRatio = -1;
		avgStability = -1;
		mqRatio = -1;

		final File clustersFile = FileUtil.checkFile(args[0], true, false);
		final File depsRsfFile = FileUtil.checkFile(args[1], true, false);
		final String readingClustersFile = "Reading in clusters file: " + clustersFile.getPath();
		System.out.println(readingClustersFile);
		logger.info(readingClustersFile);

		final Set<ConcernCluster> clusters = ConcernClusterRsf.extractConcernClustersFromRsfFile(clustersFile);

		final boolean showBuiltClusters = true;
		if (showBuiltClusters) {
			logger.debug("Found and built clusters:");
			for (final ConcernCluster cluster : clusters) {
				logger.debug(cluster.getName());
			}
		}

		// Map<String,Set<String>> clusterSmellMap = new
		// HashMap<String,Set<String>>();

		final String readingDepsFile = "Reading in deps file: " + depsRsfFile;
		System.out.println(readingDepsFile);
		logger.info(readingDepsFile);
		final Map<String, Set<String>> depMap = ClusterUtil.buildDependenciesMap(depsRsfFile);

		final StringGraph clusterGraph = ClusterUtil.buildClusterGraphUsingDepMap(depMap, clusters);

		final SimpleDirectedGraph<String, DefaultEdge> directedGraph = ClusterUtil.buildConcernClustersDiGraph(clusters, clusterGraph);

		// Map<String, Double> decayMetrics = new
		// LinkedHashMap<String,Double>();
		rciVal = detectRci(directedGraph);

		logger.info("rci: " + rciVal);

		final Set<Set<String>> twoWayPairs = detectTwoWayDeps(directedGraph);
		twoWayPairRatio = (double) twoWayPairs.size() / (double) combinations(directedGraph.vertexSet().size(), 2);
		logger.info("no. of two-way pairs: " + twoWayPairs.size());
		logger.info("no. of two-way pairs / all possible pairs: " + twoWayPairRatio);

		avgStability = detectStability(directedGraph);

		logger.info("avg stability: " + avgStability);

		RsfReader.loadRsfDataFromFile(depsRsfFile);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;
		RsfReader.loadRsfDataFromFile(clustersFile);
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;
		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);
		final Map<String, Set<MutablePair<String, String>>> internalEdgeMap = ClusterUtil.buildInternalEdgesPerCluster(clusterMap, depFacts);
		final Map<String, Set<MutablePair<String, String>>> externalEdgeMap = ClusterUtil.buildExternalEdgesPerCluster(clusterMap, depFacts);
		final Map<String, Set<MutablePair<String, String>>> intoEdgeMap = ClusterUtil.buildEdgesIntoEachCluster(clusterMap, depFacts);

		final Map<String, Double> clusterFactors = new LinkedHashMap<String, Double>();
		for (final ConcernCluster cluster : clusters) {
			final Set<MutablePair<String, String>> internalEdges = internalEdgeMap.get(cluster.getName());
			final Set<MutablePair<String, String>> externalEdges = externalEdgeMap.get(cluster.getName());
			if (internalEdges.size() == 0) {
				clusterFactors.put(cluster.getName(), new Double(0));
			} else {
				final Set<MutablePair<String, String>> edgesInto = intoEdgeMap.get(cluster.getName());
				final int interEdgesSum = edgesInto.size() + externalEdges.size();
				final double cf = (double) (2 * internalEdges.size()) / (2 * internalEdges.size() + interEdgesSum);
				clusterFactors.put(cluster.getName(), cf);
			}
		}

		double mq = 0;
		for (final Double cf : clusterFactors.values()) {
			mq += cf;
		}
		mqRatio = mq / clusters.size();

		logger.info("MQ: " + mq);
		logger.info("# of clusters: " + clusters.size());
		logger.info("MQ ratio: " + mqRatio);

		System.out.println("Wrote decay metrics to: ");
		LogUtil.printLogFiles();
		logger.info("");

		// computeMq(clusters,depsRsf)2*internalEdges.size()

	}

	private static double detectStability(SimpleDirectedGraph<String, DefaultEdge> directedGraph) {
		final Set<String> vertices = directedGraph.vertexSet();
		final Map<String, Double> stabilityMap = new LinkedHashMap<String, Double>();
		double stabilitySum = 0;
		for (final String vertex : vertices) {
			final Set<DefaultEdge> incomingEdges = directedGraph.incomingEdgesOf(vertex);
			final Set<DefaultEdge> outgoingEdges = directedGraph.outgoingEdgesOf(vertex);
			final int denom = incomingEdges.size() + outgoingEdges.size();
			double stability = 0;

			if (denom != 0) {
				stability = (double) incomingEdges.size() / (double) denom;
			}
			stabilityMap.put(vertex, stability);

			stabilitySum += stability;
		}

		final double avgStability = stabilitySum / vertices.size();
		return avgStability;

	}

	static long combinations(int n, int k) {
		long coeff = 1;
		for (int i = n - k + 1; i <= n; i++) {
			coeff *= i;
		}
		for (int i = 1; i <= k; i++) {
			coeff /= i;
		}
		return coeff;
	}

	private static Set<Set<String>> detectTwoWayDeps(SimpleDirectedGraph<String, DefaultEdge> directedGraph) {
		final Set<Set<String>> twoWayPairs = new LinkedHashSet<Set<String>>();

		final Set<DefaultEdge> actualEdges = directedGraph.edgeSet();
		for (final DefaultEdge edge : actualEdges) {
			final String sourceCluster = directedGraph.getEdgeSource(edge);
			final String targetCluster = directedGraph.getEdgeTarget(edge);
			if (directedGraph.containsEdge(targetCluster, sourceCluster)) {
				final Set<String> twoWayPair = new HashSet<String>();
				twoWayPair.add(sourceCluster);
				twoWayPair.add(targetCluster);
				twoWayPairs.add(twoWayPair);
			}
		}

		return twoWayPairs;
	}

	private static double detectRci(SimpleDirectedGraph<String, DefaultEdge> directedGraph) {

		final Set<DefaultEdge> actualEdges = directedGraph.edgeSet();
		final Set<String> vertices = directedGraph.vertexSet();

		final int potentialEdgeCount = vertices.size() * (vertices.size() - 1);
		logger.debug("# actual edges: " + actualEdges.size());
		logger.debug("# potential edges: " + potentialEdgeCount);
		final double rciVal = (double) actualEdges.size() / (double) potentialEdgeCount;

		return rciVal;
	}

}
