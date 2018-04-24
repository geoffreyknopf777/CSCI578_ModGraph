package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.util.CodeCount;
import edu.usc.softarch.arcade.util.CodeCountEntity;
import edu.usc.softarch.arcade.util.FileUtil;

public class FileManager {
	private Collection<File> files;
	private String[] filesSorted;
	private String[] relativeFilesSorted;
	private File rootDirectory;
	// private String rootDirectoryString;
	// private ArrayList<String> fileNames;
	private final HashMap<String, RelaxFile> relaxFiles = new HashMap<>();
	private static Logger logger = LogManager.getLogger(FileManager.class.getName());

	// public ArrayList<String> getFileNames() {
	// if (null == fileNames || fileNames.size() < files.size()) {
	// fileNames = new ArrayList<>();
	// for (final File f : files) {
	// fileNames.add(f.getAbsolutePath());
	// }
	// }
	// return fileNames;
	// }

	// public void setFileNames(final ArrayList<String> fileNames) {
	// this.fileNames = fileNames;
	// }

	public File getRootDirectory() {
		return rootDirectory;
	}

	public void setRootDirectory(final File rootDirectory) {
		this.rootDirectory = rootDirectory;
	}

	public HashMap<String, RelaxFile> getRelaxFiles() {
		return relaxFiles;
	}

	public Collection<File> getFiles() {
		return files;
	}

	public FileManager() {
	}

	public FileManager(final File directory, final RegexFileFilter filter) {
		logger.entry(directory, filter);
		rootDirectory = directory;
		// rootDirectoryString = rootDirectory.getAbsolutePath();
		files = FileUtils.listFiles(directory, filter, DirectoryFileFilter.DIRECTORY);
		if (null == files || files.isEmpty()) {
			System.err.println("### No files exist with the extensions of the selected programming language - exiting\n");
			logger.traceExit();
			System.exit(-1);
		}
		final ArrayList<File> garbageFiles = new ArrayList<>();
		for (final File f : files) {
			if (f.length() == 0) {
				garbageFiles.add(f);
			}
		}
		for (final File f : garbageFiles) {
			files.remove(f);
			logger.trace("Removed empty file: " + f);
		}
		// if (Config.getSelectedLanguage().equals(Language.java)) {
		// final ArrayList<File> newFiles = new ArrayList<>();
		// for (final File f : files) {
		// final File fNew = new File(f.getAbsolutePath().replace(".class",
		// ".java"));
		// if (fNew.canRead()) {
		// newFiles.add(fNew);
		// }
		// }
		// files = newFiles;
		// }

		sortFilesArray();
		logger.traceExit();
	}

	public void distribute(final int numberDirs) {
		final SourceDistributor sd = new SourceDistributor(rootDirectory, new ArrayList<>(Arrays.asList(filesSorted)), numberDirs);
		logger.debug(sd);
	}

	@Override
	public String toString() {
		logger.traceEntry();
		String out = "";
		for (final String s : relativeFilesSorted) {
			out += s + "\n";
		}
		logger.traceExit();
		return out;
	}

	public void sortFilesArray() {
		if (files.isEmpty()) {
			return;
		}
		filesSorted = new String[files.size()];
		relativeFilesSorted = new String[files.size()];
		int i = 0;
		for (final File currentFile : files) {
			final String currentFileString = currentFile.getAbsolutePath();
			filesSorted[i] = currentFileString;
			relativeFilesSorted[i++] = StringUtils.difference(rootDirectory.getAbsolutePath(), currentFileString);
		}
		Arrays.sort(filesSorted);
		Arrays.sort(relativeFilesSorted);
	}

	public void writeManifestToFile(final String fileName) {
		logger.entry(fileName);
		// String out=this.toString();
		final File f = FileUtil.checkFile(fileName, false, false);
		try {
			FileUtils.writeStringToFile(f, toString());
			// FileUtils.writeLines(f, getSortedFilesArray());
		} catch (final IOException e) {
			logger.error("Cannot write manifest to file {}", fileName);
		}
		logger.entry(fileName);
	}

	public void populateRelaxFiles() {
		if (null == filesSorted) {
			System.err.println("Array of sorted files is empty - exiting!");
			System.exit(-1);
		}
		for (final String f : filesSorted) {
			final RelaxFile rf = new RelaxFile(f);
			addRelaxFileParentHierarchy(rf);
			relaxFiles.put(rf.getName(), rf);
		}
	}

	/**
	 * Recursively add the parent hiearchy of the corresponding RelaxFile as far
	 * as it's not populated yet
	 *
	 * @param rf
	 */
	public void addRelaxFileParentHierarchy(final RelaxFile rf) {
		logger.traceEntry();
		// System.out.println("### Absolute filename = " +
		// rf.getAbsolutePath());
		final String pathString = rf.getAbsolutePath().trim().replaceAll("\\s+", " ");
		pathString.substring(0, pathString.length() - 1);
		// System.out.println("Path substring = " + pathSubstring);
		// if (pathSubstring.equals(rootDirectoryString)) {
		// System.out.println("Reached root directory of version!");
		// }
		final String rfParentName = rf.getAbsoluteFile().getParent();
		final String rfRelativeParentName = rf.getRelativeFile().getParent();
		// System.out.println("Relative parent = " + rfRelativeParentName);
		if (rfRelativeParentName == null) { // should only happen at the root of
											// the hierarchy
			// System.out.println("No Parent");
			// System.exit(-1);
			return;
		}
		RelaxFile rfParent;
		if (!relaxFiles.containsKey(rfParentName)) { // parent doesn't exist yet
			rfParent = new RelaxFile(rfParentName);
		} else { // parent already exists
			rfParent = relaxFiles.get(rfParentName);
		}
		rf.setParent(rfParent);
		relaxFiles.put(rfParentName, rfParent);
		// System.out.println("Parent = " + rfParent);
		// System.out.println("Parent Name = " + rfParentName);

		addRelaxFileParentHierarchy(rfParent);
		if (null == rfParent.getParent()) { // if we're not at the root and a
											// parent doesn't exist yet, recurse
			addRelaxFileParentHierarchy(rfParent);
		}
		logger.traceExit();
	}

	public void writeRelaxFilesDot(final File outFile, final Set<Pair<String, String>> edges) {
		final int width = 85, height = 100;
		String out = "";
		out += "digraph DirectoryStructure { size = \"" + height + "," + width + "\"; ratio = \"fill\";\n";
		out += "node [shape=record, fontsize=24, fontname=Helvetica];";
		for (final RelaxFile rf : relaxFiles.values()) {
			// Individual file (leaf node)
			if (rf.getChildren().isEmpty()) {
				out += "\t" + dotRelaxFileString(rf, true) + "];\n";
				continue;
			}
			out += "\t" + dotRelaxFileString(rf, true) + "];\n";
			// Folder (parent node)
			out += "\t\"" + rf.getName() + "\" -> {";
			for (final RelaxFile cf : rf.getChildren()) {
				out += dotRelaxFileString(cf, false) + "] ";
			}
			out += "} [color=\"" + rf.getBestColor() + "\"];\n";
			final int numChildren = rf.getChildren().size();
			if (numChildren > 1) {
				out += "subgraph \"cluster_" + rf.getName() + "\" {";
				for (final RelaxFile cf : rf.getChildren()) {
					out += "\"" + cf.getName() + "\"";
				}
				out += "};\n";
			}
		}
		out += "}";

		try {
			FileUtils.writeStringToFile(outFile, out);
		} catch (final IOException e) {
			System.err.println("Unable to write directory structure description ");
			System.exit(-1);
		}

	}

	private String dotRelaxFileString(final RelaxFile rf, final boolean addColor) {
		String outString = "";
		final String rfBasename = FilenameUtils.getBaseName(rf.getName());
		outString = "\"" + rf.getName() + "\"";
		// outString += "[shape = \"record\", label=\"<f0> " + rfBasename +
		// "|<f1> " + rf.getFileSize() + "|<f2> " + rf.getLogicalSLOC() + "\"";
		// if (addColor) {
		// outString += ", color = " + rf.getBestColor();
		// }
		final int numCols = Math.max(2, rf.getOutgoingDependencies().size());
		final String colorName = rf.getBestColor();
		String bestColorString = "";
		if (!colorName.equals("null")) {
			bestColorString = " bgcolor=\"" + colorName + "\"";
		}
		final String absolutePath = rf.getAbsolutePath();
		System.out.println("Absolute filename = " + absolutePath);
		final CodeCountEntity cce = CodeCount.getEntitiesMap().get(absolutePath);
		int lSLOC = -1;
		if (cce != null) {
			lSLOC = cce.getLogicalSLOC();
		}
		System.out.println("Logical SLOC = " + lSLOC);
		outString += "[shape=none, margin=0, label=<<table border=\"0\" cellborder=\"1\" cellspacing=\"0\" cellpadding=\"4\"><tr><td colspan=\"" + numCols + "\">" + rfBasename
				+ "</td></tr><tr><td port=\"0\">" + rf.getFileSize() + "</td><td" + bestColorString + ">" + lSLOC + "</td></tr>";

		final ArrayList<String> oDeps = rf.getOutgoingDependencies();
		outString += "<tr>";
		if (oDeps.size() > 0) {
			// final double widthPercentage = 100.0 / oDeps.size();
			// for (int i = 0; i < oDeps.size(); i++) {
			for (final String s : oDeps) {
				final String oColorName = RelaxFile.getColorFromCanonicalName(s);
				String oBestColorString = "";
				if (oColorName != null) {
					oBestColorString = " bgcolor=\"" + oColorName + "\"";
				}
				outString += "<td" + oBestColorString + ">";
				outString += "#";
				outString += "</td>";
			}

		} else {
			outString += "<td>---none---</td>";
		}
		outString += "</tr>";

		final ArrayList<String> iDeps = rf.getIncomingDependencies();
		outString += "<tr>";
		if (iDeps.size() > 0) {
			// final double widthPercentage = 100.0 / oDeps.size();
			// for (int i = 0; i < oDeps.size(); i++) {
			for (final String s : iDeps) {
				final String iColorName = RelaxFile.getColorFromCanonicalName(s);
				String iBestColorString = "";
				if (iColorName != null) {
					iBestColorString = " bgcolor=\"" + iColorName + "\"";
				}
				outString += "<td" + iBestColorString + ">";
				outString += "#";
				outString += "</td>";
			}

		} else {
			outString += "<td>---none---</td>";
		}
		outString += "</tr>";

		outString += "</table>>";
		return outString;
	}

	public String relativeFilenameToAbsoluteFilename(final String relativeFilename) {
		final String returnString = rootDirectory + File.separator + relativeFilename;
		return returnString;

	}

	public String absoluteFilenameToRelativeFilename(final String absoluteFilename) {
		logger.entry("absoluteFilenameToRelativeFilename", absoluteFilename);
		String tmp;
		if (absoluteFilename.toLowerCase().startsWith("file")) {
			tmp = absoluteFilename.split(File.pathSeparator)[1];
		} else {
			tmp = absoluteFilename;
		}
		String returnString = StringUtils.difference(rootDirectory.getAbsolutePath(), tmp);
		if (returnString.startsWith(File.separator)) {
			returnString = returnString.substring(1);
		}
		logger.traceExit("absoluteFilenameToRelativeFilename: " + returnString);
		return returnString;
	}
}
