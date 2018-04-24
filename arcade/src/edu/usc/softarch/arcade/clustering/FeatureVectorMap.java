package edu.usc.softarch.arcade.clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.usc.softarch.arcade.classgraphs.ClassGraph;
import edu.usc.softarch.arcade.classgraphs.SootClassEdge;
import edu.usc.softarch.arcade.classgraphs.StringEdge;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.functiongraph.StringTypedEdge;
import edu.usc.softarch.arcade.functiongraph.TypedEdgeGraph;
import soot.SootClass;

/**
 * @author joshua
 *
 */
public class FeatureVectorMap {

	public HashMap<SootClass, FeatureVector> sc_fv_map = new HashMap<SootClass, FeatureVector>();
	public HashMap<String, FeatureVector> featureVectorNameToFeatureVectorMap = new HashMap<String, FeatureVector>(1500);
	public HashMap<String, BitSet> nameToFeatureSetMap = new HashMap<String, BitSet>(1500);

	boolean DEBUG = false;
	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(FeatureVectorMap.class);
	private ArrayList<String> endNodesListWithNoDupes;
	private HashSet<String> startNodesSet;
	private Set<String> allNodesSet;

	public void serializeAsFastFeatureVectors() {
		logger.traceEntry();
		final FastFeatureVectors ffv = convertToFastFeatureVectors();

		ObjectOutput out;
		try {
			out = new ObjectOutputStream(new FileOutputStream(Config.getFastFeatureVectorsFilename()));
			out.writeObject(ffv);
			out.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logger.traceExit();
	}

	public FastFeatureVectors convertToFastFeatureVectors() {
		logger.traceEntry();
		logger.traceExit();
		return new FastFeatureVectors(new ArrayList<String>(allNodesSet), nameToFeatureSetMap, endNodesListWithNoDupes);
	}

	public void serializeNamesInFeatureSet() {
		logger.traceEntry();
		// Serialize to a file
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(new FileOutputStream(Config.getNamesInFeatureSetFilename()));
			out.writeObject(endNodesListWithNoDupes);
			out.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logger.traceExit();
	}

	public void serializeNameToBitSetMap() {
		logger.traceEntry();
		// Serialize to a file
		ObjectOutput out;
		try {
			out = new ObjectOutputStream(new FileOutputStream(Config.getNameToFeatureSetMapFilename()));
			out.writeObject(nameToFeatureSetMap);
			out.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		logger.traceExit();
	}

	public FeatureVectorMap(final HashMap<SootClass, FeatureVector> vecMap) {
		logger.entry(vecMap);
		sc_fv_map = vecMap;
		logger.traceExit();
	}

	public FeatureVectorMap() {
		logger.traceEntry();
		initializeMaps();
		logger.traceExit();
	}

	public FeatureVectorMap(final ClassGraph clg) {
		logger.entry(clg);
		constructFeatureVectorMapFromClassGraph(clg);
		logger.traceExit();
	}

	public FeatureVectorMap(final TypedEdgeGraph typedEdgeGraph) {
		logger.entry(typedEdgeGraph);
		constructFeatureVectorMapFromTypedEdgeGraph(typedEdgeGraph);
		logger.traceExit();
	}

	private void constructFeatureVectorMapFromTypedEdgeGraph(final TypedEdgeGraph functionGraph) {
		logger.entry(functionGraph);

		final HashSet<StringTypedEdge> edges = functionGraph.getEdges();

		final List<String> arcTypesList = Lists.transform(new ArrayList<StringTypedEdge>(edges), edge -> edge.arcTypeStr);

		final HashSet<String> arcTypesSet = Sets.newHashSet(arcTypesList);

		final List<String> startNodesList = Lists.transform(new ArrayList<StringTypedEdge>(edges), edge -> edge.srcStr);

		startNodesSet = Sets.newHashSet(startNodesList);

		final List<String> endNodesList = Lists.transform(new ArrayList<StringTypedEdge>(edges), edge -> edge.tgtStr);
		final TreeSet<String> endNodesSet = Sets.newTreeSet(endNodesList);
		endNodesListWithNoDupes = Lists.newArrayList(endNodesSet);

		final List<String> allNodesList = new ArrayList<String>(startNodesList);
		allNodesList.addAll(endNodesListWithNoDupes);

		allNodesSet = new HashSet<String>(allNodesList);

		final Set<String> nonStartNodes = new HashSet<String>(allNodesSet);
		nonStartNodes.removeAll(startNodesSet);

		int totalTrueBits = 0;
		for (final String source : allNodesSet) {
			// FeatureVector vec = new FeatureVector();
			// vec.name = source.toString();
			final BitSet featureSet = new BitSet(endNodesListWithNoDupes.size());
			for (final String arcType : arcTypesSet) {
				int bitIndex = 0;
				for (final String target : endNodesListWithNoDupes) {
					if (functionGraph.containsEdge(arcType, source, target)) {
						// logger.trace(arcType + ", " + source + ", " + target
						// + ": true");
						// vec.add(new Feature(new StringEdge(source,target),
						// 1));
						featureSet.set(bitIndex, true);
					} /*
						 * else { //logger.trace(arcType + ", " + source + ", " + target + ": false"); //vec.add(new Feature(new StringEdge(source,target), 0)); featureSet.set(bitIndex,false); }
						 */
					bitIndex++;
				}
			}

			logger.trace(featureSet);
			totalTrueBits += featureSet.cardinality();

			/*
			 * if (featureSet.size() != endNodesListWithNoDupes.size()) { logger. error("feature set and nodes list are not equal in size: "); logger.error("feature set size: " + featureSet.size()); logger.error("end nodes list without duplicates: " + endNodesListWithNoDupes.size());
			 * System.err.println( "feature set and nodes list are not equal in size - see log for more details" ); }
			 */

			nameToFeatureSetMap.put(source, featureSet);

			/*
			 * if (measureMemoryUsage) { double vecSize = 0; try { vecSize = ObjectSizer.sizeOf(vec).length; } catch (IOException e) { e.printStackTrace();System.exit(-1); } double vecSizeInMB = vecSize/(1024*1024); logger.trace("vec size in KB: " + vecSize); logger.trace("vec size in KB: " +
			 * vecSizeInMB); }
			 */

			// featureVectorNameToFeatureVectorMap.put(source, vec);

		}

		logger.trace("total true bits among feature sets: " + totalTrueBits);

		final HashSet<List<String>> featureSetEdges = new HashSet<List<String>>();
		logger.trace("Printing edges represented by feature sets...");
		for (final String source : startNodesSet) {
			final BitSet featureSet = nameToFeatureSetMap.get(source);
			for (int i = 0; i < featureSet.size(); i++) {
				if (featureSet.get(i)) {
					final String target = endNodesListWithNoDupes.get(i);
					logger.trace(source + " " + target);
					featureSetEdges.add(Lists.newArrayList(source, target));
				}
			}
		}

		if (RsfReader.untypedEdgesSet != null) {
			final Set<List<String>> intersectionSet = Sets.intersection(featureSetEdges, RsfReader.untypedEdgesSet);
			logger.trace("Printing intersection of rsf reader untyped edges set and feature set edges...");
			logger.trace("intersection set size: " + intersectionSet.size());
			logger.trace(Joiner.on("\n").join(intersectionSet));

			final Set<List<String>> differenceSet = Sets.difference(featureSetEdges, RsfReader.untypedEdgesSet);
			logger.trace("Printing difference of rsf reader untyped edges set and feature set edges...");
			logger.trace("difference set size: " + differenceSet.size());
			logger.trace(Joiner.on("\n").join(differenceSet));
		}

		/*
		 * double mapSize = 0; try { mapSize = ObjectSizer.sizeOf(featureVectorNameToFeatureVectorMap).length; } catch (IOException e) { e.printStackTrace();System.exit(-1); }
		 *
		 * double mapSizeInMB = mapSize/(1024*1024);
		 *
		 * logger.trace("map size in MB: " + mapSizeInMB);
		 */
		// logger.trace("Printing feature vector name to feature vector hash map...");
		// logger.trace("map size: " +
		// featureVectorNameToFeatureVectorMap.size());
		// logger.trace(Joiner.on("\n").withKeyValueSeparator("->").join(featureVectorNameToFeatureVectorMap));
		logger.traceExit();
	}

	private void initializeMaps() {
		logger.traceEntry();
		sc_fv_map = new HashMap<SootClass, FeatureVector>();
		featureVectorNameToFeatureVectorMap = new HashMap<String, FeatureVector>();
		logger.traceExit();
	}

	public void writeXMLFeatureVectorMapUsingFunctionDepEdges() throws TransformerException, ParserConfigurationException {
		logger.traceEntry();
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// classgraph elements
		final Document doc = docBuilder.newDocument();
		final Element rootElement = doc.createElement("FeatureVectorMap");
		doc.appendChild(rootElement);

		// classedge elements
		logger.trace("Printing out feature vector map...");
		for (final String source : allNodesSet) {
			final Element fvElem = doc.createElement("FeatureVector");
			rootElement.appendChild(fvElem);

			// set attribute to staff element
			final Attr attr = doc.createAttribute("name");
			attr.setValue(source);
			fvElem.setAttributeNode(attr);

			rootElement.appendChild(fvElem);

			final BitSet featureSet = nameToFeatureSetMap.get(source);
			for (int i = 0; i < endNodesListWithNoDupes.size(); i++) {
				final String target = endNodesListWithNoDupes.get(i);

				final Element fElem = doc.createElement("Feature");
				fvElem.appendChild(fElem);
				final Element ce = doc.createElement("ClassEdge");
				fElem.appendChild(ce);

				final Element src = doc.createElement("src");
				src.appendChild(doc.createTextNode(source));

				final Element tgt = doc.createElement("tgt");
				tgt.appendChild(doc.createTextNode(target));

				ce.appendChild(src);
				ce.appendChild(tgt);

				final Element valueElem = doc.createElement("value");
				fElem.appendChild(valueElem);
				if (featureSet.get(i)) {
					valueElem.appendChild(doc.createTextNode("1"));
				} else {
					valueElem.appendChild(doc.createTextNode("0"));
				}
			}

		}

		// write the content into xml file
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(new File(Config.getXMLFeatureVectorMapFilename()));
		transformer.transform(source, result);

		logger.trace("In " + Thread.currentThread().getStackTrace()[1].getClassName() + ". " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", Wrote "
				+ Config.getXMLFeatureVectorMapFilename());
		logger.traceExit();

	}

	public void writeXMLFeatureVectorMapUsingSootClassEdges() throws TransformerException, ParserConfigurationException {
		logger.traceEntry();
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// classgraph elements
		final Document doc = docBuilder.newDocument();
		final Element rootElement = doc.createElement("FeatureVectorMap");
		doc.appendChild(rootElement);

		// classedge elements
		logger.trace("Printing out feature vector map...");
		for (final FeatureVector fv : sc_fv_map.values()) {
			logger.trace(fv);
			final Element fvElem = doc.createElement("FeatureVector");
			rootElement.appendChild(fvElem);

			// set attribute to staff element
			final Attr attr = doc.createAttribute("name");
			attr.setValue(fv.name);
			fvElem.setAttributeNode(attr);

			rootElement.appendChild(fvElem);
			for (final Feature f : fv) {
				final Element fElem = doc.createElement("Feature");
				fvElem.appendChild(fElem);
				final Element ce = doc.createElement("ClassEdge");
				fElem.appendChild(ce);
				final Element src = doc.createElement("src");

				SootClassEdge fSootEdge = null;

				if (f.edge instanceof SootClassEdge) {
					fSootEdge = (SootClassEdge) f.edge;
				}
				if (fSootEdge != null) {
					src.appendChild(doc.createTextNode(fSootEdge.src.toString()));
				} else {
					src.appendChild(doc.createTextNode(f.edge.srcStr));
				}

				final Element tgt = doc.createElement("tgt");

				if (fSootEdge != null) {
					tgt.appendChild(doc.createTextNode(fSootEdge.tgt.toString()));
				} else {
					tgt.appendChild(doc.createTextNode(f.edge.tgtStr));
				}

				final Element type = doc.createElement("type");
				type.appendChild(doc.createTextNode(fSootEdge.getType()));

				ce.appendChild(src);
				ce.appendChild(tgt);
				ce.appendChild(type);

				final Element valueElem = doc.createElement("value");
				fElem.appendChild(valueElem);
				if (f.value == 1) {
					valueElem.appendChild(doc.createTextNode("1"));
				} else if (f.value == 0) {
					valueElem.appendChild(doc.createTextNode("0"));
				}
			}

		}

		// write the content into xml file
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(new File(Config.getXMLFeatureVectorMapFilename()));
		transformer.transform(source, result);

		logger.trace("In " + Thread.currentThread().getStackTrace()[1].getClassName() + ". " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", Wrote "
				+ Config.getXMLFeatureVectorMapFilename());
		logger.traceExit();

	}

	public void constructFeatureVectorMapFromClassGraph(final ClassGraph clg) {
		logger.entry(clg);
		for (final SootClass caller : clg.getNodes()) {
			final FeatureVector vec = new FeatureVector();
			vec.name = caller.toString();
			for (final SootClass c : clg.getNodes()) {
				SootClassEdge currEdge = null;
				for (final SootClassEdge edge : clg.getEdges()) {
					currEdge = edge;
					if (edge.getSrc().getName().trim().equals(c.getName().trim())) {
						vec.add(new Feature(new SootClassEdge(edge), 1));
					}
				}
				vec.add(new Feature(new SootClassEdge(currEdge), 0));
			}
			sc_fv_map.put(caller, vec);
		}
		logger.traceExit();
	}

	public void loadClassGraphBasedXMLFeatureVectorMap() throws ParserConfigurationException, SAXException, IOException {
		logger.traceEntry();
		final File fXmlFile = new File(Config.getXMLFeatureVectorMapFilename());
		final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		final Document doc = dBuilder.parse(fXmlFile);
		doc.getDocumentElement().normalize();

		if (DEBUG) {
			logger.trace("Root element :" + doc.getDocumentElement().getNodeName());
		}
		final NodeList fvList = doc.getElementsByTagName("FeatureVector");
		if (DEBUG) {
			logger.trace("----------------------- size: " + fvList.getLength());
		}

		for (int i = 0; i < fvList.getLength(); i++) {
			final FeatureVector fv = new FeatureVector();
			final Node fvNode = fvList.item(i);
			if (fvNode.getNodeType() == Node.ELEMENT_NODE) {
				final Element fvElem = (Element) fvNode;
				fv.name = fvElem.getAttribute("name");
				final NodeList fList = fvElem.getElementsByTagName("Feature");
				if (DEBUG) {
					logger.trace("\t" + fvNode.getNodeName() + "");
					logger.trace("\t----------------------- size:" + fList.getLength() + ", name: " + fvElem.getAttribute("name"));
				}

				for (int j = 0; j < fList.getLength(); j++) {
					final Feature f = new Feature();
					final Node fNode = fList.item(j);
					// logger.trace("\t\t" + j);
					if (fNode.getNodeType() == Node.ELEMENT_NODE) {
						obtainFeatureData(f, fNode);
						fv.add(f);
					}
				}
				featureVectorNameToFeatureVectorMap.put(fv.name, fv);
			} // end if
		} // end outer for loop on FeatureVectors
		if (DEBUG) {
			logger.trace("Pretty printing the name_fv_map:");
			prettyPrintHashMap(featureVectorNameToFeatureVectorMap);
			logger.trace("Printing name_fv_map: " + featureVectorNameToFeatureVectorMap);
		}
		logger.traceExit();
	}

	private void prettyPrintHashMap(final HashMap<String, FeatureVector> map) {
		logger.entry(map);
		final Iterator<?> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			final Map.Entry<?, ?> pair = (Map.Entry<?, ?>) iter.next();
			logger.trace(pair);
		}
		logger.traceExit();
	}

	private void obtainFeatureData(final Feature f, final Node fNode) {
		logger.entry(f, fNode);
		if (DEBUG) {
			logger.trace("\t\t" + fNode.getNodeName());
			logger.trace("\t\t-----------------------");
		}

		final Element fElement = (Element) fNode;

		// logger.trace("\t\t\tClassEdge : " +
		// getNode("ClassEdge",fElement).getNodeName());

		if (DEBUG) {
			logger.trace("\t\tvalue : " + getTagValue("value", fElement));
		}

		if (getTagValue("value", fElement).equals("0")) {
			f.value = 0;
		} else {
			f.value = 1;
		}

		final NodeList fChildren = fElement.getElementsByTagName("ClassEdge");
		for (int k = 0; k < fChildren.getLength(); k++) {
			final Node childNode = fChildren.item(k);
			final Element childElem = (Element) childNode;
			if (DEBUG) {
				logger.trace("\t\t\tSource : " + getTagValue("src", childElem));
				logger.trace("\t\t\tTarget : " + getTagValue("tgt", childElem));
			}

			final StringEdge edge = new StringEdge(getTagValue("src", childElem), getTagValue("tgt", childElem));
			f.edge = edge;

		}
		logger.traceExit();
	}

	// private static Node getNode(String sTag, Element eElement) {
	// NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
	// .getChildNodes();
	// Node nValue = nlList.item(0);
	//
	// return nValue;
	// }

	private static String getTagValue(final String sTag, final Element eElement) {
		logger.entry(sTag, eElement);
		final NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
		final Node nValue = nlList.item(0);
		logger.traceExit();
		return nValue.getNodeValue();

	}

	public void loadFunctionDepGraphBasedXMLFeatureVectorMap() {
		logger.traceEntry();
		logger.traceExit();
	}

}