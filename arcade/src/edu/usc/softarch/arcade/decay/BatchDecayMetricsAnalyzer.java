package edu.usc.softarch.arcade.decay;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;

public class BatchDecayMetricsAnalyzer {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(BatchDecayMetricsAnalyzer.class);

	public static void main(final String[] args) throws FileNotFoundException {
		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		// Directory containing all clustered rsf files
		final File clustersDir = FileUtil.checkDir(args[0], true, false);

		// Directory containing all deps rsf files
		final File depsDir = FileUtil.checkDir(args[1], true, false);

		List<File> clusterFiles = FileListing.getFileListing(clustersDir);
		final List<File> depsFiles = FileListing.getFileListing(depsDir);

		clusterFiles = FileUtil.sortFileListByVersion(clusterFiles);

		final Map<String, List<Double>> decayMetrics = new LinkedHashMap<String, List<Double>>();

		final String versionSchemeExpr = "[0-9]+\\.[0-9]+(\\.[0-9]+)*";
		for (final File clusterFile : clusterFiles) {
			if (clusterFile.getName().endsWith(".rsf")) {
				final String clusterVersion = FileUtil.extractVersionFromFilename(versionSchemeExpr, clusterFile.getName());

				// Identify appropriate deps file version
				for (final File depsFile : depsFiles) {
					if (depsFile.getName().endsWith(".rsf")) {
						final String depsVersion = FileUtil.extractVersionFromFilename(versionSchemeExpr, depsFile.getName());
						if (clusterVersion.equals(depsVersion)) {
							final String[] dmaArgs = { clusterFile.getAbsolutePath(), depsFile.getAbsolutePath() };
							DecayMetricAnalyzer.main(dmaArgs);

							List<Double> rciVals = null;
							if (decayMetrics.get("rci") != null) {
								rciVals = decayMetrics.get("rci");
							} else {
								rciVals = new ArrayList<Double>();
							}
							rciVals.add(DecayMetricAnalyzer.rciVal);
							decayMetrics.put("rci", rciVals);

							List<Double> twoWayRatios = null;
							if (decayMetrics.get("twoway") != null) {
								twoWayRatios = decayMetrics.get("twoway");
							} else {
								twoWayRatios = new ArrayList<Double>();
							}
							twoWayRatios.add(DecayMetricAnalyzer.twoWayPairRatio);
							decayMetrics.put("twoway", twoWayRatios);

							List<Double> stabilityVals = null;
							if (decayMetrics.get("stability") != null) {
								stabilityVals = decayMetrics.get("stability");
							} else {
								stabilityVals = new ArrayList<Double>();
							}
							stabilityVals.add(DecayMetricAnalyzer.avgStability);
							decayMetrics.put("stability", stabilityVals);

							List<Double> mqRatios = null;
							if (decayMetrics.get("mq") != null) {
								mqRatios = decayMetrics.get("mq");
							} else {
								mqRatios = new ArrayList<Double>();
							}
							mqRatios.add(DecayMetricAnalyzer.mqRatio);
							decayMetrics.put("mq", mqRatios);

							break;
						}
					}
				}

			}
		}

		for (final String key : decayMetrics.keySet()) {
			final List<Double> vals = decayMetrics.get(key);
			final double[] valArr = ArrayUtils.toPrimitive(vals.toArray(new Double[vals.size()]));
			final DescriptiveStatistics stats = new DescriptiveStatistics(valArr);
			final String header = "stats for " + key;
			System.out.println(header);
			logger.info(header);
			logger.info(stats);
			System.out.println(stats);
		}
	}

}
