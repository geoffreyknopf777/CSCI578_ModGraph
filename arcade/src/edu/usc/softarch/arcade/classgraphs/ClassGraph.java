package edu.usc.softarch.arcade.classgraphs;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import soot.SootClass;
import edu.usc.softarch.arcade.config.Config;

/**
 * @author joshua
 *
 */
public class ClassGraph implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -4718039019698373111L;
	private final HashSet<SootClassEdge> edges = new HashSet<SootClassEdge>();

	public ClassGraph() {

	}

	public void generateRsf() {
		try {
			final Writer out = new BufferedWriter(new FileWriter(Config.getClassGraphRsfFilename()));

			for (final SootClassEdge edge : edges) {
				out.write(edge.toRsf() + "\n");
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public void serialize(final String filename) throws IOException {
		// Write to disk with FileOutputStream
		final FileOutputStream f_out = new FileOutputStream(filename);

		// Write object with ObjectOutputStream
		final ObjectOutputStream obj_out = new ObjectOutputStream(f_out);

		// Write object out to disk
		obj_out.writeObject(this);

		obj_out.close();
	}

	public ArrayList<SootClass> getNodes() {
		final ArrayList<SootClass> cgNodes = new ArrayList<SootClass>();
		for (final SootClassEdge e : edges) {
			if (!cgNodes.contains(e.getTgt())) {
				cgNodes.add(e.getTgt());
			}
			if (!cgNodes.contains(e.getSrc())) {
				cgNodes.add(e.getSrc());
			}
		}
		return cgNodes;

	}

	protected boolean hasClass(final ArrayList<SootClass> cgNodes, final SootClass inClass) {
		boolean hasClass = false;
		for (final SootClass c : cgNodes) {
			if (inClass.toString().equals(c.toString())) {
				hasClass = true;
			}
		}
		return hasClass;
	}

	public ArrayList<SootClass> getCallerClasses(final SootClass c) {
		final ArrayList<SootClass> callerClasses = new ArrayList<SootClass>();
		for (final SootClassEdge e : edges) {
			if (e.getTgt().toString().equals(c.toString())) {
				callerClasses.add(e.getSrc());
			}
		}

		return callerClasses;
	}

	public ArrayList<SootClass> getCalleeClasses(final SootClass c) {
		final ArrayList<SootClass> calleeClasses = new ArrayList<SootClass>();
		for (final SootClassEdge e : edges) {
			if (e.getSrc().toString().equals(c.toString())) {
				calleeClasses.add(e.getTgt());
			}
		}

		return calleeClasses;
	}

	public void addEdge(final SootClass src, final SootClass tgt, final String type) {
		edges.add(new SootClassEdge(src, tgt, type));
	}

	public void addEdge(final SootClassEdge e) {
		edges.add(e);
	}

	public void removeEdge(final SootClassEdge e) {
		edges.remove(e);
	}

	public void removeEdge(final SootClass src, final SootClass tgt, final String type) {
		edges.remove(new SootClassEdge(src, tgt, type));
	}

	public boolean containsEdge(final SootClass src, final SootClass tgt, final String type) {
		/*
		 * Iterator<ClassEdge> iter = edges.iterator(); while(iter.hasNext()) {
		 * ClassEdge e = (ClassEdge)iter.next(); if (e.equals(new
		 * ClassEdge(src,tgt))) { return true; } } return false;
		 */
		return edges.contains(new SootClassEdge(src, tgt, type));
	}

	public boolean containsEdge(final SootClassEdge e) {
		return edges.contains(e);
	}

	@Override
	public String toString() {
		final Iterator<SootClassEdge> iter = edges.iterator();
		String str = "";

		while (iter.hasNext()) {
			final SootClassEdge e = iter.next();
			str += e.toString();
			if (iter.hasNext()) {
				str += ",";
			}
		}

		return str;
	}

	public String toStringWithArchElemType() {
		final Iterator<SootClassEdge> iter = edges.iterator();
		String str = "";

		while (iter.hasNext()) {
			final SootClassEdge e = iter.next();
			str += e.toStringWithArchElemType();
			if (iter.hasNext()) {
				str += ",";
			}
		}

		return str;
	}

	public int size() {
		return edges.size();
	}

	public void writeDotFile(final String filename) throws FileNotFoundException, UnsupportedEncodingException {
		final File f = new File(filename);
		if (f.getParentFile() != null) {
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdirs();
			}
		}
		final FileOutputStream fos = new FileOutputStream(f);
		final OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		final PrintWriter out = new PrintWriter(osw);

		final Iterator<SootClassEdge> iter = edges.iterator();
		// String str = "";

		out.println("digraph G {");

		while (iter.hasNext()) {
			final SootClassEdge e = iter.next();
			out.println(e.toDotString());
		}

		out.println("}");

		out.close();
	}

	public void writeDotFileWithArchElementType(final String filename) throws FileNotFoundException, UnsupportedEncodingException {
		final File f = new File(filename);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		final FileOutputStream fos = new FileOutputStream(f);
		final OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
		final PrintWriter out = new PrintWriter(osw);

		final Iterator<SootClassEdge> iter = edges.iterator();
		// String str = "";

		out.println("digraph G {");

		while (iter.hasNext()) {
			final SootClassEdge e = iter.next();
			out.println(e.toDotStringWithArchElemType());
		}

		out.println("}");

		out.close();
	}

	public void addElementTypes(final HashMap<String, String> map) {
		final Iterator<SootClassEdge> iter = edges.iterator();

		System.out.println("Current Map: " + map);
		System.out.println("Printing class edges with arch element type...");
		while (iter.hasNext()) {

			final SootClassEdge e = iter.next();

			final String srcStr = e.getSrc().getName();
			final String tgtStr = e.getTgt().getName();
			final String srcType = map.get(srcStr);
			final String tgtType = map.get(tgtStr);

			if (srcType.equals("p")) {
				e.srcType = ArchElemType.proc;
			} else if (srcType.equals("d")) {
				e.srcType = ArchElemType.data;
			} else if (srcType.equals("c")) {
				e.srcType = ArchElemType.conn;
			}

			if (tgtType.equals("p")) {
				e.tgtType = ArchElemType.proc;
			} else if (tgtType.equals("d")) {
				e.tgtType = ArchElemType.data;
			} else if (tgtType.equals("c")) {
				e.tgtType = ArchElemType.conn;
			}

			System.out.print("(" + srcStr + ":" + srcType + ",");
			System.out.print(tgtStr + ":" + tgtType + ")");
		}
		System.out.println();
	}

	public void writeXMLClassGraph() throws ParserConfigurationException, TransformerException {
		final DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// classgraph elements
		final Document doc = docBuilder.newDocument();
		final Element rootElement = doc.createElement("ClassGraph");
		doc.appendChild(rootElement);

		// classedge elements
		for (final SootClassEdge e : edges) {
			final Element ce = doc.createElement("ClassEdge");
			rootElement.appendChild(ce);
			final Element src = doc.createElement("src");
			src.appendChild(doc.createTextNode(e.src.toString()));
			final Element tgt = doc.createElement("tgt");
			tgt.appendChild(doc.createTextNode(e.tgt.toString()));
			ce.appendChild(src);
			ce.appendChild(tgt);
		}

		// write the content into xml file
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		final Transformer transformer = transformerFactory.newTransformer();
		final DOMSource source = new DOMSource(doc);
		final StreamResult result = new StreamResult(new File(Config.getXMLClassGraphFilename()));
		transformer.transform(source, result);

		System.out.println("In " + Thread.currentThread().getStackTrace()[1].getClassName() + ". " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", Wrote "
				+ Config.getXMLClassGraphFilename());

	}

	public HashSet<SootClassEdge> getEdges() {
		return edges;
	}

}
