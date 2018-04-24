package edu.usc.softarch.arcade.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;

import com.thoughtworks.xstream.XStream;

import edu.usc.softarch.arcade.classgraphs.ClassGraphTransformer;
import edu.usc.softarch.arcade.clustering.ClusteringAlgorithmType;
import edu.usc.softarch.arcade.config.datatypes.Proj;
import edu.usc.softarch.arcade.config.datatypes.RunType;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.topics.TopicModelExtractionMethod;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.weka.ClassValueMap;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

/**
 * @author joshua, additions by Daniel
 *
 */
public class Config { // Abandon all hope, ye who enter here :)

	public Config() {
	}

	/**
	 *
	 * @author daniellink
	 *
	 *         If set to preselected, use a predefined number of clusters or multiply the number of source files with a constant If set to clustergain, use Josh's algorithm
	 *
	 */
	public enum StoppingCriterionConfig {
		preselected, clustergain
	}

	public enum Language {
		java, c
	}

	public enum SimMeasure {
		uem, uemnm, js, ilm, scm
	}

	public enum Granule {
		func, file, clazz
	}

	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(Config.class);

	private static String selectedRecoveryMethodName;

	public static String getSelectedRecoveryMethodName() {
		return selectedRecoveryMethodName;
	}

	public static void setSelectedRecoveryMethodName(final String selectedRecoveryMethodName) {
		Config.selectedRecoveryMethodName = selectedRecoveryMethodName;
	}

	/* Project-specific configuration data */
	// private static File projConfigFile= new File("cfg" + File.separator +
	// "oodt-resource-0.2.cfg");
	private static File projConfigFile = new File("cfg" + File.separator + "standard_config.cfg");
	private static String currProjName = "";
	// + "joshua" + File.separator;
	private static Language selectedLanguage = Language.java;

	private static String loggingConfigFilename = "cfg" + File.separator + "extractor_logging.cfg";
	private static String DATADIR = "data" + File.separator + currProjName;

	public static String getDATADIR() {
		return DATADIR;
	}

	public static void setDATADIR(final String dATADIR) {
		DATADIR = dATADIR;
	}

	private static String projSrcDir = "";
	private static String[] selectedPkgsArray;
	private static String sootClasspathStr;
	private static String[] deselectedPkgsArray;
	private static String odemFileName = "";

	/* Clustering configuration data */
	private static ClusteringAlgorithmType currentClusteringAlgorithm = ClusteringAlgorithmType.WCA;
	private static SimMeasure currSimMeasure = SimMeasure.uem;

	private static Controller currentController;

	public static Controller getCurrentController() {
		return currentController;
	}

	public static void setCurrentController(final Controller currentController) {
		Config.currentController = currentController;
	}

	// External programs
	private static String uccLoc = "/opt/UCC/bin/UCC";

	public static String getUccLoc() {
		return uccLoc;
	}

	public static void setUccLoc(final String uccLoc_) {
		uccLoc = uccLoc_;
	}

	public boolean isRunUCC() {
		return runUCC;
	}

	public void setRunUCC(final boolean runUCC_) {
		runUCC = runUCC_;
	}

	// public static String[] uccArgs = { "-nocomplex", "-nolinks", "-nodup", "-ascii", "-unified", "-outdir" };
	private boolean runUCC = true;

	private String dotLayoutCommandDir = "/usr/local/bin";
	private String dotLayoutCommand = "twopi";
	private String dotOutputFormat = "pdf";

	public String getDotOutputFormat() {
		return dotOutputFormat;
	}

	public void setDotOutputFormat(final String dotOutputFormat) {
		this.dotOutputFormat = dotOutputFormat;
	}

	public String getDotLayoutCommand() {
		return dotLayoutCommand;
	}

	public void setDotLayoutCommand(final String dotLayoutCommand) {
		this.dotLayoutCommand = dotLayoutCommand;
	}

	public String getDotLayoutCommandDir() {
		return dotLayoutCommandDir;
	}

	public void setDotLayoutCommandDir(final String dotLayoutCommandDir) {
		this.dotLayoutCommandDir = dotLayoutCommandDir;
	}

	public String getGraphParams() {
		return graphParams;
	}

	public void setGraphParams(final String graphParams_) {
		graphParams = graphParams_;
	}

	public String getGraphOpenerLoc() {
		return graphOpenerLoc;
	}

	public void setGraphOpenerLoc(final String graphOpenerLoc_) {
		graphOpenerLoc = graphOpenerLoc_;
	}

	public boolean isRunGraphs() {
		return runGraphs;
	}

	public void setRunGraphs(final boolean runGraphs_) {
		runGraphs = runGraphs_;
	}

	private String graphParams = "-x -Goverlap=scale -Tpdf";
	private String graphOpenerLoc = "/usr/bin/open";
	private boolean runGraphs = true;

	public boolean isViewGraphs() {
		return viewGraphs;
	}

	public void setViewGraphs(final boolean viewGraphs_) {
		viewGraphs = viewGraphs_;
	}

	private boolean viewGraphs = true;

	private double arcTopicsEntitiesFactor = 0.18; // arbitrary value as found
	// in Josh's code

	private double arcNumClustersFastClustersFactor = 0.2; // ditto

	private boolean useARCAbsoluteTopicsNumber = false; // Whether the number of ARC topics should be an absolute number

	private boolean useARCAbsoluteClustersNumber = false; // Whether the number of ARC topics should be an absolute number

	public boolean isUseARCAbsoluteClustersNumber() {
		return useARCAbsoluteClustersNumber;
	}

	public void setUseARCAbsoluteClustersNumber(final boolean useARCAbsoluteClustersNumber) {
		this.useARCAbsoluteClustersNumber = useARCAbsoluteClustersNumber;
	}

	public int getArcAbsoluteClustersNumber() {
		return arcAbsoluteClustersNumber;
	}

	public void setArcAbsoluteClustersNumber(final int arcAbsoluteClustersNumber) {
		this.arcAbsoluteClustersNumber = arcAbsoluteClustersNumber;
	}

	public boolean isUseARCAbsoluteTopicsNumber() {
		return useARCAbsoluteTopicsNumber;
	}

	public void setUseARCAbsoluteTopicsNumber(final boolean useARCAbsoluteTopicsNumber) {
		this.useARCAbsoluteTopicsNumber = useARCAbsoluteTopicsNumber;
	}

	private int arcAbsoluteTopicsNumber = 100; // Absolute number of topics for ARC

	private int arcAbsoluteClustersNumber = 10; // Absolute number of clusters for ARC

	public int getArcAbsoluteTopicsNumber() {
		return arcAbsoluteTopicsNumber;
	}

	public void setArcAbsoluteTopicsNumber(final int arcAbsoluteTopicsNumber) {
		this.arcAbsoluteTopicsNumber = arcAbsoluteTopicsNumber;
	}

	public double getArcNumClustersFastClustersFactor() {
		return arcNumClustersFastClustersFactor;
	}

	public void setArcNumClustersFastClustersFactor(final double arcNumClustersFastClustersFactor_) {
		arcNumClustersFastClustersFactor = arcNumClustersFastClustersFactor_;
	}

	public double getArcTopicsEntitiesFactor() {
		return arcTopicsEntitiesFactor;
	}

	public void setArcTopicsEntitiesFactor(final double arcTopicsEntitiesFactor_) {
		arcTopicsEntitiesFactor = arcTopicsEntitiesFactor_;
	}

	public static SimMeasure getCurrSimMeasure() {
		return currSimMeasure;
	}

	public static void setCurrSimMeasure(final SimMeasure currSimMeasure) {
		Config.currSimMeasure = currSimMeasure;
	}

	// A crutch since Xtreeam doesn't save static variables and it would take too long to make stoppingCriterion not static
	private StoppingCriterionConfig instanceStoppingCriterion = StoppingCriterionConfig.clustergain;

	public StoppingCriterionConfig getInstanceStoppingCriterion() {
		// instanceStoppingCriterion = Config.stoppingCriterion;
		return instanceStoppingCriterion;
	}

	public void setInstanceStoppingCriterion(final StoppingCriterionConfig instanceStoppingCriterion) {
		this.instanceStoppingCriterion = instanceStoppingCriterion;
		Config.stoppingCriterion = instanceStoppingCriterion;
	}

	private static StoppingCriterionConfig stoppingCriterion = StoppingCriterionConfig.clustergain;

	public static StoppingCriterionConfig getStoppingCriterion() {
		return stoppingCriterion;
	}

	public static void setStoppingCriterion(final StoppingCriterionConfig stoppingCriterion_) {
		stoppingCriterion = stoppingCriterion_;
	}

	/* MALLET settings */

	private int mIterations = 1000;
	private int mRandomSeed = 10;
	private int mThreads = 2;
	private double mBeta = 0.01;
	private int mTopWords = 20;

	public int getmIterations() {
		return mIterations;
	}

	public void setmIterations(final int mIterations) {
		this.mIterations = mIterations;
	}

	public int getmRandomSeed() {
		return mRandomSeed;
	}

	public void setmRandomSeed(final int mRandomSeed) {
		this.mRandomSeed = mRandomSeed;
	}

	public int getmThreads() {
		return mThreads;
	}

	public void setmThreads(final int mThreads) {
		this.mThreads = mThreads;
	}

	public double getmBeta() {
		return mBeta;
	}

	public void setmBeta(final double mBeta) {
		this.mBeta = mBeta;
	}

	public int getmTopWords() {
		return mTopWords;
	}

	public void setmTopWords(final int mTopWords) {
		this.mTopWords = mTopWords;
	}

	/* End of MALLET settings */

	private static int numClusters = 1;
	private static String stopWordsFilename = "cfg" + File.separator + "stopwords.txt";
	public static boolean isExcelFileWritingEnabled = false;

	public static ClusteringAlgorithmType getCurrentClusteringAlgorithm() {
		return currentClusteringAlgorithm;
	}

	public static void setCurrentClusteringAlgorithm(final ClusteringAlgorithmType currentClusteringAlgorithm) {
		Config.currentClusteringAlgorithm = currentClusteringAlgorithm;
	}

	public static boolean runMojo = false;
	public static boolean usingFvMap = true;
	public static boolean ignoreDependencyFilters = false;

	/* Concern properties data */
	private static String malletTopicKeysFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/" + Config.getCurrProjStr() + "/" + Config.getCurrProjStr() + "-"
			+ Config.getNumTopics() + "-topic-keys.txt";

	private static String malletWordTopicCountsFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/" + Config.getCurrProjStr() + "/" + Config.getCurrProjStr() + "-"
			+ Config.getNumTopics() + "-word-topic-counts.txt";

	// What's up with this constant? - Daniel
	private static File malletDocTopicsFile = null;

	// "/home/joshua/Documents/Software Engineering Research/Subjects/" +
	// Config.getCurrProjStr() + "/" + Config.getCurrProjStr() + "-" +
	// Config.getNumTopics()
	// + "-doc-topics.txt";

	public static File getMalletDocTopicsFile() {
		return malletDocTopicsFile;
	}

	public static void setMalletDocTopicsFile(final File malletDocTopicsFile) {
		Config.malletDocTopicsFile = malletDocTopicsFile;
	}

	private static int numTopics = 10;
	private static List<Integer> numTopicsList = new ArrayList<>();

	public static List<Integer> getNumTopicsList() {
		return numTopicsList;
	}

	public static void setNumTopicsList(final List<Integer> numTopicsList) {
		Config.numTopicsList = numTopicsList;
	}

	public static int getNumTopics() {
		return numTopics;
	}

	public static void setNumTopics(final int numTopics) {
		Config.numTopics = numTopics;
	}

	public boolean showSvgToolTips = true;

	public boolean isShowSvgToolTips() {
		return showSvgToolTips;
	}

	public void setShowSvgToolTips(final boolean showSvgToolTips_) {
		showSvgToolTips = showSvgToolTips_;
	}

	private String formDataFileName = FileUtils.getUserDirectoryPath() + File.separator + "arcade_formdata.xml";

	/**
	 * @return the formDataFileName
	 */
	public String getFormDataFileName() {
		return formDataFileName;
	}

	/**
	 * @param formDataFileName
	 *            the formDataFileName to set
	 */
	public void setFormDataFileName(final String formDataFileName) {
		this.formDataFileName = formDataFileName;
	}

	private static String configDataFilename = FileUtils.getUserDirectoryPath() + File.separator + "arcade_configdata.xml";

	/**
	 * @return the configDataFilename
	 */
	public static String getConfigDataFilename() {
		return configDataFilename;
	}

	/**
	 * @param configDataFilename
	 *            the configDataFilename to set
	 */
	public static void setConfigDataFilename(final String configDataFilename_) {
		configDataFilename = configDataFilename_;
	}

	private File perUserArcadeDir = FileUtil.checkDir(System.getProperty("user.home") + File.separator + "arcade_userdir", true, false);

	/**
	 * @return the perUserArcadeConfigDir
	 */
	public File getPerUserArcadeDir() {
		return perUserArcadeDir;
	}

	/**
	 * @param perUserArcadeDir
	 *            the perUserArcadeConfigDir to set
	 */
	public void setPerUserArcadeDir(final File perUserArcadeDir_) {
		perUserArcadeDir = perUserArcadeDir_;
	}

	/* PKG Settings start */

	private String pythonloc = "/usr/bin/python";
	private String batchPackagerLoc = "/Users/daniellink/Documents/workspace/arcadepy/src/arc/batchpackager.py";
	private String pkgPrefixes = "";

	/* PKG Settings end */

	private boolean useSuperModel = false;

	public boolean isUseSuperModel() {
		return useSuperModel;
	}

	public void setUseSuperModel(final boolean useSuperModel_) {
		useSuperModel = useSuperModel_;
	}

	// private String superModelSelectedVersionsDirName = "noName";
	//
	// public String getSuperModelSelectedVersionsDirName() {
	// return superModelSelectedVersionsDirName;
	// }
	//
	// public void setSuperModelSelectedVersionsDirName(final String
	// superModelSelectedVersionsDirName_) {
	// superModelSelectedVersionsDirName = superModelSelectedVersionsDirName_;
	// }

	private File superModelSelectedVersionsDir = new File("noname");

	public File getSuperModelSelectedVersionsDir() {
		return superModelSelectedVersionsDir;
	}

	public void setSuperModelSelectedVersionsDir(final File superModelSelectedVersionsDir_) {
		superModelSelectedVersionsDir = superModelSelectedVersionsDir_;
		// superModelSelectedVersionsDirName =
		// superModelSelectedVersionsDir.getAbsolutePath();
	}

	private String malletExecutable = "/usr/local/mallet/bin/mallet";

	File relaxOutputDir = new File("noname");

	public File getRelaxOutputDir() {
		return relaxOutputDir;
	}

	public void setRelaxOutputDir(final File relaxOutputDir) {
		this.relaxOutputDir = relaxOutputDir;
	}

	public int getRelaxRandomSeed() {
		return relaxRandomSeed;
	}

	public void setRelaxRandomSeed(final int relaxRandomSeed) {
		this.relaxRandomSeed = relaxRandomSeed;
	}

	public int getRelaxVerbosity() {
		return relaxVerbosity;
	}

	public void setRelaxVerbosity(final int relaxVerbosity) {
		this.relaxVerbosity = relaxVerbosity;
	}

	public int getRelaxTrials() {
		return relaxTrials;
	}

	public void setRelaxTrials(final int relaxTrials) {
		this.relaxTrials = relaxTrials;
	}

	int relaxRandomSeed = 0;
	int relaxVerbosity = 1;
	int relaxTrials = 1;

	public String getRelaxClassifierFileName() {
		return relaxClassifierFileName;
	}

	private File relaxClassifierFileMain = new File("noClassiferSet");

	public File getRelaxClassifierFileMain() {
		return relaxClassifierFileMain;
	}

	public void setRelaxClassifierFileMain(final File relaxClassifierFileMain) {
		this.relaxClassifierFileMain = relaxClassifierFileMain;
	}

	public void setRelaxClassifierFileName(final String relaxClassifierFileName) {
		this.relaxClassifierFileName = relaxClassifierFileName;
	}

	private int relaxTrainingPortionPercentage = 60;

	public int getRelaxTrainingPortion() {
		return relaxTrainingPortionPercentage;
	}

	public void setRelaxTrainingPortion(final int relaxTrainingPortion) {
		relaxTrainingPortionPercentage = relaxTrainingPortion;
	}

	public File getMalletExecutable() {
		return new File(malletExecutable);
	}

	public void setMalletExecutable(final File malletExecutableFile) {
		malletExecutable = malletExecutableFile.getAbsolutePath();
	}

	public String relaxTrainingDirName;

	public String getRelaxTrainingDirName() {
		return relaxTrainingDirName;
	}

	public void setRelaxTrainingDir(final File relaxTrainingDir) {
		relaxTrainingDirName = relaxTrainingDir.getAbsolutePath();
	}

	private String relaxClassifierFileName = null;

	private String classifierMD5sum = null;

	public String getClassifierMD5sum() {
		if (classifierMD5sum == null) {
			classifierMD5sum = FileUtil.getMD5sum(FileUtil.checkFile(relaxClassifierFileName, false, false));
		}
		return classifierMD5sum;
	}

	public void setClassifierMD5sum(final String classifierMD5sum) {
		this.classifierMD5sum = classifierMD5sum;
	}

	public void setRelaxClassifierFile(final String relaxClassifierFileName) {
		this.relaxClassifierFileName = relaxClassifierFileName;
	}

	private Double matchConfidence = 0.60;

	public Double getMatchConfidence() {
		return matchConfidence;
	}

	public void setMatchConfidence(final Double matchConfidence) {
		this.matchConfidence = matchConfidence;
	}

	private String metricsDirName;

	public String getMetricsDirName() {
		return metricsDirName;
	}

	public void setMetricsDir(final String metricsDirName) {
		this.metricsDirName = metricsDirName;
	}

	public int getvDist() {
		return vDist;
	}

	public void setvDist(final int vDist) {
		this.vDist = vDist;
	}

	private int vDist;

	public static TopicModelExtractionMethod tmeMethod = TopicModelExtractionMethod.VAR_MALLET_FILE;
	public static File srcDir;

	private static String TME_METHOD = "topic_model_extraction_method";

	/* DriverEngine options */
	public static RunType runType = RunType.whole;
	public static boolean useSerializedClassGraph = true;
	public static boolean useXMLClassGraph = true;
	public static boolean forceClustering = true;
	public static boolean enableClustering = true;
	public static boolean enablePostClusteringTasks = false;
	public static boolean forceClassAndMyMethodGraphsConstruction = false;
	public static boolean performPRCalculation = true; // setting this option to
	// true prevents other
	// option phases from
	// running
	public static boolean useFastFeatureVectorsFile = false;
	private static String USE_FAST_FEATURE_VECTORS_FILE = "use_fast_feature_vectors_file";

	/* Config data to be REMOVED in the future */
	public static Proj proj = Proj.OODT_Filemgr;
	public static final String lucene1_9FinalStr = "lucene-1.9-final";
	public static final String llamaChatStr = "LlamaChat";
	public static final String freecsStr = "freecs";
	public static final String gujChatStr = "gujChat";
	public static final String lcdClockStr = "LCDClock";
	public static final String jeditStr = "jedit";
	public static final String oodtFilemgrStr = "oodt-filemgr";
	public static final String klaxStr = "KLAX";
	public static final String jigsawStr = "jigsaw2.2.6";
	public static boolean performingTwoProjectTest = false;
	public static boolean performingThreeProjectTest = true;

	private static File depsRsfFile;

	public static void setDepsRsfFile(final File depsRsfFile) {
		Config.depsRsfFile = depsRsfFile;
	}

	private static File groundTruthFile;
	private static File smellClustersFile;

	public static void setSmellClustersFile(final File smellClustersFile) {
		Config.smellClustersFile = smellClustersFile;
	}

	private static File mojoTargetFile;

	private static int startNumClustersRange;

	private static int endNumClustersRange;

	private static int rangeNumClustersStep;

	private static boolean usingPreselectedRange = false;
	private static boolean usingNumTopicsRange = false;

	private static int startNumTopicsRange;

	public static int getStartNumTopicsRange() {
		return startNumTopicsRange;
	}

	public static int getEndNumTopicsRange() {
		return endNumTopicsRange;
	}

	public static int getRangeNumTopicsStep() {
		return rangeNumTopicsStep;
	}

	private static int endNumTopicsRange;

	private static int rangeNumTopicsStep;

	private static File topicsDir;

	private static File expertDecompositionFile;

	private static String concernRecoveryFilePrefix;

	private static List<Integer> clustersToWriteList = null;

	private static Granule clusteringGranule = Granule.file;

	private static List<String> excludedEntities;

	private static String clusterStartsWith;

	public static String getClusterStartsWith() {
		return clusterStartsWith;
	}

	public static Granule getClusteringGranule() {
		return clusteringGranule;
	}

	public static void setClusteringGranule(final Granule clusteringGranule) {
		Config.clusteringGranule = clusteringGranule;
	}

	public static boolean isUsingPreselectedRange() {
		return usingPreselectedRange;
	}

	public static boolean isUsingNumTopicsRange() {
		return usingNumTopicsRange;
	}

	public static int getStartNumClustersRange() {
		return startNumClustersRange;
	}

	public static int getEndNumClustersRange() {
		return endNumClustersRange;
	}

	public static int getRangeNumClustersStep() {
		return rangeNumClustersStep;
	}

	public static void initConfigFromFile(final File configFile) {
		final Properties prop = new Properties();
		try {
			// FileInputStream stream = new FileInputStream(filename);
			// final String propertyFileContents =
			// FileUtil.readFileAsString(configFile);
			final String propertyFileContents = FileUtils.readFileToString(configFile);
			prop.load(new StringReader(propertyFileContents.replace("\\", "\\\\")));

			final String projName = prop.getProperty("project_name");
			logger.debug(projName);
			currProjName = projName;
			DATADIR = "data" + File.separator + currProjName;

			final String numClustersStr = prop.getProperty("num_clusters");
			logger.debug(numClustersStr);
			if (numClustersStr != null) {
				numClusters = Integer.parseInt(numClustersStr);
			}

			final String stopCriterionStr = prop.getProperty("stop_criterion");
			if (stopCriterionStr != null) {
				if (stopCriterionStr.equalsIgnoreCase("cluster_gain")) {
					stoppingCriterion = StoppingCriterionConfig.clustergain;
				} else if (stopCriterionStr.equalsIgnoreCase("preselected")) {
					stoppingCriterion = StoppingCriterionConfig.preselected;
				}
			}

			final String clusteringAlgorithmStr = prop.getProperty("clustering_algorithm");
			logger.debug("cluster_algorithm: " + clusteringAlgorithmStr);
			if (clusteringAlgorithmStr != null) {
				if (clusteringAlgorithmStr.equals("wca")) {
					currentClusteringAlgorithm = ClusteringAlgorithmType.WCA;
				} else if (clusteringAlgorithmStr.equals("arc")) {
					currentClusteringAlgorithm = ClusteringAlgorithmType.ARC;
					setConcernProperties(prop);
					setCurrSimMeasure(SimMeasure.js);
				} else if (clusteringAlgorithmStr.equals("limbo")) {
					currentClusteringAlgorithm = ClusteringAlgorithmType.LIMBO;
					setCurrSimMeasure(SimMeasure.ilm);
				} else {
					currentClusteringAlgorithm = ClusteringAlgorithmType.WCA;
				}
			} else {
				currentClusteringAlgorithm = ClusteringAlgorithmType.WCA;
			}

			final String lang = prop.getProperty("lang");
			if (lang.equals("java")) {
				selectedLanguage = Language.java;
			} else if (lang.equals("c")) {
				selectedLanguage = Language.c;
			}

			if (selectedLanguage.equals(Language.java)) {
				final String selectedPkgsStr = prop.getProperty("selected_pkgs");
				if (selectedPkgsStr != null) {
					selectedPkgsArray = selectedPkgsStr.split(",");
				}
			}

			/*
			 * if (selectedLanguage.equals(Language.java)) { setJavaConfigFromFile(prop); } else if (selectedLanguage.equals(Language.c)) { currProjRsfFilename = prop.getProperty("deps_rsf_file"); if (currProjRsfFilename == null) { System.err.println(
			 * "projects_rsf_loc not set properly in config file" ); } }
			 */
			depsRsfFile = FileUtil.checkFile(prop.getProperty("deps_rsf_file"), false, false);
			if (depsRsfFile == null) {
				System.out.println("WARNING: deps_rsf_file not set properly in config file");
			}

			odemFileName = prop.getProperty("odem_file");
			groundTruthFile = FileUtil.checkFile(prop.getProperty("ground_truth_file"), false, false);
			smellClustersFile = FileUtil.checkFile(prop.getProperty("smell_clusters_file"), false, false);
			mojoTargetFile = FileUtil.checkFile(prop.getProperty("mojo_target_file"), false, false);
			final String preselectedRange = prop.getProperty("preselected_range");
			if (preselectedRange != null) {
				final String[] tokens = preselectedRange.split(",");
				if (tokens.length != 3) {
					final String errMsg = "wrong number of tokens for preselected range: expected 3, got " + tokens.length;
					logger.error(errMsg);
					System.err.println(errMsg);
					System.exit(1);

				}
				usingPreselectedRange = true;
				startNumClustersRange = Integer.parseInt(tokens[0]);
				endNumClustersRange = Integer.parseInt(tokens[1]);
				rangeNumClustersStep = Integer.parseInt(tokens[2]);

				logger.debug("start: " + startNumClustersRange + ", " + "range: " + endNumClustersRange + ", " + "step: " + rangeNumClustersStep);
			}

			topicsDir = FileUtil.checkFile(prop.getProperty("topics_dir"), false, false);

			final String numTopicsRange = prop.getProperty("numtopics_range");
			if (numTopicsRange != null) {
				final String[] tokens = numTopicsRange.split(",");
				if (tokens.length != 3) {
					final String errMsg = "wrong number of tokens for numtopics range: expected 3, got " + tokens.length;
					logger.error(errMsg);
					System.err.println(errMsg);
					System.exit(1);
				}
				usingNumTopicsRange = true;
				startNumTopicsRange = Integer.parseInt(tokens[0]);
				endNumTopicsRange = Integer.parseInt(tokens[1]);
				rangeNumTopicsStep = Integer.parseInt(tokens[2]);

				logger.debug("start: " + startNumTopicsRange + ", " + "range: " + endNumTopicsRange + ", " + "step: " + rangeNumTopicsStep);
			}

			expertDecompositionFile = FileUtil.checkFile(prop.getProperty("expert_decomposition_file"), false, false);
			concernRecoveryFilePrefix = prop.getProperty("concern_recovery_file_prefix");

			final String currSimMeasureStr = prop.getProperty("sim_measure");
			if (currSimMeasureStr != null) {
				if (currSimMeasureStr.trim().equals("uem")) {
					setCurrSimMeasure(SimMeasure.uem);
				} else if (currSimMeasureStr.trim().equals("uemnm")) {
					setCurrSimMeasure(SimMeasure.uemnm);
				} else if (currSimMeasureStr.trim().equals("js")) {
					setCurrSimMeasure(SimMeasure.js);
				} else if (currSimMeasureStr.trim().equals("ilm")) {
					setCurrSimMeasure(SimMeasure.ilm);
				} else if (currSimMeasureStr.trim().equals("scm")) {
					setCurrSimMeasure(SimMeasure.scm);
				} else {
					throw new IllegalArgumentException(currSimMeasureStr + " is not a valid value for sim_measure");
				}
			} else {
				System.out.println("WARNING: No sim_measure property set");
			}

			final String granuleStr = prop.getProperty("granule");
			if (granuleStr != null) {
				if (granuleStr.trim().equals("file")) {
					clusteringGranule = Granule.file;
				} else if (granuleStr.trim().equals("func")) {
					clusteringGranule = Granule.func;
				} else if (granuleStr.trim().equals("class")) {
					clusteringGranule = Granule.clazz;
				}

				excludedEntities = new ArrayList<>();
				final String excludedEntitiesStr = prop.getProperty("excluded_entities");
				if (excludedEntitiesStr != null) {
					final String[] excludedEntitiesArray = excludedEntitiesStr.split(",");
					for (final String excludedEntity : excludedEntitiesArray) {
						excludedEntities.add(excludedEntity.trim());
					}
				}
			} else {
				System.out.println("WARNING: No granule property set");
			}

			malletDocTopicsFile = FileUtil.checkFile(prop.getProperty("doc_topics_file"), false, false);
			clusterStartsWith = prop.getProperty("cluster_starts_with");
			if (clusterStartsWith == null) {
				clusterStartsWith = null;
			} else {
				clusterStartsWith = clusterStartsWith.trim();
			}

			if (prop.getProperty("ignore_dependency_filters") != null) {
				if (prop.getProperty("ignore_dependency_filters").equals("true")) {
					ignoreDependencyFilters = true;
				} else if (prop.getProperty("ignore_dependency_filters").equals("false")) {
					ignoreDependencyFilters = false;
				}
			}

			if (prop.getProperty(TME_METHOD) != null) {
				if (prop.getProperty(TME_METHOD).equals("var_mallet_file")) {
					tmeMethod = TopicModelExtractionMethod.VAR_MALLET_FILE;
				} else if (prop.getProperty(TME_METHOD).equals("mallet_api")) {
					tmeMethod = TopicModelExtractionMethod.MALLET_API;
				} else {
					tmeMethod = TopicModelExtractionMethod.VAR_MALLET_FILE;
				}
			}

			if (prop.getProperty("src_dir") != null) {
				srcDir = FileUtil.checkDir(prop.getProperty("src_dir"), false, false);
			}

			if (prop.getProperty(USE_FAST_FEATURE_VECTORS_FILE) != null) {
				if (prop.getProperty(USE_FAST_FEATURE_VECTORS_FILE).equals("true")) {
					useFastFeatureVectorsFile = true;
				} else if (prop.getProperty(USE_FAST_FEATURE_VECTORS_FILE).equals("false")) {
					useFastFeatureVectorsFile = false;
				}
			}

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	public static File getGroundTruthFile() {
		return groundTruthFile;
	}

	public static File getSmellClustersFile() {
		return smellClustersFile;
	}

	public static void setGroundTruthFile(final File groundTruthFile) {
		Config.groundTruthFile = groundTruthFile;
	}

	public static String getOdemFile() {
		return odemFileName;
	}

	public static void setOdemFile(final String odemFile) {
		Config.odemFileName = odemFile;
	}

	private static void setConcernProperties(final Properties prop) {

		malletTopicKeysFilename = prop.getProperty("topic_keys_file");
		malletWordTopicCountsFilename = prop.getProperty("word_topic_counts_file");
		malletDocTopicsFile = FileUtil.checkFile(prop.getProperty("doc_topics_file"), false, false);

		if (malletTopicKeysFilename == null) {
			logger.error("topic_keys_file not set");
		}
		if (malletWordTopicCountsFilename == null) {
			logger.error("word_topics_file not set");
		}
		if (malletDocTopicsFile == null) {
			logger.error("doc_topics_file");
		}

	}

	// private static void setJavaConfigFromFile(Properties prop)
	// throws IOException {
	// String selectedPkgsStr = prop.getProperty("selected_pkgs");
	// String deselectedPkgsStr = prop.getProperty("deselected_pkgs");
	// String sootClassPathJarDirStr = prop
	// .getProperty("sootclasspath_jardir");
	// String eclipseDotClassPathStr = prop
	// .getProperty("eclipse_dot_classpath");
	//
	// String[] jarDirsArray;
	// sootClasspathStr = prop.getProperty("sootclasspath");
	// projSrcDir = prop.getProperty("src_dir");
	// projSrcDir.replace("\\", "\\\\");
	//
	// logger.debug("projSrcDir: " + projSrcDir);
	//
	// ArrayList<File> extraJars = new ArrayList<File>();
	//
	// if (selectedPkgsStr != null) {
	// selectedPkgsArray = selectedPkgsStr.split(",");
	// }
	// if (deselectedPkgsStr != null) {
	// deselectedPkgsArray = deselectedPkgsStr.split(",");
	// }
	// logger.debug("sootClasspathStr: " + sootClasspathStr);
	// sootClasspathArray = sootClasspathStr.split(File.pathSeparator);
	//
	// if (sootClassPathJarDirStr != null) {
	// logger.debug("Printing jars in " + sootClassPathJarDirStr);
	// jarDirsArray = sootClassPathJarDirStr.split(":");
	// for (String jarDirStr : jarDirsArray) {
	// logger.debug("jar directory: " + jarDirStr);
	// File jarDir = new File(jarDirStr);
	// findExtraJars(extraJars, jarDir);
	// }
	// }
	//
	// ClasspathBuilder builder = new ClasspathBuilder();
	// if (eclipseDotClassPathStr != null) {
	// File classpathFile = new File(eclipseDotClassPathStr);
	// logger.debug("Eclipse dot classpath location: "
	// + eclipseDotClassPathStr);
	// try {
	// ParseDotClasspath.parseDotClasspath(classpathFile, builder);
	// } catch (SAXException e) {
	// e.printStackTrace();System.exit(-1);
	// } catch (ParserConfigurationException e) {
	// e.printStackTrace();System.exit(-1);
	// }
	// }
	//
	// logger.debug(projSrcDir);
	//
	// logger.debug(selectedPkgsStr);
	// logger.debug(Arrays.toString(selectedPkgsArray));
	//
	// logger.debug(deselectedPkgsStr);
	// logger.debug(Arrays.toString(deselectedPkgsArray));
	//
	// logger.debug(sootClasspathStr);
	// logger.debug(Arrays.toString(sootClasspathArray));
	//
	// logger.debug("Updating soot classpath by adding extraJars");
	// for (File jarFile : extraJars) {
	// sootClasspathStr += File.pathSeparator + jarFile.getAbsoluteFile();
	// }
	//
	// sootClasspathStr.replaceAll(File.pathSeparator + File.pathSeparator,
	// File.pathSeparator);
	//
	// logger.debug("soot classpath with extra jars: " + sootClasspathStr);
	//
	// logger.debug("parsed dot classpath: " + builder.getResult());
	//
	// sootClasspathStr += File.pathSeparator + builder.getResult();
	//
	// logger.debug("soot classpath with eclipse dot classpath results: "
	// + sootClasspathStr);
	// logger.debug("\n");
	// }

	public static String getCurrProjStr() {
		return currProjName;
	}

	public static int getNumClusters() {
		return numClusters;
	}

	public static void setNumClusters(final int inNumClusters) {
		numClusters = inNumClusters;
	}

	public static HashMap<String, String> getCurrProjMap() {
		ClassValueMap.init();
		if (proj.equals(Proj.LlamaChat)) {
			return ClassValueMap.LlamaChatMap;
		} else if (proj.equals(Proj.FreeCS)) {
			return ClassValueMap.freecsMap;
		} else if (proj.equals(Proj.GujChat)) {
			return null;
		} else {
			return null;
		}
	}

	public static boolean isMethodInSelectedPackages(final SootMethod src) {
		final char leading = '<';

		for (final String pkg : selectedPkgsArray) {
			if (src.toString().startsWith(leading + pkg)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isMethodInDeselectedPackages(final SootMethod src) {
		final String leading = "<";

		if (deselectedPkgsArray != null) {
			for (final String pkg : deselectedPkgsArray) {
				if (src.toString().startsWith(leading + pkg)) {
					return true;
				}
			}
			return false;
		}
		return false;

	}

	public static String getProjSrcDir() {
		return projSrcDir;
	}

	public static void initProjectData(final ClassGraphTransformer t) {
		ClassGraphTransformer.LlamaChatTMD.docTopicsFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/LlamaChat/LlamaChat-doc-topics.txt";
		ClassGraphTransformer.LlamaChatTMD.topicKeysFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/LlamaChat/LlamaChat-topic-keys.txt";

		ClassGraphTransformer.freecsTMD.docTopicsFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/freecs/freecs-doc-topics.txt";
		ClassGraphTransformer.freecsTMD.topicKeysFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/freecs/freecs-topic-keys.txt";

		final String twoProjDocTopicsFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/LlamaChat_freecs/Llamachat_freecs-doc-topics.txt";
		final String twoProjTopicKeysFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/LlamaChat_freecs/Llamachat_freecs-topic-keys.txt";

		final String threeProjDocTopicsFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/threeChatServerSystems/threeChatServerSystems-doc-topics.txt";
		final String threeProjTopicKeysFilename = "/home/joshua/Documents/Software Engineering Research/Subjects/threeChatServerSystems/threeChatServerSystems-topic-keys.txt";

		if (Config.performingThreeProjectTest) {
			t.currDocTopicsFilename = threeProjDocTopicsFilename;
			t.currTopicKeysFilename = threeProjTopicKeysFilename;
		} else if (Config.performingTwoProjectTest) {
			t.currDocTopicsFilename = twoProjDocTopicsFilename;
			t.currTopicKeysFilename = twoProjTopicKeysFilename;
		} else if (Config.proj.equals(Proj.FreeCS)) {
			t.currDocTopicsFilename = ClassGraphTransformer.freecsTMD.docTopicsFilename;
			t.currTopicKeysFilename = ClassGraphTransformer.freecsTMD.topicKeysFilename;
		} else if (Config.proj.equals(Proj.LlamaChat)) {
			t.currDocTopicsFilename = ClassGraphTransformer.LlamaChatTMD.docTopicsFilename;
			t.currTopicKeysFilename = ClassGraphTransformer.LlamaChatTMD.topicKeysFilename;
		} else {
			System.err.println("Couldn't identiy the doc-topics and topic-keys files");
			System.exit(1);
		}

		if (Config.proj.equals(Proj.GujChat)) {
			t.datasetName = Config.gujChatStr;
		} else if (Config.proj.equals(Proj.LlamaChat)) {
			t.datasetName = Config.llamaChatStr;
		} else if (Config.proj.equals(Proj.FreeCS)) {
			t.datasetName = Config.freecsStr;
		} else if (Config.proj.equals(Proj.LCDClock)) {
			t.datasetName = Config.lcdClockStr;
		} else if (Config.proj.equals(Proj.JEdit)) {
			t.datasetName = Config.jeditStr;
		} else if (Config.proj.equals(Proj.Lucene1_9Final)) {
			t.datasetName = Config.lucene1_9FinalStr;
		} else if (Config.proj.equals(Proj.OODT_Filemgr)) {
			t.datasetName = Config.oodtFilemgrStr;
		} else if (Config.proj.equals(Proj.KLAX)) {
			t.datasetName = Config.klaxStr;
		} else if (Config.proj.equals(Proj.Jigsaw)) {
			t.datasetName = Config.jigsawStr;
		} else {
			System.err.println("Could not identify project string, so couldn't save to arff file");
			System.exit(1);
		}

	}

	public static boolean isClassInSelectedPackages(final String clazz) {
		if (selectedPkgsArray == null) {
			// System.out.println("Selected packages are not set so accepted any
			// package");
			return true;
		}
		for (final String pkg : selectedPkgsArray) {
			if (clazz.trim().startsWith(pkg)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isClassInSelectedPackages(final SootClass src) {
		for (final String pkg : selectedPkgsArray) {
			if (src.toString().startsWith(pkg)) {
				return true;
			}
		}

		return false;
	}

	public static boolean isClassInDeselectedPackages(final SootClass src) {

		if (deselectedPkgsArray != null) {
			for (final String pkg : deselectedPkgsArray) {
				if (src.toString().startsWith(pkg)) {
					return true;
				}
			}
			return false;
		}
		return false;

	}

	public static void setupSootClassPath() {
		logger.debug("original SOOT_CLASSPATH: " + Scene.v().getSootClassPath());

		Scene.v().setSootClassPath(sootClasspathStr);

		logger.debug("with correct classes.jar - SOOT_CLASSPATH: " + Scene.v().getSootClassPath());

	}

	public static String getXMLFeatureVectorMapFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_fvMap.xml";
	}

	public static String getXLSDepsFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_deps.xls";
	}

	public static String getXLSSimMeasureFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_sim.xls";
	}

	public static String getXMLClassGraphFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_clg.xml";
	}

	public static String getSerializedClassGraphFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_clg.data";
	}

	public static String getSerializedClustersFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_clusters.data";
	}

	public static String getClusterGraphDotFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_cluster_graph.dot";
	}

	public static String getMyCallGraphFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_mycallgraph.data";
	}

	public static String getClassGraphFilename() {
		return DATADIR + File.separator + Config.getCurrProjStr() + ".data";
	}

	public static String getClassesWithUsedMethodsFilename() {
		return DATADIR + File.separator + Config.getCurrProjStr() + "_classesWithUsedMethods.data";
	}

	public static String getClassesWithAllMethodsFilename() {
		return DATADIR + File.separator + Config.getCurrProjStr() + "_classesWithAllMethods.data";
	}

	public static String getUnusedMethodsFilename() {
		return DATADIR + File.separator + Config.getCurrProjStr() + "_unusedMethods.data";
	}

	public static String getXMLSmellArchGraphFilename() {
		return DATADIR + File.separator + Config.getCurrProjStr() + "_smellArchGraph.xml";
	}

	public static String getXMLSmellArchFilename() {
		return DATADIR + File.separator + Config.getCurrProjStr() + "_smellArch.xml";
	}

	public static String getSpecifiedSmallArchFromXML() {
		return DATADIR + File.separator + getCurrProjStr() + "_smellArch_specified.xml";
	}

	public static String getMethodInfoFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_methodInfo.xml";
	}

	public static String getNumbereNodeMappingTextFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_numberedNodeMapping.txt";
	}

	public static String getClusterGraphXMLFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_cluster_graph.xml";
	}

	public static String getTopicsFilename() {
		return Config.getCurrProjFilenamePrefix() + "_" + Config.getNumTopics() + "_topics.mallet";
	}

	public static String getDocTopicsFilename() {
		return Config.getCurrProjFilenamePrefix() + "_" + Config.getNumTopics() + "_doc_topics.txt";
	}

	public static String getTopWordsFilename() {
		return Config.getCurrProjFilenamePrefix() + "_" + Config.getNumTopics() + "_top_words_per_topic.txt";
	}

	public static String getClustersRSFFilename(final int clustersSize) {
		if (Config.currentClusteringAlgorithm.equals(ClusteringAlgorithmType.ARC)) {
			return Config.getCurrProjFilenamePrefix() + "_" + Config.currentClusteringAlgorithm.toString().toLowerCase() + "_" + stoppingCriterion.toString() + "_" + Config.currSimMeasure.toString()
					+ "_" + clustersSize + "_clusters_" + Config.getNumTopics() + "topics.rsf";
		} else {
			return Config.getCurrProjFilenamePrefix() + "_" + Config.currentClusteringAlgorithm.toString().toLowerCase() + "_" + stoppingCriterion.toString() + "_" + Config.currSimMeasure.toString()
					+ "_" + clustersSize + "_clusters.rsf";
		}
	}

	public String getDetailedClustersRsfFilename() {
		return Config.getCurrProjFilenamePrefix() + stoppingCriterion.toString() + "_" + Config.currSimMeasure.toString() + "_" + Config.getNumClusters() + "_clusters.rsf";
	}

	public static String getCurrProjFilenamePrefix() {
		return DATADIR + File.separator + getCurrProjStr();
	}

	public static String getClassGraphDotFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_class_graph.dot";
	}

	public static String getLoggingConfigFilename() {
		return loggingConfigFilename;
	}

	public static void setProjConfigFile(final File projConfigFile_) {
		Config.projConfigFile = projConfigFile_;
	}

	public static void setProjConfigFile(final String projConfigFileName) {
		Config.projConfigFile = FileUtil.checkFile(projConfigFileName, false, false);
	}

	public static File getProjConfigFile() {
		return projConfigFile;
	}

	public static String getStopWordsFilename() {
		return stopWordsFilename;
	}

	public static File getMojoTargetFile() {
		return mojoTargetFile;
	}

	public static String getXMLFunctionDepGraphFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_func_dep_graph.xml";
	}

	public static Language getSelectedLanguage() {
		return selectedLanguage;
	}

	public static void setSelectedLanguage(final Language inLang) {
		selectedLanguage = inLang;
	}

	public static void setMalletTopicKeysFilename(final String filename) {
		malletTopicKeysFilename = filename;
	}

	public static String getMalletTopicKeysFilename() {
		return malletTopicKeysFilename;
	}

	public static void setMalletWordTopicCountsFilename(final String filename) {
		malletWordTopicCountsFilename = filename;
	}

	public static String getMalletWordTopicCountsFilename() {
		return malletWordTopicCountsFilename;
	}

	public static void setMalletDocTopicsFilename(final File file) {
		malletDocTopicsFile = file;
	}

	public static String getVariableMalletDocTopicsFilename() {
		return Config.getTopicsDir() + File.separator + Config.getCurrProjStr() + "-" + Config.getNumTopics() + "-doc-topics.txt";
	}

	private static File getTopicsDir() {
		return topicsDir;
	}

	public static String getNameToFeatureSetMapFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_name_to_feature_set_map.data";
	}

	public static String getNamesInFeatureSetFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_names_in_feature_set.data";
	}

	public static String getFastFeatureVectorsFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_fast_feature_vectors.data";
	}

	public static File getDepsRsfFile() {
		return depsRsfFile;
	}

	public static String getInternalGraphDotFilename(final String clusterName) {
		final String cleanClusterName = clusterName.replaceAll("[\\/:*?\"<>|\\s]", "_");
		return DATADIR + File.separator + "internal_clusters" + File.separator + getCurrProjStr() + "_" + cleanClusterName + "_internal_cluster_graph.dot";
	}

	public static String getGroundTruthRsfFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_ground_truth.rsf";
	}

	public static String getFullGroundTruthClusterGraphDotFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_full_ground_truth_cluster_graph.dot";
	}

	public static String getNonPkgBasedGroundTruthClusterGraphDotFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_non_pkg_based_ground_truth_cluster_graph.dot";
	}

	public static String getPkgBasedGroundTruthClusterGraphDotFilename() {
		return DATADIR + File.separator + getCurrProjStr() + "_pkg_based_ground_truth_cluster_graph.dot";
	}

	public static String getTopicsPrecisionRecallCSVFilename(final int numClusters) {
		return Config.getCurrProjFilenamePrefix() + numClusters + "_clusters_topics_pr.csv";
	}

	public static File getExpertDecompositionFile() {
		return expertDecompositionFile;
	}

	public static String getConcernRecoveryFilePrefix() {
		return concernRecoveryFilePrefix;
	}

	public static String getMojoWithTopicsCSVFilename(final int numClusters) {
		return Config.getCurrProjFilenamePrefix() + numClusters + "_clusters_and_topics_mojo.csv";
	}

	public static String getMojoToAuthCSVFilename(final List<Integer> numClustersList, final String selectedAlg, final String simMeasure) {
		return Config.getCurrProjFilenamePrefix() + numClustersList.get(0) + "-" + numClustersList.get(numClustersList.size() - 1) + "_" + selectedAlg + "_" + simMeasure + "_clusters_mojo.csv";
	}

	public static String getMojoToNextCSVFilename(final List<Integer> numClustersList, final String selectedAlg, final String simMeasure) {
		return Config.getCurrProjFilenamePrefix() + numClustersList.get(0) + "-" + numClustersList.get(numClustersList.size() - 1) + "_" + selectedAlg + "_" + simMeasure + "_clusters_mojo_next.csv";
	}

	public static String getPrecisionRecallCSVFilename(final List<Integer> numClustersList) {
		return Config.getCurrProjFilenamePrefix() + numClustersList.get(0) + "-" + numClustersList.get(numClustersList.size() - 1) + "_clusters_pr.csv";
	}

	public static void setClustersToWriteList(final List<Integer> inClustersToWriteList) {
		clustersToWriteList = inClustersToWriteList;

	}

	public static List<Integer> getClustersToWriteList() {
		return clustersToWriteList;
	}

	public static String getFilteredRoutineFactsFilename() {
		return Config.getCurrProjFilenamePrefix() + "_filteredRoutineFacts.rsf";
	}

	public static String getFilteredFactsFilename() {
		return Config.getCurrProjFilenamePrefix() + "_filteredFacts.rsf";
	}

	public static String getClassGraphRsfFilename() {
		return Config.getCurrProjFilenamePrefix() + "_class_graph_facts.rsf";
	}

	public static List<String> getExcludedEntities() {
		return excludedEntities;
	}

	public void writeConfigToFile() {
		final XStream xstream = new XStream();
		// xstream.omitField(formData.class, "this$0");
		final String xml = xstream.toXML(this);
		// System.out.println(xml);
		try {
			org.apache.commons.io.FileUtils.writeStringToFile(FileUtil.checkFile(configDataFilename, false, false), xml);
		} catch (final IOException e) {
			System.out.println("Unable to save form data!");
		}
	}

	/**
	 * @return the pythonloc
	 */
	public String getPythonloc() {
		return pythonloc;
	}

	/**
	 * @param pythonloc
	 *            the pythonloc to set
	 */
	public void setPythonloc(final String pythonloc) {
		this.pythonloc = pythonloc;
	}

	/**
	 * @return the batchPackagerLoc
	 */
	public String getBatchPackagerLoc() {
		return batchPackagerLoc;
	}

	/**
	 * @param batchPackagerLoc
	 *            the batchPackagerLoc to set
	 */
	public void setBatchPackagerLoc(final String batchPackagerLoc) {
		this.batchPackagerLoc = batchPackagerLoc;
	}

	/**
	 * @return the pkgPrefixes
	 */
	public String getPkgPrefixes() {
		return pkgPrefixes;
	}

	/**
	 * @param pkgPrefixes
	 *            the pkgPrefixes to set
	 */
	public void setPkgPrefixes(final String pkgPrefixes) {
		this.pkgPrefixes = pkgPrefixes;
	}

}
