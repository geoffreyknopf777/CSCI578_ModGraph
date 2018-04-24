package edu.usc.softarch.arcade.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.regex.Pattern;

import edu.usc.softarch.arcade.config.ConfigUtil;
import edu.usc.softarch.arcade.topics.TopicUtil;

/**
 * @author joshua
 *
 */
public class SimCalcUtil {

	public static HashMap<HashSet<FeatureVector>, Integer> sharedFeaturesMap = new HashMap<HashSet<FeatureVector>, Integer>();
	public static HashMap<ArrayList<FeatureVector>, Integer> oneZeroFeaturesMap = new HashMap<ArrayList<FeatureVector>, Integer>();
	public static HashMap<ArrayList<FeatureVector>, Integer> zeroOneFeaturesMap = new HashMap<ArrayList<FeatureVector>, Integer>();

	public static HashMap<HashSet<FeatureVector>, Double> sumSharedFeaturesMap = new HashMap<HashSet<FeatureVector>, Double>();

	public static HashMap<ArrayList<FeatureVector>, Double> divergenceMap = new HashMap<ArrayList<FeatureVector>, Double>();

	public static void verifySymmetricFeatureVectorOrdering(final FeatureVector fv1, final FeatureVector fv2) {
		for (int i = 0; i < fv1.size(); i++) {
			if (!fv1.get(i).edge.tgtStr.equals(fv2.get(i).edge.tgtStr)) {
				System.out.println("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", Feature order incorrect");
				System.exit(0);
			}
		}
		System.out.println("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + ", Feature order correct...continuing");
	}

	public static void verifySymmetricClusterOrdering(final Vector<Cluster> clusters) {
		final boolean debug = false;
		final Cluster firstCluster = clusters.firstElement();
		for (final Cluster c : clusters) {
			for (int i = 0; i < firstCluster.size(); i++) {
				if (!firstCluster.get(i).edge.tgtStr.equals(c.get(i).edge.tgtStr)) {
					System.out.println("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + "Feature order incorrect");
					System.exit(0);
				}

			}
		}
		if (debug) {
			System.out.println("In, " + Thread.currentThread().getStackTrace()[1].getMethodName() + "Feature order correct...continuing");
		}
	}

	public static double getJSDivergence(final FastCluster cluster, final FastCluster otherCluster) {
		final double divergence = TopicUtil.jsDivergence(cluster.docTopicItem, otherCluster.docTopicItem);

		return divergence;
	}

	public static double getJSDivergence(final Entity entity1, final Entity entity2) {
		final double divergence = TopicUtil.jsDivergence(entity1.getDocTopicItem(), entity2.getDocTopicItem());

		return divergence;
	}

	public static double getJSDivergence(final FeatureVector fv1, final FeatureVector fv2) {
		String strippedLeafSplitClusterName = ConfigUtil.stripParensEnclosedClassNameWithPackageName(fv1);

		if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr, strippedLeafSplitClusterName)) {
			return Integer.MAX_VALUE;
		}

		if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr, strippedLeafSplitClusterName)) {
			return Integer.MAX_VALUE;
		}

		strippedLeafSplitClusterName = ConfigUtil.stripParensEnclosedClassNameWithPackageName(fv2);

		if (Pattern.matches(ConfigUtil.anonymousInnerClassRegExpr, strippedLeafSplitClusterName)) {
			return Integer.MAX_VALUE;
		}

		if (Pattern.matches(ConfigUtil.doubleInnerClassRegExpr, strippedLeafSplitClusterName)) {
			return Integer.MAX_VALUE;
		}

		final ArrayList<FeatureVector> featureVecPair = new ArrayList<FeatureVector>(2);
		featureVecPair.add(fv1);
		featureVecPair.add(fv2);

		double divergence = 0;

		if (divergenceMap.containsKey(featureVecPair)) {
			divergence = divergenceMap.get(featureVecPair);
		} else {
			divergence = TopicUtil.jsDivergence(fv1.docTopicItem, fv2.docTopicItem);
			divergenceMap.put(new ArrayList<FeatureVector>(featureVecPair), divergence);
		}

		return divergence;
	}

	public static double getUnbiasedEllenbergMeasure(final FeatureVector fv1, final FeatureVector fv2) {
		final ArrayList<FeatureVector> featureVecPair = new ArrayList<FeatureVector>(2);
		featureVecPair.add(fv1);
		featureVecPair.add(fv2);
		double sumSharedFeatures = 0;
		int num10Features = 0;
		int num01Features = 0;
		if (sumSharedFeaturesMap.containsKey(featureVecPair)) {
			sumSharedFeatures = sumSharedFeaturesMap.get(featureVecPair);
		} else {
			sumSharedFeatures = getSumSharedFeatures(fv1, fv2);
			sumSharedFeaturesMap.put(new HashSet<FeatureVector>(featureVecPair), sumSharedFeatures);
		}

		if (oneZeroFeaturesMap.containsKey(featureVecPair)) {
			num10Features = oneZeroFeaturesMap.get(featureVecPair);
		} else {
			num10Features = getNum10Features(fv1, fv2);
			oneZeroFeaturesMap.put(featureVecPair, num10Features);
		}

		if (zeroOneFeaturesMap.containsKey(featureVecPair)) {
			num01Features = zeroOneFeaturesMap.get(featureVecPair);
		} else {
			num01Features = getNum01Features(fv1, fv2);
			zeroOneFeaturesMap.put(featureVecPair, num01Features);
		}

		final double denom = 0.5 * sumSharedFeatures + num10Features + num01Features;
		if (denom == 0) {
			return denom;
		}
		return 0.5 * sumSharedFeatures / denom;
	}

	public static double getJaccardSim(final FeatureVector fv1, final FeatureVector fv2) {
		final ArrayList<FeatureVector> featureVecPair = new ArrayList<FeatureVector>(2);
		featureVecPair.add(fv1);
		featureVecPair.add(fv2);
		int numSharedFeatures = 0;
		int num10Features = 0;
		int num01Features = 0;
		if (sharedFeaturesMap.containsKey(featureVecPair)) {
			numSharedFeatures = sharedFeaturesMap.get(featureVecPair);
		} else {
			numSharedFeatures = getNumSharedFeatures(fv1, fv2);
			sharedFeaturesMap.put(new HashSet<FeatureVector>(featureVecPair), numSharedFeatures);
		}

		if (oneZeroFeaturesMap.containsKey(featureVecPair)) {
			num10Features = oneZeroFeaturesMap.get(featureVecPair);
		} else {
			num10Features = getNum10Features(fv1, fv2);
			oneZeroFeaturesMap.put(featureVecPair, num10Features);
		}

		if (zeroOneFeaturesMap.containsKey(featureVecPair)) {
			num01Features = zeroOneFeaturesMap.get(featureVecPair);
		} else {
			num01Features = getNum01Features(fv1, fv2);
			zeroOneFeaturesMap.put(featureVecPair, num01Features);
		}

		final double denom = numSharedFeatures + num10Features + num01Features;
		if (denom == 0) {
			return denom;
		}
		return numSharedFeatures / denom;
	}

	public static int getNum10Features(final FeatureVector fv1, final FeatureVector fv2) {
		int count = 0;
		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			final Feature f2 = fv2.get(i);
			// if (local_debug && DEBUG) {
			// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
			// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
			// System.out.println("f.value: " + f.value);
			// System.out.println("f2.value: " + f2.value);
			// System.out.println();
			// }

			if (f.value > 0 && f2.value == 0) {
				count++;
				// if (local_debug && DEBUG) {
				// System.out.println("Increased 11 count to: " + count);
				// }
			}
		}
		// count = getNum10FeaturesOldImpl(fv1, fv2, local_debug, count);
		return count;
	}

	// private static int getNum10FeaturesOldImpl(FeatureVector fv1,
	// FeatureVector fv2, boolean local_debug, int count) {
	// for (int i=0;i<fv1.size();i++) {
	// Feature f = fv1.get(i);
	// for (int j=0;j<fv2.size();j++) {
	// Feature f2 = fv2.get(j);
	// if (local_debug && DEBUG) {
	// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
	// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
	// System.out.println("f.value: " + f.value);
	// System.out.println("f2.value: " + f2.value);
	// System.out.println();
	// }
	// if (f.edge.tgtStr.equals(f2.edge.tgtStr) && f.value == 1 && f2.value ==
	// 0) {
	// count++;
	// if (local_debug && DEBUG)
	// System.out.println("Increased 10 count to: " + count);
	// }
	// }
	//
	// }
	// return count;
	// }
	//
	public static int getNum01Features(final FeatureVector fv1, final FeatureVector fv2) {
		int count = 0;

		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			final Feature f2 = fv2.get(i);
			// if (local_debug && DEBUG) {
			// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
			// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
			// System.out.println("f.value: " + f.value);
			// System.out.println("f2.value: " + f2.value);
			// System.out.println();
			// }

			if (f.value == 0 && f2.value > 0) {
				count++;
				// if (local_debug && DEBUG) {
				// System.out.println("Increased 11 count to: " + count);
				// }
			}
		}

		// count = getNum01FeaturesOldImpl(fv1, fv2, count);
		return count;
	}

	public static int getNumSharedFeatures(final FeatureVector fv1, final FeatureVector fv2) {
		int count = 0;

		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			final Feature f2 = fv2.get(i);
			// if (local_debug && DEBUG) {
			// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
			// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
			// System.out.println("f.value: " + f.value);
			// System.out.println("f2.value: " + f2.value);
			// System.out.println();
			// }

			if (f.edge.tgtStr.equals(f2.edge.tgtStr) && f.value == 1 && f2.value == 1) {
				count++;
				// if (local_debug && DEBUG) {
				// System.out.println("Increased 11 count to: " + count);
				// }
			}
		}

		// count = getNumSharedFeaturesOldImpl(fv1, fv2, local_debug, count);
		return count;
	}

	public static double getSumSharedFeatures(final FeatureVector fv1, final FeatureVector fv2) {
		double sharedFeatureSum = 0;
		// System.out.println("fv1: " + fv1);
		// System.out.println("fv2: " + fv2);
		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			final Feature f2 = fv2.get(i);
			// if (local_debug && DEBUG) {
			// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
			// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
			// System.out.println("f.value: " + f.value);
			// System.out.println("f2.value: " + f2.value);
			// System.out.println();
			// }

			if (f.value > 0 && f2.value > 0) {
				sharedFeatureSum = f.value + f2.value;
			}
		}

		return sharedFeatureSum;
	}

	public static double getSumOneZeroFeatures(final FeatureVector fv1, final FeatureVector fv2) {
		double oneZeroFeatureSum = 0;
		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			final Feature f2 = fv2.get(i);
			// if (local_debug && DEBUG) {
			// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
			// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
			// System.out.println("f.value: " + f.value);
			// System.out.println("f2.value: " + f2.value);
			// System.out.println();
			// }

			if (f.value > 0 && f2.value == 0) {
				oneZeroFeatureSum = f.value + f2.value;
			}
		}

		return oneZeroFeatureSum;
	}

	public static double getSumZeroOneFeatures(final FeatureVector fv1, final FeatureVector fv2) {
		double zeroOneFeatureSum = 0;
		for (int i = 0; i < fv1.size(); i++) {
			final Feature f = fv1.get(i);
			final Feature f2 = fv2.get(i);
			// if (local_debug && DEBUG) {
			// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
			// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
			// System.out.println("f.value: " + f.value);
			// System.out.println("f2.value: " + f2.value);
			// System.out.println();
			// }

			if (f.value == 0 && f2.value > 0) {
				zeroOneFeatureSum = f.value + f2.value;
			}
		}

		return zeroOneFeatureSum;
	}

	// private static int getNumSharedFeaturesOldImpl(FeatureVector fv1,
	// FeatureVector fv2, boolean local_debug, int count) {
	// for (int i = 0; i < fv1.size(); i++) {
	// Feature f = fv1.get(i);
	// for (int j = 0; j < fv2.size(); j++) {
	// Feature f2 = fv2.get(j);
	// if (local_debug && DEBUG) {
	// System.out.println("f.edge.tgtStr: " + f.edge.tgtStr);
	// System.out.println("f2.edge.tgtStr: " + f2.edge.tgtStr);
	// System.out.println("f.value: " + f.value);
	// System.out.println("f2.value: " + f2.value);
	// System.out.println();
	// }
	// if (f.edge.tgtStr.equals(f2.edge.tgtStr) && f.value == 1
	// && f2.value == 1) {
	// count++;
	// if (local_debug && DEBUG) {
	// System.out.println("Increased 11 count to: " + count);
	// }
	// }
	// }
	//
	// }
	// return count;
	// }

	public static void verifySymmetricWCAClusterOrdering(final Vector<WCACluster> wcaClusters) {
	}
}
