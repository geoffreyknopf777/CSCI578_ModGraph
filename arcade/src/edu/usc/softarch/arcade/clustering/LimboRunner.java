package edu.usc.softarch.arcade.clustering;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.util.DebugUtil;
import edu.usc.softarch.arcade.util.StopWatch;

public class LimboRunner extends ClusteringAlgoRunner {

	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(LimboRunner.class);

	public static void computeClusters(StoppingCriterion stopCriterion) {
		final StopWatch loopSummaryStopwatch = new StopWatch();

		initializeClusters(null);

		// SimCalcUtil.verifySymmetricClusterOrdering(clusters);

		loopSummaryStopwatch.start();

		final StopWatch matrixCreateTimer = new StopWatch();
		matrixCreateTimer.start();
		final List<List<Double>> simMatrix = createSimilarityMatrix(getFastClusters());
		matrixCreateTimer.stop();
		logger.debug("time to create similarity matrix: " + matrixCreateTimer.getElapsedTime());

		int clusterStepCount = 0;
		final int stepCountToStop = 5;
		final boolean stopAtClusterStep = false; // for debugging purposes
		while (stopCriterion.notReadyToStop()) {
			if (Config.getStoppingCriterion().equals(Config.StoppingCriterionConfig.clustergain)) {
				double clusterGain = 0;
				clusterGain = ClusterUtil.computeClusterGainUsingStructuralDataFromFastFeatureVectors(getFastClusters());
				checkAndUpdateClusterGain(clusterGain);
			}

			// runClusterStepForWCA();
			final StopWatch timer = new StopWatch();
			timer.start();
			// identifyMostSimClustersForConcernsMultiThreaded(data);
			final MaxSimData data = identifyMostSimClusters(simMatrix);
			timer.stop();
			logger.debug("time to identify two most similar clusters: " + timer.getElapsedTime());

			printTwoMostSimilarClustersUsingStructuralData(data);

			final FastCluster cluster = getFastClusters().get(data.rowIndex);
			final FastCluster otherCluster = getFastClusters().get(data.colIndex);
			final FastCluster newCluster = new FastCluster(ClusteringAlgorithmType.LIMBO, cluster, otherCluster);

			updateFastClustersAndSimMatrixToReflectMergedCluster(data, newCluster, simMatrix);

			if (stopAtClusterStep)
				clusterStepCount++;

			if (stopAtClusterStep)
				if (clusterStepCount == stepCountToStop) {
					loopSummaryStopwatch.stop();

					logger.debug("Time in milliseconds to compute clusters after priority queue initialization: " + loopSummaryStopwatch.getElapsedTime());

					DebugUtil.earlyExit();
				}

			performPostProcessingConditionally();
		}

		loopSummaryStopwatch.stop();

		logger.debug("Time in milliseconds to compute clusters after priority queue initialization: " + loopSummaryStopwatch.getElapsedTime());
		logger.debug("max cluster gain: " + ClusteringAlgoRunner.getMaxClusterGain());
		logger.debug("num clusters at max cluster gain: " + getNumClustersAtMaxClusterGain());

	}

	private static void updateFastClustersAndSimMatrixToReflectMergedCluster(MaxSimData data, FastCluster newCluster, List<List<Double>> simMatrix) {

		final FastCluster cluster = getFastClusters().get(data.rowIndex);
		final FastCluster otherCluster = getFastClusters().get(data.colIndex);

		int greaterIndex = -1, lesserIndex = -1;
		if (data.rowIndex == data.colIndex)
			throw new IllegalArgumentException("data.rowIndex: " + data.rowIndex + " should not be the same as data.colIndex: " + data.colIndex);
		if (data.rowIndex > data.colIndex) {
			greaterIndex = data.rowIndex;
			lesserIndex = data.colIndex;
		} else if (data.rowIndex < data.colIndex) {
			greaterIndex = data.colIndex;
			lesserIndex = data.rowIndex;
		}

		simMatrix.remove(greaterIndex);
		for (final List<Double> col : simMatrix)
			col.remove(greaterIndex);

		simMatrix.remove(lesserIndex);
		for (final List<Double> col : simMatrix)
			col.remove(lesserIndex);

		getFastClusters().remove(cluster);
		getFastClusters().remove(otherCluster);

		getFastClusters().add(newCluster);

		final List<Double> newRow = new ArrayList<Double>(getFastClusters().size());

		for (int i = 0; i < getFastClusters().size(); i++)
			newRow.add(Double.MAX_VALUE);

		simMatrix.add(newRow);

		for (int i = 0; i < getFastClusters().size() - 1; i++)
			// value to create
			// new column for
			// all but the last
			// row, which
			// already has the
			// column for the
			// new cluster
			simMatrix.get(i).add(Double.MAX_VALUE);

		if (simMatrix.size() != getFastClusters().size())
			throw new RuntimeException("simMatrix.size(): " + simMatrix.size() + " is not equal to getFastClusters().size(): " + getFastClusters().size());

		for (int i = 0; i < getFastClusters().size(); i++)
			if (simMatrix.get(i).size() != getFastClusters().size())
				throw new RuntimeException("simMatrix.get(" + i + ").size(): " + simMatrix.get(i).size() + " is not equal to getFastClusters().size(): " + getFastClusters().size());

		for (int i = 0; i < getFastClusters().size(); i++) {
			final FastCluster currCluster = getFastClusters().get(i);
			double currSimMeasure = 0;

			currSimMeasure = FastSimCalcUtil.getInfoLossMeasure(ClusteringAlgoRunner.getNumberOfEntitiesToBeClustered(), newCluster, currCluster);

			simMatrix.get(getFastClusters().size() - 1).set(i, currSimMeasure);
			simMatrix.get(i).set(getFastClusters().size() - 1, currSimMeasure);
		}

		// SimCalcUtil.verifySymmetricClusterOrdering(clusters);
		// newCluster.addClustersToPriorityQueue(clusters);
	}

	protected static void printTwoMostSimilarClustersUsingStructuralData(MaxSimData maxSimData) {
		if (logger.isDebugEnabled()) {
			logger.debug("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", \nMax Similar Clusters: ");

			ClusterUtil.printSimilarFeatures(getFastClusters().get(maxSimData.rowIndex), getFastClusters().get(maxSimData.colIndex), fastFeatureVectors);

			logger.debug(maxSimData.currentMaxSim);
			logger.debug("\n");

			logger.debug("before merge, clusters size: " + getFastClusters().size());

		}
	}

	private static MaxSimData identifyMostSimClusters(List<List<Double>> simMatrix) {
		if (simMatrix.size() != getFastClusters().size())
			throw new IllegalArgumentException("expected simMatrix.size():" + simMatrix.size() + " to be getFastClusters().size(): " + getFastClusters().size());
		for (final List<Double> col : simMatrix)
			if (col.size() != getFastClusters().size())
				throw new IllegalArgumentException("expected col.size():" + col.size() + " to be getFastClusters().size(): " + getFastClusters().size());

		final int length = simMatrix.size();
		final MaxSimData msData = new MaxSimData();
		msData.rowIndex = 0;
		msData.colIndex = 0;
		double leastInfoLossMeasure = Double.MAX_VALUE;
		for (int i = 0; i < length; i++)
			for (int j = 0; j < length; j++) {
				final double currInfoLossMeasure = simMatrix.get(i).get(j);
				if (currInfoLossMeasure < leastInfoLossMeasure && i != j) {
					leastInfoLossMeasure = currInfoLossMeasure;
					msData.rowIndex = i;
					msData.colIndex = j;
				}
			}
		msData.currentMaxSim = leastInfoLossMeasure;
		return msData;
	}

	private static List<List<Double>> createSimilarityMatrix(ArrayList<FastCluster> clusters) {

		// HashMap<HashSet<String>, Double> map = new HashMap<HashSet<String>,
		// Double>();

		final List<List<Double>> simMatrixObj = new ArrayList<List<Double>>(clusters.size());

		for (int i = 0; i < clusters.size(); i++)
			simMatrixObj.add(new ArrayList<Double>(clusters.size()));

		for (int i = 0; i < clusters.size(); i++) {
			final FastCluster cluster = clusters.get(i);
			for (int j = 0; j < clusters.size(); j++) {
				final FastCluster otherCluster = clusters.get(j);

				double currSimMeasure = 0;
				currSimMeasure = FastSimCalcUtil.getInfoLossMeasure(ClusteringAlgoRunner.getNumberOfEntitiesToBeClustered(), cluster, otherCluster);

				simMatrixObj.get(i).add(currSimMeasure);
			}

		}

		return simMatrixObj;
	}

}
