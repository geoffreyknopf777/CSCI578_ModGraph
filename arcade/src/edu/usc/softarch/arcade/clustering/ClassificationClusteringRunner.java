package edu.usc.softarch.arcade.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.SimMeasure;

public class ClassificationClusteringRunner {
	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ClassificationClusteringRunner.class);

	// private static ArrayList<FastCluster> fastClusters;

	public ClassificationClusteringRunner(final FastFeatureVectors vecs, final String srcDir, final String artifactsDir, final String classifierFileName) {
		logger.info("Creating Classification Clustering Runner");
		ClusteringAlgoRunner.setFastFeatureVectors(vecs);
		ClusteringAlgoRunner.initializeClusters(srcDir);
		// initializeClassificationForEachFastCluster(srcDir, artifactsDir, classifierFileName);
	}

	// private void initializeClassificationForEachFastCluster(final String srcDirName, final String artifactsDirName, final String classifierFileName) {
	// logger.entry(srcDirName, artifactsDirName);
	// logger.info("Initializing classification of each cluster...");
	//
	// logger.info("Using MALLET to generate classifications");
	// final File srcDir = FileUtil.checkDir(srcDirName, false, true);
	// final File artifactsDir = FileUtil.checkDir(artifactsDirName, false, true);
	// final File classifierFile = FileUtil.checkFile(classifierFileName, false, true);
	// new Classifications(srcDir, artifactsDir, classifierFile);
	// // cl.generateClassifications();
	// logger.traceExit();
	// }

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
						// TopicUtil.printTwoDocTopics(cluster.docTopicItem,
						// otherCluster.docTopicItem);
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
		logger.exit(simMatrixObj);
		return simMatrixObj;
	}

}
