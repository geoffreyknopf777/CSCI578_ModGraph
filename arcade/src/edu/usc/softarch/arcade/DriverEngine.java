package edu.usc.softarch.arcade;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import edu.usc.softarch.arcade.antipattern.detection.ADADetector;
import edu.usc.softarch.arcade.clustering.Cluster;
import edu.usc.softarch.arcade.clustering.ClusteringEngine;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.datatypes.RunType;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.util.FileUtil;

/**
 * @author joshua
 *
 */
public class DriverEngine {

	private static ArrayList<Cluster> clusters = null;

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(DriverEngine.class);

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(final String[] args) throws Exception {
		final Options options = new Options();

		final Option help = new Option("help", "print this message");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("project configuration file");
		final Option projFile = OptionBuilder.create("projfile");

		options.addOption(help);
		options.addOption(projFile);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("projfile"))
				Config.setProjConfigFile(FileUtil.checkFile(line.getOptionValue("projfile"), false, false));
			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("DriverEngine", options);
				System.exit(0);
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		if (Config.runType.equals(RunType.whole))
			run();
		else
			logger.error("Cannot determine run type");

	}

	public static void run() throws Exception {

		Config.initConfigFromFile(Config.getProjConfigFile());

		final File clustersFile = new File(Config.getSerializedClustersFilename());
		final File fastFeatureVectorsFile = new File(Config.getFastFeatureVectorsFilename());

		if (!fastFeatureVectorsFile.exists() || !Config.useFastFeatureVectorsFile) {
			System.out.println("Reading in rsf file...");
			RsfReader.loadRsfDataForCurrProj();
			RsfReader.performPreClusteringTasks();
		}

		if (Config.enableClustering)
			if (!clustersFile.exists() || fastFeatureVectorsFile.exists() || Config.forceClustering) {
				System.out.println("Performing clustering tasks...");

				// Common setup for clustering
				final int clustersToStop = Config.getStartNumClustersRange();
				logger.debug("clustersToStop: " + clustersToStop);
				System.out.println("Stopping using " + Config.getStoppingCriterion() + " criterion...");
				System.out.println("Will stop at " + clustersToStop + " clusters...");
				System.out.println("Using " + Config.getCurrentClusteringAlgorithm() + " clustering algorithm...");
				System.out.println("Using " + Config.getCurrSimMeasure() + " as the similarity measure...");
				Config.setNumClusters(clustersToStop);
				// end common setup for clustering

				if (Config.isUsingPreselectedRange()) {
					final List<Integer> clustersToWriteList = new ArrayList<Integer>();

					for (int clustersToWrite = Config.getStartNumClustersRange(); clustersToWrite <= Config.getEndNumClustersRange(); clustersToWrite += Config.getRangeNumClustersStep())
						clustersToWriteList.add(clustersToWrite);

					Config.setClustersToWriteList(clustersToWriteList);

					if (Config.isUsingNumTopicsRange()) {
						final List<Integer> numTopicsList = new ArrayList<Integer>();
						for (int numTopics = Config.getStartNumTopicsRange(); numTopics <= Config.getEndNumTopicsRange(); numTopics += Config.getRangeNumTopicsStep()) {
							logger.debug("numTopics: " + numTopics);
							System.out.println("Clustering using " + numTopics + " topics...");
							numTopicsList.add(numTopics);
							// TopicUtil.docTopics =
							// TopicUtil.getDocTopicsFromVariableMalletDocTopicsFile();
						}
						Config.setNumTopicsList(numTopicsList);
					}

				}

				final ClusteringEngine ce = new ClusteringEngine();
				ce.run();
			}

		if (Config.enablePostClusteringTasks) {
			System.out.println("Running post-clustering tasks...");
			runPostClusteringTasksUsingClusterFile();
		}
		System.out.println("Exiting...");
	}

	private static void runPostClusteringTasksUsingClusterFile() throws IOException, ClassNotFoundException, SAXException, ParserConfigurationException, TransformerException {
		final boolean enableSplitClusters = false;
		final boolean enablePrintSplitClusters = true;
		final boolean enableSmellDetection = false;

		deserializeClusters();
		logger.debug("Pre-computed clusters: ");
		logger.debug(clusters);

		logger.debug("Pretty printing deserialized clusters: ");
		ClusterUtil.prettyPrintSplitClusters(clusters);

		/*
		 * Iterator iter = clusters.iterator(); Cluster c = null; int
		 * clusterCount = 0; while (iter.hasNext()) { c = (Cluster) iter.next();
		 * clusterCount++; }
		 * 
		 * logger.debug("Cluster items size: " + c.items.size());
		 * logger.debug("Cluster items: ");
		 * logger.debug(c.itemsToStringOnLine());
		 */

		if (enableSplitClusters) {

			final ArrayList<Cluster> splitClusters = ClusterUtil.splitClusters(clusters);

			if (enablePrintSplitClusters) {
				logger.debug("In " + Thread.currentThread().getStackTrace()[1].getClassName() + "." + Thread.currentThread().getStackTrace()[1].getMethodName() + ", printing split clusters");
				ClusterUtil.printItemsInClusters(splitClusters);
				logger.debug("Pretty printing splitclusters: ");
				ClusterUtil.prettyPrintSplitClusters(splitClusters);
			}

		}

		if (enableSmellDetection)
			ADADetector.runSmellDetectionAlgorithms(clusters);
	}

	private static void deserializeClusters() {
		FileInputStream fis = null;
		ObjectInputStream in = null;
		final String filename = Config.getSerializedClustersFilename();
		logger.debug("Will try to deserialize from file " + filename);
		try {
			fis = new FileInputStream(filename);
			in = new ObjectInputStream(fis);
			logger.debug("Reading in serialized clusters...");
			final int clustersSize = in.readInt();
			Cluster c = null;
			clusters = new ArrayList<Cluster>();
			for (int i = 0; i < clustersSize; i++) {
				c = (Cluster) in.readObject();
				clusters.add(c);
			}
			logger.debug("Closing serialized cluster file...");
			in.close();
		} catch (final IOException ex) {
			ex.printStackTrace();
			System.exit(-1);
		} catch (final ClassNotFoundException ex) {
			ex.printStackTrace();
			System.exit(-1);
		}

		for (final Cluster c : clusters)
			c.preparePriorityQueue();

	}

	public static void redirectSystemErr(final String stdErrFilename) {
		try {
			System.setErr(new PrintStream(new FileOutputStream(stdErrFilename)));
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}

	}

	public static void redirectSystemOut(final String stdOutFilename) {
		try {
			System.setOut(new PrintStream(new FileOutputStream(stdOutFilename)));
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	}

}
