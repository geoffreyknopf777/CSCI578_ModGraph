package edu.usc.softarch.arcade;

import java.io.File;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.facts.driver.CSourceToDepsBuilder;
import edu.usc.softarch.arcade.facts.driver.JavaSourceToDepsBuilder;
import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;
import edu.usc.softarch.arcade.util.RecoveryParams;

//Author: Pooyan Behnamghader, Daniel Link
// Date: 09/25/2014 and later

public class DependencyExtractorSingle {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(DependencyExtractorSingle.class);

	public static void main(final String[] args) {
		logger.entry(args.toString());
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		final RecoveryParams recParms = new RecoveryParams(args);

		final String versionFolderName = recParms.getInputDirAbsolutePath();
		logger.debug("Processing directory: " + versionFolderName);

		final String absoluteClassesDir = versionFolderName + File.separatorChar + recParms.getClassesDirName();
		final File classesDirFile = new File(absoluteClassesDir);
		if (!classesDirFile.exists()) {
			System.out.println(
					"Classes dir " + absoluteClassesDir + " does not exist, therefore there are no deps to extract!");
			return;
		}

		// depsRsfFilename is the file name of the dependencies rsf file (one is
		// created per subdirectory of dir)
		final String depsRsfFilename = recParms.getOutputDirAbsolutePath() + File.separatorChar + versionFolderName
				+ "_deps.rsf";
		final File depsRsfFile = new File(depsRsfFilename);
		if (!depsRsfFile.getParentFile().exists()) {
			depsRsfFile.getParentFile().mkdirs();
		}

		extract(recParms.getInputDir(), classesDirFile, depsRsfFile);
		logger.traceExit();
	}

	/**
	 *
	 * @param versionDir
	 *            - Directory with a version of the system to be checked
	 * @param classesDir
	 *            - Name of the directory within the versionDir which has the
	 *            classes
	 * @param depsRsfFile
	 *            - File which the dependencies will be written to
	 * @param language
	 *            - Programming language of the portion of the code to be
	 *            evaluated
	 * @return
	 * @deprecated Use {@link #extract(File,File,File)} instead
	 */
	@Deprecated
	public static SourceToDepsBuilder extract(final File versionDir, final File classesDir, final File depsRsfFile,
			final String language) {
		return extract(versionDir, classesDir, depsRsfFile);
	}

	/**
	 *
	 * @param versionDir
	 *            - Directory with a version of the system to be checked
	 * @param classesDir
	 *            - Name of the directory within the versionDir which has the
	 *            classes
	 * @param depsRsfFile
	 *            - File which the dependencies will be written to
	 * @return
	 */
	public static SourceToDepsBuilder extract(final File versionDir, final File classesDir, final File depsRsfFile) {
		logger.entry(versionDir, classesDir, depsRsfFile);
		logger.debug("Getting deps for revision " + versionDir.getPath());
		SourceToDepsBuilder builder = new JavaSourceToDepsBuilder();
		if (Config.getSelectedLanguage().equals(Language.c)) {
			builder = new CSourceToDepsBuilder();
		}
		// This function generates the dependancy graph of an application from
		// the inputDir
		builder.build(classesDir, depsRsfFile);
		// builder.build(versionDir, depsRsfFile);
		if (builder.getEdges().size() == 0) {
			System.out.println("Builder for directory " + classesDir + " has no edges!");
			// return;
		}
		logger.traceExit();
		return builder;
	}
}
