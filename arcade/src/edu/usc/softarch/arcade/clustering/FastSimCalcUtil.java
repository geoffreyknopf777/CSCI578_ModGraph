package edu.usc.softarch.arcade.clustering;

import java.util.Set;

import cc.mallet.util.Maths;

public class FastSimCalcUtil {

	public static double getUnbiasedEllenbergMeasure(final FastCluster currCluster, final FastCluster otherCluster) {

		final double sumOfFeaturesInBothEntities = getSumOfFeaturesInBothEntities(currCluster, otherCluster);
		final int numberOf10Features = getNumOf10Features(currCluster, otherCluster);
		final int numberOf01Features = getNumOf01Features(currCluster, otherCluster);

		return 0.5 * sumOfFeaturesInBothEntities / (0.5 * sumOfFeaturesInBothEntities + numberOf10Features + numberOf01Features);
	}

	public static double getUnbiasedEllenbergMeasureNM(final FastCluster currCluster, final FastCluster otherCluster) {

		final double sumOfFeaturesInBothEntities = getSumOfFeaturesInBothEntities(currCluster, otherCluster);
		final int num10Features = getNumOf10Features(currCluster, otherCluster);
		final int num01Features = getNumOf01Features(currCluster, otherCluster);
		final int num00Features = getNumOf00Features(currCluster, otherCluster);
		final int numSharedFeatures = getNumOfFeaturesInBothEntities(currCluster, otherCluster);

		return 0.5 * sumOfFeaturesInBothEntities / (0.5 * sumOfFeaturesInBothEntities + 2 * ((double) num10Features + (double) num01Features) + num00Features + numSharedFeatures);
	}

	// private int getNumSharedFeatures(FastCluster currCluster,
	// FastCluster otherCluster) {
	// return 0;
	// }

	private static int getNumOf01Features(final FastCluster currCluster, final FastCluster otherCluster) {
		/*
		 * double[] currFeatures = currCluster.getFeatures(); double[]
		 * otherFeatures = otherCluster.getFeatures();
		 * 
		 * int num01Features = 0; for (int
		 * i=0;i<currCluster.getFeatures().length;i++) { if (currFeatures[i] ==
		 * 0) { if (otherFeatures[i] > 0) { num01Features++; } } }
		 */

		final Set<Integer> otherIndices = otherCluster.getNonZeroFeatureMap().keySet();

		int num01Features = 0;
		for (final Integer otherIndex : otherIndices) {
			if (currCluster.getNonZeroFeatureMap().get(otherIndex) == null) {
				num01Features++;
			}
		}

		return num01Features;
	}

	private static int getNumOf00Features(final FastCluster currCluster, final FastCluster otherCluster) {
		/*
		 * double[] currFeatures = currCluster.getFeatures(); double[]
		 * otherFeatures = otherCluster.getFeatures();
		 * 
		 * int num10Features = 0; for (int
		 * i=0;i<currCluster.getFeatures().length;i++) { if (otherFeatures[i] ==
		 * 0) { if (currFeatures[i] > 0) { num10Features++; } } }
		 */

		final Set<Integer> currIndices = currCluster.getNonZeroFeatureMap().keySet();

		int num00Features = 0;
		for (final Integer currIndex : currIndices) {
			if (otherCluster.getNonZeroFeatureMap().get(currIndex) == null && currCluster.getNonZeroFeatureMap().get(currIndex) == null) {
				num00Features++;
			}
		}

		return num00Features;
	}

	private static int getNumOf10Features(final FastCluster currCluster, final FastCluster otherCluster) {
		/*
		 * double[] currFeatures = currCluster.getFeatures(); double[]
		 * otherFeatures = otherCluster.getFeatures();
		 * 
		 * int num10Features = 0; for (int
		 * i=0;i<currCluster.getFeatures().length;i++) { if (otherFeatures[i] ==
		 * 0) { if (currFeatures[i] > 0) { num10Features++; } } }
		 */

		final Set<Integer> currIndices = currCluster.getNonZeroFeatureMap().keySet();

		int num10Features = 0;
		for (final Integer currIndex : currIndices) {
			if (otherCluster.getNonZeroFeatureMap().get(currIndex) == null) {
				num10Features++;
			}
		}

		return num10Features;
	}

	private static int getNumOfFeaturesInBothEntities(final FastCluster currCluster, final FastCluster otherCluster) {

		/*
		 * double[] currFeatures = currCluster.getFeatures(); double[]
		 * otherFeatures = otherCluster.getFeatures();
		 * 
		 * double sumSharedFeatures = 0; for (int
		 * i=0;i<currCluster.getFeatures().length;i++) { if (currFeatures[i] > 0
		 * && otherFeatures[i] > 0) { sumSharedFeatures += currFeatures[i] +
		 * otherFeatures[i]; } }
		 */

		final Set<Integer> currIndices = currCluster.getNonZeroFeatureMap().keySet();

		int numSharedFeatures = 0;
		for (final Integer currIndex : currIndices) {
			if (currCluster.getNonZeroFeatureMap().get(currIndex) != null && otherCluster.getNonZeroFeatureMap().get(currIndex) != null) {
				numSharedFeatures++;
			}
		}

		return numSharedFeatures;
	}

	private static double getSumOfFeaturesInBothEntities(final FastCluster currCluster, final FastCluster otherCluster) {

		/*
		 * double[] currFeatures = currCluster.getFeatures(); double[]
		 * otherFeatures = otherCluster.getFeatures();
		 * 
		 * double sumSharedFeatures = 0; for (int
		 * i=0;i<currCluster.getFeatures().length;i++) { if (currFeatures[i] > 0
		 * && otherFeatures[i] > 0) { sumSharedFeatures += currFeatures[i] +
		 * otherFeatures[i]; } }
		 */

		final Set<Integer> currIndices = currCluster.getNonZeroFeatureMap().keySet();

		double sumSharedFeatures = 0;
		for (final Integer currIndex : currIndices) {
			if (currCluster.getNonZeroFeatureMap().get(currIndex) != null && otherCluster.getNonZeroFeatureMap().get(currIndex) != null) {
				final Double currFeatureValue = currCluster.getNonZeroFeatureMap().get(currIndex);
				final Double otherFeatureValue = otherCluster.getNonZeroFeatureMap().get(currIndex);
				sumSharedFeatures = currFeatureValue + otherFeatureValue;
			}
		}

		return sumSharedFeatures;
	}

	public static double getStructAndConcernMeasure(final int numberOfEntitiesToBeClustered, final FastCluster cluster, final FastCluster otherCluster) {
		if (cluster.getFeaturesLength() != otherCluster.getFeaturesLength()) {
			throw new IllegalArgumentException("cluster.getFeaturesLength()!=otherCluster.getFeaturesLength()()");
		}
		final int featuresLength = cluster.getFeaturesLength();
		final double[] firstDist = new double[featuresLength];
		final double[] secondDist = new double[featuresLength];

		normalizeFeatureVectorOfCluster(cluster, featuresLength, firstDist);
		normalizeFeatureVectorOfCluster(otherCluster, featuresLength, secondDist);

		double jsDivergenceStruct = Maths.jensenShannonDivergence(firstDist, secondDist);
		if (Double.isInfinite(jsDivergenceStruct)) {
			jsDivergenceStruct = Double.MAX_VALUE;
		}

		double jsDivergenceConcern = SimCalcUtil.getJSDivergence(cluster, otherCluster);
		if (Double.isInfinite(jsDivergenceConcern)) {
			jsDivergenceConcern = Double.MIN_VALUE;
		}
		final double structAndConcernMeasure = 0.5 * jsDivergenceStruct + 0.5 * jsDivergenceConcern;

		if (Double.isNaN(structAndConcernMeasure)) {
			throw new RuntimeException("infoLossMeasure is NaN");
		}

		return structAndConcernMeasure;
	}

	public static double getInfoLossMeasure(final int numberOfEntitiesToBeClustered, final FastCluster cluster, final FastCluster otherCluster) {
		if (cluster.getFeaturesLength() != otherCluster.getFeaturesLength()) {
			throw new IllegalArgumentException("cluster.getFeaturesLength()!=otherCluster.getFeaturesLength()()");
		}
		final int featuresLength = cluster.getFeaturesLength();
		final double[] firstDist = new double[featuresLength];
		final double[] secondDist = new double[featuresLength];

		normalizeFeatureVectorOfCluster(cluster, featuresLength, firstDist);
		normalizeFeatureVectorOfCluster(otherCluster, featuresLength, secondDist);

		double jsDivergence = Maths.jensenShannonDivergence(firstDist, secondDist);
		if (Double.isInfinite(jsDivergence)) {
			jsDivergence = Double.MAX_VALUE;
		}
		final double infoLossMeasure = (cluster.getNumEntities() / numberOfEntitiesToBeClustered + otherCluster.getNumEntities() / numberOfEntitiesToBeClustered) * jsDivergence;

		if (Double.isNaN(infoLossMeasure)) {
			throw new RuntimeException("infoLossMeasure is NaN");
		}

		return infoLossMeasure;
	}

	private static void normalizeFeatureVectorOfCluster(final FastCluster cluster, final int featuresLength, final double[] firstDist) {
		for (int i = 0; i < featuresLength; i++) {
			if (cluster.getNonZeroFeatureMap().get(i) != null) {
				final double featureValue = cluster.getNonZeroFeatureMap().get(i);
				firstDist[i] = featureValue / cluster.getNonZeroFeatureMap().size();
			} else { // this feature is zero
				firstDist[i] = 0;
			}

			/*
			 * if (otherCluster.getNonZeroFeatureMap().get(i) != null) { double
			 * featureValue = otherCluster.getNonZeroFeatureMap().get(i);
			 * secondDist[i] =
			 * featureValue/otherCluster.getNonZeroFeatureMap().size(); } else {
			 * // this feature is zero secondDist[i] = 0; }
			 */
		}
	}
}
