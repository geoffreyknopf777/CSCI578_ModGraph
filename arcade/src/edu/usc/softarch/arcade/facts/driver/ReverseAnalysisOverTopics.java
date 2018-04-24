package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.mallet.util.Maths;
import edu.usc.softarch.arcade.clustering.Entity;
import edu.usc.softarch.arcade.clustering.SimCalcUtil;
import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.topics.TopicUtil;

public class ReverseAnalysisOverTopics {
	static Map<String, Integer> featureNameToBitsetIndex = new HashMap<String, Integer>();
	static int bitSetSize = 0;
	static BufferedWriter out;

	enum SimilarityMeasure {
		UELLENBERG, JS, LIMBO
	};

	SimilarityMeasure sm;

	/**
	 * method that produces a feature vector bitset for each entity in each cluster
	 **/
	public static Map<String, Map<String, Entity>> buildFeatureSetPerClusterEntity(final Map<String, Set<String>> clusterMap, final List<List<String>> depFacts) {
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

	/** method to compute similarity between a pair of entities */
	// private double computePairWiseSimilarity(final SimilarityMeasure sm,
	// final Entity entity1, final Entity entity2) {
	// if (sm == SimilarityMeasure.LIMBO) {
	// final Set<Integer> c1Indices = entity1.nonZeroFeatureMap.keySet();
	// entity1.setNonZeroFeatureMapForLibmoUsingIndices(entity1, entity2,
	// c1Indices);
	//
	// final Set<Integer> c2Indices = entity2.nonZeroFeatureMap.keySet();
	// entity2.setNonZeroFeatureMapForLibmoUsingIndices(entity1, entity2,
	// c2Indices);
	// return (getInfoLossMeasure(2, entity1, entity2));
	// }
	// // System.out.println("Entities : " + entity1.name + " " +
	// // entity2.name);
	// final BitSet fv1 = entity1.featureVector;
	// final BitSet fv2 = entity2.featureVector;
	// int count10 = 0;
	// int count01 = 0;
	// // int count00 = 0;
	// int count11 = 0;
	// int sum11 = 0;
	// for (int i = 0; i < fv1.size(); i++) {
	// if (fv1.get(i) && !fv2.get(i)) {
	// count10++;
	// } else if (!fv1.get(i) && fv2.get(i)) {
	// count01++;
	// // } else if (!fv1.get(i) && !fv2.get(i)) {
	// // count00++;
	// } else {
	// count11++;
	// sum11 = sum11 + 1 + 1;
	// }
	// }
	// // Unbiased Ellenberg for now
	// if (sm == SimilarityMeasure.UELLENBERG) {
	// final double denom = 0.5 * sum11 + count10 + count01;
	// if (denom == 0) {
	// return denom;
	// }
	// return 0.5 * sum11 / (denom);
	// } else if (sm == SimilarityMeasure.JS) {
	// final double denom = count11 + count10 + count01;
	// if (denom == 0) {
	// return denom;
	// }
	// return count11 / (denom);
	// }
	// return 0;
	// }

	public static void initializeFileIO(final Map<Integer, String> docTopicFilesMap) {
		try {
			out = new BufferedWriter(new FileWriter("outfile.csv"));

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static void writeToFile(final Double content) {
		// BufferedWriter out = new BufferedWriter( new
		// FileWriter("outfile.csv"));

		String str = "";

		// System.out.println(content);

		str += content + ",";

		try {
			out.write(str);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		/*
		 * try { out.close(); } catch (IOException e) { e.printStackTrace();System.exit(-1); }
		 */

	}

	public static double getInfoLossMeasure(final int numberOfEntitiesToBeClustered, final Entity entity1, final Entity entity2) {

		// int featuresLength = cluster.getFeaturesLength();
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
				// System.out.println("firstDist[i] is" + firstDist[i]);
			} else { // this feature is zero
				firstDist[i] = 0;
			}

			/*
			 * if (otherCluster.getNonZeroFeatureMap().get(i) != null) { double featureValue = otherCluster.getNonZeroFeatureMap().get(i); secondDist[i] = featureValue/otherCluster.getNonZeroFeatureMap().size(); } else { // this feature is zero secondDist[i] = 0; }
			 */
		}
	}

	/**
	 * method that calculates the result for every cluster and displays it
	 *
	 * @throws IOException
	 */
	private static void calculateResults(final Map<String, Map<String, Entity>> clusterNameToEntities, final Map<Integer, String> docTopicFilesMap) throws IOException {
		// int counter = 0;
		final Set<String> orderedClusterNames = new TreeSet<String>(clusterNameToEntities.keySet());
		for (final String clusterName : orderedClusterNames) {
			// counter++;
			// out.write(clusterName + ",");
			// Map<String, Entity> nameToEntity = clusterNameToEntities
			// .get(clusterName);
			// System.out.println("CLUSTER NAME: " + clusterName);

			final double clusterSimUsingJSDivergence = computeJSDivergence(clusterName, clusterNameToEntities);
			/*
			 * System.out.println("Similarity measure for cluster " + clusterName + " using JSDivergence is " + clusterSimUsingJSDivergence);
			 */
			writeToFile(clusterSimUsingJSDivergence);
			// if(counter == 24)
			// {
			// break; //REMOVE THIS if you want to compute for all clusters
			// }
		}
	}

	public static void main(final String[] args) {
		System.out.println("IN MAIN");
		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		// String depsFilename = args[0];
		// String clustersFilename = args[1];

		RsfReader.loadRsfDataFromFile("archstudio4_deps (1).rsf");
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile("archstudio4_clean_ground_truth_recovery.rsf");
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;
		System.out.println("Finished loading data from both files");

		final Map<Integer, String> docTopicFilesMap = buildDocTopicFilesMap();

		initializeFileIO(docTopicFilesMap);

		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);
		final Map<String, Map<String, Entity>> clusterNameToEntities = buildFeatureSetPerClusterEntity(clusterMap, depFacts);

		final Set<Integer> numTopicsSet = new TreeSet<Integer>(docTopicFilesMap.keySet());

		final Set<String> orderedClusterNames = new TreeSet<String>(clusterNameToEntities.keySet());

		try {
			out.write(",");
			for (final String clusterName : orderedClusterNames) {
				out.write(clusterName + ",");
			}
			out.newLine();

			for (final int numTopics : numTopicsSet) {
				final String docTopicsFilename = docTopicFilesMap.get(numTopics);

				// Reset topic model data
				TopicUtil.docTopics = null;
				for (final String clusterName : clusterNameToEntities.keySet()) {
					final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
					// Object[] entities = nameToEntity.values().toArray();
					// System.out.println("INCLUSTER NAME: " + clusterName);
					for (final String entityName : nameToEntity.keySet()) {
						final Entity entity = nameToEntity.get(entityName);
						entity.setDocTopicItem(null);
					}
				}

				initializeDocTopicsUsingFile(clusterNameToEntities, docTopicsFilename, "java");
				// ra.printClusterNameToEntities(clusterNameToEntities);

				out.write("JSDivergence" + numTopics + ",");

				calculateResults(clusterNameToEntities, docTopicFilesMap);
				out.newLine();

			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static Map<Integer, String> buildDocTopicFilesMap() {
		final Map<Integer, String> docTopicFilesMap = new HashMap<Integer, String>();
		try {
			final BufferedReader br = new BufferedReader(new FileReader("/home/joshua/Documents/Software Engineering Research/subject_systems/archstudio4/doc-topics-filelist.txt"));
			String line = null;
			while ((line = br.readLine()) != null) {
				System.out.println(line);

				final File file = new File(line);
				final Pattern pattern = Pattern.compile("-(\\d+)-");
				final Matcher matcher = pattern.matcher(file.getName());
				while (matcher.find()) {
					final String numOfTopics = matcher.group(1);
					System.out.println(numOfTopics);
					docTopicFilesMap.put(Integer.parseInt(numOfTopics), line);
				}
				System.out.println();
			}
			br.close();
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return docTopicFilesMap;
	}

	// ----------------------DOCTOPICITEM
	// STUFF-------------------------------------------//
	/** method to load doc-topic-item for each entity */
	private static void initializeDocTopicsUsingFile(final Map<String, Map<String, Entity>> clusterNameToEntities, final String filename, final String type) {
		// Reference
		// ConcernClusteringRunner.initializeDocTopicsForEachFastCluster(),
		// pretty much the same
		// thing except instead of using FastClusters, this uses Entity data
		// structure
		for (final String clusterName : clusterNameToEntities.keySet()) {
			final Map<String, Entity> nameToEntity = clusterNameToEntities.get(clusterName);
			// Object[] entities = nameToEntity.values().toArray();
			// System.out.println("INCLUSTER NAME: " + clusterName);
			for (final String entityName : nameToEntity.keySet()) {
				final Entity entity = nameToEntity.get(entityName);
				if (TopicUtil.docTopics == null) {
					TopicUtil.docTopics = TopicUtil.getDocTopicsFromFile(filename);
				}

				if (entity.getDocTopicItem() == null) {
					TopicUtil.setDocTopicForEntity(TopicUtil.docTopics, entity, type);
				}
			}
		}

	}

	// ----------------------DOCTOPICITEM
	// STUFF-------------------------------------------//
	/** method to load doc-topic-item for each entity */
	// private void initializeDocTopicsForEachEntity(
	// Map<String, Map<String, Entity>> clusterNameToEntities, String type) {
	// // Reference
	// // ConcernClusteringRunner.initializeDocTopicsForEachFastCluster(),
	// // pretty much the same
	// // thing except instead of using FastClusters, this uses Entity data
	// // structure
	// for (String clusterName : clusterNameToEntities.keySet()) {
	// Map<String, Entity> nameToEntity = clusterNameToEntities
	// .get(clusterName);
	// // Object[] entities = nameToEntity.values().toArray();
	// System.out.println("INCLUSTER NAME: " + clusterName);
	// for (String entityName : nameToEntity.keySet()) {
	// Entity entity = nameToEntity.get(entityName);
	// if (TopicUtil.docTopics == null)
	// TopicUtil.docTopics = TopicUtil
	// .getDocTopicsFromHardcodedDocTopicsFile();
	//
	// if (entity.docTopicItem == null)
	// TopicUtil.setDocTopicForEntity(TopicUtil.docTopics, entity, type);
	// }
	// }
	//
	// }

	/**
	 * Computes average JS Divergence of each cluster
	 *
	 * @param clusterName
	 * @param clusterNameToEntities
	 * @return
	 */
	private static double computeJSDivergence(final String clusterName, final Map<String, Map<String, Entity>> clusterNameToEntities) {
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
					// System.out.println("Entities are: " + entity1.name +
					// entity2.name);
					// System.out.println("simMeasure: " + simMeasure);
					// writeToFile(simMeasure);
					sum = sum + simMeasure;
					n++;
				}
			}
		}
		// System.out.println("Sum and n for JSDivergence are " + sum + " " +
		// n);
		final double average = sum / n;
		return average;

	}

}
