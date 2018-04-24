package edu.usc.softarch.arcade.clustering;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.antipattern.detection.ArchSmellDetector;
import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.SimMeasure;
import edu.usc.softarch.arcade.facts.driver.DepsMaker;
import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.topics.TopicModelExtractionMethod;
import edu.usc.softarch.arcade.topics.TopicUtil;
import edu.usc.softarch.arcade.util.CodeCount;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.JobControlUtil;
import edu.usc.softarch.arcade.util.RecoveryParams;
import edu.usc.softarch.arcade.util.convert.ClusterGraphToDotConverter;

public class BatchClusteringEngine {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(BatchClusteringEngine.class);
	Config currentConfig = Controller.currentView.getConfig();

	public static void main(final String[] args) {
		logger.entry((Object[]) args);
		// Language parameter should be deleted - use Config setting instead!
		if (args.length < 3 || args.length > 4) {
			System.out.println("Usage: BatchClusterinEngine <inputDirName> <outputDirName> <classesDir> [language]");
			System.exit(-1);
		}
		final RecoveryParams recParms = new RecoveryParams(args);
		final File[] files = recParms.getInputDir().listFiles(FileUtil.saneDirectoryFilter);
		final Set<File> fileSet = new TreeSet<>(Arrays.asList(files));
		logger.debug("All files in " + recParms.getInputDir() + ":");
		logger.debug(Joiner.on("\n").join(fileSet));
		boolean nothingtodo = true;
		for (final File versionFolder : fileSet) {
			if (versionFolder.isDirectory()) {
				logger.debug("Identified directory: " + versionFolder.getName());
				System.out.println("Identified directory: " + versionFolder.getName());
				nothingtodo = false;
				final ArchitecturalView curView = new ArchitecturalView("versionFolder");
				Controller.setCurrentView(curView);
				// final Runnable r = () -> {
				// final Runtime rt = Runtime.getRuntime();
				single(versionFolder, recParms.getOutputDir(), recParms.getClassesDirName());
				// };
				// new Thread(r).start();
			} else {
				logger.debug("Not a directory: " + versionFolder.getName());
			}
		}
		if (nothingtodo) {
			System.out.println("Nothing to do!");
		}
		logger.traceExit();
	}

	/**
	 * Run ARC on a single version of a system
	 *
	 * @param versionFolder
	 *            - Directory whose subdirectories are versions of the system
	 * @param outputDir
	 *            - Directory where the output goes
	 * @param classesDirName
	 *            - Directory within a given system that contains the classes //
	 *            what about C?
	 * @param language
	 *            - Programming language whose files are of interest (default is
	 *            Java)
	 */
	public static void single(final File versionFolder, final File outputDir, final String classesDirName) {
		logger.entry(versionFolder, outputDir, classesDirName);
		// the revision number is really just the name of the subdirectory
		final String revisionNumber = versionFolder.getName();

		logger.debug("Processing revision directory: " + revisionNumber);
		System.out.println("Processing revision directory: " + revisionNumber);

		final File classesDir = FileUtil.checkDir(versionFolder.getAbsolutePath() + File.separatorChar + classesDirName, false, true);

		final Config currentConfig = Controller.getCurrentView().getConfig();

		/**
		 * If configured to, run UCC on the selected classes directory using
		 * another thread
		 */
		if (currentConfig.isRunUCC()) {
			new CodeCount(classesDir, outputDir, versionFolder);
		}

		final SourceToDepsBuilder builder = DepsMaker.generate(revisionNumber, versionFolder, outputDir, classesDirName);
		Config.getCurrentController().runPKGbatchPackager();
		int numTopics;
		if (currentConfig.isUseARCAbsoluteTopicsNumber()) {
			numTopics = currentConfig.getArcAbsoluteTopicsNumber();
			logger.info("Number of topics set to constant value of " + numTopics);
		} else {
			numTopics = (int) (builder.getNumSourceEntities() * currentConfig.getArcTopicsEntitiesFactor());
		}
		Config.setNumTopics(numTopics);
		// final int numTopics = builder.getNumSourceEntities();
		final String fullSrcDir = versionFolder.getAbsolutePath() + File.separatorChar;
		String fullOutDirPrefix = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_" + "TTT";// numTopics;
		final String topicModelFilename = fullOutDirPrefix + "_topics.mallet";
		final String docTopicsFilename = fullOutDirPrefix + "_doc_topics.txt";
		final String topWordsFilename = fullOutDirPrefix + "_top_words_per_topic.txt";

		ConcernClusteringRunner runner = new ConcernClusteringRunner(builder.getFfVecs(), TopicModelExtractionMethod.MALLET_API, fullSrcDir, outputDir.getAbsolutePath() + File.separatorChar + "base",
				numTopics, topicModelFilename, docTopicsFilename, topWordsFilename);

		if (currentConfig.isUseSuperModel()) {
			runner = new ConcernClusteringRunner(builder.getFfVecs(), TopicModelExtractionMethod.MALLET_API, fullSrcDir, outputDir.getAbsolutePath() + File.separatorChar + "base", numTopics,
					topicModelFilename, docTopicsFilename, topWordsFilename);
		}
		// have to set some Config settings before executing the runner
		final Config.StoppingCriterionConfig stoppingCriterion = Config.getStoppingCriterion();
		final boolean useClusterGain = stoppingCriterion.equals(Config.StoppingCriterionConfig.clustergain);
		final boolean usePreSelected = stoppingCriterion.equals(Config.StoppingCriterionConfig.preselected);

		int numClusters;
		if (usePreSelected) {
			if (currentConfig.isUseARCAbsoluteClustersNumber()) {
				numClusters = currentConfig.getArcAbsoluteClustersNumber();
			} else {
				numClusters = (int) (ClusteringAlgoRunner.getFastClusters().size() * currentConfig.getArcNumClustersFastClustersFactor());
			}
			// number of clusters to obtain is based on the number of entities
			Config.setNumClusters(numClusters);
		}
		Config.setCurrSimMeasure(SimMeasure.js);

		// If we have clustergain configured, clustering needs to run twice:
		// The first time to determine the optimal number of clusters, the
		// second time to reduce the clusters to that number
		// This should not take long, so it shouldn't matter (famous last words)
		// :)

		if (usePreSelected) {
			runner.computeClustersWithConcernsAndFastClusters(new PreSelectedStoppingCriterion());
		}
		numTopics = runner.getNumTopics();

		if (useClusterGain) {
			System.out.println("Determining number of clusters with the best clustergain...");
			final ClusterGainStoppingCriterion cGainStopCrit = new ClusterGainStoppingCriterion();
			runner.computeClustersWithConcernsAndFastClusters(cGainStopCrit);
			System.out.println("Applying clustergain...");
			cGainStopCrit.setOptimalNumClusters(ClusteringAlgoRunner.getNumClustersAtMaxClusterGain());
			cGainStopCrit.setSecondRun(true);
			numTopics = runner.getNumTopics();
			runner = new ConcernClusteringRunner(builder.getFfVecs(), TopicModelExtractionMethod.MALLET_API, fullSrcDir, outputDir.getAbsolutePath() + File.separatorChar + "base", numTopics,
					topicModelFilename, docTopicsFilename, topWordsFilename);
			runner.computeClustersWithConcernsAndFastClusters(cGainStopCrit);
		}

		fullOutDirPrefix = fullOutDirPrefix.replaceFirst("TTT", Integer.toString(numTopics));

		final File topicModelFile = FileUtil.checkFile(topicModelFilename, false, false);
		topicModelFile.renameTo(FileUtil.checkFile(topicModelFilename.replaceFirst("TTT", Integer.toString(numTopics)), false, false));

		final File docTopicsFile = FileUtil.checkFile(docTopicsFilename, false, false);
		docTopicsFile.renameTo(FileUtil.checkFile(docTopicsFilename.replaceFirst("TTT", Integer.toString(numTopics)), false, false));

		final File topWordsFile = FileUtil.checkFile(topWordsFilename, false, false);
		topWordsFile.renameTo(FileUtil.checkFile(topWordsFilename.replaceFirst("TTT", Integer.toString(numTopics)), false, false));

		final File arcClustersFile = FileUtil.checkFile(fullOutDirPrefix + "_topics_" + ClusteringAlgoRunner.getFastClusters().size() + "_arc_clusters.rsf", false, false);
		// need to build the map before writing the file
		final HashMap<String, Integer> clusterNameToNodeNumberMap = ClusterUtil.createFastClusterNameToNodeNumberMap(ClusteringAlgoRunner.getFastClusters());
		ClusterUtil.writeFastClustersRsfFile(clusterNameToNodeNumberMap, ClusteringAlgoRunner.getFastClusters(), arcClustersFile);

		Config.setSmellClustersFile(arcClustersFile);
		// Config.setDepsRsfFile(depsRsfFile);
		final String detectedSmellsFilename = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_arc_smells.ser";
		// String[] smellArgs = { depsRsfFile.getAbsolutePath(),
		// arcClustersFilename, };

		// Need to provide docTopics first
		ArchSmellDetector.docTopics = TopicUtil.docTopics;
		ArchSmellDetector.tmeMethod = TopicModelExtractionMethod.MALLET_API;

		// Generate cluster graph
		final String clusterGraphFilename = FilenameUtils.removeExtension(DepsMaker.getDepsRsfFilename()) + "_clusters.dot";
		final String[] runArgs = { DepsMaker.getDepsRsfFilename(), arcClustersFile.getAbsolutePath(), clusterGraphFilename };
		logger.debug("Running smell detecion for revision " + revisionNumber);
		ArchSmellDetector.runAllDetectionAlgs(FileUtil.checkFile(detectedSmellsFilename, false, false));
		ClusterGraphToDotConverter.main(runArgs);
		if (currentConfig.isRunGraphs()) {
			final String clusterDepImageFileName = FilenameUtils.removeExtension(clusterGraphFilename) + "." + currentConfig.getDotOutputFormat();
			// Config.getDotLayoutCommandLoc();

			// final String[] dotLayoutArgs2 = {
			// currentConfig.getDotLayoutCommandDir() + File.separator +
			// currentConfig.getDotLayoutCommand(),
			// currentConfig.getGraphParams(), clusterGraphFilename, "-o",
			// clusterDepImageFileName };

			// Hard-coding circo until GUI allows command selection
			// DO NOT PUT SEVERAL PARAMETERS IN ONE STRING!
			final String[] dotLayoutArgs2 = { currentConfig.getDotLayoutCommandDir() + File.separator + "circo", "-Tpdf", clusterGraphFilename, "-o", clusterDepImageFileName };

			JobControlUtil.submitJob(dotLayoutArgs2);
			// final Runnable r = () -> {
			// try {
			//
			// final Runtime rt = Runtime.getRuntime();
			// rt.exec(Config.getDotLayoutCommandLoc() +
			// dotLayoutCommandParameters).waitFor();
			// rt.exec(currentConfig.graphOpenerLoc + " " +
			// clusterDepImageFileName);
			// } catch (final IOException e) {
			// System.out.println("Unable to generate cluster graph!");
			// e.printStackTrace();
			// } catch (final InterruptedException e) {
			// System.out.println("Generation of cluster graph was
			// interrupted!");
			// e.printStackTrace();
			// }
			// };
			// new Thread(r).start();
		}
		logger.traceExit();
	}
}
