package edu.usc.softarch.arcade.relax;

import java.util.LinkedHashMap;

public class RelaxFeatureVector {

	/**
	 *
	 */

	private String name = "";
	private LinkedHashMap<String, Double> classValues; // Mapping of class names to confidence values

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
	 * @return the classValues
	 */
	public LinkedHashMap<String, Double> getClassValues() {
		return classValues;
	}

	/**
	 * @param classValues
	 *            the classValues to set
	 */
	public void setClassValues(final LinkedHashMap<String, Double> classValues) {
		this.classValues = classValues;
	}

	public RelaxFeatureVector(final String name) {
		this.name = name;
	}

	public RelaxFeatureVector(final String name, final LinkedHashMap<String, Double> values) {
		this.name = name;
		classValues = values;
		classValues.put("no_match", 0.0); // Add a class for no_match
	}

	public void changeFeatureValue(final String className, final double value) {
		for (final String s : classValues.keySet()) {
			if (s.equals(className)) {
				classValues.replace(s, value);
			}

		}
	}

	@Override
	public String toString() {
		String out = name + "\n";
		for (final double d : classValues.values()) {
			out += d + "\n";
		}
		return out;
	}

	@Override
	public boolean equals(final Object o) {
		final RelaxFeatureVector rfv = (RelaxFeatureVector) o;
		if (name.equals(rfv.name)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 37 * hash + (name == null ? 0 : name.hashCode());
		return hash;
	}

	public boolean equals(final RelaxFeatureVector rfv) {

		if (!name.equals(rfv.name)) {
			return false;
		}
		for (final String s : classValues.keySet()) {
			if (!rfv.classValues.containsKey(s)) {
				return false;
			}

			if (!rfv.classValues.get(s).equals(classValues.get(rfv))) {
				return false;
			}
		}
		return true;

	}

}
