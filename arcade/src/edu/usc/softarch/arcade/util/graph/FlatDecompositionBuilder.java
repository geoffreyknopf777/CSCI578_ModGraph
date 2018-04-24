package edu.usc.softarch.arcade.util.graph;

import java.awt.Container;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections15.Factory;
import org.apache.logging.log4j.Logger;


import com.google.common.base.Joiner;

import edu.uci.ics.jung.graph.DelegateTree;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseGraph;
import edu.uci.ics.jung.graph.Tree;
import edu.usc.softarch.arcade.facts.driver.RsfReader;

public class FlatDecompositionBuilder {

	public enum FlatDecompType {
		compact, detailed
	}

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(FlatDecompositionBuilder.class);

	static Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i = 0;

		@Override
		public Integer create() {
			return i++;
		}
	};

	public static void buildViewer(final Tree<String, Integer> tree) {
		final JFrame frame = new JFrame();
		final Container content = frame.getContentPane();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		content.add(new TreeGraphGenerator(tree));
		frame.pack();
		frame.setVisible(true);
	}

	public static void main(final String[] args) {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		// String filename = "data/generated/generated_tree_5mc_5mh.rsf";
		// String filename =
		// "/home/joshua/recovery/Expert Decompositions/linux.Nested.Autho#254C003D.rsf";
		String nestedFilename = "";
		String flatFilename = "";
		FlatDecompType fdt = FlatDecompType.compact;
		boolean isVisualizingTree = false;

		final Options options = new Options();

		final Option help = new Option("help", "print this message");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("nested file to flatten");
		// Option visualizeOption = new Option( "visualize",
		// "show visualization of tree" );
		final Option nestedFilenameOption = OptionBuilder.create("nestedFile");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("flattened file name");
		final Option flatFilenameOption = OptionBuilder.create("flatFile");
		OptionBuilder.withArgName("c|d");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("flatten as compact (c) or detailed (d)");
		final Option flattenTypeOption = OptionBuilder.create("type");

		options.addOption(help);
		options.addOption(nestedFilenameOption);
		options.addOption(flatFilenameOption);
		options.addOption(flattenTypeOption);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				generateHelpStatement(options);
			}
			if (line.hasOption("visualize")) {
				isVisualizingTree = true;
			}
			if (line.hasOption("nestedFile")) {
				nestedFilename = line.getOptionValue("nestedFile");
			}
			if (line.hasOption("flatFile")) {
				flatFilename = line.getOptionValue("flatFile");
			}
			if (line.hasOption("type")) {
				final String typeStr = line.getOptionValue("type");
				if (typeStr.equals("c")) {
					fdt = FlatDecompType.compact;
				} else if (typeStr.equals("d")) {
					fdt = FlatDecompType.detailed;
				}
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			generateHelpStatement(options);
		}

		RsfReader.loadRsfDataFromFile(nestedFilename);
		logger.debug("Printing filtered routine facts...");
		logger.debug(Joiner.on("\n").join(RsfReader.filteredRoutineFacts));

		final DirectedGraph<String, Integer> dGraph = new DirectedSparseGraph<String, Integer>();
		for (final List<String> fact : RsfReader.filteredRoutineFacts) {
			dGraph.addEdge(edgeFactory.create(), fact.get(1), fact.get(2));
		}

		logger.debug("Printing graph...");
		logger.debug(dGraph);

		final DelegateTree<String, Integer> tree = new DelegateTree<String, Integer>(dGraph);
		for (final String vertex : tree.getVertices()) {
			if (tree.getParent(vertex) == null) {
				tree.setRoot(vertex);
			}
		}
		if (tree.getRoot() == null) {
			throw new RuntimeException("tree has no root...");
		}
		logger.debug("Printing tree...");
		logger.debug(tree);
		if (isVisualizingTree) {
			buildViewer(tree);
		}

		Map<String, List<String>> clustersMap = null;
		if (fdt.equals(FlatDecompType.compact)) {
			clustersMap = buildCompactFlatClusters(tree);
		} else if (fdt.equals(FlatDecompType.detailed)) {
			clustersMap = buildDetailedFlatClusters(tree);
		} else {
			throw new IllegalArgumentException("selected FlatDecompType is invalid: " + fdt);
		}

		logger.debug("number of flat clusters: " + clustersMap.keySet().size());
		for (final String parent : clustersMap.keySet()) {
			final List<String> members = clustersMap.get(parent);
			logger.debug("parent: " + parent);
			logger.debug("members: " + Joiner.on(",").join(members));
		}

		FileWriter fw;
		try {
			fw = new FileWriter(flatFilename);
			final BufferedWriter out = new BufferedWriter(fw);
			for (final String parent : clustersMap.keySet()) {
				final List<String> members = clustersMap.get(parent);
				for (final String member : members) {
					final String rsfLine = "contain " + parent + " " + member;
					logger.debug(rsfLine);
					out.write(rsfLine + "\n");
				}
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static void generateHelpStatement(final Options options) {
		// automatically generate the help statement
		final HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(FlatDecompositionBuilder.class.getName(), options);
		System.exit(0);
	}

	private static Map<String, List<String>> buildDetailedFlatClusters(final DelegateTree<String, Integer> tree) {
		final Map<String, List<String>> clustersMap = new HashMap<String, List<String>>();
		for (final String vertex : tree.getVertices()) {
			if (tree.getRoot().equals(vertex)) {
				continue;
			}
			if (tree.isLeaf(vertex)) {
				List<String> clusterMembers = null;
				final String parent = tree.getParent(vertex);
				if (clustersMap.containsKey(parent)) {
					clusterMembers = clustersMap.get(parent);
					clusterMembers.add(vertex);
				} else {
					clusterMembers = new ArrayList<String>();
					clusterMembers.add(vertex);
					clustersMap.put(parent, clusterMembers);
				}
			}
		}
		return clustersMap;
	}

	private static Map<String, List<String>> buildCompactFlatClusters(final DelegateTree<String, Integer> tree) {
		final Map<String, List<String>> clustersMap = new HashMap<String, List<String>>();
		final Collection<String> topLevelClusters = tree.getChildren(tree.getRoot());
		for (final String vertex : topLevelClusters) {
			final List<String> leaves = new ArrayList<String>();
			getLeavesOfBranch(vertex, leaves, tree);
			clustersMap.put(vertex, leaves);
		}
		return clustersMap;
	}

	private static void getLeavesOfBranch(final String vertex, final Collection<String> leaves, final DelegateTree<String, Integer> tree) {
		for (final String child : tree.getChildren(vertex)) {
			if (tree.isLeaf(child)) {
				leaves.add(child);
			} else {
				getLeavesOfBranch(child, leaves, tree);
			}
		}
	}
}
