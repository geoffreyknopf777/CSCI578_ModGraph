package edu.usc.softarch.arcade.metrics;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import edu.usc.softarch.arcade.util.FileUtil;

public class BatchSystemEvo {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(BatchSystemEvo.class);

	public static void main(final String[] args) throws FileNotFoundException {
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final BatchSystemEvoOptions options = new BatchSystemEvoOptions();
		final JCommander jcmd = new JCommander(options);

		try {
			jcmd.parse(args);
		} catch (final ParameterException e) {
			logger.debug(e.getMessage());
			jcmd.usage();
			System.exit(1);
		}

		logger.debug(options.parameters);
		logger.debug("\n");

		// File containing only containing recovered architectures stored as rsf
		// files
		final String clusterFilesDir = options.parameters.get(0);

		// List<File> clusterFiles = FileListing.getFileListing(FileUtil.checkDir(clusterFilesDir, true, false));

		List<File> clusterFiles = (List<File>) FileUtils.listFiles(FileUtil.checkDir(clusterFilesDir, true, false), org.apache.commons.io.filefilter.TrueFileFilter.TRUE, FileFileFilter.FILE);
		final List<File> junkFiles = new ArrayList<>();
		for (final File f : clusterFiles) {
			if (f.getName().contains("DS_Store")) {
				junkFiles.add(f);
			}
		}
		for (final File f : junkFiles) {
			clusterFiles.remove(f);
		}

		// FileUtil.sortFileListByVersion sorts the list by the versioning
		// scheme found in the filename
		clusterFiles = FileUtil.sortFileListByVersion(clusterFiles);

		if (options.distopt == 1) {
			compareOverDistanceOfOne(clusterFiles);
		} else if (options.distopt == 2) {
			compareWithVdistGt1ForAll(clusterFiles);
		} else if (options.distopt == 3) {
			compareWithVdistGt1ForSubset(clusterFiles);
		} else {
			throw new RuntimeException("Unknown value for option distopt: " + options.distopt);
		}

	}

	private static void compareOverDistanceOfOne(final List<File> clusterFiles) {
		File prevFile = null;
		final List<Double> sysEvoValues = new ArrayList<>();
		final int comparisonDistance = 1;
		System.out.println("Comparison distance is: " + comparisonDistance);
		for (int i = 0; i < clusterFiles.size(); i += comparisonDistance) {
			// System.out.println("i: " + i);
			final File currFile = clusterFiles.get(i);
			// exclude annoying .ds_store files from OSX
			if (!currFile.getName().equals(".DS_Store")) {
				if (prevFile != null && currFile != null) {
					final double sysEvoValue = computeSysEvo(prevFile, currFile);
					sysEvoValues.add(sysEvoValue);
				}
				prevFile = currFile;
			}
		}
		final Double[] sysEvoArr = new Double[sysEvoValues.size()];
		sysEvoValues.toArray(sysEvoArr);

		final DescriptiveStatistics stats = new DescriptiveStatistics(ArrayUtils.toPrimitive(sysEvoArr));

		/*
		 * System.out.println("N: " + stats.getN()); System.out.println("max: " + stats.getMax()); System.out.println("min: " + stats.getMin()); System.out.println("mean: " + stats.getMean());
		 */
		System.out.println(stats);

		System.out.println();
	}

	private static void compareWithVdistGt1ForAll(final List<File> clusterFiles) {
		// for (int comparisonDistance = 1; comparisonDistance < clusterFiles
		// .size(); comparisonDistance++) {
		// File prevFile = null;
		// System.out.println("Comparison distance is: " + comparisonDistance);
		for (int i = 0; i < clusterFiles.size(); i++) {
			final List<Double> sysEvoValues = new ArrayList<>();
			System.out.println("start index is: " + i);
			for (int j = i + 1; j < clusterFiles.size(); j++) {
				// System.out.println("i: " + i);
				final File file1 = clusterFiles.get(i);
				final File file2 = clusterFiles.get(j);
				// exclude annoying .ds_store files from OSX
				if (!file1.getName().equals(".DS_Store")) {
					// if (prevFile != null && file1 != null) {
					final double sysEvoValue = computeSysEvo(file1, file2);
					sysEvoValues.add(sysEvoValue);
					// }
					// prevFile = file1;
				}
			}
			final Double[] sysEvoArr = new Double[sysEvoValues.size()];
			sysEvoValues.toArray(sysEvoArr);

			final DescriptiveStatistics stats = new DescriptiveStatistics(ArrayUtils.toPrimitive(sysEvoArr));

			/*
			 * System.out.println("N: " + stats.getN()); System.out.println("max: " + stats.getMax()); System.out.println("min: " + stats.getMin()); System.out.println("mean: " + stats.getMean());
			 */
			System.out.println(stats);

			System.out.println();

			// if (comparisonDistance == 1)
			// break;
		}
		// }
	}

	private static void compareWithVdistGt1ForSubset(final List<File> clusterFiles) {
		for (int comparisonDistance = 1; comparisonDistance < clusterFiles.size(); comparisonDistance++) {
			File prevFile = null;
			final List<Double> sysEvoValues = new ArrayList<>();
			System.out.println("Comparison distance is: " + comparisonDistance);
			for (int i = 0; i < clusterFiles.size(); i += comparisonDistance) {
				// System.out.println("i: " + i);
				final File currFile = clusterFiles.get(i);
				// exclude annoying .ds_store files from OSX
				if (!currFile.getName().equals(".DS_Store")) {
					if (prevFile != null && currFile != null) {
						final double sysEvoValue = computeSysEvo(prevFile, currFile);
						sysEvoValues.add(sysEvoValue);
					}
					prevFile = currFile;
				}
			}
			final Double[] sysEvoArr = new Double[sysEvoValues.size()];
			sysEvoValues.toArray(sysEvoArr);

			final DescriptiveStatistics stats = new DescriptiveStatistics(ArrayUtils.toPrimitive(sysEvoArr));

			/*
			 * System.out.println("N: " + stats.getN()); System.out.println("max: " + stats.getMax()); System.out.println("min: " + stats.getMin()); System.out.println("mean: " + stats.getMean());
			 */
			System.out.println(stats);

			System.out.println();
		}
	}

	public static double computeSysEvo(final File prevFile, final File currFile) {
		final String[] sysEvoArgs = { prevFile.getAbsolutePath(), currFile.getAbsolutePath() };
		SystemEvo.main(sysEvoArgs);
		final double sysEvoValue = SystemEvo.sysEvo;
		System.out.println("SysEvo from " + prevFile.getName() + " to " + currFile.getName() + ": " + sysEvoValue);
		return sysEvoValue;
	}

}
