package edu.usc.softarch.arcade.facts.driver;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.collections15.Factory;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.logging.log4j.Logger;


import com.google.common.collect.Iterables;

import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Forest;
import edu.uci.ics.jung.graph.Tree;
import edu.uci.ics.jung.graph.util.Pair;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;

public class DominatorGroundTruthAnalyzer {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(DominatorGroundTruthAnalyzer.class);

	static Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i = 0;

		@Override
		public Integer create() {
			return i++;
		}
	};

	static Factory<String> vertexFactory = new Factory<String>() {
		int i = 0;

		@Override
		public String create() {
			return "V" + i++;
		}
	};

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String depsFilename = args[0];
		final String clustersFilename = args[1];
		final String outFilename = args[2];

		RsfReader.loadRsfDataFromFile(depsFilename);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile(clustersFilename);
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;

		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);

		final Map<String, Set<MutablePair<String, String>>> internalEdgeMap = ClusterUtil.buildInternalEdgesPerCluster(clusterMap, depFacts);

		/*
		 * logger.debug("Printing internal edges of clusters: "); for (String
		 * clusterName : internalEdgeMap.keySet()) {
		 * Set<MutablePair<String,String>> edges =
		 * internalEdgeMap.get(clusterName); logger.debug(clusterName); for
		 * (MutablePair<String,String> edge : edges) { logger.debug("\t" +
		 * edge); } }
		 */

		// Map<String,Set<MutablePair<String,String>>> externalEdgeMap =
		// ClusterUtil.buildExternalEdgesPerCluster(clusterMap, depFacts);

		/*
		 * logger.debug("Printing external edges of clusters: "); for (String
		 * clusterName : externalEdgeMap.keySet()) {
		 * Set<MutablePair<String,String>> edges =
		 * externalEdgeMap.get(clusterName); logger.debug(clusterName); for
		 * (MutablePair<String,String> edge : edges) { logger.debug("\t" +
		 * edge); } }
		 */

		// Map<String,Set<MutablePair<String,String>>> intoEdgeMap =
		// ClusterUtil.buildEdgesIntoEachCluster(clusterMap, depFacts);

		/*
		 * logger.debug("Printing edges into of clusters: "); for (String
		 * clusterName : intoEdgeMap.keySet()) { Set<MutablePair<String,String>>
		 * edges = intoEdgeMap.get(clusterName); logger.debug(clusterName); for
		 * (MutablePair<String,String> edge : edges) { logger.debug("\t" +
		 * edge); } }
		 */

		final Map<String, Double> ratioMap = computeDominatorCriteriaIndicatorValues(clusterMap, internalEdgeMap);

		try {
			final FileWriter out = new FileWriter(outFilename);
			for (final Entry<?, ?> entry : ratioMap.entrySet()) {
				out.write(entry.getKey() + "," + entry.getValue() + "\n");
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		// runGeneratedGraphTest(args);

	}

	public static Map<String, Double> computeDominatorCriteriaIndicatorValues(final Map<String, Set<String>> clusterMap, final Map<String, Set<MutablePair<String, String>>> internalEdgeMap) {
		final Map<String, String> topDomMap = new HashMap<String, String>();
		final Map<String, Integer> topDomCountMap = new HashMap<String, Integer>();
		final Map<String, Integer> numForestMap = new HashMap<String, Integer>();
		final Map<String, Forest<String, Integer>> forestMap = new HashMap<String, Forest<String, Integer>>();

		final String start = "ST";
		for (final String clusterName : internalEdgeMap.keySet()) {
			final Set<MutablePair<String, String>> edges = internalEdgeMap.get(clusterName);
			final DirectedGraph<String, Integer> graph = new DirectedSparseGraph<String, Integer>();
			for (final MutablePair<String, String> edge : edges) {
				final String source = edge.getLeft();
				final String target = edge.getRight();
				graph.addEdge(edgeFactory.create(), source, target);
			}

			logger.debug("Printing graph...");
			for (final Integer edge : graph.getEdges()) {
				final Pair<String> pair = graph.getEndpoints(edge);
				logger.debug(pair.getFirst() + ", " + pair.getSecond());
			}

			// DirectedGraph<String,Integer> origGraph =
			// dupeDirectedSparseGraph(graph);

			final Set<String> verticesWithNoPreds = new HashSet<String>();

			for (final String vertex : graph.getVertices()) {
				if (graph.getPredecessorCount(vertex) == 0) {
					System.out.println(vertex + " has no predecessors");
					verticesWithNoPreds.add(vertex);
				}
			}

			for (final String target : verticesWithNoPreds) {
				graph.addEdge(edgeFactory.create(), start, target);
			}

			logger.debug("Graph with new start:");
			logger.debug(graph);

			final MinimumSpanningForest<String, Integer> minSpanForest = new MinimumSpanningForest<String, Integer>(graph, new DelegateForest<String, Integer>(), start);

			final Forest<String, Integer> forest = minSpanForest.getForest();

			numForestMap.put(clusterName, forest.getTrees().size());
			forestMap.put(clusterName, forest);

			// start = setStartVertexToFirstWithNoPredecessors(graph, vertices,
			// start);

			final Map<String, Integer> domCountMap = computeDominatorInfo(graph, start);

			String topDom = null;
			int topCount = 0;
			for (final Entry<String, Integer> entry : domCountMap.entrySet()) {
				if (topDom == null) {
					topDom = entry.getKey();
					topCount = entry.getValue();
				}
				final String dom = entry.getKey();
				final int count = entry.getValue();
				if (count > topCount && dom.trim() != "ST") {
					topDom = dom;
					topCount = count;
				}
			}

			topDomMap.put(clusterName, topDom);
			topDomCountMap.put(clusterName, topCount);
			logger.debug("Top dominator other than ST is " + topDom + " : " + topCount);
			logger.debug("No. of entities of " + clusterName + ": " + clusterMap.get(clusterName).size());
		}

		// int properDomCount = 0;
		final Map<String, Integer> properDomMap = new HashMap<String, Integer>();
		for (final String clusterName : topDomMap.keySet()) {
			if (topDomMap.get(clusterName) != null && !topDomMap.get(clusterName).equals("ST")) {
				final int currTopCount = topDomCountMap.get(clusterName);
				final int clusterSize = clusterMap.get(clusterName).size();
				if ((double) currTopCount > (double) (clusterSize / 2)) {
					// properDomCount++;
					properDomMap.put(clusterName, currTopCount);
				}
			}
		}

		logger.debug("Cluster with proper dominators: ");
		for (final String clusterName : properDomMap.keySet()) {
			logger.debug(clusterName + ", " + topDomCountMap.get(clusterName) + ", " + clusterMap.get(clusterName).size() + ", " + topDomMap.get(clusterName));
		}

		final Set<String> clustersNoPropDoms = new HashSet<String>(clusterMap.keySet());
		clustersNoPropDoms.removeAll(properDomMap.keySet());

		logger.debug("");
		logger.debug("Clusters withOUT proper dominators: ");
		for (final String clusterName : clustersNoPropDoms) {
			logger.debug(clusterName);
		}

		logger.debug("No. of clusters with proper dominators: " + properDomMap.keySet().size());
		logger.debug("No. of total clusters: " + clusterMap.keySet().size());
		logger.debug("Percentage of proper dominators: " + (double) properDomMap.keySet().size() / (double) clusterMap.keySet().size());

		logger.debug("Number of trees in minimum spanning tree forest for each cluster:");
		for (final Entry<?, ?> entry : numForestMap.entrySet()) {
			logger.debug(entry);
		}

		logger.debug("Number of trees in minimum spanning tree forest for each cluster:");
		for (final Entry<?, ?> entry : numForestMap.entrySet()) {
			logger.debug(entry);
		}

		final Map<String, Double> ratioMap = new TreeMap<String, Double>();
		logger.debug("Comparing largest tree of cluster to entities of cluster:");
		for (final Entry<?, ?> entry : forestMap.entrySet()) {
			final String clusterName = (String) entry.getKey();
			final Set<String> entities = clusterMap.get(clusterName);
			final Forest<String, Integer> forest = (Forest<String, Integer>) entry.getValue();
			if (forest.getTrees().size() == 0) {
				logger.debug(clusterName + " has an empty forest");
				ratioMap.put(clusterName, (double) 0);
				continue;
			}
			Tree<String, Integer> largestTree = Iterables.get(forest.getTrees(), 0);
			for (final Tree<String, Integer> tree : forest.getTrees()) { // identify
				// the
				// largest
				// tree
				if (tree.getVertexCount() > largestTree.getVertexCount()) {
					largestTree = tree;
				}
			}
			final int largestTreeTrueSize = largestTree.containsVertex(start) ? largestTree.getVertexCount() - 1 : largestTree.getVertexCount();
					double ratio = (double) largestTreeTrueSize / (double) entities.size();
					if (ratio > 1) {
						ratio = 1;
					}
					logger.debug(clusterName + ", numEntites: " + entities.size() + ", size of largest tree: " + largestTreeTrueSize + ", ratio: " + ratio);
					ratioMap.put(clusterName, ratio);
		}

		int numClustersWithOneTreeInForest = 0;
		for (final Entry<?, ?> entry : numForestMap.entrySet()) {
			if (entry.getValue().equals(1)) {
				numClustersWithOneTreeInForest++;
			}
		}

		logger.debug("Number of clusters with a forest with only one tree: " + numClustersWithOneTreeInForest);
		return ratioMap;
	}

	private static Map<String, Integer> computeDominatorInfo(final DirectedGraph<String, Integer> graph, final String start) {
		final Map<String, Set<String>> domMap = new HashMap<String, Set<String>>();
		final Set<String> startDominators = new HashSet<String>();
		startDominators.add(start);
		domMap.put(start, startDominators);

		final Set<String> verticesMinusStart = new HashSet<String>(graph.getVertices());
		verticesMinusStart.remove(start);

		for (final String vertex : verticesMinusStart) {
			domMap.put(vertex, new HashSet<String>(graph.getVertices()));
		}

		boolean changedDom = true;
		while (changedDom) {
			changedDom = false;
			for (final String vertex : verticesMinusStart) {
				final Set<String> predDomIntersection = new HashSet<String>();
				if (graph.getPredecessorCount(vertex) > 0) {
					predDomIntersection.addAll(graph.getVertices());
				}
				for (final String pred : graph.getPredecessors(vertex)) {
					final Set<String> domOfPred = domMap.get(pred);
					predDomIntersection.retainAll(domOfPred);
				}
				final Set<String> oldDomOfVertex = domMap.get(vertex);
				final Set<String> newDomOfVertex = new HashSet<String>(predDomIntersection);
				newDomOfVertex.add(vertex);
				if (!newDomOfVertex.equals(oldDomOfVertex)) {
					changedDom = true;
				}
				domMap.put(vertex, newDomOfVertex);
			}
		}

		for (final String vertex : graph.getVertices()) {
			final Set<String> dominators = domMap.get(vertex);
			logger.debug("dom of " + vertex + ": " + dominators);
		}

		final Map<String, Integer> domCountMap = new HashMap<String, Integer>();
		for (final String vertex : graph.getVertices()) {
			final Set<String> dominators = domMap.get(vertex);
			for (final String dom : dominators) {
				if (domCountMap.get(dom) == null) {
					domCountMap.put(dom, 1);
				} else {
					int count = domCountMap.get(dom);
					count++;
					domCountMap.put(dom, count);
				}
			}
		}

		for (final Entry<String, Integer> entry : domCountMap.entrySet()) {
			logger.debug(entry.getKey() + " : " + entry.getValue());
		}
		return domCountMap;
	}

}
