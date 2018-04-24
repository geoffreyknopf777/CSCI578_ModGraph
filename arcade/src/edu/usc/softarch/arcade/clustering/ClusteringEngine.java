package edu.usc.softarch.arcade.clustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import jxl.Workbook;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import edu.usc.softarch.arcade.classgraphs.ClassGraph;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.util.StopWatch;

/**
 * @author joshua
 *
 */
public class ClusteringEngine {

	private final FeatureVectorMap fvMap = new FeatureVectorMap();

	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ClusteringEngine.class);

	public ClusteringEngine() {

	}

	public ClusteringEngine(final ClassGraph clg) throws TransformerException, ParserConfigurationException, SAXException, IOException {
	}

	public void run() throws Exception {

		FastFeatureVectors fastFeatureVectors = null;

		final ArrayList<FastCluster> fastClusters = null;

		final File fastFeatureVectorsFile = new File(Config.getFastFeatureVectorsFilename());

		final ObjectInputStream objInStream = new ObjectInputStream(new FileInputStream(fastFeatureVectorsFile));

		// Deserialize the object
		try {
			fastFeatureVectors = (FastFeatureVectors) objInStream.readObject();
			logger.debug("feature set size: " + fastFeatureVectors.getNamesInFeatureSet().size());
			logger.debug("Names in Feature Set:");
			logger.debug(fastFeatureVectors.getNamesInFeatureSet());
			objInStream.close();

		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logger.debug("Read in serialized feature vectors...");

		if (Config.isExcelFileWritingEnabled)
			writeXLSFromOriginalDeps();

		// logger.debug(vecMap);

		final StopWatch stopwatch = new StopWatch();

		stopwatch.start();
		// computeHierarchicalClustersUsingBasicMethod();
		if (Config.getCurrentClusteringAlgorithm().equals(ClusteringAlgorithmType.WCA)) {
			ClusteringAlgoRunner.setFastFeatureVectors(fastFeatureVectors);
			if (Config.getStoppingCriterion().equals(Config.StoppingCriterionConfig.preselected)) {
				final StoppingCriterion stopCriterion = new PreSelectedStoppingCriterion();
				WcaRunner.computeClustersWithPQAndWCA(stopCriterion);
			}
			if (Config.getStoppingCriterion().equals(Config.StoppingCriterionConfig.clustergain)) {
				final StoppingCriterion singleClusterStopCriterion = new SingleClusterStoppingCriterion();
				WcaRunner.computeClustersWithPQAndWCA(singleClusterStopCriterion);
				final StoppingCriterion clusterGainStopCriterion = new ClusterGainStoppingCriterion();
				WcaRunner.computeClustersWithPQAndWCA(clusterGainStopCriterion);
			}
		}

		for (final int numTopics : Config.getNumTopicsList()) {
			Config.setNumTopics(numTopics);
			if (Config.getCurrentClusteringAlgorithm().equals(ClusteringAlgorithmType.ARC))
				throw new Exception("Pooyan-> there is a null instead of outputDir/base");
		}

		if (Config.getCurrentClusteringAlgorithm().equals(ClusteringAlgorithmType.LIMBO)) {
			ClusteringAlgoRunner.setFastFeatureVectors(fastFeatureVectors);
			LimboRunner.computeClusters(new PreSelectedStoppingCriterion());
			if (Config.getStoppingCriterion().equals(Config.StoppingCriterionConfig.clustergain))
				LimboRunner.computeClusters(new ClusterGainStoppingCriterion());
		}
		stopwatch.stop();

		final String timeInSecsToComputeClusters = "Time in seconds to compute clusters: " + stopwatch.getElapsedTimeSecs();
		final String timeInMilliSecondsToComputeClusters = "Time in milliseconds to compute clusters: " + stopwatch.getElapsedTime();
		logger.debug(timeInSecsToComputeClusters);
		System.out.println(timeInSecsToComputeClusters);
		logger.debug(timeInMilliSecondsToComputeClusters);
		System.out.println(timeInMilliSecondsToComputeClusters);
		logger.debug("Final clusters: " + fastClusters);

	}

	// private void clusterPostProcessing() throws FileNotFoundException,
	// UnsupportedEncodingException, IOException,
	// ParserConfigurationException, TransformerException {
	// ClusterUtil.generateLeafClusters(clusters);
	// // int itemsInClusters = 0;
	// // for (Cluster c : clusters) {
	// // itemsInClusters += c.leafClusters.size();
	// // }
	//
	// StringGraph clusterGraph = ClusterUtil.generateClusterGraph(clusters);
	// logger.debug("Resulting ClusterGraph...");
	// logger.debug(clusterGraph);
	//
	// HashMap<String, Integer> clusterNameToNodeNumberMap = ClusterUtil
	// .createClusterNameToNodeNumberMap(clusters);
	// TreeMap<Integer, String> nodeNumberToClusterNameMap = ClusterUtil
	// .createNodeNumberToClusterNameMap(clusters,
	// clusterNameToNodeNumberMap);
	//
	// ClusterUtil.writeClusterRSFFile(clusterNameToNodeNumberMap, clusters);
	//
	// if (Config.runMojo) {
	// runMojoAgainstTargetFile(clusterNameToNodeNumberMap,
	// Config.getMojoTargetFile());
	// }
	//
	// clusterGraph.writeNumberedNodeDotFileWithTextMappingFile(
	// Config.getClusterGraphDotFilename(),
	// clusterNameToNodeNumberMap, nodeNumberToClusterNameMap);
	// clusterGraph.writeXMLClusterGraph(Config.getClusterGraphXMLFilename());
	//
	// if (Config.isExcelFileWritingEnabled) {
	// createXLSOfFeatureVectorsFromSplitClusters(clusters);
	// }
	//
	// logger.debug("Serializing clusters...");
	// serializeCAClusters();
	// logger.debug("Finished serializing clusters...");
	// }

	/*
	 * private void printMostSimilarClustersForEachFastCluster() {
	 * logger.debug("Printing most similar clusters for each cluster..."); for
	 * (FastCluster c : fastClusters) { logger.debug("Most similar cluster to "
	 * + c + " is " + c.getMostSimilarCluster());
	 * TopicUtil.printTwoDocTopics(c.docTopicItem,
	 * c.getMostSimilarCluster().docTopicItem); } }
	 */

	private void writeXLSFromOriginalDeps() throws IOException {
		final Collection<FeatureVector> fvColl = fvMap.featureVectorNameToFeatureVectorMap.values();
		final Vector<FeatureVector> fvVec = new Vector<FeatureVector>(fvColl);

		final WritableWorkbook workbook = Workbook.createWorkbook(new File(Config.getXLSDepsFilename()));
		final WritableSheet sheet = workbook.createSheet("First Sheet", 0);

		final FeatureVector first = fvVec.firstElement();
		int labelIndex = 0;
		for (final Feature f : first) {
			final Label label = new Label(labelIndex + 1, 0, f.edge.tgtStr);
			try {
				sheet.addCell(label);
			} catch (final RowsExceededException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (final WriteException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			labelIndex++;
		}

		for (int row = 0; row < fvVec.size(); row++) {
			final FeatureVector fv = fvVec.elementAt(row);
			final Label label = new Label(0, row + 1, fv.name);
			logger.debug(fv.name);
			try {
				sheet.addCell(label);
			} catch (final RowsExceededException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (final WriteException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			for (int featureIndex = 0; featureIndex < fv.size(); featureIndex++) {
				final Feature currentFeature = fv.get(featureIndex);
				final Feature f2 = first.get(featureIndex);

				if (!currentFeature.edge.tgtStr.equals(f2.edge.tgtStr)) {
					logger.debug("While creating xls file for original fvMap, feature indices do not match...exiting");
					System.exit(1);
				}

				final Number number = new Number(featureIndex + 1, row + 1, currentFeature.value);
				try {
					sheet.addCell(number);
				} catch (final RowsExceededException e) {
					e.printStackTrace();
					System.exit(-1);
				} catch (final WriteException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		}

		workbook.write();
		try {
			workbook.close();
		} catch (final WriteException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public class SharedFeature {
		Feature f1;
		Feature f2;

		SharedFeature(final Feature f1, final Feature f2) {
			this.f1 = f1;
			this.f2 = f2;
		}

		@Override
		public String toString() {
			return f1 + "," + f2;
		}
	}

	public Vector<SharedFeature> getSharedFeatures(final FeatureVector fv1, final FeatureVector fv2) {
		// final boolean local_debug = false;
		final Vector<SharedFeature> sharedFeatures = new Vector<SharedFeature>();
		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			for (int j = 0; j < fv2.size(); j++) {
				final Feature f2 = fv2.get(j);
				// if (local_debug && logger.isDebugEnabled()) {
				// logger.debug("f.edge.tgtStr: " + f.edge.tgtStr);
				// logger.debug("f2.edge.tgtStr: " + f2.edge.tgtStr);
				// logger.debug("f.value: " + f.value);
				// logger.debug("f2.value: " + f2.value);
				// logger.debug("\n");
				// }
				if (f.edge.tgtStr.equals(f2.edge.tgtStr) && f.value > 0 && f2.value > 0) {
					final SharedFeature sf = new SharedFeature(f, f2);
					sharedFeatures.add(sf);
					// if (local_debug && logger.isDebugEnabled()) {
					// logger.debug("Increased 11 count to: " + count);
					// }
				}
			}

		}
		return sharedFeatures;
	}
}
