package edu.usc.softarch.arcade.facts.driver;

import java.io.File;

import edu.usc.softarch.arcade.DependencyExtractorSingle;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.util.FileUtil;

;

public class DepsMaker {

	static File depsRsfFile;

	/**
	 * @return the depsRsfFile
	 */
	public static File getDepsRsfFile() {
		return depsRsfFile;
	}

	/**
	 * @param depsRsfFile
	 *            the depsRsfFile to set
	 */
	public static void setDepsRsfFile(final File depsRsfFile) {
		DepsMaker.depsRsfFile = depsRsfFile;
	}

	public static String getDepsRsfFilename() {
		return depsRsfFile.getAbsolutePath();
	}

	public static SourceToDepsBuilder generate(final String revisionNumber, final File versionFolder, final File outputDir, final String classesDirName) {
		// depsRsfFilename is the file name of the dependencies rsf file (one is
		// created per subdirectory of dir)
		depsRsfFile = FileUtil.checkFile(outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_deps.rsf", true, false);
		// final Config currentConfig = Controller.getCurrentView().getConfig();
		Config.setDepsRsfFile(depsRsfFile);
		final File classesDir = FileUtil.checkDir(versionFolder.getAbsolutePath() + File.separatorChar + classesDirName, false, true);
		final SourceToDepsBuilder builder = DependencyExtractorSingle.extract(versionFolder, classesDir, depsRsfFile);
		return builder;
	}
}
