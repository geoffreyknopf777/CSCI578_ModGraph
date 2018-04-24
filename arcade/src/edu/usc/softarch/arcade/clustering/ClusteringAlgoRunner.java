package edu.usc.softarch.arcade.clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Granule;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.util.FileListing;

public class ClusteringAlgoRunner {

	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ClusteringAlgoRunner.class);
	private static ArrayList<FastCluster> fastClusters;

	public static void setFastClusters(ArrayList<FastCluster> fastClusters) {
		ClusteringAlgoRunner.fastClusters = fastClusters;
	}

	public static ArrayList<FastCluster> getFastClusters() {
		return fastClusters;
	}

	protected static ArrayList<Cluster> clusters;
	protected static FastFeatureVectors fastFeatureVectors;
	private static double maxClusterGain = 0;

	public static double getMaxClusterGain() {
		return maxClusterGain;
	}

	public static void setMaxClusterGain(double maxClusterGain) {
		ClusteringAlgoRunner.maxClusterGain = maxClusterGain;
	}

	private static int numClustersAtMaxClusterGain;

	public static int getNumClustersAtMaxClusterGain() {
		return numClustersAtMaxClusterGain;
	}

	public static void setNumClustersAtMaxClusterGain(int numClustersAtMaxClusterGain) {
		ClusteringAlgoRunner.numClustersAtMaxClusterGain = numClustersAtMaxClusterGain;
	}

	private static int numberOfEntitiesToBeClustered = 0;

	/**
	 * @return the numberOfEntitiesToBeClustered
	 */
	public static int getNumberOfEntitiesToBeClustered() {
		return numberOfEntitiesToBeClustered;
	}

	/**
	 * @param numberOfEntitiesToBeClustered
	 *            the numberOfEntitiesToBeClustered to set
	 */
	public static void setNumberOfEntitiesToBeClustered(int numberOfEntitiesToBeClustered) {
		ClusteringAlgoRunner.numberOfEntitiesToBeClustered = numberOfEntitiesToBeClustered;
	}

	/**
	 * Initialize clusters from source directory
	 *
	 * @param srcDir
	 *            - Source directory
	 */
	protected static void initializeClusters(String srcDir) {
		fastClusters = new ArrayList<FastCluster>();

		for (final String name : fastFeatureVectors.getFeatureVectorNames()) {
			logger.debug("Feature Vector name: " + name);
			final BitSet featureSet = fastFeatureVectors.getNameToFeatureSetMap().get(name);
			final FastCluster fastCluster = new FastCluster(name, featureSet, fastFeatureVectors.getNamesInFeatureSet());
			addClusterConditionally(fastCluster);
		}

		// Doesn't look like this is considering whether we're using C or Java
		try {
			if (fastClusters.isEmpty()) {
				final List<File> javaFiles = FileListing.getFileListing(new File(srcDir), ".java");

				for (final File javaFile : javaFiles) {
					final FastCluster cluster = new FastCluster(javaFile.getPath().toString());
					fastClusters.add(cluster);
				}
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		logger.debug("Listing initial cluster names using for-each...");
		for (final FastCluster cluster : fastClusters)
			logger.debug(cluster.getName());

		// logger.info("Listing initial cluster names using indexed loop...");
		// for (int i = 0; i < fastClusters.size(); i++) {
		// final FastCluster cluster = fastClusters.get(i);
		// logger.info(cluster.getName());
		// }

		numberOfEntitiesToBeClustered = fastClusters.size();
		logger.info("number of initial clusters: " + numberOfEntitiesToBeClustered);
	}

	/**
	 * Add a cluster depending on the language and granule
	 *
	 * @param fastCluster
	 *            - the cluster to be added
	 */
	private static void addClusterConditionally(FastCluster fastCluster) {
		if (Config.ignoreDependencyFilters) {
			fastClusters.add(fastCluster);
			return;
		}

		if (Config.getSelectedLanguage().equals(Language.c)) {
			final Pattern p = Pattern.compile("\\.(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)$");
			if (Config.getClusteringGranule().equals(Granule.file) && isSingletonClusterNonexcluded(fastCluster) && !fastCluster.getName().startsWith("/") &&

			p.matcher(fastCluster.getName()).find())
				fastClusters.add(fastCluster);
			else
				logger.debug("Excluding file: " + fastCluster.getName());
		}
		if (Config.getClusteringGranule().equals(Granule.func)) {
			if (fastCluster.getName().equals("\"##\""))
				return;
			fastClusters.add(fastCluster);
		}
		if (Config.getSelectedLanguage().equals(Language.java))
			if (Config.isClassInSelectedPackages(fastCluster.getName()))
				fastClusters.add(fastCluster);
	}

	public static boolean isSingletonClusterNonexcluded(FastCluster fastCluster) {
		if (Config.getExcludedEntities() == null)
			return true;
		return !Config.getExcludedEntities().contains(fastCluster.getName());
	}

	/**
	 *
	 * @param clusterGain
	 *            current cluster gain
	 */
	protected static void checkAndUpdateClusterGain(double clusterGain) {
		logger.info("Current max cluster gain / Current cluster gain:" + maxClusterGain + " / " + clusterGain);
		if (clusterGain > maxClusterGain) {
			logger.info("Updating max cluster gain and num clusters at it...");
			maxClusterGain = clusterGain;
			numClustersAtMaxClusterGain = fastClusters.size();
		}
	}

	protected static void printTwoMostSimilarClustersUsingStructuralData(MaxSimData maxSimData) {
		if (logger.isDebugEnabled()) {
			logger.debug("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", \nMax Similar Clusters: ");

			ClusterUtil.printSimilarFeatures(maxSimData.c1, maxSimData.c2, fastFeatureVectors);

			logger.debug(maxSimData.currentMaxSim);
			logger.debug("\n");

			logger.debug("before merge, clusters size: " + fastClusters.size());

		}
	}

	/**
	 * Set the fast feature vectors instance to use
	 *
	 * @param inFastFeatureVectors
	 *            - FastFeatureVectors to use
	 */
	public static void setFastFeatureVectors(FastFeatureVectors inFastFeatureVectors) {
		fastFeatureVectors = inFastFeatureVectors;
	}

	protected static void performPostProcessingConditionally() {
		if (Config.getClustersToWriteList() == null) {
			logger.debug("Config.getClustersToWriteList() == null so skipping post processing");
			return;
		}
		if (Config.getClustersToWriteList().contains(fastClusters.size())) {
			final String postProcMsg = "Performing post processing at " + fastClusters.size() + " number of clusters";
			logger.debug(postProcMsg);
			ClusterUtil.fastClusterPostProcessing(fastClusters, fastFeatureVectors);
		}
	}
}
