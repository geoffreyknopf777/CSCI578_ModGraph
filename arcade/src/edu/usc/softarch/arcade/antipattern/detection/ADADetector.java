package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.StrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import edu.usc.softarch.arcade.callgraph.MyCallGraph;
import edu.usc.softarch.arcade.callgraph.MyClass;
import edu.usc.softarch.arcade.callgraph.MyMethod;
import edu.usc.softarch.arcade.classgraphs.StringEdge;
import edu.usc.softarch.arcade.clustering.Cluster;
import edu.usc.softarch.arcade.clustering.Feature;
import edu.usc.softarch.arcade.clustering.StringGraph;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.ConfigUtil;
import edu.usc.softarch.arcade.smellarchgraph.SmellArchGraph;
import edu.usc.softarch.arcade.topics.DocTopics;
import edu.usc.softarch.arcade.topics.StringPreProcessor;
import edu.usc.softarch.arcade.topics.TopicItem;
import edu.usc.softarch.arcade.topics.TopicKey;
import edu.usc.softarch.arcade.topics.TopicKeySet;
import edu.usc.softarch.arcade.topics.TopicUtil;
import edu.usc.softarch.arcade.topics.WordTopicCounts;
import edu.usc.softarch.arcade.topics.WordTopicItem;

/**
 * @author joshua
 *
 */

public class ADADetector {

	public static void runSmellDetectionAlgorithms(
			final ArrayList<Cluster> splitClusters) throws IOException,
			ClassNotFoundException, FileNotFoundException,
			ParserConfigurationException, SAXException, TransformerException {
		System.out.println("In "
				+ Thread.currentThread().getStackTrace()[1].getClassName()
				+ "."
				+ Thread.currentThread().getStackTrace()[1].getMethodName()
				+ ",");
		ClusterUtil.generateLeafClusters(splitClusters);
		// ClusterUtil.printItemsInClusters(splitClusters);

		generateTopicsForSplitClusters(splitClusters);
		printTopicsForSplitClusters(splitClusters);

		final HashMap<String, MyClass> classesWithUsedMethods = extracted();
		final HashMap<String, MyMethod> unusedMethods = (HashMap<String, MyMethod>) deserializeHashMap(Config
				.getUnusedMethodsFilename());

		System.out.println("Printing classes with used methods...");
		printClassesWithUsedMethods(classesWithUsedMethods);
		System.out.println("Printing unused methods...");
		printUnusedMethods(unusedMethods);

		determineInterfacesForClusters(splitClusters, classesWithUsedMethods);
		printInterfacesOfClusters(splitClusters);

		final StringGraph clusterGraph = ClusterUtil
				.generateClusterGraph(splitClusters);
		final SmellArchGraph smellArchGraph = ClusterUtil
				.generateSmellArchGraph(splitClusters);

		System.out.println("Resulting ClusterGraph...");
		System.out.println(clusterGraph);
		System.out.println();
		System.out.println("Resulting SmellArchGraph...");
		System.out.println(smellArchGraph);

		writeOutGraphsAndSmellArchToFiles(splitClusters, clusterGraph,
				smellArchGraph);

		final HashSet<TopicKey> topicKeys = TopicUtil
				.getTopicKeyListForCurrProj().set;
		ClusterUtil
		.readInSmellArchFromXML(Config.getSpecifiedSmallArchFromXML(),
				splitClusters, topicKeys);

		ClusterUtil.classifyClustersBasedOnTopicTypes(splitClusters);

		ClusterUtil.writeOutSpecifiedSmellArchToXML(splitClusters, topicKeys);

		for (final Cluster cluster1 : splitClusters) {
			for (final Cluster cluster2 : splitClusters) {
				System.out.println("Computing JS divergence for " + cluster1
						+ " and " + cluster2);
				if (!(cluster1.docTopicItem == null || cluster2.docTopicItem == null)) {
					TopicUtil.jsDivergence(cluster1.docTopicItem,
							cluster2.docTopicItem);
				}
			}
		}

		final MyCallGraph myCallGraph = deserializeMyCallGraph();

		writeMethodInfoToXML(splitClusters, myCallGraph);

		int ambiguousInterfaceCount = 0;
		ambiguousInterfaceCount = determineAmbiguousInterface(splitClusters,
				myCallGraph, ambiguousInterfaceCount);

		final TopicKeySet topicKeySet = TopicUtil.getTopicKeyListForCurrProj();
		final WordTopicCounts wordTopicCounts = TopicUtil
				.getWordTopicCountsForCurrProj();
		// HashSet<String> stopWordsSet = TopicUtil.getStopWordSet();

		System.out
				.println("Computing word-topic probabilities for all words and topics...");
		computeAndShowWordTopicProbabilitiesForAllWordsAndTopics(topicKeySet,
				wordTopicCounts);

		System.out
		.println("Computing word-topic probabilities and specificity type for methods...");
		computeWordTopicProbabilitiesAndSpecifityTypesForMethods(splitClusters,
				topicKeySet, wordTopicCounts);

		int connectorInterfaceImplCount = 0;
		connectorInterfaceImplCount = findConnectorInterfaceImplementationSmells(
				splitClusters, connectorInterfaceImplCount);

		int unacceptablyHighConnectorConcernCount = 0;
		System.out.println("Finding unacceptably high connector concern...");
		unacceptablyHighConnectorConcernCount = findUnacceptablyHighConnectorConcernSmells(
				splitClusters, unacceptablyHighConnectorConcernCount);

		int scatteredParasiticFunctionalityCount = 0;
		System.out
		.println("Finding instances of scattered parasitic functionality...");
		scatteredParasiticFunctionalityCount = findScatteredParasiticFunctionalitySmells(
				splitClusters, scatteredParasiticFunctionalityCount);

		int procCallBasedExtraneousConnectorCount = 0;
		System.out
		.println("Finding procedure call-based extraneous connector smells...");
		procCallBasedExtraneousConnectorCount = findProcCalBasedExtraneousConnectors(
				splitClusters, clusterGraph,
				procCallBasedExtraneousConnectorCount);

		int brickConcernOverloadCount = 0;
		System.out.println("Finding Brick Concern Overload smells...");
		brickConcernOverloadCount = findBrickConcernOverloadSmells(
				splitClusters, brickConcernOverloadCount);

		int unusedInterfaceCount = 0;
		System.out.println("Finding Unused Interface smells...");
		unusedInterfaceCount = findUnusedInterfaceSmells(unusedMethods,
				unusedInterfaceCount);

		int unstableBrickDependencyCount = 0;
		System.out.println("Finding Unstable Brick Dependencies smells...");
		final HashMap<String, Double> brickStabilityMap = new HashMap<String, Double>();
		final HashMap<String, Integer> brickFanInMap = new HashMap<String, Integer>();
		final HashMap<String, Integer> brickFanOutMap = new HashMap<String, Integer>();
		for (final Cluster firstCluster : splitClusters) {
			System.out.println("Current cluster: " + firstCluster);
			int fanOut = 0;
			int fanIn = 0;
			for (final StringEdge stringEdge : clusterGraph.edges) {
				if (stringEdge.srcStr.equals(firstCluster.name)) {
					fanOut++;
					brickFanOutMap.put(firstCluster.name, fanOut);
					System.out.println("\tOutgoing edge" + stringEdge);
				}
			}
			for (final StringEdge stringEdge : clusterGraph.edges) {
				if (stringEdge.tgtStr.equals(firstCluster.name)) {
					fanIn++;
					brickFanInMap.put(firstCluster.name, fanIn);
					System.out.println("\tIncoming edge" + stringEdge);
				}
			}
			final double stability = (double) fanOut
					/ (double) (fanOut + fanIn);
			System.out.println("\tstability: " + stability);
			brickStabilityMap.put(firstCluster.name, stability);
		}

		for (final StringEdge stringEdge : clusterGraph.edges) {
			final double srcStability = brickStabilityMap
					.get(stringEdge.srcStr);
			final double tgtStability = brickStabilityMap
					.get(stringEdge.tgtStr);
			if (srcStability < tgtStability) {
				System.out.println("\tUnstable brick dependency found "
						+ stringEdge);
				unstableBrickDependencyCount++;
			}
		}

		final SimpleDirectedGraph<String, DefaultEdge> directedGraph = new SimpleDirectedGraph<String, DefaultEdge>(
				DefaultEdge.class);

		/*
		 * String v1 = "v1"; String v2 = "v2"; String v3 = "v3"; String v4 =
		 * "v4";
		 *
		 * // add the vertices directedGraph.addVertex(v1);
		 * directedGraph.addVertex(v2); directedGraph.addVertex(v3);
		 * directedGraph.addVertex(v4);
		 *
		 * // add edges to create a circuit directedGraph.addEdge(v1, v2);
		 * directedGraph.addEdge(v2, v3); directedGraph.addEdge(v3, v4);
		 * directedGraph.addEdge(v4, v1);
		 */

		for (final Cluster splitCluster : splitClusters) {
			directedGraph.addVertex(splitCluster.name);
		}

		for (final StringEdge stringEdge : clusterGraph.edges) {
			if (!stringEdge.srcStr.equals(stringEdge.tgtStr)) {
				directedGraph.addEdge(stringEdge.srcStr, stringEdge.tgtStr);
			}
		}

		/*
		 * System.out.println("Printing out directed graph...");
		 * System.out.println(directedGraph);
		 */

		System.out.println("Finding cycles...");
		final CycleDetector<?, DefaultEdge> cycleDetector = new CycleDetector(
				directedGraph);
		final Set<String> cycleSet = (Set<String>) cycleDetector.findCycles();
		System.out.println("Printing the cycle set...");
		System.out.println(cycleSet);

		/*
		 * System.out.println(); System.out.println(
		 * "Printing the cycles of each vertex in the cycle set....");
		 * HashSet<Set<>> for (String clusterName : cycleSet) {
		 * System.out.println
		 * (cycleDetector.findCyclesContainingVertex(clusterName));
		 * System.out.println(); }
		 */

		System.out
		.println("Printing the strongly connected sets of the graph....");
		final StrongConnectivityInspector<?, DefaultEdge> inspector = extracted(directedGraph);
		final List<?> connectedSets = inspector.stronglyConnectedSets();

		int relevantConnectedSetCount = 0;
		for (final Object connectedSet : connectedSets) {
			System.out.println();
			System.out.println(connectedSet);
			final ArrayList<Feature> arrayList = extracted(connectedSet);
			if (arrayList.size() > 1) {
				System.out
				.println("Counting this strongly connected component set as relevant");
				relevantConnectedSetCount++;
			}

		}

		System.out.println("Finding instances of brick use overload...");

		int sumEdges = 0;
		for (final Cluster splitCluster : splitClusters) {
			final int currFanIn = (brickFanInMap.get(splitCluster.name) == null ? 0
					: brickFanInMap.get(splitCluster.name));
			final int currFanOut = (brickFanOutMap.get(splitCluster.name) == null ? 0
					: brickFanOutMap.get(splitCluster.name));
			final int totalEdges = currFanIn + currFanOut;
			sumEdges += totalEdges;
		}
		final double meanEdges = (double) sumEdges
				/ (double) splitClusters.size();
		System.out.println("mean edges: " + meanEdges);

		int sumForVar = 0;
		for (final Cluster splitCluster : splitClusters) {
			final int currFanIn = (brickFanInMap.get(splitCluster.name) == null ? 0
					: brickFanInMap.get(splitCluster.name));
			final int currFanOut = (brickFanOutMap.get(splitCluster.name) == null ? 0
					: brickFanOutMap.get(splitCluster.name));
			final int totalEdges = currFanIn + currFanOut;
			sumForVar += Math.pow(totalEdges - meanEdges, 2);
		}
		final double variance = (double) sumForVar
				/ (double) splitClusters.size();

		final double stdDev = Math.sqrt(variance);

		System.out.println("variance of edges: " + variance);
		System.out.println("standard deviation: " + stdDev);

		int brickUseOverloadCount = 0;
		for (final Cluster splitCluster : splitClusters) {
			final int currFanIn = (brickFanInMap.get(splitCluster.name) == null ? 0
					: brickFanInMap.get(splitCluster.name));
			final int currFanOut = (brickFanOutMap.get(splitCluster.name) == null ? 0
					: brickFanOutMap.get(splitCluster.name));
			final int totalEdges = currFanIn + currFanOut;

			if (totalEdges > meanEdges + stdDev) {
				System.out
				.println("Found instance of brick use overload for cluster "
						+ splitCluster);
				brickUseOverloadCount++;
			}
		}

		System.out.println("Ambiguous Interface Count: "
				+ ambiguousInterfaceCount);
		System.out.println("Unacceptably High Connector concern count: "
				+ unacceptablyHighConnectorConcernCount);
		System.out.println("Connector Interface Implementation count: "
				+ connectorInterfaceImplCount);
		System.out.println("Scattered Parasitic Functionality count: "
				+ scatteredParasiticFunctionalityCount);
		System.out.println("Procedure Call-Based Extraneous Connector count: "
				+ procCallBasedExtraneousConnectorCount);
		System.out.println("Brick Concern Overload count: "
				+ brickConcernOverloadCount);
		System.out.println("Unused Interface count: " + unusedInterfaceCount);
		System.out.println("Unstable Brick Dependency count: "
				+ unstableBrickDependencyCount);
		System.out.println("Brick Dependency Cycle count: "
				+ relevantConnectedSetCount);
		System.out
		.println("Brick Use Overload count: " + brickUseOverloadCount);
	}

	private static HashMap<String, MyClass> extracted() throws IOException,
	ClassNotFoundException {
		return (HashMap<String, MyClass>) deserializeHashMap(Config
				.getClassesWithUsedMethodsFilename());
	}

	private static StrongConnectivityInspector extracted(
			final SimpleDirectedGraph<String, DefaultEdge> directedGraph) {
		return new StrongConnectivityInspector(directedGraph);
	}

	private static ArrayList<Feature> extracted(final Object connectedSet) {
		return (ArrayList<Feature>) connectedSet;
	}

	private static MyCallGraph deserializeMyCallGraph() throws IOException,
			ClassNotFoundException {
		final String filename = Config.getMyCallGraphFilename();
		// Read from disk using FileInputStream
		final FileInputStream f_in = new FileInputStream(filename);

		// Read object using ObjectInputStream
		final ObjectInputStream obj_in = new ObjectInputStream(f_in);

		// Read an object
		final Object obj = obj_in.readObject();

		MyCallGraph locClg = null;
		if (obj instanceof MyCallGraph) {
			// Cast object to a Vector
			locClg = (MyCallGraph) obj;
		}
		obj_in.close();
		return locClg;
	}

	private static void writeMethodInfoToXML(
			final ArrayList<Cluster> splitClusters,
			final MyCallGraph myCallGraph) throws ParserConfigurationException,
			TransformerException {
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory
				.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// classgraph elements
		final Document doc = docBuilder.newDocument();
		final Element rootElement = doc.createElement("SmellArchGraph");
		doc.appendChild(rootElement);

		for (final Cluster splitCluster : splitClusters) {
			System.out.println("Current cluster: " + splitCluster);
			final Element clusterElem = doc.createElement("cluster");
			rootElement.appendChild(clusterElem);
			clusterElem.setAttribute("name", splitCluster.toString());

			for (final MyClass myClass : splitCluster.classes) {
				System.out.println("\tCurrent class: " + myClass);
				final Element classElem = doc.createElement("class");
				clusterElem.appendChild(classElem);
				classElem.setAttribute("name", myClass.className);

				for (final MyMethod myMethod : myClass.getMethods()) {
					System.out.println("\t\tCurrent method: " + myMethod);
					final Element methodElem = doc.createElement("method");
					classElem.appendChild(methodElem);
					methodElem.setAttribute("name", myMethod.toString());
					for (final String param : myMethod.getParams()) {
						System.out.println("\t\t\tCurrent param: " + param);
						final Element paramElem = doc.createElement("param");
						methodElem.appendChild(paramElem);
						paramElem.setAttribute("name", param);
					}

					final Element retValElem = doc.createElement("retval");
					methodElem.appendChild(retValElem);
					retValElem.setAttribute("name", myMethod.retVal);
				}
			}
		}

		// write the content into xml file
		final TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(new File(
				Config.getMethodInfoFilename()));
		transformer.transform(source, result);

		System.out.println("In "
				+ Thread.currentThread().getStackTrace()[1].getClassName()
				+ ". "
				+ Thread.currentThread().getStackTrace()[1].getMethodName()
				+ ", Wrote " + Config.getMethodInfoFilename());
	}

	private static int determineAmbiguousInterface(
			final ArrayList<Cluster> splitClusters,
			final MyCallGraph myCallGraph, int ambiguousInterfaceCount) {
		for (final Cluster splitCluster : splitClusters) {
			System.out.println("Current cluster: " + splitCluster);
			for (final MyClass myClass : splitCluster.classes) {
				System.out.println("\tCurrent class: " + myClass);
				for (final MyMethod myMethod : myClass.getMethods()) {
					System.out.println("\t\tCurrent method: " + myMethod);
					if (myMethod.getParams().size() == 1 && myMethod.isPublic) {
						System.out
								.println("\t\t\tCandidate Ambiguous Interface");
						final HashSet<MyMethod> targetEdges = myCallGraph
								.getTargetEdges(myMethod);
						System.out.println("\t\t\ttarget edges size: "
								+ targetEdges.size());
						int relevantTgtMethodCount = 0;
						for (final MyMethod tgtMethod : targetEdges) {
							if (splitCluster.classes
									.contains(tgtMethod.declaringClass)) {
								System.out
								.println("\t\t\t\tFound relevant taret method: "
										+ tgtMethod);
								relevantTgtMethodCount++;

							}
						}

						if (relevantTgtMethodCount > 1) {
							System.out
							.println("\t\t\tPositive Ambiguous Interface");
							ambiguousInterfaceCount++;
						}
					}
				}
			}
		}
		return ambiguousInterfaceCount;
	}

	private static void computeWordTopicProbabilitiesAndSpecifityTypesForMethods(
			final ArrayList<Cluster> splitClusters,
			final TopicKeySet topicKeySet, final WordTopicCounts wordTopicCounts)
					throws IOException, ParserConfigurationException, SAXException {
		for (final Cluster splitCluster : splitClusters) {
			System.out.println("Current cluster: " + splitCluster);

			final String strippedLeafSplitClusterName = ConfigUtil
					.stripParensEnclosedClassNameWithPackageName(splitCluster);

			if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (splitCluster.type.equals("indep")) {
				continue;
			}
			for (final MyClass myClass : splitCluster.classes) {
				System.out.println("\tCurrent class: " + myClass);
				for (final MyMethod myMethod : myClass.getMethods()) {
					System.out.println("\t\tCurrent method: " + myMethod);
					final String processedMethodName = StringPreProcessor
							.camelCaseSeparateAndStem(myMethod.name);
					System.out.println("\t\tProcssed method name: "
							+ processedMethodName);

					final HashMap<Integer, String> positionWordMap = new HashMap<Integer, String>();
					final HashMap<Integer, Double> queryGivenTopicProbabilitiesMap = new HashMap<Integer, Double>();

					computeQueryGivenTopicProbabilitiesMap(topicKeySet,
							wordTopicCounts, processedMethodName,
							positionWordMap, queryGivenTopicProbabilitiesMap);

					System.out.println();
					determineTopicForMethod(topicKeySet, myMethod,
							positionWordMap, queryGivenTopicProbabilitiesMap);

				}
			}
		}
	}

	private static void determineTopicForMethod(final TopicKeySet topicKeySet,
			final MyMethod myMethod,
			final HashMap<Integer, String> positionWordMap,
			final HashMap<Integer, Double> queryGivenTopicProbabilitiesMap)
			throws IOException, ParserConfigurationException, SAXException {
		int mostProbableTopic = -1;
		double highestProbSoFar = 0;
		for (final TopicKey topicKey : topicKeySet.set) {
			// String wordName = positionWordMap
			// .get(topicKey.topicNum);
			final Double currProb = queryGivenTopicProbabilitiesMap
					.get(topicKey.topicNum);
			System.out.println("\t\t\ttopic: " + topicKey.topicNum);

			System.out.println("\t\t\tcurrProb of query given topic: "
					+ currProb);

			if (currProb >= highestProbSoFar) {
				highestProbSoFar = currProb;
				mostProbableTopic = topicKey.topicNum;
			}

		}

		final TopicKeySet typedTopicKeySet = TopicUtil.getTypedTopicKeyList();

		System.out.println("\t\tTopic determined for " + myMethod.name);
		System.out.println("\t\t topic: " + mostProbableTopic);
		final String typeForMostProbableTopic = typedTopicKeySet
				.getTopicKeyByID(mostProbableTopic).type;
		System.out.println("\t\t topic's type: " + typeForMostProbableTopic);
		System.out.println("\t\t prob for method " + myMethod
				+ " given topic: " + highestProbSoFar);
		myMethod.type = typeForMostProbableTopic;
		System.out.println();
	}

	private static void computeQueryGivenTopicProbabilitiesMap(
			final TopicKeySet topicKeySet,
			final WordTopicCounts wordTopicCounts,
			final String processedMethodName,
			final HashMap<Integer, String> positionWordMap,
			final HashMap<Integer, Double> queryGivenTopicProbabilitiesMap) {
		for (final TopicKey topicKey : topicKeySet.set) {
			final String[] wordsInMethodName = processedMethodName.split(" ");
			// int topicCount = 0;

			double probabilitySum = 0;
			for (final String word : wordsInMethodName) {
				if (!wordTopicCounts.getWordTopicItems().containsKey(word)) {
					continue;
				}
				final WordTopicItem wtItem = wordTopicCounts
						.getWordTopicItems().get(word);

				positionWordMap.put(topicKey.topicNum, wtItem.name);
				/*
				 * if (stopWordsSet.contains(wtItem.name.trim())) { continue; }
				 */

				final double probWordGivenTopic = wtItem
						.probabilityWordGivenTopic(topicKey.topicNum);

				System.out.println("\t\t\t\tProbability " + wtItem.name
						+ " given " + topicKey.topicNum + ": "
						+ probWordGivenTopic);

				probabilitySum += probWordGivenTopic;

			}

			System.out.println("\t\t\tProbability sum for topic "
					+ topicKey.topicNum + ": " + probabilitySum);
			final double probabilityAverage = probabilitySum
					/ wordsInMethodName.length;

			System.out.println("\t\t\tProbability avg for topic "
					+ topicKey.topicNum + ": " + probabilityAverage);
			queryGivenTopicProbabilitiesMap.put(topicKey.topicNum, new Double(
					probabilityAverage));
		}
	}

	private static void printUnusedMethods(
			final HashMap<String, MyMethod> unusedMethods) {
		for (final MyMethod m : unusedMethods.values()) {
			System.out.println("\t" + m.toString());
		}

	}

	private static void printInterfacesOfClusters(
			final ArrayList<Cluster> splitClusters) {
		for (final Cluster cluster : splitClusters) {
			System.out.println("Printing interfaces of cluster " + cluster);
			for (final MyClass myClass : cluster.getClasses()) {
				System.out.println("\t comprising class: " + myClass);
				System.out.println(myClass.methodsToString(2));
			}
		}

	}

	private static void determineInterfacesForClusters(
			final ArrayList<Cluster> splitClusters,
			final HashMap<String, MyClass> classes) {
		for (final Cluster cluster : splitClusters) {
			System.out.println("Determining interfaces for cluster " + cluster);
			cluster.instantiateClasses();
			for (final Cluster leaf : cluster.leafClusters) {
				final String strippedLeafClusterName = leaf.toString()
						.substring(1, leaf.toString().length() - 1);
				System.out.println("\t" + strippedLeafClusterName);
				if (classes.containsKey(strippedLeafClusterName)) {
					final MyClass myClass = classes
							.get(strippedLeafClusterName);
					cluster.add(myClass);
				}

			}
		}

	}

	private static void printClassesWithUsedMethods(
			final HashMap<String, MyClass> classesWithMethodsInMyCallGraph) {
		for (final MyClass c : classesWithMethodsInMyCallGraph.values()) {
			System.out.println("Showing linked methods in " + c + "...");
			System.out.println(c.methodsToString(1));
		}
	}

	private static void printTopicsForSplitClusters(
			final ArrayList<Cluster> splitClusters) {
		System.out
				.println("Printing document-topic distribution for each split cluster...");
		for (final Cluster splitCluster : splitClusters) {
			System.out.println("\t" + splitCluster);
			System.out.println("\t\t" + splitCluster.docTopicItem);
		}

	}

	private static void generateTopicsForSplitClusters(
			final ArrayList<Cluster> splitClusters) {
		DocTopics docTopics = null;
		docTopics = TopicUtil.getDocTopicsFromFile();

		for (final Cluster splitCluster : splitClusters) {
			System.out.println("Current split cluster: " + splitCluster);
			final ArrayList<Cluster> currLeafClusters = splitCluster.leafClusters;
			final int leafCounter = 0;

			final String strippedLeafSplitClusterName = ConfigUtil
					.stripParensEnclosedClassNameWithPackageName(splitCluster);

			if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			System.out.println("Setting doc-topic for each item...");
			setDocTopicForEachLeafCluster(docTopics, currLeafClusters,
					leafCounter);

			// Cluster refLeaf =
			// getClusterForReferenceOfTopics(currLeafClusters);

			// verifyDocTopicOrder(splitCluster, currLeafClusters);

			// ArrayList<TopicItem> topics =
			// createZeroProportionTopicsFromReference(refLeaf);

			// int nonAnonInnerClassLeafCounter =
			// calculateNewTopicProportionsForSplitClusters(
			// splitCluster, currLeafClusters, topics);

		}
	}

	private static void setDocTopicForEachLeafCluster(
			final DocTopics docTopics,
			final ArrayList<Cluster> currLeafClusters, final int leafCounter) {
		for (final Cluster leaf : currLeafClusters) {
			System.out.println("\t" + leafCounter + ": " + leaf);
			TopicUtil.setDocTopicForCluster(docTopics, leaf);
		}
	}

	private static int findUnusedInterfaceSmells(
			final HashMap<String, MyMethod> unusedMethods,
			int unusedInterfaceCount) {
		System.out.println("Number of unused methods: "
				+ unusedMethods.values().size());
		for (final MyMethod myMethod : unusedMethods.values()) {
			if (myMethod.isPublic) {
				System.out.println("\tUnused public method found: " + myMethod);
				unusedInterfaceCount++;
			}
		}
		return unusedInterfaceCount;
	}

	private static int findBrickConcernOverloadSmells(
			final ArrayList<Cluster> splitClusters,
			int brickConcernOverloadCount) {
		for (final Cluster firstCluster : splitClusters) {
			System.out.println("Current cluster: " + firstCluster);

			boolean invalidInnerClassCluster = false;
			invalidInnerClassCluster = checkIfClusterIsAnInvalidInnerClass(
					firstCluster, invalidInnerClassCluster);
			if (invalidInnerClassCluster) {
				continue;
			}

			final double proportionThreshold = 0.20;
			final int concernNumberThreshold = 1;

			final ArrayList<TopicItem> relevantTopics = new ArrayList<TopicItem>();
			for (final TopicItem currTopicItem : firstCluster.docTopicItem.topics) {
				if (currTopicItem.proportion >= proportionThreshold) {
					relevantTopics.add(currTopicItem);
				}
			}

			if (relevantTopics.size() > concernNumberThreshold) {
				System.out.println("\tBrick concern overload FOUND for "
						+ firstCluster);
				System.out.println("\tRelevant topics are:");
				for (final TopicItem relevantTopic : relevantTopics) {
					System.out.println("\t\t" + relevantTopic);
				}
				brickConcernOverloadCount++;
			} else {
				System.out.println("\tBrick concern overload NOT found for"
						+ firstCluster);
				System.out.println("\tRelevant topics are:");
				for (final TopicItem relevantTopic : relevantTopics) {
					System.out.println("\t\t" + relevantTopic);
				}
			}

		}
		return brickConcernOverloadCount;
	}

	private static int findProcCalBasedExtraneousConnectors(
			final ArrayList<Cluster> splitClusters,
			final StringGraph clusterGraph,
			int procCallBasedExtraneousConnectorCount) {
		for (final Cluster firstCluster : splitClusters) {
			System.out.println("Current cluster: " + firstCluster);

			final String strippedLeafSplitClusterName = ConfigUtil
					.stripParensEnclosedClassNameWithPackageName(firstCluster);

			if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}
			if (firstCluster.type.equals("spec")) {
				for (final StringEdge stringEdge : clusterGraph.edges) {
					if (stringEdge.srcStr.equals(firstCluster.name)) {

						final Cluster targetCluster = getClusterByName(
								stringEdge.tgtStr, splitClusters);

						boolean invalidInnerClassCluster = false;
						invalidInnerClassCluster = checkIfClusterIsAnInvalidInnerClass(
								targetCluster, invalidInnerClassCluster);
						if (invalidInnerClassCluster) {
							continue;
						}
						if (targetCluster.type.equals("indep")) {
							final Cluster connCluster = targetCluster;
							final Cluster compCluster = firstCluster;
							final Cluster otherCompCluster = findDifferentClusterThatDependsOnConnector(
									compCluster, connCluster, splitClusters,
									clusterGraph);
							if (otherCompCluster == null) {
								System.out
										.println("Did not find proc-call based extraneous adjacent connector");
							} else {
								System.out
										.println("Elements involved in extraneous adjacent connector...");
								System.out.println("\tcompCluster: "
										+ compCluster);
								System.out.println("\tconnCluster: "
										+ connCluster);
								System.out.println("\totherCompCluster: "
										+ otherCompCluster);
								procCallBasedExtraneousConnectorCount++;
							}
						}
					} else if (stringEdge.tgtStr.equals(firstCluster.name)) {
						final Cluster srcCluster = getClusterByName(
								stringEdge.srcStr, splitClusters);
						boolean invalidInnerClassCluster = false;
						invalidInnerClassCluster = checkIfClusterIsAnInvalidInnerClass(
								srcCluster, invalidInnerClassCluster);
						if (invalidInnerClassCluster) {
							continue;
						}
						if (srcCluster.type.equals("indep")) {
							final Cluster connCluster = srcCluster;
							final Cluster compCluster = firstCluster;
							final Cluster otherCompCluster = findDifferentClusterThatDependsOnConnector(
									compCluster, connCluster, splitClusters,
									clusterGraph);
							if (otherCompCluster == null) {
								System.out
										.println("Did not find proc-call based extraneous adjacent connector");
							} else {
								System.out
										.println("Elements involved in extraneous adjacent connector...");
								System.out.println("\tcompCluster: "
										+ compCluster);
								System.out.println("\tconnCluster: "
										+ connCluster);
								System.out.println("\totherCompCluster: "
										+ otherCompCluster);
								procCallBasedExtraneousConnectorCount++;
							}
						}
					}
				}
			}
		}
		return procCallBasedExtraneousConnectorCount;
	}

	private static boolean checkIfClusterIsAnInvalidInnerClass(
			final Cluster firstCluster, boolean invalidInnerClassCluster) {
		final String strippedLeafSplitClusterName = ConfigUtil
				.stripParensEnclosedClassNameWithPackageName(firstCluster);

		if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
				strippedLeafSplitClusterName)) {
			invalidInnerClassCluster = true;
		}

		if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
				strippedLeafSplitClusterName)) {
			invalidInnerClassCluster = true;
		}
		return invalidInnerClassCluster;
	}

	private static Cluster findDifferentClusterThatDependsOnConnector(
			final Cluster compCluster, final Cluster connCluster,
			final ArrayList<Cluster> splitClusters,
			final StringGraph clusterGraph) {
		for (final Cluster cluster : splitClusters) {
			if (!cluster.name.equals(compCluster.name)) {
				for (final StringEdge stringEdge : clusterGraph.edges) {
					if (stringEdge.srcStr.equals(cluster.name)) {
						final Cluster targetCluster = getClusterByName(
								stringEdge.tgtStr, splitClusters);
						boolean invalidInnerClassCluster = false;
						invalidInnerClassCluster = checkIfClusterIsAnInvalidInnerClass(
								targetCluster, invalidInnerClassCluster);
						if (invalidInnerClassCluster) {
							continue;
						}
						if (targetCluster.type.equals("spec")
								&& !targetCluster.name.equals(compCluster.name)) {
							return targetCluster;
						}
					} else if (stringEdge.tgtStr.equals(cluster.name)) {
						final Cluster srcCluster = getClusterByName(
								stringEdge.srcStr, splitClusters);
						boolean invalidInnerClassCluster = false;
						invalidInnerClassCluster = checkIfClusterIsAnInvalidInnerClass(
								srcCluster, invalidInnerClassCluster);
						if (invalidInnerClassCluster) {
							continue;
						}
						if (srcCluster.type.equals("spec")
								&& !srcCluster.name.equals(compCluster.name)) {
							return srcCluster;
						}
					}
				}
			}
		}
		return null;
	}

	private static Cluster getClusterByName(final String tgtStr,
			final ArrayList<Cluster> splitClusters) {
		for (final Cluster cluster : splitClusters) {
			if (cluster.name.equals(tgtStr)) {
				return cluster;
			}
		}
		return null;

	}

	private static int findScatteredParasiticFunctionalitySmells(
			final ArrayList<Cluster> splitClusters,
			final int scatteredParasiticFunctionalityCount) {
		final double threshold1 = 0.30;
		final double threshold2 = 0.30;

		final HashSet<TopicItem> scatteredTopics = new HashSet<TopicItem>();

		for (final Cluster firstCluster : splitClusters) {
			System.out.println("Current cluster: " + firstCluster);

			final String strippedLeafSplitClusterName = ConfigUtil
					.stripParensEnclosedClassNameWithPackageName(firstCluster);

			if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			for (final TopicItem firstTopicItem : firstCluster.docTopicItem.topics) {
				if (firstTopicItem.proportion > threshold1) {
					for (final Cluster secondCluster : splitClusters) {
						final String strippedSecondClusterName = ConfigUtil
								.stripParensEnclosedClassNameWithPackageName(secondCluster);

						if (Pattern.matches(
								ConfigUtil.anonymousInnerClassRegExpr,
								strippedSecondClusterName)) {
							continue;
						}

						if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
								strippedSecondClusterName)) {
							continue;
						}
						for (final TopicItem secondTopicItem : secondCluster.docTopicItem.topics) {

							if (secondTopicItem.proportion > threshold2
									&& firstTopicItem.topicNum == secondTopicItem.topicNum) {
								for (final TopicItem thirdTopicItem : firstCluster.docTopicItem.topics) {
									if (thirdTopicItem.proportion > threshold1
											&& !thirdTopicItem
											.equals(firstTopicItem)) {

										System.out.println("\t" + firstCluster
												+ " has scattered topic "
												+ firstTopicItem);
										System.out.println("\t" + firstCluster
												+ " has orthogonal topic "
												+ thirdTopicItem);
										System.out.println("\t" + secondCluster
												+ " has scattered topic "
												+ secondTopicItem);
										scatteredTopics.add(firstTopicItem);
									}
								}
							}
						}
					}
				}
			}
		}
		return scatteredTopics.size();
	}

	private static void writeOutGraphsAndSmellArchToFiles(
			final ArrayList<Cluster> splitClusters,
			final StringGraph clusterGraph, final SmellArchGraph smellArchGraph) {
		try {
			clusterGraph.writeDotFile(Config.getClusterGraphDotFilename());
			clusterGraph.writeXMLClusterGraph(Config
					.getClusterGraphXMLFilename());
			smellArchGraph.writeXMLSmellArchGraph(Config
					.getXMLSmellArchGraphFilename());

			ClusterUtil.writeOutSmellArchToXML(splitClusters);

		} catch (final FileNotFoundException e) {
			e.printStackTrace();System.exit(-1);
			System.exit(-1);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();System.exit(-1);
			System.exit(-1);
		} catch (final ParserConfigurationException e) {
			e.printStackTrace();System.exit(-1);
			System.exit(-1);
		} catch (final TransformerException e) {
			e.printStackTrace();System.exit(-1);
			System.exit(-1);
		}
	}

	private static void computeAndShowWordTopicProbabilitiesForAllWordsAndTopics(
			final TopicKeySet topicKeySet, final WordTopicCounts wordTopicCounts) {
		for (final WordTopicItem wtItem : wordTopicCounts.getWordTopicItems()
				.values()) {
			for (final TopicKey topicKey : topicKeySet.set) {
				System.out.println("P(" + wtItem.name + "," + topicKey.topicNum
						+ ") = "
						+ wtItem.probabilityWordGivenTopic(topicKey.topicNum));
			}
		}
	}

	private static int findUnacceptablyHighConnectorConcernSmells(
			final ArrayList<Cluster> splitClusters,
			int unacceptablyHighConnectorConcernCount) {
		for (final Cluster splitCluster : splitClusters) {
			System.out.println("Current cluster: " + splitCluster);

			final String strippedLeafSplitClusterName = ConfigUtil
					.stripParensEnclosedClassNameWithPackageName(splitCluster);

			if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (splitCluster.type.equals("indep")) {
				continue;
			}
			for (final TopicItem topicItem : splitCluster.docTopicItem.topics) {
				System.out.println("\ttopic id : " + topicItem.topicNum);
				System.out.println("\ttopic type : " + topicItem.type);
				System.out.println("\ttopic proportion : "
						+ topicItem.proportion);
				if (topicItem.type.equals("indep")
						&& topicItem.proportion > 0.10) {
					System.out
							.println("\t counting as unacceptably high connector concern: "
									+ splitCluster);
					unacceptablyHighConnectorConcernCount++;
				}
			}
		}
		return unacceptablyHighConnectorConcernCount;
	}

	private static int findConnectorInterfaceImplementationSmells(
			final ArrayList<Cluster> splitClusters,
			int connectorInterfaceImplCount) {
		for (final Cluster splitCluster : splitClusters) {
			System.out.println("Current cluster: " + splitCluster);

			final String strippedLeafSplitClusterName = ConfigUtil
					.stripParensEnclosedClassNameWithPackageName(splitCluster);

			if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
					strippedLeafSplitClusterName)) {
				continue;
			}

			if (splitCluster.type.equals("indep")) {
				continue;
			}
			for (final MyClass myClass : splitCluster.classes) {
				System.out.println("\tCurrent class: " + myClass);
				for (final MyMethod myMethod : myClass.getMethods()) {
					System.out.println("\t\tCurrent method: " + myMethod);
					if (splitCluster.type.equals("spec") && myMethod.isPublic
							&& myMethod.type.equals("indep")) {
						System.out
						.println("\t\t\tFound instance of connector interface implementation...");
						System.out.println("\t\t\t" + myMethod + " of "
								+ splitCluster + " has type " + myMethod.type
								+ " while " + splitCluster + " has "
								+ splitCluster.type);
						connectorInterfaceImplCount++;
					}
				}
			}
		}
		return connectorInterfaceImplCount;
	}

	private static HashMap<?, ?> deserializeHashMap(final String filename)
			throws IOException, ClassNotFoundException {

		// Read from disk using FileInputStream
		final FileInputStream f_in = new FileInputStream(filename);

		// Read object using ObjectInputStream
		final ObjectInputStream obj_in = new ObjectInputStream(f_in);

		// Read an object
		final Object obj = obj_in.readObject();

		HashMap<?, ?> hashMap = null;
		if (obj instanceof HashMap<?, ?>) {
			// Cast object to a Vector
			hashMap = (HashMap<?, ?>) obj;
		}
		obj_in.close();
		return hashMap;
	}

	// private static int calculateNewTopicProportionsForSplitClusters(
	// Cluster splitCluster, ArrayList<Cluster> currLeafClusters,
	// ArrayList<TopicItem> topics) {
	// int leafCounter;
	// leafCounter = 0;
	// System.out.println("Creating new topic items for split cluster: "
	// + splitCluster);
	// int nonAnonInnerClassLeafCounter = 0;
	// for (Cluster leaf : currLeafClusters) {
	// System.out.println("\t" + leafCounter + ": " + leaf);
	// System.out.println("\t" + "doc-topic: " + leaf.docTopicItem);
	// leafCounter++;
	// String strippedClassName = ConfigUtil
	// .stripParensEnclosedClassNameWithPackageName(leaf);
	// // ".*\\$\\d+"
	// if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr,
	// strippedClassName)) {
	// continue;
	// }
	// if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr,
	// strippedClassName)) {
	// continue;
	// }
	// nonAnonInnerClassLeafCounter++;
	// for (int j = 0; j < leaf.docTopicItem.topics.size(); j++) {
	// TopicItem currLeafTopicItem = leaf.docTopicItem.topics.get(j);
	// if (haveMatchingTopicItem(topics, currLeafTopicItem)) {
	// TopicItem matchingTopicItem = TopicUtil
	// .getMatchingTopicItem(topics, currLeafTopicItem);
	// matchingTopicItem.proportion += currLeafTopicItem.proportion;
	// } else {
	// TopicItem newTopicItem = new TopicItem(currLeafTopicItem);
	// newTopicItem.proportion = newTopicItem.proportion;
	// topics.add(newTopicItem);
	// }
	// }
	//
	// }
	// splitCluster.docTopicItem = new DocTopicItem();
	// splitCluster.docTopicItem.topics = new ArrayList<TopicItem>();
	// for (TopicItem topicItem : topics) {
	// splitCluster.docTopicItem.topics.add(new TopicItem(topicItem));
	// }
	//
	// System.out.println("splitCluster " + splitCluster
	// + "'s new topics summed only...");
	// System.out.println(splitCluster.docTopicItem.topics);
	// System.out.println("nonAnonInnerClassLeafCounter: "
	// + nonAnonInnerClassLeafCounter);
	//
	// for (TopicItem topicItem : splitCluster.docTopicItem.topics) {
	// topicItem.proportion /= nonAnonInnerClassLeafCounter;
	// }
	//
	// System.out.println("splitCluster " + splitCluster
	// + "'s new topics averaged...");
	// System.out.println(splitCluster.docTopicItem.topics);
	// return nonAnonInnerClassLeafCounter;
	// }
	//
	// private static ArrayList<TopicItem>
	// createZeroProportionTopicsFromReference(
	// Cluster refLeaf) {
	// System.out.println("Copying first leafs topics to new topics...");
	// ArrayList<TopicItem> topics = new ArrayList<TopicItem>();
	// for (TopicItem topicItem : refLeaf.docTopicItem.topics) {
	// topics.add(new TopicItem(topicItem));
	// }
	//
	// System.out
	// .println("Zeroing out proportions for TopicItems in new topics...");
	// for (TopicItem topicItem : topics) {
	// topicItem.proportion = 0;
	// }
	//
	// System.out.println("Verifying zero out worked...");
	// for (TopicItem topicItem : topics) {
	// System.out.println("topicNum: " + topicItem.topicNum
	// + ", proportion: " + topicItem.proportion);
	// }
	// return topics;
	// }
	//
	// private static Cluster getClusterForReferenceOfTopics(
	// ArrayList<Cluster> currLeafClusters) {
	// Cluster refLeaf = null;
	// for (Cluster leaf : currLeafClusters) {
	// if (leaf.docTopicItem != null) {
	// refLeaf = leaf;
	// }
	// }
	// return refLeaf;
	// }

}
