package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import cc.mallet.classify.Classifier;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.types.InstanceList;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.util.mallet.Import;

/**
 * @author joshua
 *
 */
public class Classifications {

	private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Classifications.class);
	private Object[] classifierLabels;
	File testDir;

	// private final edu.usc.softarch.arcade.archview.ArchitecturalView
	// currentView = Controller.getCurrentView();

	File topicModelFile;
	File docTopicsFile;
	File topWordsFile;
	boolean checkJavaFiles;
	boolean checkCFiles;
	ArrayList<Pipe> pipeList;
	InstanceList instances;
	Pattern cLangPattern = Pattern.compile("\\.(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)$");
	InstanceList previousInstances;
	Config currentConfig = Controller.getCurrentView().getConfig();
	final int numIterations = currentConfig.getmIterations();
	Import i;

	public Classifications(final File srcDir, final File artifactsDir, final File classifierFile, final FileManager fileMan) {
		logger.entry(srcDir, artifactsDir);
		checkJavaFiles = Config.getSelectedLanguage().equals(Config.Language.java);
		checkCFiles = Config.getSelectedLanguage().equals(Config.Language.c);
		// Begin by importing documents from text to feature sequences
		i = new Import();
		pipeList = i.preparePipeList(checkCFiles, checkJavaFiles);
		instances = new InstanceList(new SerialPipes(pipeList));
		// instances = Import.buildInstances(instances, srcDir, checkCFiles,
		// checkJavaFiles);
		instances = Import.buildInstancesFromFileManager(fileMan, instances);
		// ClassifierUtils.main(null);
		Classifier relaxClassifier = null;
		try {
			relaxClassifier = ClassifierUtils.loadClassifier(classifierFile);
		} catch (ClassNotFoundException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		// ArrayList<Classification> classificationResults;
		// classificationResults = relaxClassifier.classify(instances);
		// System.out.println(classificationResults);

		classifierLabels = relaxClassifier.getLabelAlphabet().toArray();
		Clustering.setClusterLabels(new ArrayList<String>());
		ColorManager.setLabelColors(new HashMap<String, String>());
		ColorManager.setCurrentLabelID(0);
		for (final Object o : classifierLabels) {
			final String labelString = o.toString();
			Clustering.addClusterLabel(labelString);
			ColorManager.addLabel(labelString);
		}
		ColorManager.addLabel("no_match");
		logger.debug("*** Classifier Info ***");
		logger.debug(currentConfig.getRelaxClassifierFileName());
		// Object classes = relaxClassifier.getLabelAlphabet().toArray()[0];
		// logger.debug(classes);
		logger.debug("***");

		final ArrayList<String> classNames = new ArrayList<>();
		for (final Object o : classifierLabels) {
			final String label = o.toString();
			CodeEntity.getClassNames().add(label);
			logger.debug("Label: " + label);
			classNames.add(label);
		}
		CodeEntity.setClassNames(classNames);
		ClassifierUtils.printLabelingsDirBetter(relaxClassifier, fileMan, srcDir);

		logger.traceExit();
	}

	public Object[] getClassifierLabels() {
		return classifierLabels;
	}

	public void setClassifierLabels(final Object[] classifierLabels) {
		this.classifierLabels = classifierLabels;
	}

	// public Classifications(final String srcDirName, final String
	// artifactsDirName) {
	// final File srcDir = FileUtil.checkDir(srcDirName, false, true);
	// final File artifactsDir = FileUtil.checkDir(artifactsDirName, true,
	// false);
	// Classifications(srcDir, artifactsDir);
	// }
}
