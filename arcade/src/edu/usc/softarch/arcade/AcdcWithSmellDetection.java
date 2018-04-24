package edu.usc.softarch.arcade;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

import acdc.ACDC;
import edu.usc.softarch.arcade.antipattern.detection.ArchSmellDetector;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.CSourceToDepsBuilder;
import edu.usc.softarch.arcade.facts.driver.JavaSourceToDepsBuilder;
import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;
import edu.usc.softarch.arcade.util.CodeCount;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.RecoveryParams;

public class AcdcWithSmellDetection {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(AcdcWithSmellDetection.class);

	public static void main(final String[] args) {
		if (args.length < 3 || args.length > 4) {
			System.out.println("Usage: AcdcWithSmellDetection <inputDirName> <outputDirName> <classesDir> [language]");
			System.exit(-1);
		}
		final RecoveryParams recParms = new RecoveryParams(args);
		final File[] files = recParms.getInputDir().listFiles();
		final Set<File> fileSet = new TreeSet<File>(Arrays.asList(files));
		logger.debug("All files in " + recParms.getInputDir() + ":");
		logger.debug(Joiner.on("\n").join(fileSet));
		boolean nothingtodo = true;
		for (final File versionFolder : fileSet) {
			if (versionFolder.isDirectory()) {
				logger.debug("Identified directory: " + versionFolder.getName());
				nothingtodo = false;
				single(versionFolder, recParms.getOutputDir(), recParms.getClassesDirName(), recParms.getLanguage());
			} else {
				logger.debug("Not a directory: " + versionFolder.getName());
			}
		}
		if (nothingtodo) {
			System.out.println("Nothing to do!");
		}
	}

	public static void single(final File versionFolder, final File outputDir, final String classesDirName, final String language) {
		logger.debug("Processing directory: " + versionFolder.getName());
		// the revision number is really just the name of the subdirectory
		final String revisionNumber = versionFolder.getName();
		// depsRsfFilename is the file name of the dependencies rsf file (one is
		// created per subdirectory of dir)
		final String depsRsfFilename = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_deps.rsf";
		final File depsRsfFile = new File(depsRsfFilename);
		if (!depsRsfFile.getParentFile().exists()) {
			depsRsfFile.getParentFile().mkdirs();
		}

		final String absoluteClassesDir = versionFolder.getAbsolutePath() + File.separatorChar + classesDirName;
		final File classesDirFile = new File(absoluteClassesDir);
		if (!classesDirFile.exists()) {
			return;
		}

		final File classesDir = FileUtil.checkDir(versionFolder.getAbsolutePath() + File.separatorChar + classesDirName, false, true);
//		CodeCount.run(classesDir, outputDir, versionFolder);

		logger.debug("Get deps for revision " + revisionNumber);

		SourceToDepsBuilder builder = new JavaSourceToDepsBuilder();

		if ("c".equals(language)) {
			builder = new CSourceToDepsBuilder();
		}

		builder.build(versionFolder, depsRsfFile);
		if (builder.getEdges().size() == 0) {
			System.out.println("Builder has zero edges!");
			return;
		}

		Config.getCurrentController().runPKGbatchPackager();

		// acdcClusteredfile is the recovered architecture for acdc, one per
		// subdirectory of dir
		final String acdcClusteredFile = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_acdc_clustered.rsf";
		final String[] acdcArgs = { depsRsfFile.getAbsolutePath(), acdcClusteredFile };

		logger.debug("Running acdc for revision " + revisionNumber);
		ACDC.main(acdcArgs);

		// the last element of the smellArgs array is the location of the file
		// containing the detected smells (one is created per subdirectory of
		// dir)
		final String[] smellArgs = { depsRsfFile.getAbsolutePath(), acdcClusteredFile, outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_acdc_smells.ser" };
		logger.debug("Running smell detecion for revision " + revisionNumber);
		ArchSmellDetector.setupAndRunStructuralDetectionAlgs(smellArgs);
	}
}
