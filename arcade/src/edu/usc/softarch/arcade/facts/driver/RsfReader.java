package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.usc.softarch.arcade.clustering.FeatureVectorMap;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.functiongraph.TypedEdgeGraph;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.StopWatch;

public class RsfReader {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(RsfReader.class);
	public static HashSet<Object> untypedEdgesSet;
	public static TreeSet<String> startNodesSet;
	public static Iterable<List<String>> filteredRoutineFacts;
	public static List<String> filteredRoutines;
	public static HashSet<String> endNodesSet;
	public static Set<String> allNodesSet;
	public static List<List<String>> unfilteredFacts;

	public static void main(final String[] args) {
		final Options options = new Options();

		final Option help = new Option("help", "print this message");
		final Option loadRsfData = new Option("l", "loads rsf data from deps_rsf_file property in project file");
		final Option writeFilteredData = new Option("w", "write filtered rsf data from deps_rsf_file property in project file");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("project configuration file");

		// Option.builder().argName("file");
		// Option.builder().hasArg();
		// Option.builder().desc("project configuration file");

		final Option projFile = OptionBuilder.create("projfile");

		// Option projFile = Option.builder().

		options.addOption(help);
		options.addOption(projFile);
		options.addOption(loadRsfData);
		options.addOption(writeFilteredData);

		// create the parser
		final CommandLineParser parser = new DefaultParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("projfile")) {
				Config.setProjConfigFile(FileUtil.checkFile(line.getOptionValue("projfile"), false, false));
			}
			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("RsfReader", options);
				System.exit(0);
			}

			// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

			Config.initConfigFromFile(Config.getProjConfigFile());

			if (line.hasOption("l")) {
				loadRsfDataForCurrProj();
			}
			if (line.hasOption("w")) {
				writeFilteredFactsToFile();
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}
	}

	public static List<List<String>> extractFactsFromRSF(final File rsfFile) {
		// List of facts extracted from RSF File
		final List<List<String>> facts = Lists.newArrayList();

		final boolean local_debug = false;

		try {
			final BufferedReader in = new BufferedReader(new FileReader(rsfFile));
			String line;
			// int lineCount = 0;
			// final int limit = 0;
			while ((line = in.readLine()) != null) {
				// if (lineCount == limit && limit != 0) {
				// break;
				// }
				if (local_debug) {
					logger.debug(line);
				}

				if (line.trim().isEmpty()) {
					continue;
				}

				final Scanner s = new Scanner(line);
				// String expr = "([\"\"'])(?:(?=(\\?))\2.)*?\1|([^\"].*[^\"])";
				// String expr = "^[^\"][^\\s]+[^\"]$";
				// String expr = "([^\"\\s][^\\s]*[^\"\\s])"; // any non
				// whitespace characters without quotes or whitespace at the
				// start or beginning
				// String expr = "([\"][^\"]*[\"])"; // any characters in quotes
				// including the quotes
				final String expr = "([^\"\\s][^\\s]*[^\"\\s]*)|([\"][^\"]*[\"])";
				// int tokenLimit = 3;

				final String arcType = s.findInLine(expr);
				final String startNode = s.findInLine(expr);
				final String endNode = s.findInLine(expr);
				final List<String> fact = Lists.newArrayList(arcType, startNode, endNode);
				if (local_debug) {
					logger.debug(fact);
				}
				facts.add(fact);

				if (s.findInLine(expr) != null) {
					logger.error("Found non-triple in file: " + line);
					System.exit(1);
				}

				/*
				 * MatchResult result = s.match(); for (int i=1; i<=result.groupCount(); i++) logger.debug(i + ": " + result.group(i)); s.close();
				 */

				// lineCount++;
				/*
				 * if (triple.size() != 3) { logger.error( "Found non-triple in file: " + triple); System.exit(1); } logger.debug(triple);
				 */
				s.close();
			}
			in.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return facts;
	}

	public static void writeFilteredFactsToFile() {
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// String rsfFilename =
		// "/home/joshua/Documents/Software Engineering
		// Research/projects/recovery/RSFs/bash.rsf";
		// String rsfFilename =
		// "/home/joshua/workspace/MyExtractors/data/test1.rsf";
		final File rsfFile = Config.getDepsRsfFile();

		final List<List<String>> facts = extractFactsFromRSF(rsfFile);

		logger.debug("Printing stored facts...");
		logger.debug(Joiner.on("\n").join(facts));

		filteredRoutineFacts = filterRoutinesFromFacts(facts);
		logger.debug("Printing filtered routine facts...");
		logger.debug(Joiner.on("\n").join(filteredRoutineFacts));

		final Iterable<List<String>> filteredDepFacts = filterFacts(facts);

		final List<List<String>> filteredDepFactsList = Lists.newArrayList(filteredDepFacts);
		logger.debug("Printing filtered dependency facts....");
		logger.debug("number of filtered dependency facts: " + filteredDepFactsList.size());
		logger.debug(Joiner.on("\n").join(filteredDepFacts));

		try {
			writeFactsToFile(filteredRoutineFacts, Config.getFilteredRoutineFactsFilename());
			writeFactsToFile(filteredDepFacts, Config.getFilteredFactsFilename());
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		stopWatch.stop();
		logger.trace("Elapsed time in milliseconds: " + stopWatch.getElapsedTime());
	}

	private static void writeFactsToFile(final Iterable<List<String>> facts, final String fileName) throws IOException {
		final FileWriter fstream = new FileWriter(fileName);
		final BufferedWriter out = new BufferedWriter(fstream);

		for (final List<String> fact : facts) {
			out.write(fact.get(0) + " " + fact.get(1) + " " + fact.get(2) + "\n");
		}

		out.close();

	}

	public static void loadRsfDataAndFilter() {
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		// String rsfFilename =
		// "/home/joshua/Documents/Software Engineering
		// Research/projects/recovery/RSFs/bash.rsf";
		// String rsfFilename =
		// "/home/joshua/workspace/MyExtractors/data/test1.rsf";
		final File rsfFile = Config.getDepsRsfFile();

		final List<List<String>> facts = extractFactsFromRSF(rsfFile);

		logger.debug("Printing stored facts...");
		logger.debug(Joiner.on("\n").join(facts));

		filteredRoutineFacts = filterRoutinesFromFacts(facts);
		logger.debug("Printing filtered routine facts...");
		logger.debug(Joiner.on("\n").join(filteredRoutineFacts));

		filteredRoutines = filterRoutinesFromRoutineFacts();
		final List<String> sortedFilteredRoutines = new ArrayList<>(filteredRoutines);
		Collections.sort(sortedFilteredRoutines);

		logger.debug("Printing filtered routines...");
		logger.debug("Number of filtered routines: " + sortedFilteredRoutines.size());
		logger.debug(Joiner.on("\n").join(sortedFilteredRoutines));

		final Iterable<List<String>> filteredDepFacts = filterFacts(facts);

		final List<List<String>> filteredDepFactsList = Lists.newArrayList(filteredDepFacts);
		logger.debug("Printing filtered dependency facts....");
		logger.debug("number of filtered dependency facts: " + filteredDepFactsList.size());
		logger.debug(Joiner.on("\n").join(filteredDepFacts));

		final List<Object> untypedEdges = convertFactsToUntypedEdges(filteredDepFacts);

		untypedEdgesSet = Sets.newHashSet(untypedEdges);

		logger.debug("Printing untyped edges....");
		logger.debug("number of untyped edges as list: " + untypedEdges.size());
		logger.debug("number of untyped edges as set: " + untypedEdgesSet.size());
		logger.debug(Joiner.on("\n").join(untypedEdges));

		final List<String> startNodesList = convertFactsToStartNodesList(filteredDepFacts);

		final HashSet<String> rawStartNodesSet = Sets.newHashSet(startNodesList);

		logger.debug("Printing raw start nodes...");
		logger.debug("number of raw start nodes: " + rawStartNodesSet.size());
		logger.debug(Joiner.on("\n").join(rawStartNodesSet));

		final List<String> endNodesList = convertFactsToEndNodesList(filteredDepFacts);
		final HashSet<String> endNodesSet = Sets.newHashSet(endNodesList);

		logger.debug("Printing end nodes...");
		logger.debug("number of end nodes: " + endNodesSet.size());
		logger.debug(Joiner.on("\n").join(endNodesSet));

		final TreeSet<String> sortedFilteredRoutinesSet = Sets.newTreeSet(sortedFilteredRoutines);
		startNodesSet = new TreeSet<>(rawStartNodesSet);
		startNodesSet.retainAll(sortedFilteredRoutinesSet);

		logger.debug("Printing start nodes...");
		logger.debug("number of start nodes: " + startNodesSet.size());
		logger.debug(Joiner.on("\n").join(startNodesSet));

		stopWatch.stop();
		logger.trace("Elapsed time in milliseconds: " + stopWatch.getElapsedTime());
	}

	public static void loadRsfDataForCurrProj() {
		// String rsfFilename =
		// "/home/joshua/Documents/Software Engineering
		// Research/projects/recovery/RSFs/bash.rsf";
		// String rsfFilename =
		// "/home/joshua/workspace/MyExtractors/data/test1.rsf";
		final File rsfFile = Config.getDepsRsfFile();

		loadRsfDataFromFile(rsfFile);
	}

	public static void loadRsfDataFromFile(final String rsfFileName) {
		loadRsfDataFromFile(FileUtil.checkFile(rsfFileName, false, false));
	}

	public static void loadRsfDataFromFile(final File rsfFile) {
		final StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		unfilteredFacts = extractFactsFromRSF(rsfFile);

		final boolean local_debug = false;

		if (local_debug) {
			logger.debug("Printing stored facts...");
			logger.debug(Joiner.on("\n").join(unfilteredFacts));
		}

		filteredRoutineFacts = unfilteredFacts;

		final List<Object> untypedEdges = convertFactsToUntypedEdges(unfilteredFacts);

		untypedEdgesSet = Sets.newHashSet(untypedEdges);

		if (local_debug) {
			logger.debug("Printing untyped edges....");
			logger.debug("number of untyped edges as list: " + untypedEdges.size());
			logger.debug("number of untyped edges as set: " + untypedEdgesSet.size());
			logger.debug(Joiner.on("\n").join(untypedEdges));
		}
		final List<String> startNodesList = convertFactsToStartNodesList(unfilteredFacts);

		final HashSet<String> rawStartNodesSet = Sets.newHashSet(startNodesList);

		if (local_debug) {
			logger.debug("Printing raw start nodes...");
			logger.debug("number of raw start nodes: " + rawStartNodesSet.size());
			logger.debug(Joiner.on("\n").join(rawStartNodesSet));
		}

		final List<String> endNodesList = convertFactsToEndNodesList(unfilteredFacts);
		endNodesSet = Sets.newHashSet(endNodesList);

		if (local_debug) {
			logger.debug("Printing end nodes...");
			logger.debug("number of end nodes: " + endNodesSet.size());
			logger.debug(Joiner.on("\n").join(endNodesSet));
		}

		startNodesSet = new TreeSet<>(rawStartNodesSet);

		if (local_debug) {
			logger.debug("Printing start nodes...");
			logger.debug("number of start nodes: " + startNodesSet.size());
			logger.debug(Joiner.on("\n").join(startNodesSet));
		}

		allNodesSet = new HashSet<>(startNodesSet);
		allNodesSet.addAll(endNodesSet);

		stopWatch.stop();
		logger.trace("Elapsed time in milliseconds: " + stopWatch.getElapsedTime());
	}

	private static List<String> convertFactsToEndNodesList(final Iterable<List<String>> filteredFacts) {
		return Lists.transform(Lists.newArrayList(filteredFacts), fact -> fact.get(2));
	}

	private static List<String> convertFactsToStartNodesList(final Iterable<List<String>> filteredFacts) {
		return Lists.transform(Lists.newArrayList(filteredFacts), fact -> fact.get(1));
	}

	private static List<Object> convertFactsToUntypedEdges(final Iterable<List<String>> filteredFacts) {
		return Lists.transform(Lists.newArrayList((Iterable<?>) filteredFacts), fact -> Lists.newArrayList(((List<String>) fact).get(1), ((List<String>) fact).get(2)));
	}

	private static List<String> filterRoutinesFromRoutineFacts() {
		return Lists.transform(Lists.newArrayList(filteredRoutineFacts), fact -> fact.get(1));
	}

	private static Iterable<List<String>> filterRoutinesFromFacts(final List<List<String>> facts) {
		return Iterables.filter(facts, fact -> !fact.get(1).contains("/") && fact.get(0).matches("type") && fact.get(2).matches("\"Routine\""));
	}

	private static Iterable<List<String>> filterFacts(final List<List<String>> facts) {
		return Iterables.filter(facts, fact -> !fact.get(1).contains("/") && !fact.get(0).matches("level|lineno|type|file"));
	}

	public static void setupLogging() {
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());
	}

	public static void performPreClusteringTasks() throws ParserConfigurationException, TransformerException {
		Config.setSelectedLanguage(Language.c);
		Config.initConfigFromFile(Config.getProjConfigFile());
		/*
		 * writeXMLFunctionDepGraph(filteredFacts); FunctionGraph functionGraph = createFunctionGraph(filteredFacts);
		 */
		writeXMLTypedEdgeDepGraph(filteredRoutineFacts);
		final TypedEdgeGraph typedEdgeGraph = createFunctionGraph(filteredRoutineFacts);
		logger.debug("typed edge graph size: " + typedEdgeGraph.edges.size());
		// logger.debug("Printing typed edge graph...");
		// logger.debug(typedEdgeGraph);
		final FeatureVectorMap fvMap = new FeatureVectorMap(typedEdgeGraph);
		// fvMap.writeXMLFeatureVectorMapUsingFunctionDepEdges();

		fvMap.serializeAsFastFeatureVectors();
	}

	private static TypedEdgeGraph createFunctionGraph(final Iterable<List<String>> filteredFacts) {
		final TypedEdgeGraph graph = new TypedEdgeGraph();
		for (final List<String> fact : filteredFacts) {
			graph.addEdge(fact.get(0), fact.get(1), fact.get(2));
		}

		return graph;

	}

	public static void writeXMLTypedEdgeDepGraph(final Iterable<List<String>> filteredFacts) throws ParserConfigurationException, TransformerException {
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// classgraph elements
		final Document doc = docBuilder.newDocument();
		final Element rootElement = doc.createElement("FunctionDepGraph");
		doc.appendChild(rootElement);

		// classedge elements
		for (final List<String> fact : filteredFacts) {
			final Element ce = doc.createElement("edge");
			rootElement.appendChild(ce);

			final Element arcType = doc.createElement("arcType");
			arcType.appendChild(doc.createTextNode(fact.get(0)));
			ce.appendChild(arcType);

			final Element src = doc.createElement("srcNode");
			src.appendChild(doc.createTextNode(fact.get(1)));
			ce.appendChild(src);

			final Element tgt = doc.createElement("endNode");
			tgt.appendChild(doc.createTextNode(fact.get(2)));
			ce.appendChild(tgt);
		}

		// write the content into xml file
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		final DOMSource source = new DOMSource(doc);
		final File xmlFunctionDepGraphFile = new File(Config.getXMLFunctionDepGraphFilename());
		xmlFunctionDepGraphFile.getParentFile().mkdirs();
		final StreamResult result = new StreamResult(xmlFunctionDepGraphFile);
		transformer.transform(source, result);

		logger.debug("In " + Thread.currentThread().getStackTrace()[1].getClassName() + ". " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", Wrote "
				+ Config.getXMLFunctionDepGraphFilename());

	}
}
