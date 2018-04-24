package edu.usc.softarch.arcade.topics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.apache.logging.log4j.Logger;

import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.InstanceList;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.mallet.Import;

/**
 * @author joshua
 *
 */
public class DocTopics {
	ArrayList<DocTopicItem> dtItemList = new ArrayList<>();
	private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DocTopics.class);
	File srcDir;
	Config currentConfig = Controller.getCurrentView().getConfig();
	File superModelVersionDir = currentConfig.getSuperModelSelectedVersionsDir();
	File artifactsDir;
	private int numTopics;
	Import i = new Import();

	public int getNumTopics() {
		return numTopics;
	}

	public void setNumTopics(final int numTopics) {
		this.numTopics = numTopics;
	}

	File topicModelFile;
	File docTopicsFile;
	File superTopicModel;
	File topWordsFile;
	boolean checkJavaFiles;
	boolean checkCFiles;
	ArrayList<Pipe> pipeList;
	static InstanceList instances = null;
	static boolean firstTime = true;

	InstanceList previousInstances;

	// Run the model for 50 iterations and stop (this is for
	// testing only,
	// // for real applications, use 1000 to 2000 iterations)
	final int numIterations = currentConfig.getmIterations();

	public DocTopics() {
		super();
		logger.traceEntry();
		logger.traceExit();
	}

	/**
	 * Java should write copy constructors automatically
	 *
	 * @param docTopics
	 */
	public DocTopics(final DocTopics docTopics) {
		logger.entry(docTopics);
		for (final DocTopicItem docTopicItem : docTopics.dtItemList) {
			dtItemList.add(new DocTopicItem(docTopicItem));
		}
		logger.traceExit();
	}

	public DocTopics(final String srcDir, final String artifactsDir, final int numTopics, final String topicModelFilename, final String docTopicsFilename, final String topWordsFilename) {
		logger.entry(srcDir, artifactsDir, numTopics, topicModelFilename, docTopicsFilename, topWordsFilename);
		this.srcDir = FileUtil.checkDir(srcDir, false, true);
		this.artifactsDir = FileUtil.checkDir(artifactsDir, true, false);
		this.numTopics = numTopics;

		// only create one topic model
		final int last = topicModelFilename.lastIndexOf(File.separator);
		final String temp = topicModelFilename.substring(0, last) + File.separator + "topicmodel.mallet";
		topicModelFile = FileUtil.checkFile(temp, false, false);

		// topicModelFile = FileUtil.checkFile(topicModelFilename, false,
		// false);
		docTopicsFile = FileUtil.checkFile(docTopicsFilename, false, false);
		topWordsFile = FileUtil.checkFile(topWordsFilename, false, false);
		checkJavaFiles = Config.getSelectedLanguage().equals(Config.Language.java);
		checkCFiles = Config.getSelectedLanguage().equals(Config.Language.c);
		logger.debug("Java selected = " + Boolean.toString(checkJavaFiles) + ", C/C++ selected = " + Boolean.toString(checkCFiles));
		// Begin by importing documents from text to feature sequences

		if (instances == null) {
			instances = new InstanceList(new SerialPipes(i.preparePipeList(checkCFiles, checkJavaFiles)));
		}
		logger.debug("Size of instances after building pipeList = " + instances.size());
		logger.traceExit();
	}

	public int generateTopicModel(int genNumTopics) {
		logger.entry(genNumTopics);
		logger.info("Building instances for MALLET, number of topics = " + genNumTopics);
		if (currentConfig.isUseSuperModel()) {
			System.out.println("Building supermodel...");
			if (firstTime) {
				firstTime = false;
				instances = Import.buildInstances(instances, superModelVersionDir, checkCFiles, checkJavaFiles);
			}
		} else {
			System.out.println("Not using supermodel");
			System.out.println("Source dir = " + srcDir);
			final Import i = new Import();
			instances = new InstanceList(new SerialPipes(i.preparePipeList(checkCFiles, checkJavaFiles)));
			previousInstances = instances;
			instances = Import.buildInstances(instances, srcDir, checkCFiles, checkJavaFiles);
		}
		// File preIns = new File("tmp/int.data");
		// if (preIns.exists()){
		// previousInstances = InstanceList.load(preIns);
		// }else{
		// previousInstances = instances;
		// }
		// //save for next time
		// instances.save(new File("tmp/int.data"));
		// final File f = new
		// File(Config.getPerUserArcadeDir().getAbsolutePath() + File.separator
		// + "output.pipe");
		// f.createNewFile();
		// if (!f.exists()) {
		// System.out.println(f.toString() + " does not exist - exiting");
		// System.exit(-1);
		// }
		previousInstances = instances; // InstanceList.load(f);
		logger.debug("previousInstances.size() = " + previousInstances.size());
		logger.debug("instances.size() = " + instances.size());

		/*
		 * Reader fileReader = new InputStreamReader(new FileInputStream(new File( args[0])), "UTF-8"); instances.addThruPipe(new CsvIterator(fileReader, Pattern .compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, // label, // name // fields
		 */
		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		// Note that the first parameter is passed as the sum over topics, while
		// the second is
		// int numTopics = 40;
		// final double alpha = (double) 50 / (double) genNumTopics;
		// logger.info("Current MALLET alpha: " + alpha);
		// final double beta = .01;
		ParallelTopicModel model = null;
		// final File topicModelFile = new File(topicModelFilename);
		// final File docTopicsFile = new File(docTopicsFilename);
		// final File topWordsFile = new File(topWordsFilename);

		// TopicInferencer inferencer = null;
		// try {
		// inferencer = TopicInferencer.read(new
		// File(Config.getPerUserArcadeDir().getAbsolutePath() + File.separator
		// +"infer.mallet"));
		// } catch (Exception e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		if (topicModelFile.exists() && currentConfig.isUseSuperModel()) {
			System.out.println("Trying to read topic model file " + topicModelFile.getAbsolutePath());
			try {
				model = ParallelTopicModel.read(topicModelFile);
			} catch (final Exception e) {
				logger.error("Unable to read topic model file named " + topicModelFile.getAbsolutePath());
				e.printStackTrace();
				System.exit(-1);
			}
		} else {
			final boolean useNewLines = false;
			final int numTopWords = currentConfig.getmTopWords();
			boolean modelGood = false; // Whether the model has good topics
			boolean modelFinished = false; // Whether the model is done
			boolean maxReached = false;

			// Start with 2 topics
			if (!currentConfig.isUseARCAbsoluteTopicsNumber()) {
				genNumTopics = 2;
			}

			int minTopics = Integer.MAX_VALUE;
			int maxTopics = 0;

			// Iterate until the model is finished
			do {
				final int oldGenNumTopics = genNumTopics;
				final double alpha = (double) 50 / (double) genNumTopics;
				logger.info("Current MALLET alpha: " + alpha);
				final double beta = currentConfig.getmBeta();
				model = new ParallelTopicModel(genNumTopics, alpha, beta);
				model.addInstances(instances);
				//
				// // Use two parallel samplers, which each look at one half the
				// corpus and combine statistics after every iteration.
				model.setNumThreads(currentConfig.getmThreads());
				model.setNumIterations(numIterations);
				// But in MALLET may prevent seed from being set
				model.setRandomSeed(currentConfig.getmRandomSeed());

				// Adjust the number of topics until the optimal number is found
				// For now, a topic model is bad if two topics have the same
				// most common word in them

				try {
					model.estimate();
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				model.write(topicModelFile);
				try {
					model.printDocumentTopics(docTopicsFile);
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				// if (currentConfig.isUseARCAbsoluteClustersNumber()) {
				// logger.traceExit();
				// return genNumTopics;
				// }

				final Object[][] topWords = model.getTopWords(genNumTopics);
				final ArrayList<String> firstWords = new ArrayList<>();
				modelGood = true;
				if (!currentConfig.isUseARCAbsoluteClustersNumber()) {
					for (int i = 0; i < genNumTopics; i++) {
						final Object[] currentTopic = topWords[i];
						final String firstWord = (String) currentTopic[0];
						if (firstWords.contains(firstWord)) {
							modelGood = false;
							maxReached = true;
							maxTopics = genNumTopics;
							logger.debug("Model is not good");
							final int previousGenNumTopics = genNumTopics;
							genNumTopics = (genNumTopics + minTopics) / 2;
							if (previousGenNumTopics == genNumTopics) {
								logger.debug("Forcing decrease of genNumTopics");
								genNumTopics--;
							}
							logger.debug("genNumTopics = " + genNumTopics);
							if (genNumTopics < 2) {
								System.out.println("genNumTopics must be 2 or larger, cannot run ARC with just one concern - exiting");
								System.exit(-1);
							}
							break;
						} else {
							firstWords.add(firstWord);
						}
					}
					if (modelGood) {
						logger.debug("Model is good");
						minTopics = genNumTopics;
						// genNumTopics += stepSize;
						if (!maxReached) {
							genNumTopics *= 2;
						} else {
							genNumTopics = (genNumTopics + maxTopics) / 2;
						}
						logger.debug("genNumTopics = " + genNumTopics);
					}

					logger.debug("minTopics = " + minTopics + ", maxTopics = " + maxTopics);
					if (oldGenNumTopics == genNumTopics && modelGood) {
						modelFinished = true;
					}
				}
			} while (!modelFinished && !currentConfig.isUseARCAbsoluteClustersNumber());
			try {
				logger.debug(model.displayTopWords(numTopWords, useNewLines));
				model.printTopWords(topWordsFile, numTopWords, useNewLines);
			} catch (final IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		logger.debug("previousInstances.size() = " + previousInstances.size());
		logger.debug("instances.size() = " + instances.size());
		logger.debug(model.data.size());
		for (int instIndex = 0; instIndex < previousInstances.size(); instIndex++) {

			final DocTopicItem dtItem = new DocTopicItem();
			dtItem.doc = instIndex;
			dtItem.source = (String) previousInstances.get(instIndex).getName();

			dtItem.topics = new ArrayList<>();
			final double[] topicDistribution = model.getTopicProbabilities(instIndex);
			// final double[] topicDistribution =
			// inferencer.getSampledDistribution(previousInstances.get(instIndex),
			// 1000, 10, 10);
			for (int topicIdx = 0; topicIdx < topicDistribution.length; topicIdx++) {
				final TopicItem t = new TopicItem();
				t.topicNum = topicIdx;
				t.proportion = topicDistribution[topicIdx];
				dtItem.topics.add(t);
			}
			dtItemList.add(dtItem);
			// for (final DocTopicItem dti : dtItemList)
			// System.out.println(dti.toStringWithLeadingTabsAndLineBreaks(5));
		}
		logger.traceExit(genNumTopics);
		return genNumTopics;
	}

	/**
	 * @param filename
	 * @throws FileNotFoundException
	 */
	public DocTopics(final String filename) {
		logger.entry(filename);
		try {
			loadFromFile(filename);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Unable to load doc topics file named \"" + filename + "\"");
			System.exit(-1);
		}
		logger.traceExit();
	}

	public DocTopicItem getDocTopicItemForJava(final String name) {
		logger.entry(name);
		for (final DocTopicItem dti : dtItemList) {
			final String altName = name.replaceAll("/", ".").replaceAll(".java", "").trim();
			if (dti.source.endsWith(name)) {
				return dti;
			} else if (altName.equals(dti.source.trim())) {
				return dti;
			}
		}
		logger.traceExit();
		return null;
	}

	public DocTopicItem getDocTopicItemForC(final String name) {
		logger.entry(name);
		for (final DocTopicItem dti : dtItemList) {
			String strippedSource = null;
			String nameWithoutQuotations = null;
			if (dti.source.endsWith(".func")) {
				strippedSource = dti.source.substring(dti.source.lastIndexOf('/') + 1, dti.source.lastIndexOf(".func"));
				nameWithoutQuotations = name.replaceAll("\"", "");
				if (strippedSource.contains(nameWithoutQuotations)) {
					logger.traceExit();
					return dti;
				}
			} else if (dti.source.endsWith(".c") || dti.source.endsWith(".h") || dti.source.endsWith(".tbl") || dti.source.endsWith(".p") || dti.source.endsWith(".cpp") || dti.source.endsWith(".s")
					|| dti.source.endsWith(".hpp") || dti.source.endsWith(".icc") || dti.source.endsWith(".ia")) {
				// strippedSource =
				// dti.source.substring(dti.source.lastIndexOf('/')+1,dti.source.length());
				nameWithoutQuotations = name.replaceAll("\"", "");
				if (dti.source.endsWith(nameWithoutQuotations)) {
					logger.traceExit();
					return dti;
				}
			} else if (dti.source.endsWith(".S")) {
				final String dtiSourceRenamed = dti.source.replace(".S", ".c");
				nameWithoutQuotations = name.replaceAll("\"", "");
				if (dtiSourceRenamed.endsWith(nameWithoutQuotations)) {
					logger.traceExit();
					return dti;
				}
			} else {
				// System.err.println("Unknown file type for " + dti.source);
				// System.exit(1);
				// return dti;
				continue;
			}

		}
		logger.error("Cannot find doc topic for: " + name);
		logger.traceExit();
		return null;
	}

	public ArrayList<DocTopicItem> getDocTopicItemList() {
		logger.traceEntry();
		logger.traceExit();
		return dtItemList;
	}

	public void loadFromFile(final String filename) throws FileNotFoundException {
		logger.entry(filename);
		logger.debug("Loading DocTopics from file...");
		final boolean localDebug = false;
		final File f = new File(filename);

		final Scanner s = new Scanner(f);

		dtItemList = new ArrayList<>();

		while (s.hasNext()) {
			final String line = s.nextLine();
			if (line.startsWith("#")) {
				continue;
			}
			final String[] items = line.split("\\s+");

			final DocTopicItem dtItem = new DocTopicItem();
			dtItem.doc = new Integer(items[0]).intValue();
			dtItem.source = items[1];

			dtItem.topics = new ArrayList<>();

			TopicItem t = new TopicItem();
			for (int i = 2; i < items.length; i++) {
				if (i % 2 == 0) {
					t.topicNum = new Integer(items[i]).intValue();
				} else {
					t.proportion = new Double(items[i]).doubleValue();
					dtItem.topics.add(t);
					t = new TopicItem();
				}
			}
			dtItemList.add(dtItem);
			if (localDebug) {
				logger.debug(line);
			}
			s.close();
		}

		if (localDebug) {
			logger.debug("\n");
		}
		for (final DocTopicItem dtItem : dtItemList) {
			if (localDebug) {
				logger.debug(dtItem);
			}
		}
		logger.traceExit();
	}

}
