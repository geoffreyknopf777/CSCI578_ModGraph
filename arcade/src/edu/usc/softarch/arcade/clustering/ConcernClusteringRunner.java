package edu.usc.softarch.arcade.clustering;

//import static edu.usc.softarch.arcade.clustering.ClusteringAlgoRunner.getFastClusters();

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.SimMeasure;
import edu.usc.softarch.arcade.topics.DocTopics;
import edu.usc.softarch.arcade.topics.TopicModelExtractionMethod;
import edu.usc.softarch.arcade.topics.TopicUtil;
import edu.usc.softarch.arcade.util.ExtractionContext;
import edu.usc.softarch.arcade.util.StopWatch;

public class ConcernClusteringRunner extends ClusteringAlgoRunner {
	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ConcernClusteringRunner.class);
	private boolean useClusterGain;
	private int numTopics;

	// public TopicModelExtractionMethod tmeMethod =
	// TopicModelExtractionMethod.VAR_MALLET_FILE;
	// public String srcDir = "";
	// public int numTopics = 0;
	// private String topicModelFilename;

	public int getNumTopics() {
		logger.traceEntry();
		logger.traceExit();
		return numTopics;
	}

	public void setNumTopics(final int numTopics) {
		logger.entry(numTopics);
		this.numTopics = numTopics;
		logger.traceExit();
	}

	/**
	 * @param vecs
	 *            feature vectors (dependencies) of entities
	 * @param tmeMethod
	 *            method of topic model extraction
	 * @param srcDir
	 *            directories with java or c files
	 * @param numTopics
	 *            number of topics to extract
	 */
	ConcernClusteringRunner(final FastFeatureVectors vecs, final TopicModelExtractionMethod tmeMethod, final String srcDir, final String artifactsDir, final int numTopics,
			final String topicModelFilename, final String docTopicsFilename, final String topWordsFilename) {
		logger.entry(vecs, tmeMethod, srcDir, artifactsDir, numTopics, topicModelFilename, docTopicsFilename, topWordsFilename);
		setFastFeatureVectors(vecs);
		initializeClusters(srcDir);
		initializeDocTopicsForEachFastCluster(tmeMethod, srcDir, artifactsDir, numTopics, topicModelFilename, docTopicsFilename, topWordsFilename);
		logger.traceExit();
	}

	public void computeClustersWithConcernsAndFastClusters(final StoppingCriterion stoppingCriterion) {
		logger.entry(stoppingCriterion);
		final StopWatch loopSummaryStopwatch = new StopWatch();

		// SimCalcUtil.verifySymmetricClusterOrdering(clusters);
		/*
		 * if (logger.isDebugEnabled()) { printMostSimilarClustersForEachCluster(); }
		 */
		loopSummaryStopwatch.start();
		final StopWatch matrixCreateTimer = new StopWatch();
		matrixCreateTimer.start();
		final List<List<Double>> simMatrix = createSimilarityMatrix(ClusteringAlgoRunner.getFastClusters());
		matrixCreateTimer.stop();
		logger.debug("time to create similarity matrix: " + matrixCreateTimer.getElapsedTime());

		useClusterGain = stoppingCriterion.getClass().equals(ClusterGainStoppingCriterion.class);
		while (stoppingCriterion.notReadyToStop()) {
			if (useClusterGain && !((ClusterGainStoppingCriterion) stoppingCriterion).isSecondRun()) {
				final double clusterGain = ClusterUtil.computeClusterGainUsingTopics(getFastClusters());
				checkAndUpdateClusterGain(clusterGain);
			}

			final StopWatch timer = new StopWatch();
			timer.start();
			// identifyMostSimClustersForConcernsMultiThreaded(data);
			final MaxSimData data = identifyMostSimClusters(simMatrix);
			timer.stop();
			logger.debug("time to identify two most similar clusters: " + timer.getElapsedTime());

			final boolean isPrintingTwoMostSimilar = false;
			if (isPrintingTwoMostSimilar) {
				// printDataForTwoMostSimilarClustersWithTopicsForConcerns(data);
				printDataForTwoMostSimilarClustersWithTopicsForConcerns(data);
			}

			// printDataForClustersBeingMerged(data);

			final FastCluster newCluster = mergeFastClustersUsingTopics(data);

			/*
			 * if (logger.isDebugEnabled()) { printStructuralDataForClustersBeingMerged(newCluster); //logger.debug("\t\t" // + newCluster.docTopicItem // .toStringWithLeadingTabsAndLineBreaks(2)); }
			 */

			updateFastClustersAndSimMatrixToReflectMergedCluster(data, newCluster, simMatrix);

			performPostProcessingConditionally();

			final boolean isShowingPostMergeClusterInfo = false;

			logger.debug("after merge, clusters size: " + getFastClusters().size());
			if (isShowingPostMergeClusterInfo) {
				ClusterUtil.printFastClustersByLine(ClusteringAlgoRunner.getFastClusters());
			}
		}

		loopSummaryStopwatch.stop();
		logger.info("Time in milliseconds to compute clusters: " + loopSummaryStopwatch.getElapsedTime());
		if (useClusterGain) {
			logger.info("max cluster gain: " + ClusteringAlgoRunner.getMaxClusterGain());
			logger.info("num clusters at max cluster gain: " + getNumClustersAtMaxClusterGain());
		}
		logger.traceExit();
	}

	private static MaxSimData identifyMostSimClusters(final List<List<Double>> simMatrix) {
		logger.entry(simMatrix);
		if (simMatrix.size() != getFastClusters().size()) {
			throw new IllegalArgumentException("expected simMatrix.size():" + simMatrix.size() + " to be getFastClusters().size(): " + getFastClusters().size());
		}
		for (final List<Double> col : simMatrix) {
			if (col.size() != getFastClusters().size()) {
				throw new IllegalArgumentException("expected col.size():" + col.size() + " to be getFastClusters().size(): " + getFastClusters().size());
			}
		}

		final int length = simMatrix.size();
		final MaxSimData msData = new MaxSimData();
		msData.rowIndex = 0;
		msData.colIndex = 0;
		double smallestJsDiv = Double.MAX_VALUE;
		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				final double currJsDiv = simMatrix.get(i).get(j);
				if (currJsDiv < smallestJsDiv && i != j) {
					smallestJsDiv = currJsDiv;
					msData.rowIndex = i;
					msData.colIndex = j;
				}
			}
		}
		msData.currentMaxSim = smallestJsDiv;
		logger.traceExit();
		return msData;
	}

	private int initializeDocTopicsForEachFastCluster(final TopicModelExtractionMethod tmeMethod, final String srcDir, final String artifactsDir, final int numTopics, final String topicModelFilename,
			final String docTopicsFilename, final String topWordsFilename) {
		logger.entry(tmeMethod, srcDir, artifactsDir, docTopicsFilename, topWordsFilename);
		logger.info("Initializing doc-topics for each cluster...");

		if (tmeMethod == TopicModelExtractionMethod.VAR_MALLET_FILE) {
			logger.info("Getting topics from mallet file");
			TopicUtil.docTopics = TopicUtil.getDocTopicsFromVariableMalletDocTopicsFile();
			for (final FastCluster c : getFastClusters()) {
				TopicUtil.setDocTopicForFastClusterForMalletFile(TopicUtil.docTopics, c);
			}
		} else if (tmeMethod == TopicModelExtractionMethod.MALLET_API) {
			logger.info("Using MALLET to generate topic model");
			TopicUtil.docTopics = new DocTopics(srcDir, artifactsDir, numTopics, topicModelFilename, docTopicsFilename, topWordsFilename);
			this.numTopics = TopicUtil.docTopics.generateTopicModel(numTopics);
			// final File topicModelFile =
			// FileUtil.checkFile(topicModelFilename, false, true);
			// topicModelFile.renameTo(FileUtil.checkFile(topicModelFilename.replaceFirst("TTT",
			// Integer.toString(this.numTopics)), false, false));

			for (final FastCluster c : getFastClusters()) {
				TopicUtil.setDocTopicForFastClusterForMalletApi(TopicUtil.docTopics, c);
			}
		}

		final List<FastCluster> jspRemoveList = new ArrayList<FastCluster>();
		for (final FastCluster c : getFastClusters()) {
			if (c.getName().endsWith("_jsp")) {
				logger.info("Adding " + c.getName() + " to jspRemoveList...");
				jspRemoveList.add(c);
			}
		}

		logger.debug("Removing jspRemoveList from getFastClusters()");
		for (final FastCluster c : jspRemoveList) {
			getFastClusters().remove(c);
		}

		final Map<String, String> parentClassMap = new HashMap<String, String>();
		for (final FastCluster c : getFastClusters()) {
			if (c.getName().contains("$")) {
				logger.debug("Nested class singleton cluster with missing doc topic: " + c.getName());
				final String[] tokens = c.getName().split("\\$");
				final String parentClassName = tokens[0];
				parentClassMap.put(c.getName(), parentClassName);
			}
		}

		logger.info("Removing singleton clusters which have no doc-topic and are non-inner classes...");
		final List<FastCluster> excessClusters = new ArrayList<FastCluster>();
		for (final FastCluster c : getFastClusters()) {
			if (c.docTopicItem == null) {
				if (!c.getName().contains("$")) {
					logger.debug("Could not find doc-topic for non-inner class: " + c.getName());
					excessClusters.add(c);
				}
			}
		}

		final List<FastCluster> excessInners = new ArrayList<FastCluster>();
		for (final FastCluster excessCluster : excessClusters) {
			for (final FastCluster cluster : getFastClusters()) {
				if (parentClassMap.containsKey(cluster)) {
					final String parentClass = parentClassMap.get(cluster);
					if (parentClass.equals(excessCluster.getName())) {
						excessInners.add(cluster);
					}
				}
			}
		}

		getFastClusters().removeAll(excessClusters);
		getFastClusters().removeAll(excessInners);

		final ArrayList<FastCluster> updatedFastClusters = new ArrayList<FastCluster>(ClusteringAlgoRunner.getFastClusters());
		for (final String key : parentClassMap.keySet()) {
			for (final FastCluster nestedCluster : getFastClusters()) {
				if (nestedCluster.getName().equals(key)) {
					for (final FastCluster parentCluster : getFastClusters()) {
						if (parentClassMap.get(key).equals(parentCluster.getName())) {
							final FastCluster mergedCluster = mergeFastClustersUsingTopics(nestedCluster, parentCluster);
							updatedFastClusters.remove(parentCluster);
							updatedFastClusters.remove(nestedCluster);
							updatedFastClusters.add(mergedCluster);
						}
					}
				}
			}
		}
		ClusteringAlgoRunner.setFastClusters(updatedFastClusters);

		final List<FastCluster> clustersWithMissingDocTopics = new ArrayList<FastCluster>();
		for (final FastCluster c : getFastClusters()) {
			if (c.docTopicItem == null) {
				logger.debug("Could not find doc-topic for: " + c.getName());
				clustersWithMissingDocTopics.add(c);
			}
		}

		logger.info("Removing clusters with missing doc topics...");
		getFastClusters().removeAll(clustersWithMissingDocTopics);

		final boolean ignoreMissingDocTopics = true;
		if (ignoreMissingDocTopics) {
			logger.info("Removing clusters with missing doc topics...");
			for (final FastCluster c : clustersWithMissingDocTopics) {
				logger.debug("Removing cluster: " + c.getName());
				getFastClusters().remove(c);
			}
			logger.info("New initial clusters size: " + getFastClusters().size());
		}

		logger.debug("New initial fast clusters:");
		logger.debug(Joiner.on("\n").join(getFastClusters()));
		logger.exit(numTopics);
		return this.numTopics;
	}

	private static void printDataForTwoMostSimilarClustersWithTopicsForConcerns(final MaxSimData data) {
		logger.entry(data);
		System.out.println("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", \nMax Similar Clusters: ");
		System.out.println("sim value(" + data.rowIndex + "," + data.colIndex + "): " + data.currentMaxSim);
		System.out.println("\n");
		System.out.println("most sim clusters: " + getFastClusters().get(data.rowIndex).getName() + ", " + getFastClusters().get(data.colIndex).getName());
		TopicUtil.printTwoDocTopics(getFastClusters().get(data.rowIndex).docTopicItem, getFastClusters().get(data.colIndex).docTopicItem);

		System.out.println("before merge, fast clusters size: " + getFastClusters().size());
		logger.traceExit();

	}

	private static FastCluster mergeFastClustersUsingTopics(final MaxSimData data) {
		logger.entry(data);
		final FastCluster cluster = getFastClusters().get(data.rowIndex);
		final FastCluster otherCluster = getFastClusters().get(data.colIndex);
		logger.traceExit();
		return mergeFastClustersUsingTopics(cluster, otherCluster);
	}

	private static FastCluster mergeFastClustersUsingTopics(final FastCluster cluster, final FastCluster otherCluster) {
		logger.entry(cluster, otherCluster);
		final FastCluster newCluster = new FastCluster(ClusteringAlgorithmType.LIMBO, cluster, otherCluster);

		newCluster.docTopicItem = TopicUtil.mergeDocTopicItems(cluster.docTopicItem, otherCluster.docTopicItem);
		logger.traceExit();
		return newCluster;
	}

	private static void updateFastClustersAndSimMatrixToReflectMergedCluster(final MaxSimData data, final FastCluster newCluster, final List<List<Double>> simMatrix) {
		logger.entry(data, newCluster, simMatrix);
		final FastCluster cluster = getFastClusters().get(data.rowIndex);
		final FastCluster otherCluster = getFastClusters().get(data.colIndex);

		int greaterIndex = -1, lesserIndex = -1;
		if (data.rowIndex == data.colIndex) {
			throw new IllegalArgumentException("data.rowIndex: " + data.rowIndex + " should not be the same as data.colIndex: " + data.colIndex);
		}
		if (data.rowIndex > data.colIndex) {
			greaterIndex = data.rowIndex;
			lesserIndex = data.colIndex;
		} else if (data.rowIndex < data.colIndex) {
			greaterIndex = data.colIndex;
			lesserIndex = data.rowIndex;
		}

		simMatrix.remove(greaterIndex);
		for (final List<Double> col : simMatrix) {
			col.remove(greaterIndex);
		}

		simMatrix.remove(lesserIndex);
		for (final List<Double> col : simMatrix) {
			col.remove(lesserIndex);
		}

		getFastClusters().remove(cluster);
		getFastClusters().remove(otherCluster);

		getFastClusters().add(newCluster);

		final List<Double> newRow = new ArrayList<Double>(getFastClusters().size());

		for (int i = 0; i < getFastClusters().size(); i++) {
			newRow.add(Double.MAX_VALUE);
		}

		simMatrix.add(newRow);

		for (int i = 0; i < getFastClusters().size() - 1; i++) {
			// value to create
			// new column for
			// all but the last
			// row, which
			// already has the
			// column for the
			// new cluster
			simMatrix.get(i).add(Double.MAX_VALUE);
		}

		if (simMatrix.size() != getFastClusters().size()) {
			throw new RuntimeException("simMatrix.size(): " + simMatrix.size() + " is not equal to getFastClusters().size(): " + getFastClusters().size());
		}

		for (int i = 0; i < getFastClusters().size(); i++) {
			if (simMatrix.get(i).size() != getFastClusters().size()) {
				throw new RuntimeException("simMatrix.get(" + i + ").size(): " + simMatrix.get(i).size() + " is not equal to getFastClusters().size(): " + getFastClusters().size());
			}
		}

		for (int i = 0; i < getFastClusters().size(); i++) {
			final FastCluster currCluster = getFastClusters().get(i);
			double currJSDivergence = Double.MAX_VALUE;
			if (Config.getCurrSimMeasure().equals(SimMeasure.js)) {
				currJSDivergence = SimCalcUtil.getJSDivergence(newCluster, currCluster);
			} else if (Config.getCurrSimMeasure().equals(SimMeasure.scm)) {
				currJSDivergence = FastSimCalcUtil.getStructAndConcernMeasure(ClusteringAlgoRunner.getNumberOfEntitiesToBeClustered(), newCluster, currCluster);
			} else {
				throw new IllegalArgumentException("Invalid similarity measure: " + Config.getCurrSimMeasure());
			}
			simMatrix.get(getFastClusters().size() - 1).set(i, currJSDivergence);
			simMatrix.get(i).set(getFastClusters().size() - 1, currJSDivergence);
		}

		// SimCalcUtil.verifySymmetricClusterOrdering(clusters);
		// newCluster.addClustersToPriorityQueue(clusters);
		logger.traceExit();
	}

	/**
	 *
	 * @param clusters
	 * @param data
	 * @param cluster
	 * @deprecated Found with a comment that it was deprecated
	 */
	@Deprecated
	public static void identifyMostSimilarClusterForConcerns(final List<FastCluster> clusters, final MaxSimData data, final FastCluster cluster) {
		logger.entry(clusters, data, cluster);
		// HashMap<HashSet<String>, Double> map = new HashMap<HashSet<String>,
		// Double>();

		for (final FastCluster otherCluster : clusters) {
			final boolean isShowingEachSimilarityComparison = false;
			if (isShowingEachSimilarityComparison) {
				if (logger.isDebugEnabled()) {
					logger.debug("Comparing " + cluster.getName() + " to " + otherCluster.getName());
					TopicUtil.printTwoDocTopics(cluster.docTopicItem, otherCluster.docTopicItem);
				}
			}
			if (cluster.getName().equals(otherCluster.getName())) {
				continue;
			}

			/*
			 * HashSet<String> clusterPair = new HashSet<String>(); clusterPair.add(cluster.getName()); clusterPair.add(otherCluster.getName());
			 */

			double currJSDivergence = 0;
			// if (map.containsKey(clusterPair)) {
			// currJSDivergence = map.get(clusterPair);
			// } else {
			currJSDivergence = SimCalcUtil.getJSDivergence(cluster, otherCluster);
			// map.put(clusterPair, currJSDivergence);
			// }

			if (currJSDivergence <= data.currentMaxSim) {
				data.currentMaxSim = currJSDivergence;
				data.c1 = cluster;
				data.c2 = otherCluster;
				final boolean showCurrentMostSimilar = false;
				if (showCurrentMostSimilar) {
					if (logger.isDebugEnabled()) {
						logger.debug("Updated most similar values: ");
						logger.debug("currentMostSim: " + data.currentMaxSim);
						logger.debug("c1: " + data.c1.getName());
						logger.debug("c2: " + data.c2.getName());
						TopicUtil.printTwoDocTopics(data.c1.docTopicItem, data.c2.docTopicItem);
					}
				}
			}
		}

		final boolean isShowingMaxSimClusters = false;
		if (isShowingMaxSimClusters) {
			if (logger.isDebugEnabled()) {
				logger.debug("In, " + ExtractionContext.getCurrentClassAndMethodName() + " Max Similar Clusters: ");
				logger.debug(data.c1.getName());
				logger.debug(data.c2.getName());
				logger.debug(data.currentMaxSim);
				logger.debug("\n");
			}
		}
		logger.traceExit();
	}

	public static List<List<Double>> createSimilarityMatrix(final List<FastCluster> clusters) {
		logger.entry(clusters);
		final List<List<Double>> simMatrixObj = new ArrayList<List<Double>>(clusters.size());

		for (int i = 0; i < clusters.size(); i++) {
			simMatrixObj.add(new ArrayList<Double>(clusters.size()));
		}

		for (int i = 0; i < clusters.size(); i++) {
			final FastCluster cluster = clusters.get(i);
			for (int j = 0; j < clusters.size(); j++) {
				final FastCluster otherCluster = clusters.get(j);
				final boolean isShowingEachSimilarityComparison = false;
				if (isShowingEachSimilarityComparison) {
					if (logger.isDebugEnabled()) {
						logger.debug("Comparing " + cluster.getName() + " to " + otherCluster.getName());
						TopicUtil.printTwoDocTopics(cluster.docTopicItem, otherCluster.docTopicItem);
					}
				}

				/*
				 * if (cluster.getName().equals(otherCluster.getName())) { continue; }
				 */

				/*
				 * HashSet<String> clusterPair = new HashSet<String>(); clusterPair.add(cluster.getName()); clusterPair.add(otherCluster.getName());
				 */

				double currJSDivergence = 0;
				// if (map.containsKey(clusterPair)) {
				// currJSDivergence = map.get(clusterPair);
				// } else {
				if (Config.getCurrSimMeasure().equals(SimMeasure.js)) {
					currJSDivergence = SimCalcUtil.getJSDivergence(cluster, otherCluster);
				} else if (Config.getCurrSimMeasure().equals(SimMeasure.scm)) {
					currJSDivergence = FastSimCalcUtil.getStructAndConcernMeasure(ClusteringAlgoRunner.getNumberOfEntitiesToBeClustered(), cluster, otherCluster);
				} else {
					throw new IllegalArgumentException("Invalid similarity measure: " + Config.getCurrSimMeasure());
				}

				simMatrixObj.get(i).add(currJSDivergence);
			}

		}
		logger.traceExit();
		return simMatrixObj;
	}

}
