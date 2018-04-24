package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.LogUtil;

public class SmellEvolutionAnalyzer {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(SmellEvolutionAnalyzer.class);

	public static void main(final String[] args) throws FileNotFoundException {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		LogUtil.printLogFiles();

		// inputDirFilename is the directory containing the .ser files which
		// contain detected smells
		final String inputDirFilename = args[0];

		List<File> fileList = FileListing.getFileListing(FileUtil.checkDir(inputDirFilename, false, false));
		fileList = FileUtil.sortFileListByVersion(fileList);
		final List<File> orderedSerFiles = new ArrayList<File>();
		for (final File file : fileList) {
			if (file.getName().endsWith(".ser")) {
				orderedSerFiles.add(file);
			}
		}

		final double[] smellCounts = new double[orderedSerFiles.size()];
		int idx = 0;
		for (final File file : orderedSerFiles) {
			logger.debug(file.getName());
			final Set<Smell> smells = SmellUtil.deserializeDetectedSmells(file);
			logger.debug("\tcontains " + smells.size() + " smells");

			smellCounts[idx] = smells.size();
			idx++;

			logger.debug("\tListing detected smells for file" + file.getName() + ": ");
			for (final Smell smell : smells) {
				logger.debug("\t" + SmellUtil.getSmellAbbreviation(smell) + " " + smell);
			}

			final Pattern p = Pattern.compile("[0-9]+\\.[0-9]+(\\.[0-9]+)*");
			final Matcher m = p.matcher(file.getName());
			if (m.find()) {
				System.out.println(m.group(0));
			}
		}

		final DescriptiveStatistics stats = new DescriptiveStatistics(smellCounts);
		System.out.println();
		System.out.println(stats);
		logger.debug(stats);

	}

}