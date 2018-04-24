package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.classgraphs.StringEdge;
import edu.usc.softarch.arcade.clustering.StringGraph;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.GroundTruthFileParser;
import edu.usc.softarch.extractors.cda.odem.Type;

public class GroundTruthRecoveryGraphBuilder {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(GroundTruthRecoveryGraphBuilder.class);

	public static void main(final String[] args) {
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

			if (line.hasOption("projfile")) {
				Config.setProjConfigFile(line.getOptionValue("projfile"));
			}
			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("GroundTruthRecoveryGraphBuilder", options);
				System.exit(0);
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		Config.initConfigFromFile(Config.getProjConfigFile());

		System.out.println("Reading in odem file " + Config.getOdemFile() + "...");

		ODEMReader.setTypesFromODEMFile(Config.getOdemFile());
		final List<Type> allTypes = ODEMReader.getAllTypes();
		final HashMap<String, Type> typeMap = new HashMap<String, Type>();
		for (final Type t : allTypes) {
			typeMap.put(t.getName().trim(), t);
		}

		System.out.println("Reading in ground truth file: " + Config.getGroundTruthFile());

		if (Config.getGroundTruthFile().getPath().endsWith(".rsf")) {
			GroundTruthFileParser.parseRsf(Config.getGroundTruthFile());
		} else {
			GroundTruthFileParser.parseHadoopStyle(Config.getGroundTruthFile());
		}
		final Set<ConcernCluster> nonPkgBasedClusters = GroundTruthFileParser.getClusters();

		final StringGraph nonPkgBasedClusterGraph = ClusterUtil.buildClusterGraphUsingOdemClasses(typeMap, nonPkgBasedClusters);
		logger.debug("Printing cluster graph of hdfs and mapred...");
		logger.debug(nonPkgBasedClusterGraph);

		final Set<String> allClasses = new HashSet<String>();
		for (final Type type : allTypes) {
			allClasses.add(type.getName().trim());
		}
		final Set<String> nodesInClusterGraph = ClusterUtil.getNodesInClusterGraph(nonPkgBasedClusterGraph);
		logger.debug("Number of nodes in cluster graph: " + nodesInClusterGraph.size());

		final Set<String> classesInClusterGraph = ClusterUtil.getClassesInClusters(nonPkgBasedClusters);
		logger.debug("Number of classes in all clusters: " + classesInClusterGraph.size());

		final Set<String> unClusteredClasses = new HashSet<String>(allClasses);
		unClusteredClasses.removeAll(classesInClusterGraph);

		logger.debug("Unclustered classes...");
		int classCount = 0;
		for (final String c : unClusteredClasses) {
			logger.debug(classCount + ": " + c);
			classCount++;
		}

		final Set<String> packagesOfUnclusteredClasses = new HashSet<String>();
		for (final String c : unClusteredClasses) {
			packagesOfUnclusteredClasses.add(c.substring(c.indexOf("org"), c.lastIndexOf(".")));
		}

		logger.debug("Packages of unclustered classes");
		int pkgCount = 0;
		for (final String pkg : packagesOfUnclusteredClasses) {
			logger.debug(pkgCount + ": " + pkg);
			pkgCount++;
		}

		final Set<String> topLevelPackagesOfUnclusteredClasses = new HashSet<String>();
		final String topLevelPkgPatternStr = "org\\.apache\\.hadoop\\.\\w+";
		final Pattern topLevelPkgPattern = Pattern.compile(topLevelPkgPatternStr);

		for (final String pkg : packagesOfUnclusteredClasses) {
			final Matcher m = topLevelPkgPattern.matcher(pkg);
			while (m.find()) {
				topLevelPackagesOfUnclusteredClasses.add(m.group(0));
			}
		}

		logger.debug("Top-level packages of unclustered classes");
		pkgCount = 0;
		for (final String pkg : topLevelPackagesOfUnclusteredClasses) {
			logger.debug(pkgCount + ": " + pkg);
			pkgCount++;
		}

		final Set<ConcernCluster> pkgBasedClusters = ClusterUtil.buildGroundTruthClustersFromPackages(topLevelPackagesOfUnclusteredClasses, unClusteredClasses);
		final StringGraph pkgBasedClusterGraph = ClusterUtil.buildClusterGraphUsingOdemClasses(typeMap, pkgBasedClusters);

		final Set<ConcernCluster> allClusters = new HashSet<ConcernCluster>(nonPkgBasedClusters);
		allClusters.addAll(pkgBasedClusters);

		final StringGraph fullClusterGraph = ClusterUtil.buildClusterGraphUsingOdemClasses(typeMap, allClusters);

		final Set<String> twoWayClusters = new HashSet<String>();
		logger.debug("Clusters that would be merged together...");
		int mergeCount = 0;
		for (final StringEdge edge : fullClusterGraph.edges) {
			final StringEdge reversedEdge = new StringEdge(edge.tgtStr, edge.srcStr);
			if (fullClusterGraph.containsEdge(reversedEdge)) {
				logger.debug("\t Would be merged: " + edge.srcStr + ", " + edge.tgtStr);
				twoWayClusters.add(edge.srcStr.trim());
				twoWayClusters.add(edge.tgtStr.trim());
				mergeCount++;
			}
		}
		logger.debug("Total clusters that would be merged: " + mergeCount);

		logger.debug("Clusters involved in two-way associations...");
		int clusterCount = 0;
		for (final String cluster : twoWayClusters) {
			logger.debug(clusterCount + ": " + cluster);
			clusterCount++;
		}

		final Set<StringGraph> internalGraphs = ClusterUtil.buildInternalGraphs(typeMap, allClusters);

		final String dotFileWritingMsg = "Writing out dot files for cluster graphs...";
		System.out.println(dotFileWritingMsg);
		logger.debug(dotFileWritingMsg);
		try {
			nonPkgBasedClusterGraph.writeDotFile(Config.getNonPkgBasedGroundTruthClusterGraphDotFilename());
			pkgBasedClusterGraph.writeDotFile(Config.getPkgBasedGroundTruthClusterGraphDotFilename());
			fullClusterGraph.writeDotFile(Config.getFullGroundTruthClusterGraphDotFilename());

			for (final StringGraph graph : internalGraphs) {
				graph.writeDotFile(Config.getInternalGraphDotFilename(graph.getName()));
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final String rsfFileWritingMsg = "Writing out ground truth RSF file " + Config.getGroundTruthRsfFilename() + "...";
		System.out.println(rsfFileWritingMsg);
		logger.debug(rsfFileWritingMsg);

		try {
			final FileWriter fw = new FileWriter(Config.getGroundTruthRsfFilename());
			final BufferedWriter out = new BufferedWriter(fw);
			clusterCount = 0;
			for (final ConcernCluster cluster : nonPkgBasedClusters) {
				for (final String entity : cluster.getEntities()) {
					final String rsfLine = "contain " + cluster.getName().replaceAll("[:\\s]", "_") + " " + entity;
					logger.debug(rsfLine);
					out.write(rsfLine + "\n");
				}
				clusterCount++;
			}

			for (final ConcernCluster cluster : pkgBasedClusters) {
				for (final String entity : cluster.getEntities()) {
					final String rsfLine = "contain " + cluster.getName().replaceAll("[:\\s]", "_") + " " + entity;
					logger.debug(rsfLine);
					out.write(rsfLine + "\n");
				}
				clusterCount++;
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
