package edu.usc.softarch.gexf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import net.gexf._1.DefaultedgetypeType;
import net.gexf._1.EdgeContent;
import net.gexf._1.EdgesContent;
import net.gexf._1.GexfContent;
import net.gexf._1.GraphContent;
import net.gexf._1.MetaContent;
import net.gexf._1.ModeType;
import net.gexf._1.NodeContent;
import net.gexf._1.NodesContent;
import net.gexf._1.ObjectFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;


import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.RsfReader;

public class ConvertRsfToGexf {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ConvertRsfToGexf.class);

	public static void main(final String[] args) {
		String containerRsfFilename = "/home/joshua/recovery/Expert Decompositions/linuxFullAuthcontain.rsf";
		String depsFilename = "/home/joshua/recovery/RSFs/linuxRel.rsf";
		String outputGexfFile = "linux_auth_recovery.gexf";
		boolean isStrippingExtensions = false;

		final Options options = new Options();

		final Option help = new Option("help", "print this message");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("authoritative recovery file");
		final Option authFile = OptionBuilder.create("authfile");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("dependencies file");
		final Option depFile = OptionBuilder.create("depfile");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("output gexf file");
		final Option gexfFile = OptionBuilder.create("gexffile");

		final Option stripExt = new Option("stripext",
				"strips .h and .c extensions from facts files");

		options.addOption(help);
		options.addOption(authFile);
		options.addOption(depFile);
		options.addOption(gexfFile);
		options.addOption(stripExt);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("authfile")) {
				containerRsfFilename = line.getOptionValue("authfile");
			}
			if (line.hasOption("depfile")) {
				depsFilename = line.getOptionValue("depfile");
			}
			if (line.hasOption("gexffile")) {
				outputGexfFile = line.getOptionValue("gexffile");
			}
			if (line.hasOption("stripext")) {
				isStrippingExtensions = true;
			}
			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(ConvertRsfToGexf.class.getName(), options);
				System.exit(0);
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		Config.initConfigFromFile(Config.getProjConfigFile());

		RsfReader.loadRsfDataFromFile(containerRsfFilename);
		final Iterable<List<String>> containerFacts = RsfReader.filteredRoutineFacts;

		RsfReader.loadRsfDataFromFile(depsFilename);
		final Iterable<List<String>> depsFacts = RsfReader.filteredRoutineFacts;

		// transformContaineesToMatchDepNodes(containerFacts, depsFacts);

		if (isStrippingExtensions) {
			stripExtensions(containerFacts, depsFacts);
		}

		for (final List<String> fact : containerFacts) {
			logger.debug(fact);
		}

		final Set<String> allNodesSet = new HashSet<String>();
		for (final List<String> fact : containerFacts) {
			allNodesSet.add(fact.get(1));
			allNodesSet.add(fact.get(2));
		}

		final List<String> containersList = Lists.transform(
				Lists.newArrayList(containerFacts), fact -> fact.get(1));

		final Set<String> containerSet = new HashSet<String>(containersList);

		logger.debug("The containers...");
		logger.debug(Joiner.on("\n").join(containerSet));

		Iterables.filter(containerFacts, fact -> !fact.get(2).endsWith(".h")
				|| !fact.get(2).endsWith(".c"));

		logger.debug("The dependencies...");
		logger.debug(Joiner.on("\n").join(depsFacts));

		// attemptToUseGexfJavaLibrary(containerFacts, containerSet);
		try {
			final JAXBContext jc = JAXBContext.newInstance(GexfContent.class);

			final ObjectFactory objFactory = new ObjectFactory();
			final GexfContent gexfContent = objFactory.createGexfContent();

			final MetaContent metaContent = objFactory.createMetaContent();
			metaContent.setLastmodifieddate(getXMLGregorianCalendarNow());
			metaContent.getCreatorOrKeywordsOrDescription().add(
					objFactory.createCreator("extractors"));
			metaContent
					.getCreatorOrKeywordsOrDescription()
					.add(objFactory
							.createDescription("a hierarchical recovery graph"));
			gexfContent.setMeta(metaContent);

			final GraphContent graphContent = objFactory.createGraphContent();
			graphContent.setMode(ModeType.STATIC);
			graphContent.setDefaultedgetype(DefaultedgetypeType.DIRECTED);
			final NodesContent nodesContent = objFactory.createNodesContent();

			graphContent.getAttributesOrNodesOrEdges().add(nodesContent);
			gexfContent.setGraph(graphContent);

			// create NodeContent map
			final Map<String, NodeContent> nodeContentMap = new HashMap<String, NodeContent>();
			for (final String node : allNodesSet) {
				final NodeContent nodeContent = objFactory.createNodeContent();
				nodeContent.setId(node);
				nodeContent.setLabel(node);
				nodeContentMap.put(node, nodeContent);
			}

			// add child nodes to parents
			final Set<String> accountedNodes = new HashSet<String>();
			for (final String container : containerSet) {
				for (final List<String> fact : containerFacts) {
					if (container.equals(fact.get(2))
							&& !fact.get(1).equals(fact.get(2))) {
						final NodeContent parentNode = nodeContentMap.get(fact
								.get(1));
						final NodeContent childNode = nodeContentMap.get(fact
								.get(2));
						NodesContent parentNodesContent = getNodesContent(parentNode);
						if (parentNodesContent == null) {
							parentNodesContent = objFactory
									.createNodesContent();
							parentNode.getAttvaluesOrSpellsOrNodes().add(
									parentNodesContent);

						}
						parentNodesContent.getNode().add(childNode);
						accountedNodes.add(childNode.getId());

					}
				}
			}

			final Set<String> remainingContainerNodes = new HashSet<String>(
					containerSet);
			remainingContainerNodes.removeAll(accountedNodes);

			for (final String node : remainingContainerNodes) {
				nodesContent.getNode().add(nodeContentMap.get(node));
			}

			final Set<String> remainingNonContainerNodes = new HashSet<String>(
					allNodesSet);
			remainingNonContainerNodes.removeAll(accountedNodes);
			remainingNonContainerNodes.removeAll(remainingContainerNodes);

			final Map<String, List<String>> containerMap = new HashMap<String, List<String>>();

			for (final List<String> fact : containerFacts) {
				containerMap.put(fact.get(2), fact);
			}

			for (final String node : remainingNonContainerNodes) {
				final List<String> fact = containerMap.get(node);
				final NodeContent parentNode = nodeContentMap.get(fact.get(1));
				final NodeContent childNode = nodeContentMap.get(fact.get(2));
				NodesContent parentNodesContent = getNodesContent(parentNode);
				if (parentNodesContent == null) {
					parentNodesContent = objFactory.createNodesContent();
					parentNode.getAttvaluesOrSpellsOrNodes().add(
							parentNodesContent);

				}
				parentNodesContent.getNode().add(childNode);
			}

			final int totalNodeCount = countNodes(nodesContent);
			System.out.println("Total nodes in gexf file: " + totalNodeCount);
			System.out.println("Total nodes from rsf: " + allNodesSet.size());

			final EdgesContent edgesContent = objFactory.createEdgesContent();
			for (final List<String> fact : depsFacts) {
				final EdgeContent edgeContent = objFactory.createEdgeContent();
				edgeContent.setSource(fact.get(1));
				edgeContent.setTarget(fact.get(2));
				edgeContent.setLabel(fact.get(0));
				edgesContent.getEdge().add(edgeContent);
			}
			graphContent.getAttributesOrNodesOrEdges().add(edgesContent);

			final Marshaller m = jc.createMarshaller();

			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

			final FileOutputStream fos = new FileOutputStream(outputGexfFile);
			m.marshal(gexfContent, fos);
		} catch (final PropertyException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final JAXBException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final DatatypeConfigurationException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void stripExtensions(
			final Iterable<List<String>> containerFacts,
			final Iterable<List<String>> depsFacts) {
		for (final List<String> fact : containerFacts) {
			String source = fact.get(1);
			String target = fact.get(2);
			source = source.replace(".c", "");
			source = source.replace(".h", "");
			target = target.replace(".c", "");
			target = target.replace(".h", "");
			fact.remove(fact.size() - 1);
			fact.remove(fact.size() - 1);
			fact.add(source);
			fact.add(target);
		}

		for (final List<String> fact : depsFacts) {
			String source = fact.get(1);
			String target = fact.get(2);
			source = source.replace(".c", "");
			source = source.replace(".h", "");
			target = target.replace(".c", "");
			target = target.replace(".h", "");
			fact.remove(fact.size() - 1);
			fact.remove(fact.size() - 1);
			fact.add(source);
			fact.add(target);
		}
	}

	private static int countNodes(final NodesContent nodesContent) {
		int nodeCount = 0;
		if (nodesContent == null) {
			return 0;
		}
		if (nodesContent.getNode().size() != 0) {
			for (final NodeContent nodeContent : nodesContent.getNode()) {
				nodeCount++;
				nodeCount += countNodes(getNodesContent(nodeContent));
			}
		}
		return nodeCount;
	}

	private static NodesContent getNodesContent(final NodeContent parentNode) {
		for (final Object obj : parentNode.getAttvaluesOrSpellsOrNodes()) {
			if (obj instanceof NodesContent) {
				return (NodesContent) obj;
			}
		}
		return null;

	}

	public static XMLGregorianCalendar getXMLGregorianCalendarNow()
			throws DatatypeConfigurationException {
		final GregorianCalendar gregorianCalendar = new GregorianCalendar();
		final DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		final XMLGregorianCalendar now = datatypeFactory
				.newXMLGregorianCalendar(gregorianCalendar);
		return now;
	}

	// private static Node getNode(final String container, final List<Node>
	// nodes) {
	// for (final Node node : nodes) {
	// if (node.getId().equals(container)) {
	// return node;
	// }
	// }
	// return null;
	// }
}
