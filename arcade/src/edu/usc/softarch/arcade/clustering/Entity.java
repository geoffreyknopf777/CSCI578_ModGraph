package edu.usc.softarch.arcade.clustering;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import edu.usc.softarch.arcade.topics.DocTopicItem;

/**
 * Cluster entity
 * 
 * @author daniellink
 *
 */
public class Entity {
	protected String name;
	private BitSet featureVector = new BitSet();
	private HashMap<Integer, Double> nonZeroFeatureMap = new HashMap<>();
	private int numOfEntities = 1;
	private DocTopicItem docTopicItem;

	public Set<String> featureSet = new HashSet<>();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * @return the featureSet
	 */
	public Set<String> getFeatureSet() {
		return featureSet;
	}

	/**
	 * @param featureSet
	 *            the featureSet to set
	 */
	public void setFeatureSet(final Set<String> featureSet) {
		this.featureSet = featureSet;
	}

	/**
	 * @return the featureVector
	 */
	public BitSet getFeatureVector() {
		return featureVector;
	}

	/**
	 * @param featureVector
	 *            the featureVector to set
	 */
	public void setFeatureVector(final BitSet featureVector) {
		this.featureVector = featureVector;
	}

	/**
	 * @return the nonZeroFeatureMap
	 */
	public HashMap<Integer, Double> getNonZeroFeatureMap() {
		return nonZeroFeatureMap;
	}

	/**
	 * @param nonZeroFeatureMap
	 *            the nonZeroFeatureMap to set
	 */
	public void setNonZeroFeatureMap(final HashMap<Integer, Double> nonZeroFeatureMap) {
		this.nonZeroFeatureMap = nonZeroFeatureMap;
	}

	/**
	 * @return the numOfEntities
	 */
	public int getNumOfEntities() {
		return numOfEntities;
	}

	/**
	 * @param numOfEntities
	 *            the numOfEntities to set
	 */
	public void setNumOfEntities(final int numOfEntities) {
		this.numOfEntities = numOfEntities;
	}

	/**
	 * @return the docTopicItem
	 */
	public DocTopicItem getDocTopicItem() {
		return docTopicItem;
	}

	/**
	 * @param docTopicItem
	 *            the docTopicItem to set
	 */
	public void setDocTopicItem(final DocTopicItem docTopicItem) {
		this.docTopicItem = docTopicItem;
	}

	public Entity() {

	}

	public Entity(final String name) {
		this.name = name;
	}

	public void initializeNonZeroFeatureMap(final int bitSetSize) {
		nonZeroFeatureMap = new HashMap<>();
		for (int i = 0; i < bitSetSize; i++) {
			if (featureVector.get(i)) {
				final double one = 1;
				nonZeroFeatureMap.put(i, one);
			}
		}
	}

	public void setNonZeroFeatureMapForLibmoUsingIndices(final Entity c1, final Entity c2, final Set<Integer> c1Indices) {
		for (final Integer index : c1Indices) {
			final Double c1Value = c1.nonZeroFeatureMap.get(index);
			final Double c2Value = c2.nonZeroFeatureMap.get(index);

			Double newFeatureValue = null;
			if (c1Value == null && c2Value != null) {
				newFeatureValue = new Double(c2Value * c2.getNumOfEntities() / (c1.getNumOfEntities() + c2.getNumOfEntities()));

			} else if (c2Value == null && c1Value != null) {
				newFeatureValue = new Double(c1Value * c1.getNumOfEntities() / (c1.getNumOfEntities() + c2.getNumOfEntities()));
			} else if (c1Value != null && c2Value != null) {
				newFeatureValue = new Double((c1Value * c1.getNumOfEntities() + c2Value * c2.getNumOfEntities()) / (c1.getNumOfEntities() + c2.getNumOfEntities()));
			}

			if (newFeatureValue != null) {
				nonZeroFeatureMap.put(index, newFeatureValue);
			}

		}
	}
}
