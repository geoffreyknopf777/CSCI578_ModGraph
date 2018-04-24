/**
 *
 */
package edu.usc.softarch.arcade.relax;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.logging.log4j.Logger;

/**
 * @author daniellink
 *
 */
public class Clustering {
	private static ArrayList<Cluster> clusterList = new ArrayList<>();
	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(Clustering.class.getName());
	private static boolean clusterBaseVectorsInitialized = false;
	private static ArrayList<LinkedHashMap<String, Double>> baseVectors;
	private static HashMap<Integer, Cluster> clusterMap = new HashMap<>();
	private static HashMap<String, String> entityNameBestLabelMap = new HashMap<>();
	private static HashMap<String, Integer> entityNameBestLabelIDMap = new HashMap<>();
	private static ArrayList<String> clusterLabels = new ArrayList<>();

	public static ArrayList<String> getClusterLabels() {
		return clusterLabels;
	}

	public static void setClusterLabels(final ArrayList<String> clusterLabels) {
		Clustering.clusterLabels = clusterLabels;
	}

	public static void addClusterLabel(final String s) {
		clusterLabels.add(s);
	}

	public static HashMap<String, Integer> getEntityNameBestLabelIDMap() {
		return entityNameBestLabelIDMap;
	}

	public static void setEntityNameBestLabelIDMap(final HashMap<String, Integer> entityNameBestLabelIDMap) {
		Clustering.entityNameBestLabelIDMap = entityNameBestLabelIDMap;
	}

	public static HashMap<String, String> getEntityNameBestLabelMap() {
		return entityNameBestLabelMap;
	}

	public static void setEntityNameBestLabelMap(final HashMap<String, String> entityNameBestLabelMap) {
		Clustering.entityNameBestLabelMap = entityNameBestLabelMap;
	}

	public static HashMap<Integer, Cluster> getClusterMap() {
		return clusterMap;
	}

	public static void setClusterMap(final HashMap<Integer, Cluster> clusterMap) {
		Clustering.clusterMap = clusterMap;
	}

	/**
	 * @return the clusterList
	 */
	public ArrayList<Cluster> getClusterList() {
		logger.traceEntry();
		logger.traceExit();
		return clusterList;
	}

	/**
	 * @param clusterList
	 *            the clusterList to set
	 */
	public void setClusterList(final ArrayList<Cluster> clusterList) {
		logger.traceEntry();
		Clustering.clusterList = clusterList;
		logger.traceExit();
	}

	/**
	 * @return the nextClusterID
	 */
	public int getNextClusterID() {
		logger.traceEntry();
		logger.traceExit();
		return nextClusterID;
	}

	/**
	 * @param nextClusterID
	 *            the nextClusterID to set
	 */
	public void setNextClusterID(final int nextClusterID) {
		logger.entry(nextClusterID);
		Clustering.nextClusterID = nextClusterID;
		logger.traceExit();
	}

	private static int nextClusterID = 0;

	/**
	 * In order to make RELAX additive, all clusters must be generated and always be in the same order
	 */
	private static void addAllClusters() {
		for (final String s : CodeEntity.getClassNames()) {
			final Cluster cl = new Cluster();
			cl.setId(nextClusterID);
			cl.setName(s);
			clusterList.add(cl);
			clusterMap.put(nextClusterID++, cl);
		}
	}

	/**
	 * Process one entity, i.e. add it to the appropriate cluster
	 *
	 * @param ce
	 *            - the entity
	 */
	public static void processEntity(final CodeEntity ce) {
		logger.entry(ce);
		// if (ce.isNoMatch()) {
		// System.out.println("Entity is not a match - not adding to concern clusters!");
		// return;
		// }
		final ArrayList<Double> similarities = getSimilarities(ce);
		final DecimalFormat df = new DecimalFormat("#0.00");
		String sims = "";
		for (final double d : similarities) {
			sims += df.format(d) + " ";
		}
		logger.debug("Similarity = " + sims);

		int bestSimilarityIndex = -1;
		Double bestSimilarity = Double.MIN_VALUE;
		for (int i = 0; i < similarities.size(); i++) {
			final Double currentSimilarity = similarities.get(i);
			if (currentSimilarity > bestSimilarity) {
				bestSimilarity = currentSimilarity;
				bestSimilarityIndex = i;
			}
		}
		final String bestLabel = ce.getBestLabel();
		// Will have to be changed once more clusters than categories are
		// possible
		final String bestSimilarityLabel = CodeEntity.getClassNames().get(bestSimilarityIndex);
		logger.debug("Best Label = " + bestLabel + ", best similarity label = " + bestSimilarityLabel);
		// if (!clusterWithLabelExists(bestLabel)) {
		// addNewCluster(bestLabel);
		// }
		if (clusterList.isEmpty()) {
			addAllClusters();
		}
		final Cluster cl = getClusterWithLabel(bestLabel);
		cl.getEntities().add(ce);
		entityNameBestLabelMap.put(ce.getName(), bestLabel);
		entityNameBestLabelIDMap.put(ce.getName(), cl.getId());
		logger.traceExit();
	}

	private static Cluster getClusterWithLabel(final String label) {
		logger.entry(label);
		for (final Cluster c : clusterList) {
			if (c.getName() == label) {
				logger.traceExit();
				return c;
			}
		}
		logger.traceExit();
		return null;
	}

	public static int getClusterID(final String label) {
		return getClusterWithLabel(label).getId();
	}

	public static String clusterListToString() {
		logger.traceEntry();
		String output = "Contents of all clusters:\n";
		for (final Cluster c : clusterList) {
			output += c.toString();
		}
		logger.traceExit();
		return output;
	}

	public static String clusterListToContainString(final String versionFolderName, final boolean useClasses) {
		logger.traceEntry();
		final ArrayList<String> outputList = new ArrayList<>();
		// final String[] outputArray = new String[entityCount];
		String output = "";
		for (final Cluster c : clusterList) {
			final String clusterName = c.getName();
			final ArrayList<String> canNames = new ArrayList<>();
			for (final CodeEntity e : c.getEntities()) {
				if (useClasses) {
					final String classCanonicalName = e.getCanonicalName();
					if (canNames.contains(classCanonicalName)) {
						continue;
					}
					canNames.add(classCanonicalName);
					outputList.add("contain " + clusterName + " " + classCanonicalName + "\n");
				} else {
					outputList.add("contain " + clusterName + " " + e.getAbsoluteFileName() + "\n");
				}
			}
		}
		final String[] outputArray = outputList.toArray(new String[0]);
		Arrays.sort(outputArray);
		for (final String s : outputArray) {
			output += s;
		}
		logger.traceExit();
		return output;
	}

	public static void resetClusterList() {
		logger.traceEntry();
		clusterList = new ArrayList<>();
		nextClusterID = 0;
		logger.traceExit();
	}

	private static ArrayList<Double> getSimilarities(final CodeEntity c) {
		logger.entry(c);
		final ArrayList<Double> returnList = new ArrayList<>();
		// final CosineSimilarity<Double> metric = new
		// org.simmetrics.metrics.CosineSimilarity<Double>();
		// final Multiset<Double> entityVectorValues = HashMultiset.create();
		final ArrayList<Double> entityVectorValues = new ArrayList<>();
		for (final Double d : c.getClassificationVector().getClassValues().values()) {
			entityVectorValues.add(d);
		}
		if (!clusterBaseVectorsInitialized) {
			baseVectors = new ArrayList<>();
			for (int i = 0; i < CodeEntity.getClassNames().size(); i++) {
				final LinkedHashMap<String, Double> currentBaseVector = new LinkedHashMap<>();
				// final Multiset<Double> currentBaseVectorValues =
				// HashMultiset.create();
				final ArrayList<Double> currentBaseVectorValues = new ArrayList<>();
				for (int j = 0; j < CodeEntity.getClassNames().size(); j++) {
					Double d;
					if (i == j) {
						d = 1.0;
					} else {
						d = 0.0;
					}
					currentBaseVector.put(CodeEntity.getClassNames().get(j), d);
					currentBaseVectorValues.add(d);
				}
				baseVectors.add(currentBaseVector);
				// baseVectorValues.add(currentBaseVectorValues);
			}
			// for (final LinkedHashMap<String, Double> baseVector :
			// baseVectors) {
			// logger.debug(baseVector);
			// }
		}
		for (final LinkedHashMap<String, Double> baseVector : baseVectors) {
			final Double[] baseValues = baseVector.values().toArray(new Double[baseVector.size()]);
			final Double[] cValues = c.getClassAffinities().values().toArray(new Double[baseVector.size()]);
			final double comparisonResult = cosineSimilarity(cValues, baseValues);
			returnList.add(comparisonResult);
			logger.trace("Comparison result = " + comparisonResult);
		}
		// For now, this is random so we some values out of it
		logger.traceExit();
		return returnList;

	}

	public static double cosineSimilarity(final Double[] vectorA, final Double[] vectorB) {
		logger.entry(vectorA, vectorB);
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;
		for (int i = 0; i < vectorA.length; i++) {
			dotProduct += vectorA[i] * vectorB[i];
			normA += Math.pow(vectorA[i], 2);
			normB += Math.pow(vectorB[i], 2);
		}
		logger.traceExit();
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}

	public static void printAllClusters() {
		logger.traceEntry();
		for (final Cluster c : clusterList) {
			logger.debug(c);
		}
		logger.traceExit();
	}

	/**
	 * The final output after recovery
	 */
	public static void printClusterInfo() {
		logger.traceEntry();
		final int entityCount = CodeEntity.getEntityCount();
		final DecimalFormat percentageFormat = new DecimalFormat("#00.00");
		System.out.println("### Cluster Info ###");
		System.out.println("Number of entities: " + entityCount);
		System.out.println("Number of clusters: " + clusterList.size());
		for (final Cluster currentCluster : clusterList) {
			final int currentClusterSize = currentCluster.getEntities().size();
			final Double percentage = currentClusterSize * 100.0 / entityCount;
			final String clusterName = String.format("%1$-20s", currentCluster.getName());
			final String clusterSize = String.format("%1$-10s", currentClusterSize);
			final String percent = String.format("%1$-6s", percentageFormat.format(percentage));
			System.out.println(clusterName + " has " + clusterSize + " entities, making up " + percent + "% of all entities.");
		}
		final int noMatchCount = CodeEntity.getNoMatchCount();
		final Double noMatchPercentage = noMatchCount * 100.0 / entityCount;
		System.out.println(CodeEntity.getNoMatchCount() + " entities (" + percentageFormat.format(noMatchPercentage) + "%) did not match any category.");
		logger.traceExit();
	}
}
