package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import mojo.MoJoCalculator;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.util.FileUtil;

public class BatchSmellDetectionRunner {

	public static void main(final String[] args) {

		// String smellClassesFilename = args[0];
		final File gtRsfsDir = FileUtil.checkDir(args[1], false, false);
		final File docTopicsFile = FileUtil.checkFile(args[2], true, false);
		final String selectedLang = args[3];
		final File depsRsfFile = FileUtil.checkFile(args[4], true, false);
		final File techniquesDir = FileUtil.checkDir(args[5], true, false);
		final File groundTruthFile = FileUtil.checkFile(args[6], true, false);
		// obtain rsf files in output directory
		// final File gtRsfsDirFile = new File(gtRsfsDir);
		final File[] newGtFiles = gtRsfsDir.listFiles((FileFilter) file -> file.getName().endsWith(".rsf"));

		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		Config.setMalletDocTopicsFilename(docTopicsFile);
		if (selectedLang.equals("c")) {
			Config.setSelectedLanguage(Config.Language.c);
		} else if (selectedLang.equals("java")) {
			Config.setSelectedLanguage(Config.Language.java);
		}
		Config.setDepsRsfFile(depsRsfFile);
		try {
			final String mojoFmMappingFilename = "mojofm_mapping.csv";
			final PrintWriter writer = new PrintWriter(techniquesDir.getPath() + File.separatorChar + mojoFmMappingFilename, "UTF-8");
			for (final File gtRsfFile : newGtFiles) {
				Config.setSmellClustersFile(gtRsfFile);
				final String prefix = FileUtil.extractFilenamePrefix(gtRsfFile);
				final String detectedSmellsFilename = techniquesDir + prefix + "_smells.ser";

				ArchSmellDetector.runAllDetectionAlgs(FileUtil.checkFile(detectedSmellsFilename, false, false));

				final MoJoCalculator mojoCalc = new MoJoCalculator(gtRsfFile, groundTruthFile, null);
				final double mojoFmValue = mojoCalc.mojofm();
				System.out.println(mojoFmValue);

				writer.println(detectedSmellsFilename + "," + mojoFmValue);

			}
			writer.close();

		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
