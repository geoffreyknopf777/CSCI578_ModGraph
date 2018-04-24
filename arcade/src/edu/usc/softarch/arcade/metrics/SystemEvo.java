package edu.usc.softarch.arcade.metrics;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.driver.ConcernClusterRsf;
import edu.usc.softarch.arcade.util.FileUtil;

public class SystemEvo {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(SystemEvo.class);

	public static double sysEvo = 0;

	public static void main(final String[] args) {
		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		sysEvo = 0;
		final SystemEvoOptions options = new SystemEvoOptions();
		final JCommander jcmd = new JCommander(options);

		try {
			jcmd.parse(args);
		} catch (final ParameterException e) {
			logger.trace(e.getMessage());
			jcmd.usage();
			System.exit(1);
		}

		logger.trace(options.parameters);
		logger.trace("\n");

		final File sourceRsfFile = FileUtil.checkFile(options.parameters.get(0), false, false);
		final File targetRsfFile = FileUtil.checkFile(options.parameters.get(1), false, false);

		final Set<ConcernCluster> sourceClusters = ConcernClusterRsf.extractConcernClustersFromRsfFile(sourceRsfFile);
		final Set<ConcernCluster> targetClusters = ConcernClusterRsf.extractConcernClustersFromRsfFile(targetRsfFile);

		logger.trace("Source clusters: ");
		logger.trace(clustersToString(sourceClusters));
		logger.trace("Target clusters: ");
		logger.trace(clustersToString(targetClusters));

		final double numClustersToRemove = sourceClusters.size() > targetClusters.size() ? sourceClusters.size() - targetClusters.size() : 0;
		final double numClustersToAdd = targetClusters.size() > sourceClusters.size() ? targetClusters.size() - sourceClusters.size() : 0;

		logger.trace("\n");
		logger.trace("number of clusters to remove: " + numClustersToRemove);
		logger.trace("number of clusters to add: " + numClustersToAdd);

		final Set<String> sourceEntities = getAllEntitiesInClusters(sourceClusters);
		final Set<String> targetEntities = getAllEntitiesInClusters(targetClusters);

		logger.trace("\n");
		logger.trace("source entities: " + sourceEntities);
		logger.trace("target entities: " + targetEntities);

		final Set<String> entitiesToRemove = new HashSet<>(sourceEntities);
		entitiesToRemove.removeAll(targetEntities);
		final Set<String> entitiesToAdd = new HashSet<>(targetEntities);
		entitiesToAdd.removeAll(sourceEntities);

		logger.trace("\n");
		logger.trace("entities to remove: " + entitiesToRemove);
		logger.trace("entities to add: " + entitiesToAdd);

		// We need to determine the intersection of entities between clusters in
		// the source and target to help us minimize the number of moves
		// Pooyan!: We are maping this problem to a problem of Maximum Weighted
		// Matching, and we use Hungurian Algorithm to solve it.

		final int ns = sourceClusters.size();
		final int nt = targetClusters.size();
		final Map<Integer, ConcernCluster> sourceNumToCluster = new HashMap<>(); // Pooyan!
		// It
		// maps
		// every
		// source_cluster
		// to
		// a
		// number
		// from
		// 0
		// to
		// ns-1
		final Map<ConcernCluster, Integer> sourceClusterToNum = new HashMap<>(); // Pooyan!
		// It
		// maps
		// every
		// target_cluster
		// to
		// a
		// number
		// from
		// 0
		// to
		// nm-1
		int counter = 0;
		for (final ConcernCluster source : sourceClusters) {
			sourceNumToCluster.put(counter, source);
			sourceClusterToNum.put(source, counter);
			counter++;
		}

		final Map<Integer, ConcernCluster> targetNumToCluster = new HashMap<>();
		final Map<ConcernCluster, Integer> targetClusterToNum = new HashMap<>();
		counter = 0;
		for (final ConcernCluster target : targetClusters) {
			targetNumToCluster.put(counter, target);
			targetClusterToNum.put(target, counter);
			counter++;
		}

		final MWBMatchingAlgorithm ma = new MWBMatchingAlgorithm(ns, nt);// Pooyan!
		// Initiating
		// the
		// mathching
		for (int i = 0; i < ns; i++) {
			for (int j = 0; j < nt; j++) {
				ma.setWeight(i, j, 0);
			}
		}

		for (final ConcernCluster sourceCluster : sourceClusters) {
			for (final ConcernCluster targetCluster : targetClusters) {
				final Set<String> entitiesIntersection = new HashSet<>(sourceCluster.getEntities());
				entitiesIntersection.retainAll(targetCluster.getEntities());
				ma.setWeight(sourceClusterToNum.get(sourceCluster), targetClusterToNum.get(targetCluster), entitiesIntersection.size()); // Pooyan
				// the
				// weight
				// of
				// (source,target) as
				// the interesection
				// between them
			}
		}

		final Map<ConcernCluster, Set<String>> sourceClusterMatchEntities = new HashMap<>(); // Pooyan!
		// It
		// keeps
		// the
		// source
		// Cluster
		// Match
		// Entities,
		// not
		// necessarily
		// the
		// max
		// match
		final Map<ConcernCluster, ConcernCluster> matchOfSourceInTarget = new HashMap<>();// Pooyan!
		// It
		// keeps
		// the
		// matched
		// cluster
		// in
		// target
		// for
		// every
		// source
		final Map<ConcernCluster, ConcernCluster> matchOfTargetInSource = new HashMap<>();// Pooyan!
		// It
		// keeps
		// the
		// matched
		// cluster
		// in
		// source
		// for
		// every
		// target

		final int[] match = ma.getMatching(); // Pooyan! calculates the max
		// weighted
		// match;

		for (int i = 0; i < match.length; i++) {

			final ConcernCluster source = sourceNumToCluster.get(i);
			ConcernCluster target = new ConcernCluster();
			target.setName("-1"); // Pooyan! dummy, in case that the cluster is
			// not matched to any cluster, to avoid null
			// pointer exceptions
			if (match[i] != -1) {
				target = targetNumToCluster.get(match[i]);
			}
			matchOfSourceInTarget.put(source, target); // Pooyan! set the match
			// of source
			matchOfTargetInSource.put(target, source); // Pooyan! set the match
			// of target
			final Set<String> entitiesIntersection = new HashSet<>(source.getEntities());
			entitiesIntersection.retainAll(target.getEntities());
			sourceClusterMatchEntities.put(source, entitiesIntersection);
			logger.trace("Pooyan -> " + source.getName() + " is matched to " + target.getName() + " - the interesection size is " + entitiesIntersection.size());
		}

		logger.trace("\n");
		logger.trace("Pooyan -> cluster -> intersecting entities in the matched source clusters");
		logger.trace(Joiner.on("\n").withKeyValueSeparator("->").join(sourceClusterMatchEntities));
		logger.trace("Pooyan -> cluster -> matched clusters in target cluster for every source cluster");
		logger.trace(Joiner.on("\n").withKeyValueSeparator("->").useForNull("null").join(matchOfSourceInTarget));

		// int sourceClusterRemovalCount = 0;
		final Set<ConcernCluster> removedSourceClusters = new HashSet<>();

		// Pooyan! unmatched clusters must be removed
		for (final ConcernCluster source : sourceClusters) {
			final ConcernCluster matched = matchOfSourceInTarget.get(source);
			if (matched.getName().equals("-1")) {
				// sourceClusterRemovalCount++;
				removedSourceClusters.add(source);
			}
		}
		logger.trace("Pooyan -> Removed source clusters:");
		logger.trace(Joiner.on(",").join(removedSourceClusters));

		final Set<String> entitiesToMoveInRemovedSourceClusters = new HashSet<>(); // Pooyan!
		// These
		// are
		// the
		// entities
		// in
		// the
		// removed
		// source
		// clusters
		// which
		// exists
		// in
		// the
		// target
		// clusters
		// and
		// have
		// to
		// be
		// moved
		logger.trace("Entities of removed clusters:");
		for (final ConcernCluster source : removedSourceClusters) {
			final Set<String> entities = source.getEntities();
			entities.removeAll(entitiesToRemove); // Make sure we are not trying
			// to move entities that no
			// longer exist in the
			// target cluster
			logger.trace("Pooyan -> these in enitities in: " + source.getName() + " will be moved: " + entities);
			entitiesToMoveInRemovedSourceClusters.addAll(entities);
		}

		// The clusters that remain after removal of clusters
		final Set<ConcernCluster> remainingSourceClusters = new HashSet<>(sourceClusters);
		remainingSourceClusters.removeAll(removedSourceClusters);

		// for each cluster, the map gives the set of entities that may be moved
		// (not including added or
		// removed entities)
		final Map<ConcernCluster, Set<String>> entitiesToMoveInCluster = new HashMap<>();
		for (final ConcernCluster remainingCluster : remainingSourceClusters) {
			final Set<String> matchedIntersectionEntities = sourceClusterMatchEntities.get(remainingCluster);
			final Set<String> currEntitiesToMove = new HashSet<>(remainingCluster.getEntities());
			if (matchOfSourceInTarget.get(remainingCluster) != null && matchOfTargetInSource.get(matchOfSourceInTarget.get(remainingCluster)).equals(remainingCluster)) {
				currEntitiesToMove.removeAll(matchedIntersectionEntities); // the
				// problem
				// is
				// here!!!
				// It
				// should
				// move
				// the
				// maxIntersecting
				// Entities
				// since
				// the
				// cluster
				// in
				// the
				// other
				// arc
				// is
				// assigned
				// to
				// another
				// cluster
			} else {
				// logger.trace("Pooyan -> /*");
				// logger.trace("Pooyan -> remainingCluster: "+remainingCluster.getName());
				// logger.trace("Pooyan -> clusterToMaxIntersectingCluster.get(remainingCluster): "
				// +clusterToMaxIntersectingCluster.get(remainingCluster).getName());
				// logger.trace("Pooyan -> targetClusterMatchInSource.get(clusterToMaxIntersectingCluster.get(remainingCluster)): "+targetClusterMatchInSource.get(clusterToMaxIntersectingCluster.get(remainingCluster)));
			}

			currEntitiesToMove.removeAll(entitiesToAdd);
			currEntitiesToMove.removeAll(entitiesToRemove);
			entitiesToMoveInCluster.put(remainingCluster, currEntitiesToMove);
			for (final String e : currEntitiesToMove) {
				logger.trace("Pooyan -> remaining cluster " + remainingCluster.getName() + ", adn current entity to move :" + e);
			}
		}

		final Set<String> allEntitiesToMove = new HashSet<>();
		for (final Set<String> currEntitiesToMove : entitiesToMoveInCluster.values()) {
			allEntitiesToMove.addAll(currEntitiesToMove); // entities to move in
			// clusters not
			// removed
		}
		allEntitiesToMove.addAll(entitiesToMoveInRemovedSourceClusters); // entities
		// to
		// move
		// in
		// removed
		// clusters
		// (i.e.,
		// all
		// the
		// entities
		// in
		// those
		// clusters)
		allEntitiesToMove.addAll(entitiesToAdd);
		allEntitiesToMove.addAll(entitiesToRemove);

		for (final String e : allEntitiesToMove) {
			logger.trace("Pooyan -> enitity to be moved: " + e);
		}
		logger.trace("entities to move in each cluster: ");
		logger.trace(Joiner.on("\n").withKeyValueSeparator("->").join(entitiesToMoveInCluster));

		final int movesForAddedEntities = entitiesToAdd.size();
		final int movesForRemovedEntities = entitiesToRemove.size();

		logger.trace("\n");
		logger.trace("moves for added entities: " + movesForAddedEntities);
		logger.trace("moves for removed entities: " + movesForRemovedEntities);

		// Don't think I need this block for actual sysevo computation
		final Map<String, ConcernCluster> entityToTargetCluster = new HashMap<>();
		for (final ConcernCluster sourceCluster : sourceClusters) {
			final Set<String> sourceEntitiesToMove = new HashSet<>(sourceCluster.getEntities()); // entities
			// that
			// exist
			// already
			// and might be moved
			sourceEntitiesToMove.removeAll(entitiesToAdd); // so you need to
			// ignore added
			// entities
			sourceEntitiesToMove.removeAll(entitiesToRemove); // and removed
			// entities.
			for (final ConcernCluster targetCluster : targetClusters) {
				final Set<String> currTargetEntitites = targetCluster.getEntities();
				final Set<String> intersectingEntities = new HashSet<>(sourceEntitiesToMove); // entities
				// in
				// both
				// the
				// current
				// source and target cluster
				intersectingEntities.retainAll(currTargetEntitites);
				logger.trace("intersecting entities: ");
				logger.trace(intersectingEntities);
				for (final String entity : intersectingEntities) { // mark that
					// these
					// source
					// entities
					// belong to
					// this target
					// cluster
					entityToTargetCluster.put(entity, targetCluster);
				}
			}
		}

		logger.trace("Pooyan -> numClustersToRemove: " + numClustersToRemove);
		logger.trace("Pooyan -> numClustersToAdd: " + numClustersToAdd);
		logger.trace("Pooyan -> entitiesToRemove.size(): " + entitiesToRemove.size());
		logger.trace("Pooyan -> entitiesToAdd.size(): " + entitiesToAdd.size());
		logger.trace("Pooyan -> allEntitiesToMove.size(): " + allEntitiesToMove.size());
		logger.trace("Show which target cluster each entity (not added or removed) belongs to");

		logger.trace(Joiner.on("\n").withKeyValueSeparator("->").join(entityToTargetCluster));

		final double numer = numClustersToRemove + numClustersToAdd + entitiesToRemove.size() + entitiesToAdd.size() + allEntitiesToMove.size();
		final double denom = sourceClusters.size() + 2 * (double) sourceEntities.size() + targetClusters.size() + 2 * (double) targetEntities.size();
		logger.trace("Pooyan -> denum: " + denom);

		final double localSysEvo = (1 - numer / denom) * 100;

		logger.trace("sysevo: " + localSysEvo);

		sysEvo = localSysEvo;

	}

	// private static Map<ConcernCluster, Set<String>>
	// entriesSortedByEntitiesSize(
	// Map<ConcernCluster, Set<String>> map) {
	// List<Entry<ConcernCluster, Set<String>>> list = new
	// ArrayList<Entry<ConcernCluster, Set<String>>>(
	// map.entrySet());
	//
	// // sort list based on comparator
	// Collections.sort(list, new Comparator() {
	// @Override
	// public int compare(Object o1, Object o2) {
	// // return ((Comparable) ((Map.Entry) (o1)).getValue())
	// // .compareTo(((Map.Entry) (o2)).getValue());
	// Entry<ConcernCluster, Set<String>> e1 = (Entry<ConcernCluster,
	// Set<String>>) o1;
	// Entry<ConcernCluster, Set<String>> e2 = (Entry<ConcernCluster,
	// Set<String>>) o2;
	// return e1.getValue().size() - e2.getValue().size(); // ascending
	// // order
	// }
	// });
	//
	// Map<ConcernCluster, Set<String>> sortedMap = new
	// LinkedHashMap<ConcernCluster, Set<String>>();
	// for (Entry<ConcernCluster, Set<String>> entry : list) {
	// sortedMap.put(entry.getKey(), entry.getValue());
	// }
	// return sortedMap;
	// }

	private static String clustersToString(final Set<ConcernCluster> sourceClusters) {
		String output = "";
		for (final ConcernCluster cluster : sourceClusters) {
			output += cluster.getName() + ": ";
			for (final String entity : cluster.getEntities()) {
				output += entity + " ";
			}
			output += "\n";
		}
		return output;
	}

	private static Set<String> getAllEntitiesInClusters(final Set<ConcernCluster> clusters) {
		final Set<String> entities = new HashSet<>();
		for (final ConcernCluster cluster : clusters) {
			entities.addAll(cluster.getEntities());
		}
		return entities;
	}

}