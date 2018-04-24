package edu.usc.softarch.arcade.topics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import cc.mallet.util.Maths;
import edu.usc.softarch.arcade.clustering.Cluster;
import edu.usc.softarch.arcade.clustering.Entity;
import edu.usc.softarch.arcade.clustering.FastCluster;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.config.ConfigUtil;
import edu.usc.softarch.arcade.util.DebugUtil;

/**
 * @author joshua
 *
 */
public class TopicUtil {

	public static DocTopics docTopics;
	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(TopicUtil.class);

	public static String convertJavaClassWithPackageNameToDocTopicName(final String name) {
		return name.replaceAll("\\.", "/") + ".java";
	}

	public static double klDivergence(final double[] sortedP, final double[] sortedQ) {
		double divergence = 0;

		final double verySmallVal = 0.00000001;

		for (int i = 0; i < sortedP.length; i++) {

			double denominator = 0;
			double numerator = 0;

			if (sortedQ[i] == 0) {
				denominator = verySmallVal;
			} else {
				denominator = sortedQ[i];
			}
			if (sortedP[i] == 0) {
				numerator = 2 * verySmallVal;
			} else {
				numerator = sortedP[i];
			}

			divergence += sortedP[i] * Math.log(numerator / denominator);
		}

		return divergence;
	}

	public static double symmKLDivergence(final DocTopicItem pDocTopicItem, final DocTopicItem qDocTopicItem) {
		double divergence = 0;
		final boolean local_debug = false;

		if (pDocTopicItem.topics.size() != qDocTopicItem.topics.size()) {
			logger.error("P size: " + pDocTopicItem.topics.size());
			logger.error("Q size: " + qDocTopicItem.topics.size());
			logger.error("P and Q for Kullback Leibler Divergence not the same size...exiting");
			System.exit(0);
		}

		final double[] sortedP = new double[pDocTopicItem.topics.size()];
		final double[] sortedQ = new double[qDocTopicItem.topics.size()];

		for (final TopicItem pTopicItem : pDocTopicItem.topics) {
			for (final TopicItem qTopicItem : qDocTopicItem.topics) {
				if (pTopicItem.topicNum == qTopicItem.topicNum) {
					sortedP[pTopicItem.topicNum] = pTopicItem.proportion;
					sortedQ[qTopicItem.topicNum] = qTopicItem.proportion;
				}
			}
		}

		divergence = .5 * (klDivergence(sortedP, sortedQ) + klDivergence(sortedQ, sortedP));
		if (local_debug) {
			logger.debug("P distribution values: ");
			for (final double element : sortedP) {
				System.out.format("%.3f,", element);
			}
			logger.debug("\n");

			logger.debug("Q distribution values: ");
			for (final double element : sortedQ) {
				System.out.format("%.3f,", element);
			}
			logger.debug("\n");

			logger.debug("Symmetric Kullback Leibler Divergence: " + divergence);
		}

		return divergence;
	}

	public static double jsDivergence(final DocTopicItem pDocTopicItem, final DocTopicItem qDocTopicItem) {
		// return 0;
		double divergence = 0;
		final boolean localDebug = false;

		if (pDocTopicItem.topics.size() != qDocTopicItem.topics.size()) {
			logger.error("P size: " + pDocTopicItem.topics.size());
			logger.error("Q size: " + qDocTopicItem.topics.size());
			logger.error("P and Q for Jensen Shannon Divergence not the same size...exiting");
			System.exit(0);
		}

		final double[] sortedP = new double[pDocTopicItem.topics.size()];
		final double[] sortedQ = new double[qDocTopicItem.topics.size()];

		for (final TopicItem pTopicItem : pDocTopicItem.topics) {
			sortedP[pTopicItem.topicNum] = pTopicItem.proportion;
		}

		for (final TopicItem qTopicItem : qDocTopicItem.topics) {
			sortedQ[qTopicItem.topicNum] = qTopicItem.proportion;
		}

		// divergence = jsDivergence(sortedP, sortedQ);
		divergence = Maths.jensenShannonDivergence(sortedP, sortedQ);
		// divergence = 0;

		if (localDebug) {
			logger.debug("P distribution values: ");
			for (final double element : sortedP) {
				System.out.format("%.3f,", element);
			}
			logger.debug("\n");

			logger.debug("Q distribution values: ");
			for (final double element : sortedQ) {
				System.out.format("%.3f,", element);
			}
			logger.debug("\n");

			logger.debug("Jensen Shannon Divergence: " + divergence);
			logger.debug("Symmetric Kullback Leibler Divergence: " + symmKLDivergence(pDocTopicItem, qDocTopicItem));
		}

		return divergence;
	}

	public static TopicKeySet getTypedTopicKeyList() throws IOException, ParserConfigurationException, SAXException {
		final File smellArchXMLFile = new File(Config.getSpecifiedSmallArchFromXML());
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(smellArchXMLFile);
		doc.getDocumentElement().normalize();

		logger.debug("Root element :" + doc.getDocumentElement().getNodeName());
		final NodeList topicsList = doc.getElementsByTagName("topics");

		logger.debug("Getting info on topics...");
		logger.debug("----------------------- size: " + topicsList.getLength());

		final TopicKeySet topicKeys = TopicUtil.getTopicKeyListForCurrProj();
		final Node topicsNode = topicsList.item(0);
		if (topicsNode.getNodeType() == Node.ELEMENT_NODE) {
			final Element topicsElem = (Element) topicsNode;

			final NodeList topicList = topicsElem.getElementsByTagName("topic");

			for (int i = 0; i < topicList.getLength(); i++) {
				final Node topicNode = topicList.item(i);
				if (topicNode.getNodeType() == Node.ELEMENT_NODE) {
					final Element topicElem = (Element) topicNode;
					final int topicNum = Integer.parseInt(topicElem.getAttribute("id"));
					logger.debug("topic id: " + topicNum);
					final String topicItemTypeFromXML = topicElem.getAttribute("type").trim();
					logger.debug("\ttype: " + topicItemTypeFromXML);

					for (final TopicKey topicKey : topicKeys.set) {
						if (topicKey.topicNum == topicNum) {
							topicKey.type = topicItemTypeFromXML;
						}
					}

				}
			}
		}
		return topicKeys;
	}

	public static HashSet<String> getStopWordSet() throws IOException {
		final File f = new File(Config.getStopWordsFilename());

		final BufferedReader input = new BufferedReader(new FileReader(f));

		final HashSet<String> stopWordsSet = new HashSet<String>();
		String line;
		while ((line = input.readLine()) != null) {
			stopWordsSet.add(line.trim());
		}
		input.close();
		return stopWordsSet;
	}

	public static TopicKeySet getTopicKeyListForCurrProj() throws FileNotFoundException {
		return new TopicKeySet(Config.getMalletTopicKeysFilename());
	}

	public static WordTopicCounts getWordTopicCountsForCurrProj() throws FileNotFoundException {
		return new WordTopicCounts(Config.getMalletWordTopicCountsFilename());
	}

	public static DocTopics getDocTopicsFromFile() {
		DocTopics docTopics = null;
		System.out.println("Loading doc topic file from " + Config.getMalletDocTopicsFile());
		docTopics = new DocTopics(Config.getMalletDocTopicsFile().getPath());
		return docTopics;
	}

	public static DocTopics getDocTopicsFromFile(final String filename) {
		DocTopics docTopics = null;
		docTopics = new DocTopics(filename);
		return docTopics;
	}

	public static DocTopics getDocTopicsFromHardcodedDocTopicsFile() {
		DocTopics docTopics = null;
		docTopics = new DocTopics("/home/joshua/ser/subject_systems/archstudio4/archstudio4-550-doc-topics.txt");
		return docTopics;
	}

	public static DocTopics getDocTopicsFromVariableMalletDocTopicsFile() {
		DocTopics docTopics = null;
		System.out.println("Loading doc topic file from " + Config.getVariableMalletDocTopicsFilename());
		docTopics = new DocTopics(Config.getVariableMalletDocTopicsFilename());
		// docTopics = new DocTopics("archstudio4-180-doc-topics.txt");
		return docTopics;
	}

	public static void setDocTopicForCluster(final DocTopics docTopics, final Cluster leaf) {
		final boolean localDebug = false;
		String strippedLeafClassName = ConfigUtil.stripParensEnclosedClassNameWithPackageName(leaf);
		final String dollarSign = "$";
		if (strippedLeafClassName.contains(dollarSign)) {
			// ".*$[a-zA-Z]+
			final String anonInnerClassRegExpr = ".*\\$\\D.*";
			if (Pattern.matches(anonInnerClassRegExpr, strippedLeafClassName)) {
				if (localDebug) {
					logger.debug("\t\tfound inner class: " + strippedLeafClassName);
				}

				strippedLeafClassName = strippedLeafClassName.substring(strippedLeafClassName.lastIndexOf('$') + 1, strippedLeafClassName.length());

				if (localDebug) {
					logger.debug("\t\tstripped to name to: " + strippedLeafClassName);
				}
			}
		}

		if (localDebug) {
			logger.debug("\t" + strippedLeafClassName);
		}
		leaf.docTopicItem = docTopics.getDocTopicItemForJava(strippedLeafClassName);
		if (localDebug) {
			logger.debug("\t" + "doc-topic: " + leaf.docTopicItem);
		}
	}

	/**
	 * pretty much the same method as above, except uses Entities instead of FastClusters. Appends .java and ignores the entities whose names have $ sign in them
	 *
	 * @param docTopics
	 * @param leaf
	 */

	public static void setDocTopicForEntity(final DocTopics docTopics, final Entity leaf, final String type) {
		if (type.equals("java")) {
			final boolean localDebug = false;
			String strippedLeafClassName = ConfigUtil.stripParensEnclosedClassNameWithPackageName(leaf);
			// String strippedLeafClassName = leaf.name;

			final String dollarSign = "$";
			if (strippedLeafClassName.contains(dollarSign)) {
				// System.out.println("Contains a dollar sign " +
				// strippedLeafClassName);
				// ".*$[a-zA-Z]+
				final String anonInnerClassRegExpr = ".*\\$\\D.*";
				if (Pattern.matches(anonInnerClassRegExpr, strippedLeafClassName)) {
					if (localDebug) {
						logger.debug("\t\tfound inner class: " + strippedLeafClassName);
					}

					strippedLeafClassName = strippedLeafClassName.substring(strippedLeafClassName.lastIndexOf('$') + 1, strippedLeafClassName.length());

					if (localDebug) {
						logger.debug("\t\tstripped to name to: " + strippedLeafClassName);
					}
				}
			} else {

				if (localDebug) {
					logger.debug("\t" + strippedLeafClassName);
				}
				final StringBuilder sb = new StringBuilder(strippedLeafClassName);
				sb.append(".java");
				// System.out.println("sb is " + sb.toString());
				leaf.setDocTopicItem(docTopics.getDocTopicItemForJava(sb.toString()));

				/*
				 * System.out.println("The docTopics for " + strippedLeafClassName + "are "); for (int i = 0; i < leaf.docTopicItem.topics.size(); i++) { System.out.print(" " + leaf.docTopicItem.topics.get(i)); } System.out.println();
				 */
			}
		} else if (type.equals("c")) {
			leaf.setDocTopicItem(docTopics.getDocTopicItemForC(leaf.getName()));
			if (leaf.getDocTopicItem() == null) {
				try {
					throw new Exception("Could not obtain doc topic item for: " + leaf.getName());
				} catch (final Exception e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
		} else {
			try {
				throw new Exception("cannot set doc topic for entity with type: " + type);
			} catch (final Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

	}

	public static void setDocTopicForFastClusterForMalletFile(final DocTopics docTopics, final FastCluster leaf) {
		final boolean localDebug = false;
		String strippedLeafClasName = leaf.getName();
		final String dollarSign = "$";
		if (strippedLeafClasName.contains(dollarSign)) {
			// ".*$[a-zA-Z]+
			final String anonInnerClassRegExpr = ".*\\$\\D.*";
			if (Pattern.matches(anonInnerClassRegExpr, strippedLeafClasName)) {
				if (localDebug) {
					logger.debug("\t\tfound inner class: " + strippedLeafClasName);
				}

				strippedLeafClasName = strippedLeafClasName.substring(strippedLeafClasName.lastIndexOf('$') + 1, strippedLeafClasName.length());

				if (localDebug) {
					logger.debug("\t\tstripped to name to: " + strippedLeafClasName);
				}
			}
		}

		if (localDebug) {
			logger.debug("\t" + strippedLeafClasName);
		}
		if (Config.getSelectedLanguage().equals(Language.c)) {
			leaf.docTopicItem = docTopics.getDocTopicItemForC(strippedLeafClasName);
			logger.debug("set " + (leaf.docTopicItem == null ? "null" : leaf.docTopicItem.source) + " as doc topic for " + strippedLeafClasName);
		} else if (Config.getSelectedLanguage().equals(Language.java)) {
			final String docTopicName = convertJavaClassWithPackageNameToDocTopicName(leaf.getName());
			leaf.docTopicItem = docTopics.getDocTopicItemForJava(docTopicName);
		} else {
			leaf.docTopicItem = docTopics.getDocTopicItemForJava(strippedLeafClasName);
		}
		if (localDebug) {
			logger.debug("\t" + "doc-topic: " + leaf.docTopicItem);
		}
	}

	public static DocTopicItem getDocTopicForString(final DocTopics docTopics, final String element) {
		final boolean localDebug = false;
		String strippedLeafClasName = element;
		final String dollarSign = "$";
		if (strippedLeafClasName.contains(dollarSign)) {
			// ".*$[a-zA-Z]+
			final String anonInnerClassRegExpr = ".*\\$\\D.*";
			if (Pattern.matches(anonInnerClassRegExpr, strippedLeafClasName)) {
				if (localDebug) {
					logger.debug("\t\tfound inner class: " + strippedLeafClasName);
				}

				strippedLeafClasName = strippedLeafClasName.substring(strippedLeafClasName.lastIndexOf('$') + 1, strippedLeafClasName.length());

				if (localDebug) {
					logger.debug("\t\tstripped to name to: " + strippedLeafClasName);
				}
			}
		}

		DocTopicItem docTopicItem = null;
		if (localDebug) {
			logger.debug("\t" + strippedLeafClasName);
		}
		if (Config.getSelectedLanguage().equals(Language.c)) {
			docTopicItem = docTopics.getDocTopicItemForC(strippedLeafClasName);
		} else if (Config.getSelectedLanguage().equals(Language.java)) {
			docTopicItem = docTopics.getDocTopicItemForJava(strippedLeafClasName);
		} else {
			docTopicItem = docTopics.getDocTopicItemForJava(strippedLeafClasName);
		}
		if (localDebug) {
			logger.debug("\t" + "doc-topic: " + docTopicItem);
		}

		return docTopicItem;
	}

	/**
	 * Merge two topics
	 *
	 * @param docTopicItem1
	 *            - first topic
	 * @param docTopicItem2
	 *            - second topic
	 * @return - DocTopicItem with the merged topic
	 */
	public static DocTopicItem mergeDocTopicItems(final DocTopicItem docTopicItem1, final DocTopicItem docTopicItem2) {
		// If one of them is null, return the other
		if (docTopicItem1 == null) {
			return new DocTopicItem(docTopicItem2);
		}
		if (docTopicItem2 == null) {
			return new DocTopicItem(docTopicItem1);
		}

		final DocTopicItem mergedDocTopicItem = new DocTopicItem(docTopicItem1);
		Collections.sort(docTopicItem1.topics, new TopicItemByTopicNumComparator());
		Collections.sort(docTopicItem2.topics, new TopicItemByTopicNumComparator());
		Collections.sort(mergedDocTopicItem.topics, new TopicItemByTopicNumComparator());
		for (int i = 0; i < docTopicItem1.topics.size(); i++) {
			final TopicItem ti1 = docTopicItem1.topics.get(i);
			final TopicItem ti2 = docTopicItem2.topics.get(i);
			final TopicItem mergedTopicItem = mergedDocTopicItem.topics.get(i);
			/*
			 * if (!(ti1.topicNum == ti2.topicNum)) { logger.error("In mergeDocTopicItems, nonmatching docTopicItems"); }
			 */

			logger.trace("ti1.topicNum: " + ti1.topicNum);
			logger.trace("ti2.topicNum: " + ti2.topicNum);
			logger.trace("ti1.proportion: " + ti1.proportion);
			logger.trace("ti2.proportion: " + ti2.proportion);

			assert ti1.topicNum == ti2.topicNum : "In mergeDocTopicItems, nonmatching docTopicItems";
			mergedTopicItem.proportion = (ti1.proportion + ti2.proportion) / 2;

			logger.trace("mergedTopicItem.topicNum: " + mergedTopicItem.topicNum);
			logger.trace("mergedTopicItem.proportion: " + mergedTopicItem.proportion);

		}
		return mergedDocTopicItem;
	}

	public static void printTwoDocTopics(final DocTopicItem docTopicItem, final DocTopicItem docTopicItem2) {

		if (docTopicItem == null) {
			System.out.println(DebugUtil.addMethodInfo(" first arg is null...returning"));
			return;
		}

		if (docTopicItem2 == null) {
			System.out.println(DebugUtil.addMethodInfo(" second arg is null...returning"));
			return;
		}

		Collections.sort(docTopicItem.topics, new TopicItemByTopicNumComparator());
		Collections.sort(docTopicItem2.topics, new TopicItemByTopicNumComparator());

		System.out.println(String.format("%5s%64s%64s\n", "", docTopicItem.source, docTopicItem2.source));

		for (int i = 0; i < docTopicItem.topics.size(); i++) {
			System.out.println(String.format("%32s%32f%32f\n", docTopicItem.topics.get(i).topicNum, docTopicItem.topics.get(i).proportion, docTopicItem2.topics.get(i).proportion));
		}

	}

	public static TopicItem getMatchingTopicItem(final ArrayList<TopicItem> topics, final TopicItem inTopicItem) {
		for (final TopicItem currTopicItem : topics) {
			if (currTopicItem.topicNum == inTopicItem.topicNum) {
				return currTopicItem;
			}
		}
		return null;
	}

	public static void printDocTopicProportionSum(final DocTopicItem docTopicItem) {
		if (docTopicItem == null) {
			logger.debug("cannot sum doc-topic propoertion for null DocTopicItem");
			return;
		}
		double sum = 0;
		for (final TopicItem ti : docTopicItem.topics) {
			sum += ti.proportion;
		}
		logger.debug("doc-topic proportion sum: " + sum);

	}

	public static void setDocTopicForFastClusterForMalletApi(final DocTopics docTopics2, final FastCluster c) {
		if (Config.getSelectedLanguage().equals(Language.java)) {
			c.docTopicItem = docTopics.getDocTopicItemForJava(c.getName());
		}
		if (Config.getSelectedLanguage().equals(Language.c)) {
			c.docTopicItem = docTopics.getDocTopicItemForC(c.getName());
		}

	}

}
