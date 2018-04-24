/**
 *
 */
package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.Trial;
import cc.mallet.pipe.iterator.CsvIterator;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Labeling;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.util.ClassInfo;

public class ClassifierUtils {

	private static Logger logger = LogManager.getLogger(ClassifierUtils.class.getName());

	public static void main(final String[] args) {
		logger.traceEntry();
		logger.traceExit();
	}

	// public static Classifier trainClassifier(final InstanceList trainingInstances) {
	// logger.traceEntry();
	// final ClassifierTrainer<?> trainer = new MaxEntTrainer();
	// logger.traceExit();
	// return trainer.train(trainingInstances);
	// }

	public static Classifier loadClassifier(final File serializedFile) throws FileNotFoundException, IOException, ClassNotFoundException {
		logger.traceEntry();
		// The standard way to save classifiers and Mallet data
		// for repeated use is through Java serialization.
		// Here we load a serialized classifier from a file.

		Classifier classifier;

		final ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serializedFile));
		classifier = (Classifier) ois.readObject();
		ois.close();
		logger.traceExit();
		return classifier;
	}

	public static void saveClassifier(final Classifier classifier, final File serializedFile) throws IOException {
		logger.traceEntry();
		// The standard method for saving classifiers in
		// Mallet is through Java serialization. Here we write the classifier
		// object to the specified file.

		final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedFile));
		oos.writeObject(classifier);
		oos.close();
		logger.traceExit();
	}

	public static void printLabelingsCSV(final Classifier classifier, final File file) throws IOException {
		logger.entry(classifier, file);
		// Create a new iterator that will read raw instance data from the lines
		// of a file.
		// Lines should be formatted as:
		//
		// [name] [label] [data ... ]
		//
		// in this case, "label" is ignored.

		final CsvIterator reader = new CsvIterator(new FileReader(file), "(\\w+)\\s+(\\w+)\\s+(.*)", 3, 2, 1); // (data,label,name)
																												// field
																												// indices

		// Create an iterator that will pass each instance through the same pipe
		// that was used to create the training data for the classifier.
		final java.util.Iterator<Instance> instances = classifier.getInstancePipe().newIteratorFrom(reader);

		// Classifier.classify() returns a Classification object that includes
		// the instance, the classifier, and the classification results (the
		// labeling). Here we only care about the Labeling.
		while (instances.hasNext()) {
			final Labeling labeling = classifier.classify(instances.next()).getLabeling();

			// print the labels with their weights in descending order (ie best
			// first)
			String outputString = "";
			for (int rank = 0; rank < labeling.numLocations(); rank++) {
				outputString += labeling.getLabelAtRank(rank) + ":" + labeling.getValueAtRank(rank) + " ";
			}
			logger.debug(outputString);
		}
		logger.traceExit();
	}

	public static void printLabelingsDir(final Classifier classifier, final File directory) {
		logger.entry(classifier, directory);
		FileIterator f = new FileIterator(directory);
		while (f.hasNext()) {
			logger.debug(f.next().getName());
		}
		// System.exit(0);
		f = new FileIterator(directory);
		final Iterator<Instance> i = classifier.getInstancePipe().newIteratorFrom(f);
		while (i.hasNext()) {
			logger.debug(i.next().getName());
			// logger.debug(i.next().getData());
			final Labeling labeling = classifier.classify(i.next()).getLabeling();
			String outputString = "";
			for (int rank = 0; rank < labeling.numLocations(); rank++) {
				outputString += labeling.getLabelAtRank(rank) + ":" + labeling.getValueAtRank(rank) + " ";
			}
			logger.debug(outputString);
		}
		logger.traceExit();
	}

	/**
	 * @param classifier
	 * @param fileMan
	 * @param directory
	 */
	public static void printLabelingsDirBetter(final Classifier classifier, final FileManager fileMan, final File directory) {
		logger.entry(classifier, fileMan);
		logger.debug("*** Labelings start ***");
		final List<File> fileList = (List<File>) fileMan.getFiles();
		logger.trace("=== Files that were evaluated: ===");
		final DecimalFormat dfFiles = new DecimalFormat("000000");
		int fileCount = 0;
		final ArrayList<String> listedFiles = new ArrayList<>();
		for (final File f : fileList) {
			if (!f.isDirectory()) {
				final String name = f.getAbsolutePath();
				logger.trace(dfFiles.format(fileCount++) + " " + name);
				logger.trace(name);
				listedFiles.add(name);
			}
		}
		Collections.sort(listedFiles);
		logger.trace("=== End of file list ===");
		fileCount = 0;
		FileFilter ff = null;
		if (Config.getSelectedLanguage().equals(Config.Language.c)) {
			// pattern =
			// ".(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)";Pattern.compile(pattern);
			ff = new SuffixFileFilter(new String[] { ".c", ".cpp", "cc", "s", "h", "hpp", "icc", "ia", "tbl", "p" });
		} else if (Config.getSelectedLanguage().equals(Config.Language.java)) {
			ff = new SuffixFileFilter(".java");
		} else {
			System.out.println("No supported language selected - exiting");
			System.exit(-1);
		}

		final FileIterator f = new FileIterator(directory, ff);
		final Iterator<Instance> it = classifier.getInstancePipe().newIteratorFrom(f);
		CodeEntity currentEntity = null;
		final ArrayList<String> instanceNames = new ArrayList<>();
		// final LabelAlphabet lA = classifier.getLabelAlphabet();
		// System.out.println(lA);
		// CodeEntity.setClassNames(classifier.get);
		while (it.hasNext()) {
			final Instance currentInstance = it.next();
			final File instanceFile = new File(currentInstance.getSource().toString());
			if (instanceFile.length() == 0) {
				continue;
			}

			final String instanceName = ((File) currentInstance.getSource()).getAbsolutePath();
			final String fullFileName = currentInstance.getSource().toString();

			if (Config.getSelectedLanguage().equals(Language.java)) { // Show the full canonical name of the class
				final File entityFile = new File(fullFileName);
				FilenameUtils.getBaseName(entityFile.getAbsolutePath());
				final String packageName = ClassInfo.getJavaFilePackage(fullFileName);
				logger.debug("---\n###### Package Name = " + packageName + ", Basename = " + FilenameUtils.getBaseName(entityFile.getAbsolutePath()));
				// System.out.println("###### Canonical Name: " + packageName + "." + FilenameUtils.getBaseName(entityFile.getAbsolutePath()) + " ######");

			}
			instanceNames.add(fullFileName);
			final LinkedHashMap<String, Double> currentHash = new LinkedHashMap<>();
			final String pathString = instanceName.replace(" ", "\\ ");
			logger.debug("*** Current Instance #" + dfFiles.format(fileCount++) + " = " + pathString);

			logger.trace("+++ Current Instance data = " + currentInstance.getData());
			final Labeling labeling = classifier.classify(currentInstance).getLabeling();

			// logger.debug("\nLabels by index:");
			for (int index = 0; index < labeling.numLocations(); index++) {
				final String labelString = labeling.getAlphabet().toArray()[index].toString();
				final Double labelValue = labeling.value(index);
				currentHash.put(labelString, labelValue);
			}
			currentEntity = new CodeEntity(instanceName, currentHash);
			// currentHash.put("nomatch", 0.0);
			final DecimalFormat df = new DecimalFormat("#0.00");
			logger.debug("Best Index = " + labeling.getBestIndex() + ", Best Value = " + df.format(labeling.getBestValue()) + ", Best Label = " + labeling.getBestLabel());

			// String rankingsString = "Labels by rank: ";
			for (int rank = 0; rank < labeling.numLocations(); rank++) {
				// final DecimalFormat df = new DecimalFormat("#0.00");
				currentEntity.getLabelsByRank().put(labeling.getLabelAtRank(rank).toString(), labeling.getValueAtRank(rank));
				// rankingsString += labeling.getLabelAtRank(rank) + ": ";
				// rankingsString += df.format(labeling.getValueAtRank(rank)) + " ";
			}
			logger.debug(currentEntity.getRankinString());
			// String orderString = "Labels in order: ";
			// for (int pos = 0; pos < labeling.numLocations(); pos++) {
			// orderString += labeling.labelAtLocation(pos) + ": ";
			// orderString += df.format(labeling.valueAtLocation(pos)) + " ";
			// }
			// logger.debug(orderString);
			logger.debug(currentEntity.getOrderString());

			if (labeling.getBestValue() < TopLevel.getCurrentConfig().getMatchConfidence()) {
				currentEntity.setNoMatch(true);
				logger.debug("*** NO MATCH! ***");
			}
			// logger.debug("Current Entity:");
			// logger.debug(currentEntity);
			TopLevel.entities.add(currentEntity);
			Clustering.processEntity(currentEntity);
		}
		Collections.sort(instanceNames);

		compareFileLists(listedFiles, instanceNames);
		logger.debug("*** Labelings end ***");
		logger.traceExit();
	}

	public static void compareFileLists(final ArrayList<String> first, final ArrayList<String> second) {
		final ArrayList<String> fF = new ArrayList<>(), sS = new ArrayList<>();
		fF.addAll(first);
		sS.addAll(second);
		for (final String s : fF) {
			if (!sS.contains(s)) {
				logger.debug("Missing file from second: " + s);
			}
		}
		for (final String s : sS) {
			if (!fF.contains(s)) {
				logger.debug("Missing file from first: " + s);
			}
		}
	}

	public static void evaluate(final Classifier classifier, final File file) throws IOException {
		logger.entry(classifier, file);
		// Create an InstanceList that will contain the test data.
		// In order to ensure compatibility, process instances with the pipe
		// used to process the original training instances.

		final InstanceList testInstances = new InstanceList(classifier.getInstancePipe());

		// Create a new iterator that will read raw instance data from
		// the lines of a file.
		// Lines should be formatted as:
		//
		// [name] [label] [data ... ]

		final CsvIterator reader = new CsvIterator(new FileReader(file), "(\\w+)\\s+(\\w+)\\s+(.*)", 3, 2, 1); // (data, label, name) field indices

		// Add all instances loaded by the iterator to our instance list,
		// passing the raw input data through the classifier's original input
		// pipe.

		testInstances.addThruPipe(reader);

		final Trial trial = new Trial(classifier, testInstances);

		// The Trial class implements many standard evaluation
		// metrics. See the JavaDoc API for more details.

		logger.debug("Accuracy: " + trial.getAccuracy());

		// precision, recall, and F1 are calcuated for a specific
		// class, which can be identified by an object (usually
		// a String) or the integer ID of the class

		logger.debug("F1 for class 'good': " + trial.getF1("good"));

		logger.debug("Precision for class '" + classifier.getLabelAlphabet().lookupLabel(1) + "': " + trial.getPrecision(1));
		logger.traceExit();
	}

	// public static Trial testTrainSplit(final InstanceList instances) {
	// logger.entry(instances);
	// final int TRAINING = 0;
	// final int TESTING = 1;
	//
	// // Split the input list into training (90%) and testing (10%) lists.
	// // The division takes place by creating a copy of the list, randomly
	// // shuffling the copy, and then allocating instances to each sub-list
	// // based on the provided proportions.
	//
	// final InstanceList[] instanceLists = instances.split(new Randoms(), new double[] { 0.9, 0.1, 0.0 });
	//
	// // The third position is for the "validation" set, which is a set of
	// // instances not used directly for training, but available for
	// // determining when to stop training and for estimating optimal
	// // settings of nuisance parameters. Most Mallet ClassifierTrainers can
	// // not currently take advantage of validation sets.
	//
	// final Classifier classifier = trainClassifier(instanceLists[TRAINING]);
	// logger.traceExit();
	// return new Trial(classifier, instanceLists[TESTING]);
	// }
}
