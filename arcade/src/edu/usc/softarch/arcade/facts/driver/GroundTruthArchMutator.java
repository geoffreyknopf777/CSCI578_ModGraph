package edu.usc.softarch.arcade.facts.driver;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import mojo.MoJoCalculator;
import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.util.FileUtil;

public class GroundTruthArchMutator {

	public static void main(final String groundTruthFileName, final String outputDirName) {
		final File groundTruthFile = FileUtil.checkFile(groundTruthFileName, false, false);
		final File outputDir = FileUtil.checkFile(outputDirName, true, false);
		final Set<ConcernCluster> clusters = ConcernClusterRsf.extractConcernClustersFromRsfFile(groundTruthFile);
		final Set<String> allEntitiesSet = new LinkedHashSet<String>();
		final List<String> allEntitiesList = new ArrayList<String>();

		for (final ConcernCluster cluster : clusters) {
			allEntitiesList.addAll(cluster.getEntities());
		}

		allEntitiesSet.addAll(allEntitiesList);
		allEntitiesList.clear();
		allEntitiesList.addAll(allEntitiesSet);

		final int seedLimit = 10;
		for (int seed = 0; seed < seedLimit; seed++) {
			final Random rand = new Random(seed);
			final int tenPercentOfEntities = (int) Math.ceil(0.10 * allEntitiesList.size());
			final List<String> selectedEntities = new ArrayList<String>();
			System.out.println("Randomly selected entitites:");
			for (int i = 0; i < tenPercentOfEntities; i++) {
				final String selectedEntity = allEntitiesList.get(rand.nextInt(allEntitiesList.size()));
				selectedEntities.add(selectedEntity);
				System.out.println(selectedEntity);
			}

			for (final String entity : selectedEntities) {
				final ConcernCluster containingCluster = findContainingCluster(clusters, entity);
				assert containingCluster != null : "Obtained null cluster for " + entity;

				final ConcernCluster targetCluster = randomlySelectTargetCluster(clusters, containingCluster, seed);

				final int containingClusterBeforeSize = containingCluster.getEntities().size();
				containingCluster.getEntities().remove(entity);
				assert containingCluster.getEntities().size() == containingClusterBeforeSize - 1;

				final int targetClusterBeforeSize = targetCluster.getEntities().size();
				targetCluster.getEntities().add(entity);
				assert targetCluster.getEntities().size() == targetClusterBeforeSize + 1;
			}

			writeMutatedClusterRsfFile(clusters, seed, groundTruthFile, outputDir);
		}

		// obtain rsf files in output directory
		final File[] newGtFiles = outputDir.listFiles((FileFilter) file -> file.getName().endsWith(".rsf"));

		final String mojoFmMappingFilename = "mojofm_mapping.csv";
		try {
			final PrintWriter writer = new PrintWriter(outputDir.getPath() + File.separatorChar + mojoFmMappingFilename, "UTF-8");
			for (final File newGtFile : newGtFiles) {
				final MoJoCalculator mojoCalc = new MoJoCalculator(newGtFile, groundTruthFile, null);
				final double mojoFmValue = mojoCalc.mojofm();
				System.out.println(mojoFmValue);

				writer.println(newGtFile.getAbsolutePath() + "," + mojoFmValue);

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

	public static void writeMutatedClusterRsfFile(final Set<ConcernCluster> clusters, final long seed, final File groundTruthFile, final File outputDir) {
		final String suffix = FileUtil.extractFilenameSuffix(groundTruthFile);
		final String prefix = FileUtil.extractFilenamePrefix(groundTruthFile);

		final String newGroundTruthFilename = outputDir.getPath() + File.separatorChar + prefix + "_" + seed + suffix;

		try {
			final PrintWriter writer = new PrintWriter(newGroundTruthFilename, "UTF-8");
			for (final ConcernCluster cluster : clusters) {
				for (final String entity : cluster.getEntities()) {
					final String line = "contain " + cluster.getName() + " " + entity;
					writer.println(line);
				}
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

	private static ConcernCluster randomlySelectTargetCluster(final Set<ConcernCluster> clusters, final ConcernCluster containingCluster, final long seed) {

		final Set<ConcernCluster> copiedClusters = new HashSet<ConcernCluster>(clusters);
		copiedClusters.remove(containingCluster);

		final List<ConcernCluster> reducedClustersList = new ArrayList<ConcernCluster>(copiedClusters);
		final Random rand = new Random(seed);

		final ConcernCluster targetCluster = reducedClustersList.get(rand.nextInt(reducedClustersList.size()));
		assert targetCluster != null : "Obtained null cluster when randomly selecting target cluster";
		return targetCluster;
	}

	private static ConcernCluster findContainingCluster(final Set<ConcernCluster> clusters, final String inEntity) {
		for (final ConcernCluster cluster : clusters) {
			for (final String clusterEntity : cluster.getEntities()) {
				if (clusterEntity.equals(inEntity)) {
					return cluster;
				}
			}
		}
		return null;
	}

}
