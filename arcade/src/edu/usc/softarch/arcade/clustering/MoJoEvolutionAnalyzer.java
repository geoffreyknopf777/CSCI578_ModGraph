package edu.usc.softarch.arcade.clustering;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import mojo.MoJoCalculator;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;

public class MoJoEvolutionAnalyzer {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(MoJoEvolutionAnalyzer.class);

	public static void main(final String[] args) throws FileNotFoundException {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		// File containing only containing recovered architectures stored as rsf
		// files
		final String clusterFilesDir = args[0];

		List<File> clusterFiles = FileListing.getFileListing(FileUtil.checkDir(clusterFilesDir, false, false));
		// FileUtil.sortFileListByVersion sorts the list by the versioning
		// scheme found in the filename
		clusterFiles = FileUtil.sortFileListByVersion(clusterFiles);

		for (int comparisonDistance = 1; comparisonDistance < clusterFiles.size(); comparisonDistance++) {
			File prevFile = null;
			final List<Double> mojoFmValues = new ArrayList<Double>();
			System.out.println("Comparison distance is: " + comparisonDistance);
			for (int i = 0; i < clusterFiles.size(); i += comparisonDistance) {
				// System.out.println("i: " + i);
				final File currFile = clusterFiles.get(i);
				// exclude annoying .ds_store files from OSX
				if (!currFile.getName().equals(".DS_Store")) {
					if (prevFile != null && currFile != null) {
						final double mojoFmValue = doMoJoFMComparison(currFile, prevFile);
						mojoFmValues.add(mojoFmValue);
					}
					prevFile = currFile;
				}
			}
			final Double[] mojoFmArr = new Double[mojoFmValues.size()];
			mojoFmValues.toArray(mojoFmArr);

			final DescriptiveStatistics stats = new DescriptiveStatistics(ArrayUtils.toPrimitive(mojoFmArr));

			/*
			 * System.out.println("N: " + stats.getN());
			 * System.out.println("max: " + stats.getMax());
			 * System.out.println("min: " + stats.getMin());
			 * System.out.println("mean: " + stats.getMean());
			 */
			System.out.println(stats);

			System.out.println();
		}

	}

	public static double doMoJoFMComparison(final File prevFile, final File currFile) {
		final MoJoCalculator mojoCalc = new MoJoCalculator(prevFile, currFile, null);
		final double mojoFmValue = mojoCalc.mojofm();
		System.out.println("MoJoFM from " + prevFile.getName() + " to " + currFile.getName() + ": " + mojoFmValue);
		return mojoFmValue;
	}

}
