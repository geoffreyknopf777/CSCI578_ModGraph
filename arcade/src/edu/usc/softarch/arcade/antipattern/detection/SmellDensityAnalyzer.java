package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.driver.ConcernClusterRsf;
import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.LogUtil;
import edu.usc.softarch.arcade.util.MapUtil;

public class SmellDensityAnalyzer {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(SmellDensityAnalyzer.class);

	public static void main(final String[] args) throws FileNotFoundException {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		LogUtil.printLogFiles();

		// inputDir is the directory containing the .ser files which
		// contain detected smells
		final File inputDir = FileUtil.checkDir(args[0], false, false);

		// directory containing the cluster rsf files matching the smells .ser
		// files
		final File clustersDir = FileUtil.checkDir(args[1], false, false);

		List<File> fileList = FileListing.getFileListing(inputDir);
		fileList = FileUtil.sortFileListByVersion(fileList);
		final Set<File> orderedSerFiles = new LinkedHashSet<File>();
		for (final File file : fileList) {
			if (file.getName().endsWith(".ser")) {
				orderedSerFiles.add(file);
			}
		}

		Map<String, Set<Smell>> versionSmells = new LinkedHashMap<String, Set<Smell>>();
		final String versionSchemeExpr = "[0-9]+\\.[0-9]+(\\.[0-9]+)*";

		for (final File file : orderedSerFiles) {
			logger.debug(file.getName());
			final Set<Smell> smells = SmellUtil.deserializeDetectedSmells(file);
			logger.debug("\tcontains " + smells.size() + " smells");

			logger.debug("\tListing detected smells for file" + file.getName() + ": ");
			for (final Smell smell : smells) {
				logger.debug("\t" + SmellUtil.getSmellAbbreviation(smell) + " " + smell);

			}

			final String version = FileUtil.extractVersionFromFilename(versionSchemeExpr, file.getName());

			assert !version.equals("") : "Could not extract version";
			versionSmells.put(version, smells);
		}

		Map<String, Set<ConcernCluster>> versionClusters = new LinkedHashMap<String, Set<ConcernCluster>>();
		final List<File> clustersFileList = FileListing.getFileListing(clustersDir);
		for (final File file : clustersFileList) {
			// Pattern p = Pattern.compile(versionSchemeExpr);
			// Matcher m = p.matcher(file.getName());
			final Set<ConcernCluster> clusters = ConcernClusterRsf.extractConcernClustersFromRsfFile(file);

			final String version = FileUtil.extractVersionFromFilename(versionSchemeExpr, file.getName());
			assert !version.equals("") : "Could not extract version";
			versionClusters.put(version, clusters);
		}

		versionClusters = MapUtil.sortByKeyVersion(versionClusters);
		versionSmells = MapUtil.sortByKeyVersion(versionSmells);

		final double[] smellDensityArr = new double[versionClusters.keySet().size()];
		final double[] clustersRatioArr = new double[versionClusters.keySet().size()];
		int idx = 0;
		for (final String version : versionClusters.keySet()) {
			final Set<Smell> smells = versionSmells.get(version);

			final Set<ConcernCluster> allSmellyClusters = new HashSet<ConcernCluster>();
			for (final Smell smell : smells) {
				allSmellyClusters.addAll(smell.clusters);
			}

			final Set<ConcernCluster> clusters = versionClusters.get(version);
			final double smellDensity = (double) smells.size() / (double) clusters.size();
			smellDensityArr[idx] = smellDensity;

			final double affectedClustersRatio = (double) allSmellyClusters.size() / (double) clusters.size();
			clustersRatioArr[idx] = affectedClustersRatio;

			idx++;

			System.out.println("version: " + version);
			System.out.println("# smells: " + smells.size());
			System.out.println("# clusters: " + clusters.size());
			System.out.println("smell density: " + smellDensity);
			System.out.println("ratio of smelly clusters to total clusters: " + affectedClustersRatio);
			System.out.println();
		}
		System.out.println("Smell density stats:");
		final DescriptiveStatistics smellDensityStats = new DescriptiveStatistics(smellDensityArr);
		System.out.println(smellDensityStats);

		System.out.println("Clusters ratio stats:");
		final DescriptiveStatistics clustersRatioStats = new DescriptiveStatistics(clustersRatioArr);
		System.out.println(clustersRatioStats);

	}

}
