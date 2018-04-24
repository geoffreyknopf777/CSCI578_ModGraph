package edu.usc.softarch.arcade.antipattern.detection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.logging.log4j.Logger;


import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.util.FileUtil;

public class SmellDetectionEvaluator {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(SmellDetectionEvaluator.class);

	// the key is a pair consisting of a smell type and technique filename
	// the value is the ratio of the matched smell count for that type over the
	// number of ground truth smells for that type for the technique
	// this is essentially the coverage ratio for the technique over a smell
	// type
	public static Map<Pair<Class, String>, Double> smellTypeTechRatioMap = new TreeMap<Pair<Class, String>, Double>(new ClassStringPairComparator());
	public static boolean configureLogging = true;

	public static void resetData() {
		smellTypeTechRatioMap = new HashMap<Pair<Class, String>, Double>();
	}

	public static class FileScorePairAscending implements Comparator<Pair<String, Double>> {

		@Override
		public int compare(final Pair<String, Double> p1, final Pair<String, Double> p2) {
			return p1.getRight().compareTo(p2.getRight()); // ascending order
		}
	}

	public static class FileScorePairDescending implements Comparator<Pair<String, Double>> {

		@Override
		public int compare(final Pair<String, Double> p1, final Pair<String, Double> p2) {
			return p2.getRight().compareTo(p1.getRight()); // descending order
		}
	}

	public static void main(final String[] args) {
		if (configureLogging) {
			//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		}

		resetData();

		final File groundTruthFile = FileUtil.checkFile(args[0], false, false);
		final File techniquesDir = FileUtil.checkDir(args[1], false, false);
		final File smellTechTableFilename = FileUtil.checkFile(args[2], false, false);
		final File detectedSmellsGtFilename = FileUtil.checkFile(args[3], false, false);
		String smellFileToMojoFilename = null;
		if (args.length == 5) {
			smellFileToMojoFilename = args[4];
		}
		final String gtFileOutput = "ground truth file: " + groundTruthFile;
		final String techDirOutput = "techniques directory: " + techniquesDir;
		System.out.println(gtFileOutput);
		logger.debug(gtFileOutput);
		System.out.println(techDirOutput);
		logger.debug(techDirOutput);

		// final File techDir = new File(techniquesDir);
		// assert techDir.exists() : techniquesDir + " does not exist.";

		// Deserialize detected gt smells
		final Set<Smell> detectedGtSmells = SmellUtil.deserializeDetectedSmells(detectedSmellsGtFilename);
		logger.debug("");
		logger.debug("Listing detected gt smells: ");
		for (final Smell smell : detectedGtSmells) {
			logger.debug(SmellUtil.getSmellAbbreviation(smell) + " " + smell);
		}

		// obtain ser files in techniques directory
		final File[] detectedSmellsFiles = techniquesDir.listFiles((FileFilter) file -> file.getName().endsWith(".ser"));

		// obtain names of detected smells files in techniques directory
		final List<File> dsFileList = Arrays.asList(detectedSmellsFiles);
		final List<String> dsFilenames = Lists.newArrayList(Iterables.transform(dsFileList, file -> file.getAbsolutePath()));

		logger.debug(Joiner.on(",").join(dsFilenames));

		final Map<String, Set<Smell>> fileToSmellInstancesMap = new LinkedHashMap<String, Set<Smell>>();
		for (final String dsFilename : dsFilenames) {
			final Set<Smell> detectedSmells = SmellUtil.deserializeDetectedSmells(dsFilename);
			fileToSmellInstancesMap.put(dsFilename, detectedSmells);
		}

		logger.debug("");
		logger.debug("Listing technique filenames: ");
		logger.debug(Joiner.on("\n").join(fileToSmellInstancesMap.keySet()));

		// Key: Ground-truth smell, Value: All matching smell and the technique
		// from which the smell originates
		final Map<Smell, Set<Pair<Smell, String>>> gtSmellToPairMap = new HashMap<Smell, Set<Pair<Smell, String>>>();

		// Key: ser filename, Value: coverage ratio
		final Map<String, Double> fileCoverageMap = new HashMap<String, Double>();
		for (final Entry<String, Set<Smell>> fsiEntry : fileToSmellInstancesMap.entrySet()) {
			final String currFilename = fsiEntry.getKey();
			final Set<Smell> detectedTechSmells = fsiEntry.getValue();

			final Map<Smell, Smell> maxSmellMap = new LinkedHashMap<Smell, Smell>();
			for (final Smell gtSmell : detectedGtSmells) {
				double maxSim = 0;
				Smell maxSmell = null;
				for (final Smell techSmell : detectedTechSmells) {
					if (gtSmell.getClass().equals(techSmell.getClass())) {
						final double sim = calcSimOfSmellInstances(gtSmell, techSmell);
						// logger.debug(gtSmell + ", " + techSmell + ": " +
						// sim);
						if (sim > maxSim) {
							maxSim = sim;
							maxSmell = techSmell;
						}
					}
				}
				maxSmellMap.put(gtSmell, maxSmell);
			}

			logger.debug("");
			logger.debug("Each ground-truth smell and it's max match from " + currFilename + ": ");
			for (final Smell gtSmell : maxSmellMap.keySet()) {
				final Smell techSmell = maxSmellMap.get(gtSmell);
				if (techSmell != null) {
					final double sim = calcSimOfSmellInstances(gtSmell, techSmell);
					logger.debug(SmellUtil.getSmellAbbreviation(gtSmell) + " " + gtSmell + ", " + techSmell + ": " + sim);
				} else {
					logger.debug(SmellUtil.getSmellAbbreviation(gtSmell) + " " + gtSmell + ", " + techSmell + ": 0");
				}
			}

			logger.debug("");
			logger.debug("Smells > 0.5 matching for " + currFilename + ": ");
			int above50Count = 0;
			for (final Smell gtSmell : maxSmellMap.keySet()) {
				final Smell maxSmell = maxSmellMap.get(gtSmell);
				if (maxSmell != null) {
					final double sim = calcSimOfSmellInstances(gtSmell, maxSmell);
					if (sim > .5) {
						logger.debug(SmellUtil.getSmellAbbreviation(gtSmell) + " " + gtSmell + ", " + maxSmell + ": " + sim);
						final Pair<Smell, String> smellFilePair = new ImmutablePair<Smell, String>(maxSmell, currFilename);
						if (gtSmellToPairMap.containsKey(gtSmell)) {
							final Set<Pair<Smell, String>> pairs = gtSmellToPairMap.get(gtSmell);
							pairs.add(smellFilePair);
						} else {
							final Set<Pair<Smell, String>> pairs = new HashSet<Pair<Smell, String>>();
							pairs.add(smellFilePair);
							gtSmellToPairMap.put(gtSmell, pairs);
						}

						above50Count++;
					}
				}
			}
			final double coverageRatio = (double) above50Count / (double) maxSmellMap.keySet().size();
			logger.debug("Ratio of Smells above 0.5 matching: " + coverageRatio);
			fileCoverageMap.put(currFilename, coverageRatio);
		}

		if (args.length == 5) {
			computePearsonCorrelationOfMojoFMToSmellAccuracy(smellFileToMojoFilename, fileCoverageMap);
		}

		final String allHighMatchedSmellsStr = Joiner.on("\n").withKeyValueSeparator("=").join(gtSmellToPairMap);
		logger.debug("");
		logger.debug("All high matched smells: ");
		logger.debug(allHighMatchedSmellsStr);

		for (final Smell gtSmell : gtSmellToPairMap.keySet()) {
			final Set<Pair<Smell, String>> pairs = gtSmellToPairMap.get(gtSmell);
			for (final Pair<Smell, String> smellFilePair : pairs) {
				logger.debug(gtSmell + " -> " + smellFilePair.getLeft() + " from " + smellFilePair.getRight());
			}
		}

		logger.debug("");
		logger.debug("Showing matched and unmatched smells:");
		for (final Smell gtSmell : detectedGtSmells) {
			if (gtSmellToPairMap.containsKey(gtSmell)) {
				final Set<Pair<Smell, String>> smellFilePairs = gtSmellToPairMap.get(gtSmell);
				for (final Pair<Smell, String> smellFilePair : smellFilePairs) {
					logger.debug(gtSmell + " -> " + smellFilePair.getLeft() + " from " + smellFilePair.getRight());
				}
			} else {
				logger.debug(gtSmell + " has no match");
			}
		}

		// key: a type of smell, value: the number of instances of that type of
		// smell in the ground truth
		final Map<Class, AtomicInteger> gtSmellTypeCountMap = new HashMap<Class, AtomicInteger>();
		for (final Class smellClass : SmellUtil.getSmellClasses()) {
			for (final Smell gtSmell : detectedGtSmells) {
				if (smellClass.isInstance(gtSmell)) {
					if (gtSmellTypeCountMap.containsKey(smellClass)) {
						gtSmellTypeCountMap.get(smellClass).incrementAndGet();
					} else {
						gtSmellTypeCountMap.put(smellClass, new AtomicInteger(1));
					}

				}
			}
		}

		logger.debug("");
		logger.debug("Number of instances for each smell type in groud-truth smells:");
		logger.debug(Joiner.on("\n").withKeyValueSeparator("=").join(gtSmellTypeCountMap));

		// map containing the number of instances a technique matched a smell
		// type
		// key: (type of smell, technique filename), value: the number of
		// instances a technique matched a smell type
		final Map<Pair<Class, String>, AtomicInteger> matchedSmellTypeCountMap = new HashMap<Pair<Class, String>, AtomicInteger>();
		logger.debug("");
		logger.debug("Populating the map counting how many times a smell type is matched by a technique:");
		for (final Smell gtSmell : detectedGtSmells) {

			if (gtSmellToPairMap.containsKey(gtSmell)) {
				final Set<Pair<Smell, String>> smellFilePairs = gtSmellToPairMap.get(gtSmell);
				for (final Pair<Smell, String> smellFilePair : smellFilePairs) {
					final Smell matchedSmell = smellFilePair.getLeft();
					final String resultsFilename = smellFilePair.getRight();
					for (final Class smellClass : SmellUtil.getSmellClasses()) {
						if (smellClass.isInstance(matchedSmell)) {
							logger.debug(resultsFilename + ":" + matchedSmell + " is an instance of " + smellClass.getSimpleName());
							final Pair<Class, String> pair = new ImmutablePair<Class, String>(smellClass, resultsFilename);
							if (matchedSmellTypeCountMap.containsKey(pair)) {
								matchedSmellTypeCountMap.get(pair).incrementAndGet();
							} else {
								matchedSmellTypeCountMap.put(pair, new AtomicInteger(1));
							}
						}
					}
				}
			} else {
				logger.debug(gtSmell + " has no match");
			}
		}

		logger.debug("");
		logger.debug("Number of instances for each smell type in matched smells:");
		logger.debug(Joiner.on("\n").withKeyValueSeparator("=").join(matchedSmellTypeCountMap));

		final List<String> dsShortFilenames = Lists.newArrayList(Iterables.transform(dsFilenames, filename -> FileUtil.extractFilenamePrefix(filename)));
		String smellTechTable = "";
		smellTechTable += "," + Joiner.on(",").join(dsShortFilenames);
		smellTechTable += "\n";
		for (final Class<?> smellClass : SmellUtil.getSmellClasses()) {
			smellTechTable += smellClass.getSimpleName() + ",";
			if (!gtSmellTypeCountMap.containsKey(smellClass)) {
				// for (final String filename : dsFilenames) {
				for (int i = 0; i < dsFilenames.size(); i++) { // Changed by
					// Daniel, test!
					smellTechTable += "NA,";
				}
			} else {
				for (final String filename : dsFilenames) {
					final Pair<Class, String> pair = new ImmutablePair<Class, String>(smellClass, filename);
					if (matchedSmellTypeCountMap.containsKey(pair)) {
						final double ratio = (double) matchedSmellTypeCountMap.get(pair).get() / (double) gtSmellTypeCountMap.get(pair.getLeft()).get();
						smellTypeTechRatioMap.put(pair, ratio);
						smellTechTable += ratio + ",";
					} else {
						smellTechTable += "0,";
					}
				}
			}
			smellTechTable += "\n";
		}
		logger.debug("\n" + smellTechTable);

		PrintWriter writer;
		try {
			writer = new PrintWriter(smellTechTableFilename, "UTF-8");

			writer.println(smellTechTable);
			writer.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		final double numofMatchedGtSmells = gtSmellToPairMap.keySet().size();
		final double numOfTotalGtSmells = detectedGtSmells.size();

		logger.debug("");
		logger.debug("number of matched gt smells: " + numofMatchedGtSmells);
		logger.debug("number of total gt smells: " + numOfTotalGtSmells);
		final double detectedGtSmellsRatio = numofMatchedGtSmells / numOfTotalGtSmells;
		logger.debug("Ratio of detected gt smells: " + detectedGtSmellsRatio);

		// analyzeSmellsPerTypeAcrossTechniques(groundTruthFile,
		// smellTechTableFilename, csvFileToSmellsMap, techDirFile);

	}

	private static void computePearsonCorrelationOfMojoFMToSmellAccuracy(final String smellFileToMojoFilename, final Map<String, Double> fileCoverageMap) {
		// key: smell filename, value: mojofm score
		final List<Pair<String, Double>> fileScorePairs = new ArrayList<Pair<String, Double>>();
		final Path smellFileToMojoPath = Paths.get(smellFileToMojoFilename);
		try (InputStream in = Files.newInputStream(smellFileToMojoPath); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				final String[] tokens = line.split(",");
				final String filename = tokens[0];
				final double mojoFmScore = Double.parseDouble(tokens[1]);
				fileScorePairs.add(new ImmutablePair<String, Double>(filename, mojoFmScore));
			}
		} catch (final IOException x) {
			System.err.println(x);
		}

		Collections.sort(fileScorePairs, new FileScorePairAscending());
		logger.debug("");
		for (final Pair<String, Double> pair : fileScorePairs) {
			logger.debug(pair.getLeft() + "," + pair.getRight());
		}

		final double[] coverageArray = new double[fileCoverageMap.entrySet().size()];
		final double[] scoreArray = new double[fileScorePairs.size()];
		int i = 0;
		for (final Pair<String, Double> pair : fileScorePairs) {
			final String filename = pair.getLeft();
			final double score = pair.getRight();
			final double coverage = fileCoverageMap.get(filename);
			scoreArray[i] = score;
			coverageArray[i] = coverage;
			i++;
		}

		final PearsonsCorrelation pearsons = new PearsonsCorrelation();
		final double correlationCoefficient = pearsons.correlation(scoreArray, coverageArray);

		final List<Double> coverageList = Arrays.asList(ArrayUtils.toObject(coverageArray));
		final List<Double> scoreList = Arrays.asList(ArrayUtils.toObject(scoreArray));
		logger.debug("");
		logger.debug("MoJoFM Scores:");
		logger.debug(scoreList);
		logger.debug("Coverage Ratios:");
		logger.debug(coverageList);
		logger.debug("MoJoFM to Coverage correlation: " + correlationCoefficient);
	}

	private static double calcSimOfSmellInstances(final Smell gtSmell, final Smell techSmell) {
		final Set<String> gtEntities = new HashSet<String>();
		final Set<String> techEntities = new HashSet<String>();

		for (final ConcernCluster cluster : gtSmell.clusters) {
			gtEntities.addAll(cluster.getEntities());
		}
		for (final ConcernCluster cluster : techSmell.clusters) {
			techEntities.addAll(cluster.getEntities());
		}

		final Set<String> intersection = new HashSet<String>(gtEntities);
		intersection.retainAll(techEntities);

		final Set<String> union = new HashSet<String>(gtEntities);
		union.addAll(techEntities);

		final double simRatio = (double) intersection.size() / (double) union.size();

		return simRatio;
	}

	// private static void logSmellToClassesMap(
	// final Map<String, Set<String>> smellToClassesMap,
	// final int valuesLimit) {
	// final int valuesLimitIndex = valuesLimit - 1;
	// for (final Entry<String, Set<String>> s2cEntry : smellToClassesMap
	// .entrySet()) {
	// final List<String> classesList = new ArrayList<String>(
	// s2cEntry.getValue());
	// List<String> limitedClassesList = null;
	// if (classesList.size() < valuesLimit) {
	// limitedClassesList = classesList;
	// } else {
	// limitedClassesList = classesList.subList(0, valuesLimitIndex);
	// }
	// final String formattedEntry = s2cEntry.getKey() + " : "
	// + limitedClassesList;
	// logger.debug(formattedEntry);
	// }
	// }
}
