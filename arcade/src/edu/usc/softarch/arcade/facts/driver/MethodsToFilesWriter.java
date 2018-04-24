package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.tartarus.snowball.SnowballStemmer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MethodsToFilesWriter {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(MethodsToFilesWriter.class);

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String fileName = args[0];
		final String langKeywordsFilename = args[1];
		final String malletInstancesFilename = args[2];

		final Map<String, String> methodToContentMap = new HashMap<String, String>();

		final Set<String> langKeywords = new HashSet<String>();
		try {
			final BufferedReader in = new BufferedReader(new FileReader(langKeywordsFilename));
			String word = null;
			while ((word = in.readLine()) != null) {
				word = word.trim();
				langKeywords.add(word);
			}
			in.close();
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		extractMethodInfo(fileName, methodToContentMap);

		try {
			final Charset charset = StandardCharsets.UTF_8;
			final Path path = Paths.get(malletInstancesFilename);
			final BufferedWriter out = Files.newBufferedWriter(path, charset);

			for (final Entry<String, String> entry : methodToContentMap.entrySet()) {
				final String methodNameNoSpaces = entry.getKey();
				String methodContentClean = entry.getValue();

				methodContentClean = methodContentClean.replaceAll("[^A-Za-z0-9]", " "); // remove
																							// any
																							// non-alphanumeric
				// characters
				methodContentClean = methodContentClean.replaceAll("\\s+", " "); // replace
				// multiple
				// white
				// space
				// with
				// single
				// space
				final String[] methodContentCleanArray = methodContentClean.split(" ");
				methodContentClean = "";
				for (final String word : methodContentCleanArray) {
					if (!langKeywords.contains(word)) { // add word if it is not
						// a PL keyword
						methodContentClean += " " + word;
					}
				}
				final String[] methodContentCamelCaseSplitArray = methodContentClean.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"); // split
				// over
				// camel
				// case
				methodContentClean = StringUtils.join(methodContentCamelCaseSplitArray, " ");
				final String[] methodContentWordSplitArray = methodContentClean.split(" ");

				String methodContentStemmed = "";

				for (final String word : methodContentWordSplitArray) {
					final Class<?> stemClass = Class.forName("org.tartarus.snowball.ext.porterStemmer");
					final SnowballStemmer stemmer = (SnowballStemmer) stemClass.newInstance();
					stemmer.setCurrent(word);
					stemmer.stem();
					final String stemmedWord = stemmer.getCurrent();
					if (stemmedWord.length() == 1) { // do not add words of
						// length 1
						continue;
					}
					methodContentStemmed += " " + stemmedWord;
				}

				final String line = methodNameNoSpaces + " X " + methodContentStemmed;
				logger.debug(line);
				out.write(line);
				out.newLine();

			}
		} catch (final ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final InstantiationException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IllegalAccessException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void extractMethodInfo(final String fileName, final Map<String, String> methodToContentMap) {
		try {

			final File fXmlFile = new File(fileName);
			final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			final Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			logger.debug("Root element :" + doc.getDocumentElement().getNodeName());
			final NodeList nList = doc.getElementsByTagName("class");
			logger.debug("-----------------------");

			int classCounter = 0;
			int methodCounter = 0;
			for (int temp = 0; temp < nList.getLength(); temp++) {

				final Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					final Element classElement = (Element) nNode;

					// logger.debug(temp + " - " + eElement.getTextContent());

					if (getTagValue("name", classElement) == null) {
						continue;
					}

					final String containerName = getContainerNameOfClassElement(classElement);

					// String containerName =
					// getPackageNameOfClassElement(classElement);

					final String className = getTagValue("name", classElement);

					logger.debug(classCounter + " - container name: " + containerName);
					logger.debug(classCounter + " - class name: " + className);

					final NodeList constructorList = classElement.getElementsByTagName("constructor");

					for (int fIndex = 0; fIndex < constructorList.getLength(); fIndex++) {
						final Node constructorNode = constructorList.item(fIndex);
						if (constructorNode.getNodeType() == Node.ELEMENT_NODE) {
							final Element constructorElement = (Element) constructorNode;
							final Element nameElement = getChildElementByTagName("name", constructorElement);

							final Element paramListElement = getChildElementByTagName("parameter_list", constructorElement);

							final String methodNameNoSpaces = prepareMethodNameNoSpaces(methodCounter, nameElement, paramListElement);

							storeFunctionInfo(methodToContentMap, containerName, className, constructorElement, methodNameNoSpaces);
						}
						methodCounter++;

					}

					final NodeList functionList = classElement.getElementsByTagName("function");

					for (int fIndex = 0; fIndex < functionList.getLength(); fIndex++) {
						final Node functionNode = functionList.item(fIndex);
						if (functionNode.getNodeType() == Node.ELEMENT_NODE) {
							final Element functionElement = (Element) functionNode;
							final Element nameElement = getChildElementByTagName("name", functionElement);

							final Element paramListElement = getChildElementByTagName("parameter_list", functionElement);

							final String methodNameNoSpaces = prepareMethodNameNoSpaces(methodCounter, nameElement, paramListElement);

							storeFunctionInfo(methodToContentMap, containerName, className, functionElement, methodNameNoSpaces);
						}
						methodCounter++;
					}

					classCounter++;

					/*
					 * if (getTagValue("name", eElement) == null) {
					 * logger.debug(temp + "block: " + getTagValue("block",
					 * eElement)); }
					 */

				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static String prepareMethodNameNoSpaces(final int methodCounter, final Element nameElement, final Element paramListElement) {
		final String methodName = getElementValue(nameElement) + paramListElement.getTextContent().trim().replaceAll(" +", " ").replaceAll("[\n\t]+", "");
		final String methodNameNoSpaces = methodName.replaceAll("\\s", "_");

		logger.debug("\t" + methodCounter + " - " + methodName);
		logger.debug("\t" + methodCounter + " - " + methodNameNoSpaces);
		return methodNameNoSpaces;
	}

	private static void storeFunctionInfo(final Map<String, String> methodToContentMap, final String containerName, final String className, final Element functionElement,
			final String methodNameNoSpaces) {
		final String methodContent = functionElement.getTextContent();
		logger.debug("\t\t" + methodContent);

		final String fullMethodNameNoSpaces = containerName + "#" + className + "#" + methodNameNoSpaces;

		methodToContentMap.put(fullMethodNameNoSpaces, methodContent);
	}

	private static String getElementValue(final Element element) {
		return element.getChildNodes().item(0).getNodeValue();
	}

	private static String getTagValue(final String tag, final Element element) {
		final NodeList nlList = element.getElementsByTagName(tag).item(0).getChildNodes();

		final Node nValue = nlList.item(0);

		return nValue.getNodeValue();
	}

	private static String getPackageNameFromPackageElement(final Element packageElement) {
		final NodeList nodeList = packageElement.getElementsByTagName("name");
		String packageName = "";

		for (int i = 0; i < nodeList.getLength(); i++) {
			final Node currNode = nodeList.item(i);
			packageName += currNode.getFirstChild().getNodeValue();
			if (i + 1 < nodeList.getLength()) {
				packageName += ".";
			}
		}

		return packageName;
	}

	private static Element getChildElementByTagName(final String tag, final Element element) {
		final NodeList nodeList = element.getChildNodes();

		for (int i = 0; i < nodeList.getLength(); i++) {
			final Node node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				final Element childElement = (Element) node;
				if (childElement.getNodeName().equals(tag)) {
					return childElement;
				}
			}
		}

		return null;
	}

	private static String getContainerNameOfClassElement(final Element classElement) {

		String containerName = "";

		Element currElement = null;
		Node currNode = classElement.getPreviousSibling();
		if (currNode == null) {
			currNode = classElement.getParentNode();
			if (currNode.getNodeType() == Node.ELEMENT_NODE) {
				currElement = (Element) currNode;
				containerName = updateContainerName(containerName, currElement);
			}
		}

		while (currNode.getNodeType() != Node.ELEMENT_NODE) {
			final Node prevNode = currNode;
			currNode = currNode.getPreviousSibling();
			if (currNode == null) {
				currNode = prevNode.getParentNode();
				if (currNode.getNodeType() == Node.ELEMENT_NODE) {
					currElement = (Element) currNode;
					containerName = updateContainerName(containerName, currElement);
				}
			}
		}
		currElement = (Element) currNode;

		while (!currElement.getNodeName().equals("package")) {
			final Node prevNode = currNode;
			currNode = currNode.getPreviousSibling();
			if (currNode == null) {
				currNode = prevNode.getParentNode();
				if (currNode.getNodeType() == Node.ELEMENT_NODE) {
					currElement = (Element) currNode;
					containerName = updateContainerName(containerName, currElement);
				}
			} else {
				if (currNode.getNodeType() == Node.ELEMENT_NODE) {
					currElement = (Element) currNode;
				}
			}
		}

		final Element packageElement = currElement;
		final String packageName = getPackageNameFromPackageElement(packageElement);

		if (containerName != "") {
			containerName = packageName + "." + containerName;
		} else {
			containerName = packageName;
		}

		return containerName;

	}

	private static String updateContainerName(String containerName, final Element currElement) {
		if (currElement.getNodeName().equals("class")) {
			if (containerName == "") {
				containerName = getTagValue("name", currElement);
			} else {
				containerName = getTagValue("name", currElement) + "$" + containerName;
			}
		}
		return containerName;
	}

}
