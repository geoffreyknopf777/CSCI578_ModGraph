package edu.usc.softarch.arcade;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mojo.MoJoCalculator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.util.FileUtil;

public class MetricsDriver {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(MetricsDriver.class);

	public static void main(final String[] args) {
		String computedFilePrefix = "/home/joshua/workspace/MyExtractors/data/linux/linux_";
		File authClusteringFile = new File("/home/joshua/recovery/Expert Decompositions/linuxFullAuthcontain.rsf");
		String selectedAlg = "arc";
		String simMeasure = "uem";
		String stoppingCriterion = "preselected";
		File computedClusters = new File("/home/joshua/workspace/acdc/linux_acdc_clustered.rsf");
		final Options options = new Options();

		final Option help = new Option("help", "print this message");
		final Option useExpertDecompFile = new Option("use_expert_decomp_file", "uses the expert decomposition file property from project config file");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("project configuration file");
		final Option projFile = OptionBuilder.create("projfile");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("prefix of computed clustering file");
		final Option computedFilePrefixOption = OptionBuilder.create("computedFilePrefix");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("authoritative clustering file");
		final Option authClusteringFileOption = OptionBuilder.create("authClusteringFile");

		OptionBuilder.withArgName("selectedAlg");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("Select the algorithm used to create computed clustering file [acdc|wca|arc]");
		final Option algOption = OptionBuilder.create("alg");

		OptionBuilder.withArgName("simMeasure");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("Select the similarity measured used to create computed clustering file [uem|uemnm|js]");
		final Option simMeasureOption = OptionBuilder.create("simMeasure");

		OptionBuilder.withArgName("criterion");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("Select the stopping criterion [preselected|clustergain]");
		final Option stoppingCriterionOption = OptionBuilder.create("stoppingCriterion");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("File containing clusters computed by clustering algorithm");
		final Option computedClustersFileOption = OptionBuilder.create("computedClustersFile");

		options.addOption(help);
		options.addOption(useExpertDecompFile);
		options.addOption(projFile);
		options.addOption(computedFilePrefixOption);
		options.addOption(authClusteringFileOption);
		options.addOption(algOption);
		options.addOption(simMeasureOption);
		options.addOption(stoppingCriterionOption);
		options.addOption(computedClustersFileOption);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("projfile")) {
				Config.setProjConfigFile(FileUtil.checkFile(line.getOptionValue("projfile"), false, false));
			}
			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(MetricsDriver.class.getName(), options);
				System.exit(0);
			}
			if (line.hasOption("use_expert_decomp_file")) {
			}
			if (line.hasOption("computedFilePrefix")) {
				computedFilePrefix = line.getOptionValue("computedFilePrefix");
			}
			if (line.hasOption("authClusteringFile")) {
				authClusteringFile = FileUtil.checkFile(line.getOptionValue("authClusteringFile"), false, false);
			}
			if (line.hasOption("alg")) {
				selectedAlg = line.getOptionValue("alg");
			}
			if (line.hasOption("simMeasure")) {
				simMeasure = line.getOptionValue("simMeasure");
			}
			if (line.hasOption("stoppingCriterion")) {
				stoppingCriterion = line.getOptionValue("stoppingCriterion");
			}
			if (line.hasOption("computedClustersFile")) {
				computedClusters = FileUtil.checkFile(line.getOptionValue("computedClustersFile"), false, false);
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		logger.debug("Running from " + MetricsDriver.class.getName());
		Config.initConfigFromFile(Config.getProjConfigFile());

		// String computedRsfFilename = Config.getClustersRSFFilename();
		// String selectedAlg = "arc";

		// Builds rsf file from expert decompositions file and performs metric
		// calcs
		/*
		 * if (Config.getExpertDecompositionFile() != null) {
		 * buildExpertDecompositionClustersFromRSFFile();
		 * performMoJoOperationsWithExpertDecompositions(selectedAlg,
		 * simMeasure,stoppingCriterion,computedFilePrefix);
		 * performPrecisionRecallOperationsForConcernRecovery(selectedAlg,
		 * simMeasure, stoppingCriterion, computedFilePrefix); System.exit(0); }
		 */

		// performMoJoOperationsForConcernRecovery(selectedAlg,
		// simMeasure,stoppingCriterion);

		if (selectedAlg.equals("wca") || selectedAlg.equals("limbo")) {
			performMoJoOperationsForMultipleClustersOnSingleAuthClusteringFile(selectedAlg, simMeasure, stoppingCriterion, computedFilePrefix, authClusteringFile);
			/*
			 * performPrecisionRecallOperationsForMultipleClustersOnSingleAuthClusteringFile
			 * (selectedAlg, simMeasure, stoppingCriterion, computedFilePrefix,
			 * authClusteringFile); if (isUsingExpertDecompFile) {
			 * performPrecisionRecallOperations(selectedAlg, simMeasure,
			 * stoppingCriterion); }
			 */
		}

		if (selectedAlg.equals("acdc")) {
			performMojoForSingleAuthClustering(computedClusters, authClusteringFile);
			// performPrecisionRecallForSingleAuthClustering(computedClustersFile,authClusteringFile);
		}

		if (selectedAlg.equals("arc")) {
			performMoJoForMultiClustersOnFile(computedFilePrefix, authClusteringFile, selectedAlg, simMeasure, stoppingCriterion);
			// performPrecisionRecallForMultiClustersOnFile(computedFilePrefix,authClusteringFile,selectedAlg,
			// simMeasure,stoppingCriterion);
		}
	}

	// private static void performPrecisionRecallForMultiClustersOnFile(
	// String computedFilePrex, String authClusteringFile,
	// String selectedAlg, String simMeasure, String stoppingCriterion) {
	// List<Integer> numClustersList = new ArrayList<Integer>();
	// List<Integer> numTopicsList = new ArrayList<Integer>();
	//
	// for (int numClusters = Config.getStartNumClustersRange(); numClusters <=
	// Config
	// .getEndNumClustersRange(); numClusters += Config
	// .getRangeNumClustersStep()) {
	// for (int numTopics = Config.getStartNumTopicsRange(); numTopics <= Config
	// .getEndNumTopicsRange(); numTopics += Config
	// .getRangeNumTopicsStep()) {
	//
	// numClustersList.add(numClusters);
	// numTopicsList.add(numTopics);
	//
	// String computedRsfFilename = null;
	// if (!selectedAlg.equals("arc")) {
	// computedRsfFilename = constructNonTopicComputedRsfFilename(
	// computedFilePrex, selectedAlg, simMeasure,
	// stoppingCriterion, numClusters);
	// } else {
	// computedRsfFilename = constructTopicBasedComputedRsfFilename(
	// computedFilePrex, selectedAlg, simMeasure,
	// stoppingCriterion, numClusters, numTopics);
	// }
	//
	// HashSet<HashSet<String>> allIntraPairsFromClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(computedRsfFilename);
	//
	// HashSet<HashSet<String>> allIntraPairsFromAuthClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(authClusteringFile);
	//
	// double precision = PrecisionRecallCalculator.computePrecision(
	// allIntraPairsFromClustersRsf,
	// allIntraPairsFromAuthClustersRsf);
	// String precisionOutput = "Precision of " + computedRsfFilename
	// + " compared to " + authClusteringFile + " : "
	// + precision;
	// logger.debug(precisionOutput);
	// System.out.println(precisionOutput);
	//
	// double recall = PrecisionRecallCalculator.computeRecall(
	// allIntraPairsFromClustersRsf,
	// allIntraPairsFromAuthClustersRsf);
	// String recallOutput = "Recall of " + computedRsfFilename
	// + " compared to " + authClusteringFile + " : " + recall;
	// logger.debug(recallOutput);
	// System.out.println(recallOutput);
	//
	// }
	//
	// }
	//
	// }

	private static String constructTopicBasedComputedRsfFilename(final String computedFilePrex, final String selectedAlg, final String simMeasure, final String stoppingCriterion,
			final int numClusters, final int numTopics) {
		return computedFilePrex + selectedAlg + "_" + stoppingCriterion + "_" + simMeasure + "_" + numClusters + "_clusters_" + numTopics + "topics.rsf";
	}

	private static String constructNonTopicComputedRsfFilename(final String computedFilePrex, final String selectedAlg, final String simMeasure, final String stoppingCriterion, final int numClusters) {
		return computedFilePrex + selectedAlg + "_" + stoppingCriterion + "_" + simMeasure + "_" + numClusters + "_clusters.rsf";
	}

	private static void performMoJoForMultiClustersOnFile(final String computedFilePrex, final File authClusteringFile, final String selectedAlg, final String simMeasure,
			final String stoppingCriterion) {
		final List<Integer> numClustersList = new ArrayList<Integer>();
		final List<Integer> numTopicsList = new ArrayList<Integer>();

		// List<Long> mojoToNextList = new ArrayList<Long>();
		// List<Double> mojoFmToNextList = new ArrayList<Double>();

		final List<Long> mojoToAuthList = new ArrayList<Long>();
		final List<Double> mojoFmToAuthList = new ArrayList<Double>();

		long mojoSum = 0;
		double mojoFmSum = 0;
		int computedClusterCount = 0;

		/*
		 * for (int numClusters = Config.getStartNumClustersRange(); numClusters
		 * < Config .getEndNumClustersRange(); numClusters += Config
		 * .getRangeNumClustersStep()) { for (int numTopics =
		 * Config.getStartNumTopicsRange(); numTopics <= Config
		 * .getEndNumTopicsRange(); numTopics += Config
		 * .getRangeNumTopicsStep()) {
		 *
		 * numClustersList.add(numClusters); numTopicsList.add(numTopics);
		 *
		 * String computedRsfFilename1 = null; String computedRsfFilename2 =
		 * null; if (!selectedAlg.equals("arc")) { computedRsfFilename1 =
		 * constructNonTopicComputedRsfFilename(computedFilePrex, selectedAlg,
		 * simMeasure, stoppingCriterion, numClusters); computedRsfFilename2 =
		 * constructNonTopicComputedRsfFilename(computedFilePrex, selectedAlg,
		 * simMeasure, stoppingCriterion, numClusters+1); } else {
		 * computedRsfFilename1 =
		 * constructTopicBasedComputedRsfFilename(computedFilePrex, selectedAlg,
		 * simMeasure, stoppingCriterion, numClusters, numTopics);
		 * computedRsfFilename2 =
		 * constructTopicBasedComputedRsfFilename(computedFilePrex, selectedAlg,
		 * simMeasure, stoppingCriterion, numClusters+1, numTopics); }
		 *
		 * File computedRsfFile1 = new File(computedRsfFilename1); File
		 * computedRsfFile2 = new File(computedRsfFilename2);
		 *
		 * if (!computedRsfFile1.exists()) {
		 * System.err.println(computedRsfFile1.getName() +
		 * " does not exist, so skipping it"); continue; }
		 *
		 * if (!computedRsfFile2.exists()) {
		 * System.err.println(computedRsfFile2.getName() +
		 * " does not exist, so skipping it"); continue; }
		 *
		 * String usingComputedFilename = "Using " + computedRsfFilename1 +
		 * " and " + computedRsfFilename2 + " as computed clusters...";
		 * logger.debug(usingComputedFilename);
		 * System.out.println(usingComputedFilename);
		 *
		 * MoJoCalculator mojoCalc = new MoJoCalculator( computedRsfFilename1,
		 * computedRsfFilename2, null); long mojoValue = mojoCalc.mojo();
		 * mojoToNextList.add(mojoValue); String mojoOutput = "MoJo of " +
		 * computedRsfFilename1 + " compared to " + computedRsfFilename2 + ": "
		 * + mojoValue; logger.debug(mojoOutput);
		 * System.out.println(mojoOutput); mojoSum += mojoValue;
		 *
		 * mojoCalc = new MoJoCalculator(computedRsfFilename1,
		 * computedRsfFilename2, null); double mojoFmValue = mojoCalc.mojofm();
		 * mojoFmToNextList.add(mojoFmValue); mojoOutput = "MoJoFM of " +
		 * computedRsfFilename1 + " compared to " + computedRsfFilename2 + ": "
		 * + mojoFmValue; logger.debug(mojoOutput);
		 * System.out.println(mojoOutput); mojoFmSum += mojoFmValue;
		 *
		 * computedClusterCount++; }
		 *
		 * }
		 *
		 * createMojoListsCSVFile(Config.getMojoToNextCSVFilename(numClustersList
		 * , selectedAlg,
		 * simMeasure),numClustersList,mojoToNextList,mojoFmToNextList
		 * ,selectedAlg,simMeasure,numTopicsList);
		 *
		 * System.out.println(
		 * "---------------------------------------------------------------");
		 */

		mojoSum = 0;
		mojoFmSum = 0;
		computedClusterCount = 0;

		double maxMojoFM = 0;
		int numClustersAtMax = 0;
		int numTopicsAtMax = 0;

		for (int numClusters = Config.getStartNumClustersRange(); numClusters <= Config.getEndNumClustersRange(); numClusters += Config.getRangeNumClustersStep()) {
			for (int numTopics = Config.getStartNumTopicsRange(); numTopics <= Config.getEndNumTopicsRange(); numTopics += Config.getRangeNumTopicsStep()) {

				numClustersList.add(numClusters);
				numTopicsList.add(numTopics);

				File computedRsfFile = null;
				if (!selectedAlg.equals("arc")) {
					computedRsfFile = FileUtil.checkFile(constructNonTopicComputedRsfFilename(computedFilePrex, selectedAlg, simMeasure, stoppingCriterion, numClusters), false, false);
				} else {
					computedRsfFile = FileUtil.checkFile(constructTopicBasedComputedRsfFilename(computedFilePrex, selectedAlg, simMeasure, stoppingCriterion, numClusters, numTopics), false, false);
				}

				final String usingComputedFilename = "Using " + computedRsfFile + " as computed clusters...";
				logger.debug(usingComputedFilename);
				System.out.println(usingComputedFilename);

				MoJoCalculator mojoCalc = new MoJoCalculator(computedRsfFile, authClusteringFile, null);
				final long mojoValue = mojoCalc.mojo();
				mojoToAuthList.add(mojoValue);
				String mojoOutput = "MoJo of " + computedRsfFile + " compared to authoritative clustering: " + mojoValue;
				logger.debug(mojoOutput);
				System.out.println(mojoOutput);
				mojoSum += mojoValue;

				mojoCalc = new MoJoCalculator(computedRsfFile, authClusteringFile, null);
				final double mojoFmValue = mojoCalc.mojofm();
				mojoFmToAuthList.add(mojoFmValue);
				mojoOutput = "MoJoFM of " + computedRsfFile + " compared to authoritative clustering: " + mojoFmValue;
				logger.debug(mojoOutput);
				System.out.println(mojoOutput);
				mojoFmSum += mojoFmValue;

				if (mojoFmValue > maxMojoFM) {
					maxMojoFM = mojoFmValue;
					numClustersAtMax = numClusters;
					numTopicsAtMax = numTopics;
				}

				computedClusterCount++;
			}

		}
		final double mojoAvg = mojoSum / computedClusterCount;
		final String mojoAvgOutput = "MoJo averge: " + mojoAvg;
		logger.debug(mojoAvgOutput);
		System.out.println(mojoAvgOutput);
		// mojoAvgList.add(mojoAvg);

		final double mojoFmAvg = mojoFmSum / computedClusterCount;
		final String mojoFmAvgOutput = "MoJoFM averge: " + mojoFmAvg;
		logger.debug(mojoFmAvgOutput);
		System.out.println(mojoFmAvgOutput);
		// mojoFmAvgList.add(mojoFmAvg);

		System.out.println("max mojo fm: " + maxMojoFM);
		System.out.println("num clusters at max: " + numClustersAtMax);
		System.out.println("num topics at max: " + numTopicsAtMax);

		System.out.println("Writing MoJo and MoJoFM to csv file " + Config.getMojoToAuthCSVFilename(numClustersList, selectedAlg, simMeasure) + " ...");

		createMojoToAuthListsCSVFile(numClustersList, mojoToAuthList, mojoFmToAuthList, selectedAlg, simMeasure, numTopicsList);

	}

	/*
	 * private static void performMoJoOperationsForFile(String
	 * computedRsfFilename, String selectedAlg) { List<Integer> numClustersList
	 * = new ArrayList<Integer>(); List<Double> mojoAvgList = new
	 * ArrayList<Double>(); List<Double> mojoFmAvgList = new
	 * ArrayList<Double>(); for (int numClusters =
	 * Config.getStartNumClustersRange(); numClusters <= Config
	 * .getEndNumClustersRange(); numClusters += Config
	 * .getRangeNumClustersStep()) {
	 *
	 * numClustersList.add(numClusters);
	 *
	 * String usingComputedFilename = "Using " + computedRsfFilename +
	 * " as computed clusters..."; logger.debug(usingComputedFilename);
	 * System.out.println(usingComputedFilename);
	 *
	 * performMojoOperationsForComputedRsfFilenameWithExpertDecompositions(
	 * mojoAvgList, mojoFmAvgList, computedRsfFilename);
	 *
	 * } System.out.println("Writing MoJo average, and MoJoFM to csv file " +
	 * Config.getMojoCSVFilename(numClustersList,selectedAlg) + " ...");
	 *
	 * createMojoCSVFile(numClustersList,mojoAvgList,mojoFmAvgList,selectedAlg);
	 *
	 * }
	 */

	// private static void
	// performPrecisionRecallOperationsForMultipleClustersOnSingleAuthClusteringFile(
	// String selectedAlg, String simMeasure, String stoppingCriterion,
	// String computedFilePrex, String authClusteringFile) {
	// List<Integer> numClustersList = new ArrayList<Integer>();
	//
	// for (int numClusters = Config.getStartNumClustersRange(); numClusters <=
	// Config
	// .getEndNumClustersRange(); numClusters += Config
	// .getRangeNumClustersStep()) {
	//
	// numClustersList.add(numClusters);
	//
	// String computedRsfFilename = constructNonTopicComputedRsfFilename(
	// computedFilePrex, selectedAlg, simMeasure,
	// stoppingCriterion, numClusters);
	// String usingComputedFilename = "Using " + computedRsfFilename
	// + " as computed clusters...";
	// logger.debug(usingComputedFilename);
	// System.out.println(usingComputedFilename);
	//
	// HashSet<HashSet<String>> allIntraPairsFromClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(computedRsfFilename);
	//
	// HashSet<HashSet<String>> allIntraPairsFromAuthClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(authClusteringFile);
	//
	// double precision = PrecisionRecallCalculator.computePrecision(
	// allIntraPairsFromClustersRsf,
	// allIntraPairsFromAuthClustersRsf);
	// String precisionOutput =
	// "Precision of computed data compared to authoritative clustering: "
	// + precision;
	// logger.debug(precisionOutput);
	// System.out.println(precisionOutput);
	//
	// double recall = PrecisionRecallCalculator.computeRecall(
	// allIntraPairsFromClustersRsf,
	// allIntraPairsFromAuthClustersRsf);
	// String recallOutput =
	// "Recall of computed data compared to decomposition: "
	// + recall;
	// logger.debug(recallOutput);
	// System.out.println(recallOutput);
	//
	// }
	//
	// }

	private static void performMoJoOperationsForMultipleClustersOnSingleAuthClusteringFile(final String selectedAlg, final String simMeasure, final String stoppingCriterion,
			final String computedFilePrex, final File authClusteringFile) {
		final List<Integer> numClustersList = new ArrayList<Integer>();
		final List<Long> mojoList = new ArrayList<Long>();
		final List<Double> mojoFmList = new ArrayList<Double>();

		double maxMojoFm = 0;
		double maxNumClusters = 0;
		double sumMojoFm = 0;
		for (int numClusters = Config.getStartNumClustersRange(); numClusters <= Config.getEndNumClustersRange(); numClusters += Config.getRangeNumClustersStep()) {

			numClustersList.add(numClusters);

			final String computedRsfFilename = constructNonTopicComputedRsfFilename(computedFilePrex, selectedAlg, simMeasure, stoppingCriterion, numClusters);
			final String usingComputedFilename = "Using " + computedRsfFilename + " as computed clusters...";
			logger.debug(usingComputedFilename);
			System.out.println(usingComputedFilename);

			MoJoCalculator mojoCalc = new MoJoCalculator(FileUtil.checkFile(computedRsfFilename, false, false), authClusteringFile, null);
			final long mojoValue = mojoCalc.mojo();
			mojoList.add(mojoValue);
			String mojoOutput = "MoJo of " + computedRsfFilename + " compared to authoritative clustering: " + mojoValue;
			logger.debug(mojoOutput);
			System.out.println(mojoOutput);

			mojoCalc = new MoJoCalculator(FileUtil.checkFile(computedRsfFilename, false, false), authClusteringFile, null);
			final double mojoFmValue = mojoCalc.mojofm();
			mojoFmList.add(mojoFmValue);
			mojoOutput = "MoJoFM of " + computedRsfFilename + " compared to authoritative clustering: " + mojoFmValue;
			logger.debug(mojoOutput);
			System.out.println(mojoOutput);

			sumMojoFm += mojoFmValue;

			if (mojoFmValue > maxMojoFm) {
				maxMojoFm = mojoFmValue;
				maxNumClusters = numClusters;
			}
		}

		final double avgMojoFm = sumMojoFm / numClustersList.size();

		final String writingMojoToCsvMsg = "Writing MoJo and MoJoFM to csv file " + Config.getMojoToAuthCSVFilename(numClustersList, selectedAlg, simMeasure) + " ...";
		final String maxMojoFmMsg = "max mojo fm: " + maxMojoFm;
		final String numClusAtMaxMojoFmMsg = "no. clusters at max mojo fm: " + maxNumClusters;
		final String avgMojoFmMsg = "avg mojo fm: " + avgMojoFm;

		System.out.println(writingMojoToCsvMsg);
		System.out.println();
		System.out.println(maxMojoFmMsg);
		System.out.println(numClusAtMaxMojoFmMsg);
		System.out.println(avgMojoFmMsg);

		logger.debug(writingMojoToCsvMsg);
		logger.debug("\n");
		logger.debug(maxMojoFmMsg);
		logger.debug(numClusAtMaxMojoFmMsg);
		logger.debug(avgMojoFmMsg);

		createMojoListsCSVFile(numClustersList, mojoList, mojoFmList, selectedAlg, simMeasure);
	}

	// private static void performPrecisionRecallForSingleAuthClustering(
	// String computedRsfFilename, String authClusteringFile) {
	// HashSet<HashSet<String>> allIntraPairsFromClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(computedRsfFilename);
	//
	// HashSet<HashSet<String>> allIntraPairsFromAuthClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(authClusteringFile);
	//
	// double precision = PrecisionRecallCalculator.computePrecision(
	// allIntraPairsFromClustersRsf, allIntraPairsFromAuthClustersRsf);
	// String precisionOutput =
	// "Precision of computed data compared to authoritative clustering: "
	// + precision;
	// logger.debug(precisionOutput);
	// System.out.println(precisionOutput);
	//
	// double recall = PrecisionRecallCalculator.computeRecall(
	// allIntraPairsFromClustersRsf, allIntraPairsFromAuthClustersRsf);
	// String recallOutput =
	// "Recall of computed data compared to decomposition: "
	// + recall;
	// logger.debug(recallOutput);
	// System.out.println(recallOutput);
	// }

	private static void performMojoForSingleAuthClustering(final File computedRsfFile, final File authClusteringFile) {
		MoJoCalculator mojoCalc = new MoJoCalculator(computedRsfFile, authClusteringFile, null);
		final long mojoValue = mojoCalc.mojo();
		String mojoOutput = "MoJo of " + computedRsfFile + " compared to authoritative clustering: " + mojoValue;
		logger.debug(mojoOutput);
		System.out.println(mojoOutput);

		mojoCalc = new MoJoCalculator(computedRsfFile, authClusteringFile, null);
		final double mojoFmValue = mojoCalc.mojofm();
		mojoOutput = "MoJoFM of " + computedRsfFile + " compared to authoritative clustering: " + mojoFmValue;
		logger.debug(mojoOutput);
		System.out.println(mojoOutput);

	}

	/*
	 * private static void performMoJoOperationsForConcernRecovery(String
	 * selectedAlg, String stoppingCriterion) { for (int
	 * numClusters=Config.getStartNumClustersRange
	 * ();numClusters<=Config.getEndNumClustersRange
	 * ();numClusters+=Config.getRangeNumClustersStep()) { List<Integer>
	 * numTopicsList = new ArrayList<Integer>(); List<Double> mojoAvgList = new
	 * ArrayList<Double>(); List<Double> mojoFmAvgList = new
	 * ArrayList<Double>(); for (int numTopics =
	 * Config.getStartNumTopicsRange();
	 * numTopics<=Config.getEndNumTopicsRange();numTopics
	 * +=Config.getRangeNumTopicsStep()) { numTopicsList.add(numTopics);
	 *
	 * String computedRsfFilename = Config.getConcernRecoveryFilePrefix() +
	 * selectedAlg + "_" + stoppingCriterion + "_" + numClusters + "_clusters_"
	 * + numTopics + "topics.rsf"; String usingComputedFilename = "Using " +
	 * computedRsfFilename + " as computed clusters...";
	 * logger.debug(usingComputedFilename);
	 * System.out.println(usingComputedFilename);
	 *
	 * performMojoOperationsForComputedRsfFilenameWithExpertDecompositions(
	 * mojoAvgList, mojoFmAvgList, computedRsfFilename);
	 *
	 *
	 *
	 * }
	 *
	 * System.out.println(
	 * "Writing topics, MoJo average, and MoJoFM to csv file to csv file...");
	 * createMojoCSVWithTopicsFile
	 * (numClusters,numTopicsList,mojoAvgList,mojoFmAvgList); }
	 *
	 *
	 *
	 * }
	 */

	// private static void performPrecisionRecallOperationsForConcernRecovery(
	// String selectedAlg, String simMeasure, String stoppingCriterion,
	// String computedFilePrefix) {
	//
	// for (int numClusters = Config.getStartNumClustersRange(); numClusters <=
	// Config
	// .getEndNumClustersRange(); numClusters += Config
	// .getRangeNumClustersStep()) {
	// List<Integer> numTopicsList = new ArrayList<Integer>();
	// List<Double> precisionAvgList = new ArrayList<Double>();
	// List<Double> recallAvgList = new ArrayList<Double>();
	// for (int numTopics = Config.getStartNumTopicsRange(); numTopics <= Config
	// .getEndNumTopicsRange(); numTopics += Config
	// .getRangeNumTopicsStep()) {
	// numTopicsList.add(numTopics);
	//
	// String computedRsfFilename = null;
	//
	// if (!selectedAlg.equals("arc")) {
	// computedRsfFilename = constructNonTopicComputedRsfFilename(
	// computedFilePrefix, selectedAlg, simMeasure,
	// stoppingCriterion, numClusters);
	// } else {
	// computedRsfFilename = constructTopicBasedComputedRsfFilename(
	// computedFilePrefix, selectedAlg, simMeasure,
	// stoppingCriterion, numClusters, numTopics);
	// }
	//
	// HashSet<HashSet<String>> allIntraPairsFromClustersRsf =
	// IntraPairFromClustersRsfBuilder
	// .buildIntraPairsFromClustersRsf(computedRsfFilename);
	// buildRequiredIntraPairDataFromExpertDecomposition();
	//
	// String usingComputedFilename = "Using " + computedRsfFilename
	// + " as computed clusters...";
	// logger.debug(usingComputedFilename);
	// System.out.println(usingComputedFilename);
	// int decompositionCounter = 0;
	// double precisionSum = 0;
	// for (ExpertDecomposition decomposition :
	// ExpertDecompositionBuilder.expertDecompositions) {
	// double precision = PrecisionRecallCalculator
	// .computePrecision(allIntraPairsFromClustersRsf,
	// decomposition.allIntraPairs);
	// String precisionOutput =
	// "Precision of computed data compared to decomposition "
	// + decompositionCounter + " " + precision;
	// logger.debug(precisionOutput);
	// System.out.println(precisionOutput);
	// precisionSum += precision;
	// decompositionCounter++;
	// }
	//
	// decompositionCounter = 0;
	// double recallSum = 0;
	// for (ExpertDecomposition decomposition :
	// ExpertDecompositionBuilder.expertDecompositions) {
	// double recall = PrecisionRecallCalculator.computeRecall(
	// allIntraPairsFromClustersRsf,
	// decomposition.allIntraPairs);
	// String recallOutput =
	// "Recall of computed data compared to decomposition "
	// + decompositionCounter + " " + recall;
	// logger.debug(recallOutput);
	// System.out.println(recallOutput);
	// recallSum += recall;
	// decompositionCounter++;
	// }
	//
	// double precisionAvg = precisionSum / decompositionCounter;
	// String precisionAvgOutput = "Precision averge: " + precisionAvg;
	// logger.debug(precisionAvgOutput);
	// System.out.println(precisionAvgOutput);
	// precisionAvgList.add(precisionAvg);
	//
	// double recallAvg = recallSum / decompositionCounter;
	// String recallAvgOutput = "Recall average: " + recallAvg;
	// logger.debug(recallAvgOutput);
	// System.out.println(recallAvgOutput);
	// recallAvgList.add(recallAvg);
	// } // numTopics loop
	// System.out
	// .println("Writing topics, precision average, and recall average to csv file...");
	// createTopicsPrecisionRecallCSVFile(numClusters, numTopicsList,
	// precisionAvgList, recallAvgList);
	// }// numClusters loop
	//
	// }

	private static void createMojoListsCSVFile(final List<Integer> numClustersList, final List<Long> mojoList, final List<Double> mojoFmList, final String selectedAlg, final String simMeasure) {
		try {
			final FileWriter fstream = new FileWriter(Config.getMojoToAuthCSVFilename(numClustersList, selectedAlg, simMeasure));
			final BufferedWriter out = new BufferedWriter(fstream);

			out.write("number of clusters,");
			for (final int numClusters : numClustersList) {
				out.write(numClusters + ",");
			}
			out.write("\n");

			writeMojoListsToFile(mojoList, mojoFmList, out);

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static void createMojoToAuthListsCSVFile(final List<Integer> numClustersList, final List<Long> mojoList, final List<Double> mojoFmList, final String selectedAlg, final String simMeasure,
			final List<Integer> numTopicsList) {
		try {
			final FileWriter fstream = new FileWriter(Config.getMojoToAuthCSVFilename(numClustersList, selectedAlg, simMeasure));
			final BufferedWriter out = new BufferedWriter(fstream);

			out.write("number of topics,");
			for (final int numTopics : numTopicsList) {
				out.write(numTopics + ",");
			}
			out.write("\n");

			out.write("number of clusters,");
			for (final int numClusters : numClustersList) {
				out.write(numClusters + ",");
			}
			out.write("\n");

			writeMojoListsToFile(mojoList, mojoFmList, out);

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static void writeMojoListsToFile(final List<Long> mojoList, final List<Double> mojoFmList, final BufferedWriter out) throws IOException {
		out.write("MoJo,");
		for (final long mojo : mojoList) {
			out.write(mojo + ",");
		}
		out.write("\n");

		out.write("MoJoFM,");
		for (final double mojoFm : mojoFmList) {
			out.write(mojoFm + ",");
		}
		out.write("\n");

	}

	// private static void writeMojoAveragesToFile(final List<Double>
	// mojoAvgList,
	// final List<Double> mojoFmAvgList, final BufferedWriter out)
	// throws IOException {
	// out.write("MoJo average,");
	// for (final double mojoAvg : mojoAvgList) {
	// out.write(mojoAvg + ",");
	// }
	// out.write("\n");
	//
	// out.write("MoJoFM average,");
	// for (final double mojoFm : mojoFmAvgList) {
	// out.write(mojoFm + ",");
	// }
	// out.write("\n");
	// }

	/*
	 * private static void createMojoCSVWithTopicsFile(int numClusters,
	 * List<Integer> numTopicsList, List<Double> mojoAvgList, List<Double>
	 * mojoFmAvgList) { try { FileWriter fstream = new FileWriter(
	 * Config.getMojoWithTopicsCSVFilename(numClusters)); BufferedWriter out =
	 * new BufferedWriter(fstream); writeNumTopicsListToFile(numTopicsList,
	 * out);
	 *
	 * writeMojoAveragesToFile(mojoAvgList, mojoFmAvgList, out);
	 *
	 * out.close(); } catch (IOException e) {
	 * e.printStackTrace();System.exit(-1); }
	 *
	 * }
	 */
}
