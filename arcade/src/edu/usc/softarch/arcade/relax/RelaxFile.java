package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;
import edu.usc.softarch.arcade.util.ClassInfo;

public class RelaxFile {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(RelaxFile.class);
	// relative filename
	private String relativePath;
	private String absolutePath;
	private File relativeFile;
	private File absoluteFile;
	// private final File file;
	private final ArrayList<RelaxFile> children = new ArrayList<>();
	private final ArrayList<String> childrenNames = new ArrayList<>();
	private String bestColor;
	private Integer bestColorIndex;
	private String bestLabel;
	private RelaxFile parent;
	private long fileSize;
	private ArrayList<String> incomingDependencies;
	private ArrayList<String> outgoingDependencies;
	private ArrayList<RelaxFile> incomingDependencyFiles;
	private ArrayList<RelaxFile> outgoingDependencyFiles;
	private String packageName;
	private String canonicalName;
	private static HashMap<String, RelaxFile> canonicalToRelaxFile = new HashMap<>();
	private int physicalSLOC;
	private int logicalSLOC;

	public static String getColorFromCanonicalName(final String canonicalName) {
		final RelaxFile rf = canonicalToRelaxFile.get(canonicalName);
		if (rf == null) {
			return null;
		}
		final String color = rf.getBestColor();
		return color;
	}

	public ArrayList<RelaxFile> getIncomingDependencyFiles() {
		return incomingDependencyFiles;
	}

	public void setIncomingDependencyFiles(final ArrayList<RelaxFile> incomingDependencyFiles) {
		this.incomingDependencyFiles = incomingDependencyFiles;
	}

	public ArrayList<RelaxFile> getOutgoingDependencyFiles() {
		return outgoingDependencyFiles;
	}

	public void setOutgoingDependencyFiles(final ArrayList<RelaxFile> outgoingDependencyFiles) {
		this.outgoingDependencyFiles = outgoingDependencyFiles;
	}

	public static HashMap<String, RelaxFile> getCanonicalToRelaxFile() {
		return canonicalToRelaxFile;
	}

	public static void setCanonicalToRelaxFile(final HashMap<String, RelaxFile> canonicalToRelaxFile) {
		RelaxFile.canonicalToRelaxFile = canonicalToRelaxFile;
	}

	public String getCanonicalName() {
		return canonicalName;
	}

	public void setCanonicalName(final String canonicalName) {
		this.canonicalName = canonicalName;
	}

	public File getRelativeFile() {
		return relativeFile;
	}

	public void setRelativeFile(final File relativeFile) {
		this.relativeFile = relativeFile;
	}

	public File getAbsoluteFile() {
		return absoluteFile;
	}

	public void setAbsoluteFile(final File absoluteFile) {
		this.absoluteFile = absoluteFile;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(final String packageName) {
		this.packageName = packageName;
	}

	public ArrayList<String> getIncomingDependencies() {
		return incomingDependencies;
	}

	public void setIncomingDependencies(final ArrayList<String> incomingDependencies) {
		this.incomingDependencies = incomingDependencies;
	}

	public ArrayList<String> getOutgoingDependencies() {
		return outgoingDependencies;
	}

	public void setOutgoingDependencies(final ArrayList<String> outgoingDependencies) {
		this.outgoingDependencies = outgoingDependencies;
	}

	public int getLogicalSLOC() {
		return logicalSLOC;
	}

	public void setLogicalSLOC(final int logicalSLOC) {
		this.logicalSLOC = logicalSLOC;
	}

	public int getPhysicalSLOC() {
		return physicalSLOC;
	}

	public void setPhysicalSLOC(final int physicalSLOC) {
		this.physicalSLOC = physicalSLOC;
	}

	public long getFileSize() {
		if (fileSize == 0) {
			getChildrenInfo();
		}
		return fileSize;
	}

	public String getBestColor() {
		if (null == bestColor) {
			getChildrenInfo();
		}
		return bestColor;
	}

	/**
	 * Get info about the children of a node
	 */
	private void getChildrenInfo() {
		String color = ColorManager.getNoMatchColor();
		final Integer bestLabelID = Clustering.getEntityNameBestLabelIDMap().get(relativePath);
		if (null == bestLabelID) { // Label not set yet
			final int numChildren = children.size();
			if (numChildren > 0) { // Case: Folder
				final long[] labelWeights = new long[CodeEntity.getClassNames().size()];
				final long[] childrenSizes = new long[numChildren];
				long overallSize = 0;
				int childIndex = 0;
				for (final RelaxFile childFile : children) {
					final int colIndex = childFile.getBestColorIndex();
					final long size = childFile.getFileSize();
					// labelWeights[colIndex]++;
					labelWeights[colIndex] += size;
					childrenSizes[childIndex++] = size;
					overallSize += size;
				}
				final long highestWeight = NumberUtils.max(labelWeights);
				bestColorIndex = ArrayUtils.indexOf(labelWeights, highestWeight);
				color = ColorManager.getColorByIndex(bestColorIndex);

				fileSize = overallSize;

			} else { // Label not set yet and not a folder - should not happen
				// This should not be reachable - bestLabelID should be set
				System.err.println("### Label not set yet and not a folder when processing file " + relativePath);
				System.err.println("### Exiting");
				System.exit(-1);
			}
		} else { // Individual file
			color = ColorManager.getColorByIndex(bestLabelID);
			// System.out.println("Best Label ID = " + bestLabelID + ", Best Color = " + color);
			bestColorIndex = bestLabelID;
			if (bestColorIndex > CodeEntity.getClassNames().size()) {
				System.err.print("Index of best color is out of range!");
				System.exit(-1);
			}
		}
		bestColor = color;
	}

	public int getBestColorIndex() {
		if (null == bestColorIndex) {
			getChildrenInfo();
		}
		return bestColorIndex;
	}

	public String getBestLabel() {
		return bestLabel;
	}

	public ArrayList<String> getChildrenNames() {
		return childrenNames;
	}

	public String getName() {
		return relativePath;
	}

	// public File getFile() {
	// return file;
	// }

	public ArrayList<RelaxFile> getChildren() {
		return children;
	}

	public RelaxFile getParent() {
		return parent;
	}

	public void setParent(final RelaxFile parent) {
		this.parent = parent;
		if (!parent.getChildrenNames().contains(relativePath)) {
			parent.getChildren().add(this);
			parent.getChildrenNames().add(relativePath);
		}
	}

	/**
	 *
	 * @param fileName
	 *            - the absolute filename from the file system
	 */
	public RelaxFile(final String fileName) {
		logger.entry(fileName);
		final File theFile = new File(fileName);
		setParams(theFile);
		logger.traceExit();
	}

	public RelaxFile(final File theFile) {
		logger.entry(theFile);
		setParams(theFile);
		logger.traceExit();
	}

	private void setParams(final File theFile) {
		logger.entry(theFile);
		incomingDependencies = new ArrayList<>();
		outgoingDependencies = new ArrayList<>();
		relativePath = TopLevel.getfManager().absoluteFilenameToRelativeFilename(theFile.getAbsolutePath());
		// System.out.println("Relative Path = " + relativePath);
		relativeFile = new File(relativePath);
		absolutePath = TopLevel.getfManager().relativeFilenameToAbsoluteFilename(relativePath);
		// System.out.println("Absolute Path = " + absolutePath);
		absoluteFile = new File(absolutePath);
		if (absoluteFile.exists()) {
			fileSize = FileUtils.sizeOf(absoluteFile);
			packageName = ClassInfo.getJavaFilePackage(theFile.getAbsolutePath());
			if (packageName == null && !theFile.getName().contains(".java")) {
				canonicalName = null;
				logger.traceExit();
				return;
			}
			final String rawName = packageName + "." + theFile.getName();
			canonicalName = StringUtils.removeEnd(rawName, ".java");
			canonicalToRelaxFile.put(canonicalName, this);
			// System.out.println("Package Name = " + packageName);
			// System.out.println("Canonical Name = " + canonicalName);
		} else {
			System.err.println("File does not exist when creating RelaxFile: " + theFile);
		}
		logger.traceExit();
	}

	public void addDependencies(final SourceToDepsBuilder s) {
		logger.traceEntry();
		if (absoluteFile.isDirectory()) {
			logger.traceExit();
			return;
		}
		logger.trace("Adding dependencies to " + packageName);
		for (final Pair<String, String> p : s.getEdges()) {
			if (p.getLeft().equals(canonicalName)) {
				logger.trace("--> " + p.getRight());
				outgoingDependencies.add(p.getRight());
			}
			if (p.getRight().equals(canonicalName)) {
				logger.trace("--> " + p.getLeft());
				incomingDependencies.add(p.getLeft());
			}
		}
		logger.traceExit();
	}

	@Override
	public String toString() {
		return relativePath;
	}
}
