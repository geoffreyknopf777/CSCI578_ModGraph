package edu.usc.softarch.arcade.weka;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.datatypes.Proj;

/**
 * @author joshua
 *
 */
public class AddTargetClassToArff {

	public static void main(final String[] args) {
		String filename = "";

		try {
			if (Config.proj.equals(Proj.LlamaChat)) {
				filename = "/Users/joshuaga/Documents/workspace/MyExtractors/datasets/LlamaChat/" + Config.llamaChatStr + "_withFieldAccessInfo.arff";
				addTargetAttributes(filename, ClassValueMap.LlamaChatMap);
			} else if (Config.proj.equals(Proj.FreeCS)) {
				filename = "/Users/joshuaga/Documents/workspace/MyExtractors/datasets/freecs/" + Config.freecsStr + "_withFieldAccessInfo.arff";
				addTargetAttributes(filename, ClassValueMap.freecsMap);
			} else {
				System.out.println("Cannot identify current project..exiting");
				System.exit(1);
			}

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void addTargetAttributes(final String filename, final HashMap<String, String> map) throws IOException {
		final FileReader fr = new FileReader(filename);
		final Instances instances = new Instances(fr);
		final FastVector attVals = new FastVector();
		attVals.addElement("p");
		attVals.addElement("d");
		attVals.addElement("c");
		instances.insertAttributeAt(new Attribute("class", attVals), instances.numAttributes());

		final Iterator<?> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			final Map.Entry pairs = (Map.Entry) iter.next();
			final Instance instance = findMatchingInstance(instances, (String) pairs.getKey());
			if (instance == null) {
				System.out.println("Cannot find instance: " + pairs.getKey());
				System.exit(1);
			}
			instance.setValue(instances.attribute("class"), (String) pairs.getValue());
		}

		final ArffSaver saver = new ArffSaver();
		saver.setInstances(instances);
		try {
			final String[] splitFileName = filename.split("\\.");
			final String fullFileName = splitFileName[0] + "_withTargetClasses.arff";
			saver.setFile(new File(fullFileName));
			saver.setDestination(new File(fullFileName)); // **not**
			// necessary
			// in 3.5.4 and
			// later
			saver.writeBatch();
			System.out.println("Wrote file: " + fullFileName);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static Instance findMatchingInstance(final Instances data, final String className) {
		final Enumeration<?> instances = data.enumerateInstances();
		final boolean DEBUG_findMatchningInstance = false;
		final Attribute name = data.attribute("name");
		while (instances.hasMoreElements()) {
			final Instance instance = (Instance) instances.nextElement();
			final String instanceName = instance.stringValue(name);
			if (DEBUG_findMatchningInstance) {
				System.out.println("Comparing " + "'" + instanceName + "' to '" + className + "'");
			}
			if (instanceName.equals(className)) {
				return instance;
			}
		}
		return null;
	}
}
