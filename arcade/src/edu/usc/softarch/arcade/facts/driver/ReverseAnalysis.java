package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;

import cc.mallet.util.Maths;
import edu.usc.softarch.arcade.MetricsDriver;
import edu.usc.softarch.arcade.clustering.Entity;
import edu.usc.softarch.arcade.clustering.SimCalcUtil;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.topics.TopicUtil;

public class ReverseAnalysis {
	static Map<String, Integer> featureNameToBitsetIndex = new HashMap<String, Integer>();
	static int bitSetSize = 0;
	BufferedWriter out;

	enum SimilarityMeasure {
		UELLENBERG, JS, LIMBO, BUNCH, UNM, PKG
	};

	SimilarityMeasure sm;

	enum LangType {
		JAVA, C
	};

	static LangType selectedLangType;

	/**
	 * method that calculates the result for every cluster and displays it
	 *
	 * @throws IOException
	 */
	private void calculateResults(final Map<String, Map<String, Entity>> clusterNameToEntities, final Map<String, Set<MutablePair<String, String>>> internalEdgeMap,
			final Map<String, Set<MutablePair<String, String>>> externalEdgeMap, final HashMap<String, Integer> pkgSizeMap) throws IOException {
		final Map<String, Set<String>> clusterNameToEntitiesNames = new HashMap<String, Set<String>>();
		for (final String clusterName : clusterNameToEntities.keySet()) {

			final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
			final Object[] entities = nameToEntity.values().toArray();

			final Set<String> entityNames = new HashSet<String>();
			for (final Object obj : entities) {
				final Entity entity = (Entity) obj;
				entityNames.add(entity.getName());
			}
			clusterNameToEntitiesNames.put(clusterName, entityNames);

		}

		final Map<String, Double> domValMap = DominatorGroundTruthAnalyzer.computeDominatorCriteriaIndicatorValues(clusterNameToEntitiesNames, internalEdgeMap);

		for (final String clusterName : clusterNameToEntities.keySet()) {
			out.write(clusterName + ",");
			// Map<String, Entity> nameToEntity = clusterNameToEntities
			// .get(clusterName);

			System.out.println("CLUSTER NAME: " + clusterName);

			final double clusterSimUsingUE = computeClusterSimilarity(SimilarityMeasure.UELLENBERG, clusterName, clusterNameToEntities, internalEdgeMap, externalEdgeMap);
			System.out.println("Similarity measure for cluster " + clusterName + " using UE is " + clusterSimUsingUE);
			writeToFile(clusterSimUsingUE);

			final double clusterSimUsingUNM = computeClusterSimilarity(SimilarityMeasure.UNM, clusterName, clusterNameToEntities, internalEdgeMap, externalEdgeMap);
			System.out.println("Similarity measure for cluster " + clusterName + " using UNM is " + clusterSimUsingUNM);
			writeToFile(clusterSimUsingUNM);

			final double clusterSimUsingLimbo = computeClusterSimilarity(SimilarityMeasure.LIMBO, clusterName, clusterNameToEntities, internalEdgeMap, externalEdgeMap);
			writeToFile(clusterSimUsingLimbo);
			System.out.println("Similarity measure for cluster " + clusterName + " using LIMBO is " + clusterSimUsingLimbo);

			final double clusterSimUsingBunch = computeClusterSimilarity(SimilarityMeasure.BUNCH, clusterName, clusterNameToEntities, internalEdgeMap, externalEdgeMap);
			writeToFile(clusterSimUsingBunch);
			System.out.println("Similarity measure for cluster " + clusterName + " using BUNCH is " + clusterSimUsingBunch);

			final double clusterSimUsingJSDivergence = computeJSDivergence(clusterName, clusterNameToEntities);
			System.out.println("Similarity measure for cluster " + clusterName + " using JSDivergence is " + clusterSimUsingJSDivergence);
			writeToFile(clusterSimUsingJSDivergence); // - UNCOMMENT THIS OUT
			// FOR JSDIVERGENCE

			final double clusterSimUsingDom = domValMap.get(clusterName);
			System.out.println("Similarity measure for cluster " + clusterName + " using Subgraph Dominator Pattern is " + clusterSimUsingJSDivergence);
			writeToFile(clusterSimUsingDom); // - UNCOMMENT THIS OUT FOR
			// JSDIVERGENCE

			final double clusterSimUsingPkg = computePkgClusterSim(clusterName, clusterNameToEntities, pkgSizeMap);
			writeToFile(clusterSimUsingPkg);
			System.out.println("Similarity measure for cluster " + clusterName + " using PKG is " + clusterSimUsingPkg);

			out.newLine();
		}
		out.close();
	}

	private static HashMap<String, Integer> computePkgSizes(final List<List<String>> pkgFacts) {
		final HashMap<String, Integer> pkgSizeMap = new HashMap<String, Integer>();

		for (final List<String> fact : pkgFacts) {
			final String pkgName = fact.get(1);

			if (pkgSizeMap.containsKey(pkgName)) {
				pkgSizeMap.put(pkgName, pkgSizeMap.get(pkgName) + 1);
			} else {
				pkgSizeMap.put(pkgName, 1);
			}

		}

		return pkgSizeMap;
	}

	private double computePkgClusterSim(final String clusterName, final Map<String, Map<String, Entity>> clusterNameToEntities, final HashMap<String, Integer> pkgSizeMap) {
		final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
		final Object[] entities = nameToEntity.values().toArray();

		if (entities.length == 0) {
			System.out.println(clusterName + " has no entities, so skipping");
			return 0;
		}

		final List<String> entityNames = new ArrayList<String>();
		for (final Object obj : entities) {
			final Entity entity = (Entity) obj;
			entityNames.add(entity.getName());
		}

		String delimiter = "";
		String regexDelimiter = "";
		if (selectedLangType.equals(LangType.C)) {
			delimiter = "/";
			regexDelimiter = delimiter;
		} else if (selectedLangType.equals(LangType.JAVA)) {
			delimiter = ".";
			regexDelimiter = "\\.";
		} else {
			throw new RuntimeException("Invalid language selected");
		}

		final Map<String, Integer> pkgCountMap = new HashMap<String, Integer>();
		for (final Object obj : entities) {
			final Entity entity = (Entity) obj;

			final String[] tokens = entity.getName().split(regexDelimiter);
			String directoryName = "";
			final List<String> directoryNameParts = new ArrayList<String>();
			for (int i = 0; i < tokens.length - 1; i++) {
				directoryNameParts.add(tokens[i]);
			}
			directoryName = StringUtils.join(directoryNameParts, delimiter);

			if (pkgCountMap.containsKey(directoryName)) {
				pkgCountMap.put(directoryName, pkgCountMap.get(directoryName) + 1);
			} else {
				pkgCountMap.put(directoryName, 1);
			}
		}

		// Collection<Integer> pkgCounts = pkgCountMap.values();
		int maxCount = 0;
		String maxPkgName = "";
		boolean maxUpdated = false;
		for (final Entry<String, Integer> entry : pkgCountMap.entrySet()) {
			final int pkgCount = entry.getValue();
			final String pkgName = entry.getKey();
			if (pkgCount > maxCount) {
				maxCount = pkgCount;
				maxPkgName = pkgName;
				maxUpdated = true;
			}
		}
		assert maxUpdated;
		if (maxPkgName.endsWith(delimiter)) {
			maxPkgName = maxPkgName.substring(0, maxPkgName.length() - 1);
		}

		int pkgSize = 0;
		if (maxPkgName.equals("")) {
			pkgSize = pkgSizeMap.get("default.ss");
		} else {
			pkgSize = pkgSizeMap.get(maxPkgName);
		}
		assert pkgSize != 0;

		final double samePkgToClusterSizeRatio = (double) maxCount / (double) entities.length;
		// double samePkgToPkgSizeRatio = (double)maxCount/(double)pkgSize;
		// double simValWithSize =
		// (samePkgToClusterSizeRatio+samePkgToPkgSizeRatio)/2;
		final double simValNoSize = samePkgToClusterSizeRatio;

		return simValNoSize;

	}

	/**
	 * method calculates the average sim measure for a cluster
	 *
	 * @throws IOException
	 **/
	private double computeClusterSimilarity(final SimilarityMeasure sm, final String clusterName, final Map<String, Map<String, Entity>> clusterNameToEntities,
			final Map<String, Set<MutablePair<String, String>>> internalEdgeMap, final Map<String, Set<MutablePair<String, String>>> externalEdgeMap) throws IOException {
		if (sm == SimilarityMeasure.BUNCH) {
			double countInternalEdges = 0.0;
			double countExternalEdges = 0.0;
			// Set<MutablePair<String, String>> intEdges = internalEdgeMap
			// .get(clusterName);
			// Set<MutablePair<String,String>> extEdges =
			// externalEdgeMap.get(clusterName);
			// for (MutablePair<String,String> edge : intEdges)
			{
				countInternalEdges++;
			}
			// for (MutablePair<String,String> edge : extEdges)
			{
				countExternalEdges++;
			}
			final double cf = 2 * countInternalEdges / (2 * countInternalEdges + countExternalEdges);
			return cf;
		} else {
			final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
			final Object[] entities = nameToEntity.values().toArray();
			double sum = 0;
			int n = 0; // number of simMeasure values
			for (int i = 0; i < entities.length; i++) // double-for loop to get
			// two entities to
			// compute pairwise
			// similarity on
			{
				for (int j = i + 1; j < entities.length; j++) {
					final double simMeasure = computePairWiseSimilarity(sm, (Entity) entities[i], (Entity) entities[j]);
					sum = sum + simMeasure;
					n++;
				}
			}
			System.out.println("Sum and n are " + sum + " " + n);
			final double average = sum / n;
			return average;
		}
	}

	/** method to compute similarity between a pair of entities */
	private double computePairWiseSimilarity(final SimilarityMeasure sm, final Entity entity1, final Entity entity2) {
		if (sm == SimilarityMeasure.LIMBO) {
			final Set<Integer> c1Indices = entity1.getNonZeroFeatureMap().keySet();
			entity1.setNonZeroFeatureMapForLibmoUsingIndices(entity1, entity2, c1Indices);

			final Set<Integer> c2Indices = entity2.getNonZeroFeatureMap().keySet();
			entity2.setNonZeroFeatureMapForLibmoUsingIndices(entity1, entity2, c2Indices);
			return getInfoLossMeasure(2, entity1, entity2);
		}

		final BitSet fv1 = entity1.getFeatureVector();
		final BitSet fv2 = entity2.getFeatureVector();
		int count10 = 0;
		int count01 = 0;
		int count00 = 0;
		int count11 = 0;
		int sum11 = 0;
		for (int i = 0; i < fv1.size(); i++) {
			if (fv1.get(i) && !fv2.get(i)) {
				count10++;
			} else if (!fv1.get(i) && fv2.get(i)) {
				count01++;
			} else if (!fv1.get(i) && !fv2.get(i)) {
				count00++;
			} else {
				count11++;
				sum11 = sum11 + 1 + 1;
			}
		}
		if (sm == SimilarityMeasure.UELLENBERG) {
			final double denom = 0.5 * sum11 + count10 + count01;
			if (denom == 0) {
				return denom;
			}
			return 0.5 * sum11 / denom;
		} else if (sm == SimilarityMeasure.JS) {
			final double denom = count11 + count10 + count01;
			if (denom == 0) {
				return denom;
			}
			return count11 / denom;
		} else if (sm == SimilarityMeasure.UNM) {
			final double result = 0.5 * sum11 / (0.5 * sum11 + 2 * ((double) count10 + (double) count01) + count00 + count11);
			return result;
		}
		return 0;
	}

	/**
	 * method that produces a feature vector bitset for each entity in each cluster
	 **/
	public Map<String, Map<String, Entity>> buildFeatureSetPerClusterEntity(final Map<String, Set<String>> clusterMap, final List<List<String>> depFacts) {
		final Map<String, Map<String, Entity>> map = new HashMap<String, Map<String, Entity>>();

		for (final String clusterName : clusterMap.keySet()) { // for each
			// cluster
			// name
			Map<String, Entity> entityToFeatures = new HashMap<String, Entity>(); // using
			// a
			// map<String,Entity>
			// instead
			// of
			// a
			// list
			// of
			// entities
			// so
			// that
			// getting
			// the
			// feature
			// vector for an Entity name will be faster. Mapping name of entity
			// to Entity object.
			for (final List<String> depFact : depFacts) {
				Entity entity;
				final String source = depFact.get(1);
				final String target = depFact.get(2);

				if (clusterMap.get(clusterName).contains(source)) // if cluster
				// contains
				// entity
				{
					Set<String> featureSet; // featureSet contains a list of all
					// featureNames for that entity

					if (map.get(clusterName) != null) // if cluster already
					// exists in map that is
					// being built
					{
						entityToFeatures = map.get(clusterName);
					}
					if (entityToFeatures.get(source) != null) {
						featureSet = entityToFeatures.get(source).featureSet;
						entity = entityToFeatures.get(source);
					} else // otherwise create new ones
					{
						entity = new Entity(source);
						featureSet = new HashSet<String>();
					}
					featureSet.add(target); // adding target to set of features
					// for that entity
					entity.featureSet = featureSet;
					if (featureNameToBitsetIndex.get(target) == null) // if this
					// target
					// has
					// never
					// been
					// encountered
					// yet
					{
						featureNameToBitsetIndex.put(target, new Integer(bitSetSize));
						entity.getFeatureVector().set(bitSetSize); // setting the
						// spot for this
						// feature as 1
						// in the
						// entitie's
						// feature
						// vector
						bitSetSize++;
					} else {
						entity.getFeatureVector().set(featureNameToBitsetIndex.get(target)); // setting
						// that
						// feature
						// to
						// true
					}
					entity.initializeNonZeroFeatureMap(bitSetSize);
					entityToFeatures.put(source, entity);
				}
			}

			map.put(clusterName, entityToFeatures);
		}

		return map;
	}

	/*-----------------------------LIMBO STUFF--------------------------------------------*/
	/** copied pasted */
	public static double getInfoLossMeasure(final int numberOfEntitiesToBeClustered, final Entity entity1, final Entity entity2) {

		final double[] firstDist = new double[bitSetSize];
		final double[] secondDist = new double[bitSetSize];
		normalizeFeatureVectorOfCluster(entity1, bitSetSize, firstDist);
		normalizeFeatureVectorOfCluster(entity2, bitSetSize, secondDist);

		double jsDivergence = Maths.jensenShannonDivergence(firstDist, secondDist);
		System.out.println("JsDivergence is " + jsDivergence);
		if (Double.isInfinite(jsDivergence)) {
			jsDivergence = Double.MAX_VALUE;
		}
		System.out.println("numentities of entity1 " + entity1.getNumOfEntities());
		final double infoLossMeasure = ((double) entity1.getNumOfEntities() / numberOfEntitiesToBeClustered + (double) entity2.getNumOfEntities() / numberOfEntitiesToBeClustered) * jsDivergence;
		System.out.println("InfoLossMeasure is " + infoLossMeasure);
		if (Double.isNaN(infoLossMeasure)) {
			throw new RuntimeException("infoLossMeasure is NaN");
		}
		return infoLossMeasure;
	}

	private static void normalizeFeatureVectorOfCluster(final Entity entity, final int featuresLength, final double[] firstDist) {
		for (int i = 0; i < featuresLength; i++) {
			if (entity.getNonZeroFeatureMap().get(i) != null) {
				final double featureValue = entity.getNonZeroFeatureMap().get(i);
				firstDist[i] = featureValue / entity.getNonZeroFeatureMap().size();
			} else { // this feature is zero
				firstDist[i] = 0;
			}
		}
	}

	// ----------------------DOCTOPICITEM
	// STUFF----------------------------------------------------//
	/**
	 * method to load doc-topic-item for each entity - reference ConcernClusteringRunner.initializeDocTopicsForEachFastCluster(), pretty much the same /thing except instead of using FastClusters, this uses Entity data structure
	 */
	/*
	 * private void initializeDocTopicsForEachEntity(Map<String,Map<String,Entity>> clusterNameToEntities) { for(String clusterName: clusterNameToEntities.keySet()) { Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName); Object[] entities = nameToEntity.values().toArray();
	 * System.out.println("INCLUSTER NAME: " + clusterName); for(String entityName: nameToEntity.keySet()) { Entity entity = nameToEntity.get(entityName); if (TopicUtil.docTopics == null) TopicUtil.docTopics = TopicUtil.getDocTopicsFromVariableMalletDocTopicsFile(); if (entity.docTopicItem == null)
	 * TopicUtil.setDocTopicForEntity(TopicUtil.docTopics, entity); } }
	 *
	 * }
	 */

	private void initDocTopics(final Map<String, Map<String, Entity>> clusterNameToEntities, final String docTopicsFilename, final String type) {
		for (final String clusterName : clusterNameToEntities.keySet()) {
			final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
			// Object[] entities = nameToEntity.values().toArray();
			System.out.println("INCLUSTER NAME: " + clusterName);
			for (final String entityName : nameToEntity.keySet()) {
				final Entity entity = nameToEntity.get(entityName);
				if (TopicUtil.docTopics == null) {
					TopicUtil.docTopics = TopicUtil.getDocTopicsFromFile(docTopicsFilename);
				}
				if (entity.getDocTopicItem() == null) {
					TopicUtil.setDocTopicForEntity(TopicUtil.docTopics, entity, type);
				}
			}
		}

	}

	/**
	 * Computes average JS Divergence of each cluster
	 *
	 * @param clusterName
	 * @param clusterNameToEntities
	 * @return
	 */
	private double computeJSDivergence(final String clusterName, final Map<String, Map<String, Entity>> clusterNameToEntities) {
		final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
		final Object[] entities = nameToEntity.values().toArray();
		double sum = 0;
		int n = 0; // number of simMeasure values
		for (int i = 0; i < entities.length; i++) {
			for (int j = i + 1; j < entities.length; j++) {
				final Entity entity1 = (Entity) entities[i];
				final Entity entity2 = (Entity) entities[j];
				if (entity1.getDocTopicItem() != null && entity2.getDocTopicItem() != null) // this
				// makes
				// sure
				// anonymous inner
				// classes don't get
				// included in the computation
				{
					final double simMeasure = SimCalcUtil.getJSDivergence(entity1, entity2);
					sum = sum + simMeasure;
					n++;
				}
			}
		}
		final double average = sum / n;
		return average;
	}

	/*----------------------------- MAIN ----------------------------------------------------------*/
	public static void main(final String[] args) {
		String depsFilename = "";
		String authFilename = "";
		String topicsFilename = "";
		String langType = "";
		String outFilename = "outfile.csv";
		String pkgFilename = "";
		final Options options = new Options();

		final Option help = new Option("help", "print this message");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("RSF dependencies file");
		final Option depsFileOption = OptionBuilder.create("depsFile");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("RSF authoritative file");
		final Option authFileOption = OptionBuilder.create("authFile");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("doc topics file");
		final Option topicsFileOption = OptionBuilder.create("topicsFile");

		OptionBuilder.withArgName("langType");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("language type [java|c]");
		final Option langTypeOption = OptionBuilder.create("langType");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("output csv file");
		final Option outFileOption = OptionBuilder.create("outFile");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("rsf file for packaged-based clustering");
		final Option pkgFileOption = OptionBuilder.create("pkgFile");

		options.addOption(help);
		options.addOption(depsFileOption);
		options.addOption(authFileOption);
		options.addOption(topicsFileOption);
		options.addOption(outFileOption);
		options.addOption(langTypeOption);
		options.addOption(pkgFileOption);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(MetricsDriver.class.getName(), options);
				System.exit(0);
			}
			if (line.hasOption("depsFile")) {
				depsFilename = line.getOptionValue("depsFile");
			}
			if (line.hasOption("authFile")) {
				authFilename = line.getOptionValue("authFile");
			}
			if (line.hasOption("topicsFile")) {
				topicsFilename = line.getOptionValue("topicsFile");
			}
			if (line.hasOption("outFile")) {
				outFilename = line.getOptionValue("outFile");
			}
			if (line.hasOption("langType")) {
				langType = line.getOptionValue("langType");
				if (langType.equals("java")) {
					selectedLangType = LangType.JAVA;
				} else if (langType.equals("c")) {
					selectedLangType = LangType.C;
				} else {
					System.err.println("ERROR: Invalid language selected, forcing selection of java");
					selectedLangType = LangType.JAVA;
				}

			}
			if (line.hasOption("pkgFile")) {
				pkgFilename = line.getOptionValue("pkgFile");
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		// RsfReader.loadRsfDataFromFile("archstudio4_deps (1).rsf");
		// RsfReader.loadRsfDataFromFile("hadoop-0.19-odem-facts.rsf");
		// RsfReader.loadRsfDataFromFile("bash_make_dep_facts.rsf");
		// RsfReader.loadRsfDataFromFile("oodt_0.2_full_clean_odem_facts.rsf");
		RsfReader.loadRsfDataFromFile(depsFilename);
		// RsfReader.loadRsfDataFromFile("mozilla.flat.compact.Author#88AD5.clean.rsf");
		// RsfReader.loadRsfDataFromFile("mozilla.static.flat.compact.rsf");
		// RsfReader.loadRsfDataFromFile("mozilla.static.rel.rsf");
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		// RsfReader.loadRsfDataFromFile("archstudio4_clean_ground_truth_recovery.rsf");
		// RsfReader.loadRsfDataFromFile("hadoop-0.19_ground_truth.rsf");
		// RsfReader.loadRsfDataFromFile("bash_1.14_ground_truth_recovery.rsf");
		// RsfReader.loadRsfDataFromFile("oodt_0.2_full_ground_truth_recovery.rsf");
		RsfReader.loadRsfDataFromFile(authFilename);
		// RsfReader.loadRsfDataFromFile("mozilla.static.flat.compact.rsf");
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile(pkgFilename);
		final List<List<String>> pkgFacts = RsfReader.unfilteredFacts;

		System.out.println("Finished loading data from all files");
		ReverseAnalysis ra;
		ra = new ReverseAnalysis();
		ra.initializeFileIO(outFilename);

		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);
		final Map<String, Set<MutablePair<String, String>>> internalEdgeMap = ClusterUtil.buildInternalEdgesPerCluster(clusterMap, depFacts);
		final Map<String, Set<MutablePair<String, String>>> externalEdgeMap = ClusterUtil.buildExternalEdgesPerCluster(clusterMap, depFacts);

		final HashMap<String, Integer> pkgSizeMap = computePkgSizes(pkgFacts);

		final Map<String, Map<String, Entity>> clusterNameToEntities = ra.buildFeatureSetPerClusterEntity(clusterMap, depFacts);
		ra.initDocTopics(clusterNameToEntities, topicsFilename, langType); // -UNCOMMENT
		// OUT
		// FOR
		// JSDIVERGENCE
		ra.printClusterNameToEntities(clusterNameToEntities);
		try {
			ra.calculateResults(clusterNameToEntities, internalEdgeMap, externalEdgeMap, pkgSizeMap);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	/*------------------------ UTILITY FUNCTIONS------------------------------------------------- */
	public void initializeFileIO(final String outFilename) {
		try {
			out = new BufferedWriter(new FileWriter(outFilename));
			out.write("ClusterName" + ",");
			out.write("Unbiased Ellenberg" + ",");
			out.write("UnbiasedEllenberg-NM" + ",");
			out.write("LIMBO" + ",");
			out.write("Bunch" + ",");
			out.write("JSDivergence" + ",");
			out.write("Dom" + ",");
			out.write("PKG" + ",");
			out.newLine();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private void writeToFile(final Double content) {
		String str = "";
		System.out.println(content);
		str += content + ",";
		try {
			out.write(str);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private void printClusterNameToEntities(final Map<String, Map<String, Entity>> clusterNameToEntities) {
		for (final String clusterName : clusterNameToEntities.keySet()) {
			final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);

			System.out.println("CLUSTER NAME: " + clusterName);
			for (final String entityName : nameToEntity.keySet()) {
				System.out.println("---Entity name--- : " + entityName);
				// Entity entity = nameToEntity.get(entityName);
				// System.out.println("Entity's featureSet: ");
				// Set<String> featureSet = entity.featureSet;
				/*
				 * for(String featureName: featureSet) { System.out.println(featureName); }
				 */
				/*
				 * System.out.print("Feature vector bitset: "); for(int i = 0; i < this.bitSetSize; i++) { System.out.print(entity.featureVector.get(i)); }
				 */
			}
		}
	}

}
