/**
 *
 */
package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;

import edu.usc.softarch.arcade.clustering.Entity;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.util.ClassInfo;
import edu.usc.softarch.arcade.util.FileUtil;

/**
 * Code entity (for clustering)
 *
 * @author daniellink
 *
 */
public class CodeEntity extends Entity {
	private static ArrayList<String> classNames = new ArrayList<>(); // Possible classes
	private String bestLabel; // Concern label that best represents the entity
	private Double bestValue; // Confidence value of bestLabel
	private boolean isNoMatch = false;
	private static int entityCount;
	private final RelaxFeatureVector classificationVector;
	private static HashMap<String, CodeEntity> codeEntities = new HashMap<>();
	private static Integer noMatchCount;
	private String shortFileName;
	private String absoluteFileName;
	private String relativeFileName;
	private String canonicalName;
	private LinkedHashMap<String, Double> labelsByRank = new LinkedHashMap<>();

	// private static ArrayList<String> labelsInOrder = new ArrayList<>();

	// public ArrayList<String> getLabelsInOrder() {
	// return labelsInOrder;
	// }

	private static DecimalFormat df = new DecimalFormat("#0.00");

	public HashMap<String, Double> getLabelsByRank() {
		return labelsByRank;
	}

	public void setLabelsByRank(final LinkedHashMap<String, Double> labelsByRank) {
		this.labelsByRank = labelsByRank;
	}

	public String getRankinString() {
		String rankingsString = "Labels by rank: ";
		for (final String rank : labelsByRank.keySet()) {
			rankingsString += rank + ": ";
			rankingsString += df.format(labelsByRank.get(rank)) + " ";
		}
		return rankingsString;
	}

	public String getOrderString() {
		String orderString = "Labels in order: ";
		for (final String labelName : classNames) {
			if (labelName == "no_match") {
				continue;
			}
			orderString += labelName + ": ";
			final Double ulf = labelsByRank.get(labelName);
			orderString += df.format(ulf) + " ";
		}
		return orderString;
	}

	public static int getEntityCount() {
		return entityCount;
	}

	public static void setEntityCount(final int entityCount) {
		CodeEntity.entityCount = entityCount;
	}

	public boolean isNoMatch() {
		return isNoMatch;
	}

	public void setNoMatch(final boolean isNoMatch) {
		this.isNoMatch = isNoMatch;
		if (isNoMatch == true) {
			noMatchCount++;
			bestLabel = "no_match";
		}
	}

	public static Integer getNoMatchCount() {
		return noMatchCount;
	}

	public static void setNoMatchCount(final Integer noMatchCount) {
		CodeEntity.noMatchCount = noMatchCount;
	}

	public String getCanonicalName() {
		return canonicalName;
	}

	public void setCanonicalName(final String canonicalName) {
		this.canonicalName = canonicalName;
	}

	public String getShortFileName() {
		return shortFileName;
	}

	public void setShortFileName(final String shortFileName) {
		this.shortFileName = shortFileName;
	}

	public String getAbsoluteFileName() {
		return absoluteFileName;
	}

	public String getPasteableFileName() {
		final String pathString = absoluteFileName.replace(" ", "\\ ");
		return pathString;
	}

	public void setAbsoluteFileName(final String absoluteFileName) {
		this.absoluteFileName = absoluteFileName;
	}

	public String getRelativeFileName() {
		return relativeFileName;
	}

	public void setRelativeFileName(final String relativeFileName) {
		this.relativeFileName = relativeFileName;
	}

	public static HashMap<String, CodeEntity> getCodeEntities() {
		return codeEntities;
	}

	public static void setCodeEntities(final HashMap<String, CodeEntity> codeEntities) {
		CodeEntity.codeEntities = codeEntities;
	}

	/**
	 * @return the classNames
	 */
	public static ArrayList<String> getClassNames() {
		return classNames;
	}

	/**
	 * @param classNames
	 *            the classNames to set
	 */
	public static void setClassNames(final ArrayList<String> classNames) {
		CodeEntity.classNames = classNames;
		CodeEntity.classNames.add("no_match");
	}

	public RelaxFeatureVector getClassificationVector() {
		return classificationVector;
	}

	public CodeEntity(final String fileName, final RelaxFeatureVector rfv) {
		classificationVector = rfv;
		setEntity(fileName);
		entityCount++;
	}

	public CodeEntity(final String fileName, final LinkedHashMap<String, Double> values) {
		classificationVector = new RelaxFeatureVector(name, values);
		setEntity(fileName);
		entityCount++;
	}

	private void setEntity(final String fileName) {
		if (fileName.contains("%20")) {
			System.err.println("File name contains '%20': " + fileName);
			System.exit(-1);
		}
		name = TopLevel.getfManager().absoluteFilenameToRelativeFilename(fileName);
		absoluteFileName = fileName;
		relativeFileName = name;
		shortFileName = new File(fileName).getName();
		TopLevel.getCurrentConfig();
		if (Config.getSelectedLanguage().equals(Language.java)) {
			canonicalName = ClassInfo.getJavaFilePackage(fileName) + "." + FilenameUtils.getBaseName(shortFileName);
		} else {
			canonicalName = relativeFileName;
		}
		bestLabel = "";
		bestValue = 0.0;
		for (final String key : classNames) {
			Double currentValue = classificationVector.getClassValues().get(key);
			if (key == "no_match") {
				currentValue = 0.0;
				if (isNoMatch) {
					currentValue = 1.0;
				}
			}
			try {
				if (currentValue >= bestValue) {
					bestValue = currentValue;
					bestLabel = key;
				}
			} catch (final NullPointerException n) {
				System.err.println("Cannot find value for key: " + key);
				System.exit(-1);
			}
		}
		codeEntities.put(name, this);
	}

	/**
	 * @return the bestLabel
	 */
	public String getBestLabel() {
		return bestLabel;
	}

	public String getBestLabelColor() {
		// return ColorManager.getColorByIndex(Clustering.getClusterID(bestLabel));
		return ColorManager.getColorByName(bestLabel);
	}

	/**
	 * @return the bestValue
	 */
	public Double getBestValue() {
		return bestValue;
	}

	/**
	 * @return the classAffinities
	 */
	public LinkedHashMap<String, Double> getClassAffinities() {
		return classificationVector.getClassValues();
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return name;
	}

	public String getFileNameNoFolder() {
		final String[] ronkel = name.split(File.separator);
		final int a = ronkel.length - 1;
		final String returnString = ronkel[a];
		return returnString;
	}

	public String getClassName() {
		return FileUtil.extractFilenamePrefix(getFileNameNoFolder());
	}

	public String getFileNameAsDiff(final String s) {
		final String name2 = name.split(File.pathSeparator)[1];
		final String ronkel = StringUtils.difference(s, name2);
		// System.out.println("### s=" + s + "\n###name2=" + name2 + ",\n###ronkel=" + ronkel);
		return ronkel;
		// return name;
	}

	public int getNumberOfLAbels() {
		return classNames.size();
	}

	/**
	 * @param bestLabel
	 *            the bestLabel to set
	 */
	public void setBestLabel(final String bestLabel) {
		this.bestLabel = bestLabel;
	}

	/**
	 * @param bestValue
	 *            the bestValue to set
	 */
	public void setBestValue(final Double bestValue) {
		this.bestValue = bestValue;
	}

	/**
	 * @param classAffinities
	 *            the classAffinities to set
	 */
	public void setClassAffinities(final LinkedHashMap<String, Double> classAffinities) {
		classificationVector.setClassValues(classAffinities);
	}

	/**
	 * @param fileName
	 *            the fileName to set
	 */
	public void setFileName(final String fileName) {
		name = fileName;
	}

	public String infoString() {
		String out = "";
		out += TopLevel.getCurrentConfig().getRelaxClassifierFileName() + " " + TopLevel.getCurrentConfig().getClassifierMD5sum() + "\n";
		out += classificationVector;
		return out;
	}

	@Override
	public String toString() {
		final DecimalFormat df = new DecimalFormat("#0.00");
		String s = name + "\n";
		for (final String key : classNames) {
			s += key + " " + df.format(classificationVector.getClassValues().get(key)) + " ";
		}
		s += "Best label = " + bestLabel + " " + df.format(bestValue) + "\n";
		return s;
	}

}
