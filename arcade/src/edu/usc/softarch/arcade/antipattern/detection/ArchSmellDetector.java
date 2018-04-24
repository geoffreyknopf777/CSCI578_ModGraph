package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.logging.log4j.Logger;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import com.google.common.base.Joiner;
import com.google.common.primitives.Doubles;
import com.thoughtworks.xstream.XStream;

import edu.usc.softarch.arcade.clustering.StringGraph;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.driver.ConcernClusterRsf;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.topics.DocTopicItem;
import edu.usc.softarch.arcade.topics.DocTopics;
import edu.usc.softarch.arcade.topics.TopicItem;
import edu.usc.softarch.arcade.topics.TopicModelExtractionMethod;
import edu.usc.softarch.arcade.topics.TopicUtil;
import edu.usc.softarch.arcade.util.FileUtil;

public class ArchSmellDetector {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ArchSmellDetector.class);
	public static TopicModelExtractionMethod tmeMethod = TopicModelExtractionMethod.VAR_MALLET_FILE;

	static final Comparator<TopicItem> TOPIC_PROPORTION_ORDER = (t1, t2) -> {
		final Double prop1 = t1.proportion;
		final Double prop2 = t2.proportion;
		return prop1.compareTo(prop2);
	};

	public static DocTopics docTopics;

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		logger.entry(args);
		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String detectedSmellsFilename = setupOld(args);
		runAllDetectionAlgs(FileUtil.checkFile(detectedSmellsFilename, false, false));
		logger.traceExit();
	}

	public static void setupAndRunStructuralDetectionAlgs(final String[] args) {
		logger.entry(args);
		final File depsRsfFile = FileUtil.checkFile(args[0], true, false);
		final File clustersRsfFile = FileUtil.checkFile(args[1], true, false);
		final File detectedSmellsFile = FileUtil.checkFile(args[2], true, false);
		Config.setDepsRsfFile(depsRsfFile);
		Config.setSmellClustersFile(clustersRsfFile);
		runStructuralDetectionAlgs(detectedSmellsFile);
		logger.traceExit();
	}

	private static String setupOld(final String[] args) {
		logger.entry(args);
		String detectedSmellsFilename = "";

		final Options options = new Options();

		final Option help = new Option("help", "print this message");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("project configuration file");
		final Option projFile = OptionBuilder.create("projfile");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("detected smells to affected classes file");
		final Option smellClassesFile = OptionBuilder.create("smellClassesFile");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("serialized detected smells file");
		final Option detectedSmellsFile = OptionBuilder.create("detectedSmellsFile");

		options.addOption(help);
		options.addOption(projFile);
		options.addOption(smellClassesFile);
		options.addOption(detectedSmellsFile);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("projfile")) {
				Config.setProjConfigFile(FileUtil.checkFile(line.getOptionValue("projfile"), false, false));
			}
			if (line.hasOption("detectedSmellsFile")) {
				detectedSmellsFilename = line.getOptionValue("detectedSmellsFile");
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		Config.initConfigFromFile(Config.getProjConfigFile());
		logger.traceExit();
		return detectedSmellsFilename;
	}

	public static void runAllDetectionAlgs(final File detectedSmellsFile) {
		logger.entry(detectedSmellsFile);
		final Set<Smell> detectedSmells = new LinkedHashSet<Smell>();
		System.out.println("Reading in clusters file: " + Config.getSmellClustersFile());
		final Set<ConcernCluster> clusters = ConcernClusterRsf
				.extractConcernClustersFromRsfFile(Config.getSmellClustersFile());

		Controller.getCurrentView().setClusters(clusters);

		final boolean showBuiltClusters = true;
		if (showBuiltClusters) {
			logger.debug("Found and built clusters:");
			for (final ConcernCluster cluster : clusters) {
				logger.debug(cluster.getName());
			}
		}

		buildConcernClustersFromConfigTopicsFile(clusters);

		for (final ConcernCluster cluster : clusters) {
			if (cluster.getDocTopicItem() != null) {
				logger.debug(cluster.getName() + " has topics: ");
				final DocTopicItem docTopicItem = cluster.getDocTopicItem();
				Collections.sort(docTopicItem.topics, TOPIC_PROPORTION_ORDER);
				for (final TopicItem topicItem : docTopicItem.topics) {
					logger.debug("\t" + topicItem);
				}
			}
		}

		final Map<String, Set<String>> clusterSmellMap = new HashMap<String, Set<String>>();

		detectBco(detectedSmells, clusters, clusterSmellMap);

		// detectSpfOld(clusters, clusterSmellMap);
		detectSpfNew(clusters, clusterSmellMap, detectedSmells);

		final Map<String, Set<String>> depMap = ClusterUtil.buildDependenciesMap(Config.getDepsRsfFile());

		final StringGraph clusterGraph = ClusterUtil.buildClusterGraphUsingDepMap(depMap, clusters);
		System.out.print("");

		final SimpleDirectedGraph<String, DefaultEdge> directedGraph = ClusterUtil.buildConcernClustersDiGraph(clusters,
				clusterGraph);

		detectBdc(detectedSmells, clusters, clusterSmellMap, directedGraph);

		detectBuo(detectedSmells, clusters, clusterSmellMap, directedGraph);

		for (final String clusterName : clusterSmellMap.keySet()) {
			final Set<String> smellList = clusterSmellMap.get(clusterName);
			logger.debug(clusterName + " has smells " + Joiner.on(",").join(smellList));
		}

		final Map<String, Set<String>> smellClustersMap = buildSmellToClustersMap(clusterSmellMap);

		for (final Entry<String, Set<String>> entry : smellClustersMap.entrySet()) {
			logger.debug(entry.getKey() + " : " + entry.getValue());
		}

		// buildSmellToClassesMap(clusters, smellClustersMap);

		for (final Smell smell : detectedSmells) {
			logger.debug(SmellUtil.getSmellAbbreviation(smell) + " " + smell);
		}

		serializeDetectedSmells(detectedSmellsFile, detectedSmells);

		Controller.getCurrentView().setSmellyClusters(clusterSmellMap.keySet());
		Controller.getCurrentView().setSmellClusters(new HashMap<String, Set<String>>(smellClustersMap));
		logger.traceExit();
	}

	private static void runStructuralDetectionAlgs(final File detectedSmellsFile) {
		logger.entry(detectedSmellsFile);
		final Set<Smell> detectedSmells = new LinkedHashSet<Smell>();
		System.out.println("Reading in clusters file: " + Config.getSmellClustersFile());
		final Set<ConcernCluster> clusters = ConcernClusterRsf
				.extractConcernClustersFromRsfFile(Config.getSmellClustersFile());

		final boolean showBuiltClusters = true;
		if (showBuiltClusters) {
			logger.debug("Found and built clusters:");
			for (final ConcernCluster cluster : clusters) {
				logger.debug(cluster.getName());
			}
		}

		final Map<String, Set<String>> clusterSmellMap = new HashMap<String, Set<String>>();

		final Map<String, Set<String>> depMap = ClusterUtil.buildDependenciesMap(Config.getDepsRsfFile());

		final StringGraph clusterGraph = ClusterUtil.buildClusterGraphUsingDepMap(depMap, clusters);
		System.out.print("");

		final SimpleDirectedGraph<String, DefaultEdge> directedGraph = ClusterUtil.buildConcernClustersDiGraph(clusters,
				clusterGraph);

		detectBdc(detectedSmells, clusters, clusterSmellMap, directedGraph);

		detectBuo(detectedSmells, clusters, clusterSmellMap, directedGraph);

		for (final String clusterName : clusterSmellMap.keySet()) {
			final Set<String> smellList = clusterSmellMap.get(clusterName);
			logger.debug(clusterName + " has smells " + Joiner.on(",").join(smellList));
		}

		final Map<String, Set<String>> smellClustersMap = buildSmellToClustersMap(clusterSmellMap);

		for (final Entry<String, Set<String>> entry : smellClustersMap.entrySet()) {
			logger.debug(entry.getKey() + " : " + entry.getValue());
		}

		// buildSmellToClassesMap(clusters, smellClustersMap);

		for (final Smell smell : detectedSmells) {
			logger.debug(SmellUtil.getSmellAbbreviation(smell) + " " + smell);
		}

		serializeDetectedSmells(detectedSmellsFile, detectedSmells);
		logger.traceExit();
	}

	private static void serializeDetectedSmells(final File detectedSmellsFile, final Set<Smell> detectedSmells) {
		logger.entry(detectedSmellsFile, detectedSmells);
		try {
			PrintWriter writer;
			writer = new PrintWriter(detectedSmellsFile, "UTF-8");

			final XStream xstream = new XStream();
			final String xml = xstream.toXML(detectedSmells);

			writer.println(xml);

			writer.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logger.traceExit();
	}

	// private static void buildSmellToClassesMap(Set<ConcernCluster> clusters,
	// Map<String, Set<String>> smellClustersMap) {
	// // create smell to classes map
	// Map<String, Set<String>> smellClassesMap = new HashMap<String,
	// Set<String>>();
	// for (Entry<String, Set<String>> entry : smellClustersMap.entrySet()) {
	// String smell = entry.getKey();
	// Set<String> mClusters = entry.getValue();
	// for (String clusterName : mClusters) {
	// ConcernCluster cluster = findCluster(clusters, clusterName);
	// assert cluster != null : "Could not find cluster "
	// + clusterName;
	// if (smellClassesMap.containsKey(smell)) {
	// Set<String> classes = smellClassesMap.get(smell);
	// classes.addAll(cluster.getEntities());
	// } else {
	// Set<String> classes = new HashSet<String>();
	// classes.addAll(cluster.getEntities());
	// smellClassesMap.put(smell, classes);
	// }
	// }
	// }
	// }

	private static Map<String, Set<String>> buildSmellToClustersMap(final Map<String, Set<String>> clusterSmellMap) {
		logger.entry(clusterSmellMap);
		// create smell to clusters map
		final Map<String, Set<String>> smellClustersMap = new HashMap<String, Set<String>>();
		for (final String clusterName : clusterSmellMap.keySet()) {
			final Set<String> smellList = clusterSmellMap.get(clusterName);
			for (final String smell : smellList) {
				if (smellClustersMap.containsKey(smell)) {
					final Set<String> mClusters = smellClustersMap.get(smell);
					mClusters.add(clusterName);
				} else {
					final Set<String> mClusters = new HashSet<String>();
					mClusters.add(clusterName);
					smellClustersMap.put(smell, mClusters);
				}
			}
		}
		logger.traceExit();
		return smellClustersMap;
	}

	private static void detectBuo(final Set<Smell> detectedSmells, final Set<ConcernCluster> clusters,
			final Map<String, Set<String>> clusterSmellMap,
			final SimpleDirectedGraph<String, DefaultEdge> directedGraph) {
		logger.entry(detectedSmells, clusters, clusterSmellMap);
		final Set<String> vertices = directedGraph.vertexSet();

		logger.debug("Computing the in and out degress of each vertex");
		final List<Double> inDegrees = new ArrayList<Double>();
		final List<Double> outDegrees = new ArrayList<Double>();
		for (final String vertex : vertices) {
			boolean analyzeThisVertex = true;
			if (Config.getClusterStartsWith() != null) {
				analyzeThisVertex = vertex.startsWith(Config.getClusterStartsWith());
			}
			if (analyzeThisVertex) {
				logger.debug("\t" + vertex);
				final int inDegree = directedGraph.inDegreeOf(vertex);
				final int outDegree = directedGraph.outDegreeOf(vertex);
				logger.debug("\t\t in degree: " + inDegree);
				logger.debug("\t\t out degree: " + outDegree);
				inDegrees.add((double) inDegree);
				outDegrees.add((double) outDegree);
			}
		}

		final double[] inAndOutDegreesArray = new double[inDegrees.size()];
		for (int i = 0; i < inDegrees.size(); i++) {
			final double inPlusOutAtI = inDegrees.get(i) + outDegrees.get(i);
			inAndOutDegreesArray[i] = inPlusOutAtI;
		}

		final double[] inDegreesArray = Doubles.toArray(inDegrees);
		final double[] outDegreesArray = Doubles.toArray(outDegrees);
		final double meanInDegrees = StatUtils.mean(inDegreesArray);
		final double meanOutDegrees = StatUtils.mean(outDegreesArray);
		final double meanInAndOutDegrees = StatUtils.mean(inAndOutDegreesArray);

		final StandardDeviation stdDev = new StandardDeviation();
		final double stdDevInDegrees = stdDev.evaluate(inDegreesArray);
		final double stdDevOutDegrees = stdDev.evaluate(outDegreesArray);
		final double stdDevInAndOutDegrees = stdDev.evaluate(inAndOutDegreesArray);
		logger.debug("mean of in degrees: " + meanInDegrees);
		logger.debug("mean of out degrees: " + meanOutDegrees);
		logger.debug("mean of in plus out degrees: " + meanInAndOutDegrees);
		logger.debug("std dev of in degrees: " + stdDevInDegrees);
		logger.debug("std dev of out degrees: " + stdDevOutDegrees);
		logger.debug("std dev of in plus out degrees: " + stdDevInAndOutDegrees);

		final double stdDevFactor = 1.5;
		for (final String vertex : vertices) {
			boolean analyzeThisVertex = true;
			if (Config.getClusterStartsWith() != null) {
				analyzeThisVertex = vertex.startsWith(Config.getClusterStartsWith());
			}
			if (analyzeThisVertex) {
				final int inDegree = directedGraph.inDegreeOf(vertex);
				final int outDegree = directedGraph.outDegreeOf(vertex);
				if (inDegree > meanInDegrees + stdDevFactor * stdDevInDegrees) {
					logger.debug("\t\t" + vertex + " has brick use overload for in degrees");
					logger.debug("\t\t in degree: " + inDegree);
					updateSmellMap(clusterSmellMap, vertex, "buo");
					addDetectedBuoSmell(detectedSmells, clusters, vertex);
				}
				if (outDegree > meanOutDegrees + stdDevFactor * stdDevOutDegrees) {
					logger.debug("\t\t" + vertex + " has brick use overload for out degrees");
					logger.debug("\t\t out degree: " + outDegree);
					updateSmellMap(clusterSmellMap, vertex, "buo");
					addDetectedBuoSmell(detectedSmells, clusters, vertex);
				}
				if (inDegree + outDegree > meanInDegrees + meanOutDegrees + stdDevFactor * stdDevOutDegrees
						+ stdDevFactor * stdDevInDegrees) {
					logger.debug("\t\t" + vertex + " has brick use overload for both in and out degrees");
					logger.debug("\t\t in degree: " + inDegree);
					logger.debug("\t\t out degree: " + outDegree);
					updateSmellMap(clusterSmellMap, vertex, "buo");
					addDetectedBuoSmell(detectedSmells, clusters, vertex);
				}
				final int inPlusOutDegree = inDegree + outDegree;
				if (inPlusOutDegree > meanInAndOutDegrees + stdDevFactor * stdDevInAndOutDegrees) {
					logger.debug("\t\t" + vertex + " has brick use overload for in plus out degrees");
					logger.debug("\t\t in plus out degrees: " + inPlusOutDegree);
					updateSmellMap(clusterSmellMap, vertex, "buo");
					addDetectedBuoSmell(detectedSmells, clusters, vertex);
				}

			}
		}
		logger.traceExit();
	}

	// brick dependency cycle
	private static void detectBdc(final Set<Smell> detectedSmells, final Set<ConcernCluster> clusters,
			final Map<String, Set<String>> clusterSmellMap,
			final SimpleDirectedGraph<String, DefaultEdge> directedGraph) {
		logger.entry(detectedSmells, clusters, clusterSmellMap);
		System.out.println("Finding cycles...");
		// CycleDetector cycleDetector = new CycleDetector(directedGraph);
		// Set<String> cycleSet = cycleDetector.findCycles();
		// logger.debug("Printing the cycle set, i.e., the set of all vertices
		// which participate in at least one cycle in this graph...");
		// logger.debug(cycleSet);

		final StrongConnectivityInspector<String, DefaultEdge> inspector = new StrongConnectivityInspector<String, DefaultEdge>(
				directedGraph);
		final List<Set<String>> connectedSets = inspector.stronglyConnectedSets();
		// logger.debug("Printing the strongly connected sets of the
		// graph....");
		// logger.debug(Joiner.on("\n").join(connectedSets));

		int relevantConnectedSetCount = 0;
		final Set<Set<String>> bdcConnectedSets = new HashSet<Set<String>>();
		for (final Set<String> connectedSet : connectedSets) {
			if (connectedSet.size() > 2) {
				logger.debug("Counting this strongly connected component set as relevant");
				logger.debug(connectedSet);
				relevantConnectedSetCount++;
				for (final String clusterName : connectedSet) {
					updateSmellMap(clusterSmellMap, clusterName, "bdc");
				}
				logger.debug("scc size: " + connectedSet.size());
				bdcConnectedSets.add(connectedSet);
			}
		}

		for (final Set<String> bdcConnectedSet : bdcConnectedSets) {
			final Smell bdc = new BdcSmell();
			final Set<ConcernCluster> bdcClusters = new HashSet<ConcernCluster>();
			for (final String clusterName : bdcConnectedSet) {
				final ConcernCluster cluster = getMatchingCluster(clusterName, clusters);
				assert cluster != null : "No matching cluster found for " + clusterName;
				bdcClusters.add(cluster);

			}
			bdc.clusters = new HashSet<ConcernCluster>(bdcClusters);
			detectedSmells.add(bdc);
		}

		logger.debug("Number of strongly connected components: " + relevantConnectedSetCount);
		logger.traceExit();
	}

	// brick concern overload
	private static StandardDeviation detectBco(final Set<Smell> detectedSmells, final Set<ConcernCluster> clusters,
			final Map<String, Set<String>> clusterSmellMap) {
		logger.entry(detectedSmells, clusters, clusterSmellMap);
		System.out.println("Finding brick concern overload instances...");
		final double concernOverloadTopicThreshold = .10;
		// double concernCountThreshold = 2;
		final Map<ConcernCluster, Integer> concernCountMap = new HashMap<ConcernCluster, Integer>();
		for (final ConcernCluster cluster : clusters) {
			if (cluster.getDocTopicItem() != null) {
				logger.debug("Have doc-topics for " + cluster.getName());
				final DocTopicItem docTopicItem = cluster.getDocTopicItem();
				int concernCount = 0;
				for (final TopicItem topicItem : docTopicItem.topics) {
					if (topicItem.proportion > concernOverloadTopicThreshold) {
						logger.debug(
								"\t" + cluster.getName() + " is beyond concern overload threshold for " + topicItem);
						concernCount++;
					}
				}
				concernCountMap.put(cluster, concernCount);
			}
		}

		final StandardDeviation stdDev = new StandardDeviation();

		final int[] intConcernCountValues = ArrayUtils.toPrimitive(concernCountMap.values().toArray(new Integer[0]));
		final double[] doubleConcernCountValues = new double[intConcernCountValues.length];
		for (int i = 0; i < intConcernCountValues.length; i++) {
			doubleConcernCountValues[i] = intConcernCountValues[i];
		}
		final double concernCountMean = StatUtils.mean(doubleConcernCountValues);
		final double concernCountStdDev = stdDev.evaluate(doubleConcernCountValues);
		logger.debug("relevant concern count mean: " + concernCountMean);
		logger.debug("relevant concern count standard deviation: " + concernCountStdDev);

		for (final ConcernCluster cluster : concernCountMap.keySet()) {
			final int concernCount = concernCountMap.get(cluster);
			if (concernCount > concernCountMean + concernCountStdDev) {
				logger.debug("\t" + cluster.getName() + " has brick concern overload.");

				final Smell bco = new BcoSmell();
				bco.clusters.add(cluster);
				detectedSmells.add(bco);

				updateSmellMap(clusterSmellMap, cluster.getName(), "bco");
			}
		}
		logger.traceExit();
		return stdDev;
	}

	private static void buildConcernClustersFromConfigTopicsFile(final Set<ConcernCluster> clusters) {
		logger.entry(clusters);
		if (tmeMethod == TopicModelExtractionMethod.VAR_MALLET_FILE) {
			docTopics = new DocTopics(Config.getMalletDocTopicsFile().getPath());
			for (final ConcernCluster cluster : clusters) {
				logger.debug("Building doctopics for " + cluster.getName());
				for (final String entity : cluster.getEntities()) {
					if (cluster.getDocTopicItem() == null) {
						DocTopicItem newDocTopicItem = null;
						if (Config.getSelectedLanguage() == Config.Language.java) {
							newDocTopicItem = setDocTopicItemForJavaFromMalletFile(entity);
						} else if (Config.getSelectedLanguage() == Config.Language.c) {
							newDocTopicItem = docTopics.getDocTopicItemForC(entity);
						} else {
							newDocTopicItem = setDocTopicItemForJavaFromMalletFile(entity);
						}
						cluster.setDocTopicItem(newDocTopicItem);
					} else {
						DocTopicItem entityDocTopicItem = null;
						DocTopicItem mergedDocTopicItem = null;
						if (Config.getSelectedLanguage() == Config.Language.java) {
							entityDocTopicItem = setDocTopicItemForJavaFromMalletFile(entity);
						} else if (Config.getSelectedLanguage() == Config.Language.c) {
							entityDocTopicItem = docTopics.getDocTopicItemForC(entity);
						} else {
							entityDocTopicItem = setDocTopicItemForJavaFromMalletFile(entity);
						}

						mergedDocTopicItem = TopicUtil.mergeDocTopicItems(cluster.getDocTopicItem(),
								entityDocTopicItem);
						cluster.setDocTopicItem(mergedDocTopicItem);
					}
				}
			}
		} else if (tmeMethod == TopicModelExtractionMethod.MALLET_API) {
			for (final ConcernCluster cluster : clusters) {
				logger.debug("Building doctopics for " + cluster.getName());
				for (final String entity : cluster.getEntities()) {
					if (cluster.getDocTopicItem() == null) {
						DocTopicItem newDocTopicItem = null;
						if (Config.getSelectedLanguage() == Config.Language.java) {
							newDocTopicItem = setDocTopicItemForJavaFromMalletApi(entity);
						}
						cluster.setDocTopicItem(newDocTopicItem);
					} else {
						DocTopicItem entityDocTopicItem = null;
						DocTopicItem mergedDocTopicItem = null;
						if (Config.getSelectedLanguage() == Config.Language.java) {
							entityDocTopicItem = setDocTopicItemForJavaFromMalletApi(entity);
						}

						mergedDocTopicItem = TopicUtil.mergeDocTopicItems(cluster.getDocTopicItem(),
								entityDocTopicItem);
						cluster.setDocTopicItem(mergedDocTopicItem);
					}
				}
			}
		}
		logger.traceExit();
	}

	private static void addDetectedBuoSmell(final Set<Smell> detectedSmells, final Set<ConcernCluster> clusters,
			final String vertex) {
		logger.entry(detectedSmells, clusters, vertex);
		final Smell buo = new BuoSmell();
		buo.clusters.add(getMatchingCluster(vertex, clusters));
		detectedSmells.add(buo);
		logger.traceExit();
	}

	private static ConcernCluster getMatchingCluster(final String clusterName, final Set<ConcernCluster> clusters) {
		logger.entry(clusterName, clusters);
		for (final ConcernCluster cluster : clusters) {
			if (cluster.getName().equals(clusterName)) {
				logger.traceExit();
				return cluster;
			}
		}
		logger.traceExit();
		return null;
	}

	private static DocTopicItem setDocTopicItemForJavaFromMalletFile(final String entity) {
		logger.entry(entity);
		DocTopicItem newDocTopicItem;
		final String docTopicName = TopicUtil.convertJavaClassWithPackageNameToDocTopicName(entity);
		newDocTopicItem = docTopics.getDocTopicItemForJava(docTopicName);
		logger.traceExit();
		return newDocTopicItem;
	}

	private static DocTopicItem setDocTopicItemForJavaFromMalletApi(final String entity) {
		logger.entry(entity);
		DocTopicItem newDocTopicItem;
		newDocTopicItem = docTopics.getDocTopicItemForJava(entity);
		logger.traceExit();
		return newDocTopicItem;
	}

	// private static ConcernCluster findCluster(Set<ConcernCluster> clusters,
	// String clusterName) {
	// for (ConcernCluster cluster : clusters) {
	// if (cluster.getName().equals(clusterName)) {
	// return cluster;
	// }
	// }
	// return null;
	// }
	// scattered parasitic functionality
	private static void detectSpfNew(final Set<ConcernCluster> clusters,
			final Map<String, Set<String>> clusterSmellsMap, final Set<Smell> detectedSmells) {
		logger.entry(clusters, clusterSmellsMap);
		System.out.println("Finding scattered parasitic functionality instances...");
		final double scatteredConcernThreshold = .20;
		final double parasiticConcernThreshold = scatteredConcernThreshold;

		final Map<Integer, Integer> topicNumCountMap = new HashMap<Integer, Integer>();
		final Map<Integer, Set<ConcernCluster>> scatteredTopicToClustersMap = new HashMap<Integer, Set<ConcernCluster>>();

		for (final ConcernCluster cluster : clusters) {
			if (cluster.getDocTopicItem() != null) {
				final DocTopicItem dti = cluster.getDocTopicItem();
				for (final TopicItem ti : dti.topics) {
					if (ti.proportion >= scatteredConcernThreshold) {
						// count the number of times the topic appears
						if (topicNumCountMap.containsKey(ti.topicNum)) {
							int topicNumCount = topicNumCountMap.get(ti.topicNum);
							topicNumCount++;
							topicNumCountMap.put(ti.topicNum, topicNumCount);
						} else {
							topicNumCountMap.put(ti.topicNum, 1);
						}

						// determine which clusters have each topic
						if (scatteredTopicToClustersMap.containsKey(ti.topicNum)) {
							final Set<ConcernCluster> clustersWithTopic = scatteredTopicToClustersMap.get(ti.topicNum);
							clustersWithTopic.add(cluster);

						} else {
							final Set<ConcernCluster> clustersWithTopic = new HashSet<ConcernCluster>();
							clustersWithTopic.add(cluster);
							scatteredTopicToClustersMap.put(ti.topicNum, clustersWithTopic);

						}
					}
				}
			}
		}

		final double[] topicCounts = new double[topicNumCountMap.keySet().size()];
		double topicCountMean = 0;
		double topicCountStdDev = 0;

		int topicNumCounter = 0;
		for (final int topicNum : topicNumCountMap.values()) {
			topicCounts[topicNumCounter] = topicNum;
			topicNumCounter++;
		}
		topicCountMean = StatUtils.mean(topicCounts);
		final StandardDeviation stdDev = new StandardDeviation();
		topicCountStdDev = stdDev.evaluate(topicCounts);

		logger.debug("topic count mean: " + topicCountMean);
		logger.debug("topic count standard deviation: " + topicCountStdDev);

		logger.debug("topic num : count");
		for (final Map.Entry<Integer, Integer> entry : topicNumCountMap.entrySet()) {
			logger.debug(entry.getKey() + " : " + entry.getValue());
		}
		logger.debug("topic num : clusters with topic");
		for (final Map.Entry<Integer, Set<ConcernCluster>> entry : scatteredTopicToClustersMap.entrySet()) {
			logger.debug(entry.getKey() + " : " + entry.getValue());
		}

		for (final int topicNum : topicNumCountMap.keySet()) {
			final int topicCount = topicNumCountMap.get(topicNum);
			if (topicCount > topicCountMean + topicCountStdDev) {
				final Set<ConcernCluster> clustersWithScatteredTopics = scatteredTopicToClustersMap.get(topicNum);

				final Set<ConcernCluster> affectedClusters = new HashSet<ConcernCluster>();
				for (final ConcernCluster cluster : clustersWithScatteredTopics) {
					if (cluster.getDocTopicItem() != null) {
						final DocTopicItem dti = cluster.getDocTopicItem();
						for (final TopicItem ti : dti.topics) {
							if (ti.topicNum != topicNum) {
								if (ti.proportion >= parasiticConcernThreshold) {
									logger.debug(cluster.getName() + " has spf with scattered concern " + topicNum);

									if (clusterSmellsMap.containsKey(cluster.getName())) {
										final Set<String> smells = clusterSmellsMap.get(cluster.getName());
										smells.add("spf");
									} else {
										final Set<String> smells = new HashSet<String>();
										smells.add("spf");
										clusterSmellsMap.put(cluster.getName(), smells);
									}

									affectedClusters.add(cluster);
								}
							}
						}
					}
				}

				final Smell spf = new SpfSmell(topicNum);
				spf.clusters = new HashSet<ConcernCluster>(affectedClusters);
				detectedSmells.add(spf);

			}
		}
		logger.traceExit();
	}

	private static void updateSmellMap(final Map<String, Set<String>> clusterSmellMap, final String clusterName,
			final String smellAbrv) {
		logger.entry(clusterSmellMap, clusterName, smellAbrv);
		Set<String> smellList = null;
		if (clusterSmellMap.containsKey(clusterName)) {
			smellList = clusterSmellMap.get(clusterName);
			smellList.add(smellAbrv);
			// clusterSmellMap.put(clusterName, smellList);
		} else {
			smellList = new HashSet<String>();
			smellList.add(smellAbrv);
			clusterSmellMap.put(clusterName, smellList);
		}
		logger.traceExit();
	}
}
