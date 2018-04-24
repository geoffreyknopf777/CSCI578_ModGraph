package edu.usc.softarch.arcade;

import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.antipattern.detection.ArchSmellDetector;
import edu.usc.softarch.arcade.util.FileUtil;

public class PkgsWithSmellDetection {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(PkgsWithSmellDetection.class);

	public static void main(final String[] args) {
		logger.entry(args.toString());
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final PkgsWithSmellDetectionOptions psdOptions = new PkgsWithSmellDetectionOptions();
		final JCommander jcmd = new JCommander(psdOptions);

		try {
			jcmd.parse(args);
		} catch (final ParameterException e) {
			System.out.println(e.getMessage());
			jcmd.usage();
			System.exit(1);
		}

		final File clustersDir = FileUtil.checkDir(psdOptions.clustersDir, false, false);
		final File smellsDir = FileUtil.checkDir(psdOptions.smellsDir, false, false);
		final File depsDir = FileUtil.checkDir(psdOptions.depsDir, false, false);

		final File[] files = clustersDir.listFiles();
		final Set<File> fileSet = new TreeSet<File>(Arrays.asList(files));
		logger.debug("All files in " + clustersDir + ":");
		logger.debug(Joiner.on("\n").join(fileSet));
		for (final File file : fileSet) {
			if (file.isDirectory()) {
				logger.debug("Identified directory: " + file.getName());
			}
		}

		for (final File file : fileSet) {
			if (file.getName().endsWith("_pkgs.rsf")) {
				final File clustersPkgFile = file;
				final String pkgFilePrefix = file.getName().replace("_pkgs.rsf", "");
				final String expectedDepsFilename = pkgFilePrefix + ".rsf";
				File identifiedDepsFile = null;
				final File[] potentialDepsFiles = depsDir.listFiles();
				for (final File potentialDepsFile : potentialDepsFiles) {
					if (potentialDepsFile.getName().equals(expectedDepsFilename)) {
						identifiedDepsFile = potentialDepsFile;
					}
				}

				if (identifiedDepsFile == null) {
					System.err.println("Could not find deps file: " + expectedDepsFilename);
					System.exit(2);
				}

				// the last element of the smellArgs array is the location of
				// the file containing the detected smells (one is created per
				// subdirectory of dir)
				final String[] smellArgs = { identifiedDepsFile.getAbsolutePath(), clustersPkgFile.getAbsolutePath(),
						smellsDir.getAbsolutePath() + File.separatorChar + pkgFilePrefix + "_pkg_smells.ser" };
				logger.debug("Running smell detecion for release " + pkgFilePrefix);
				ArchSmellDetector.setupAndRunStructuralDetectionAlgs(smellArgs);
			}
		}
		logger.traceExit();
	}
}
