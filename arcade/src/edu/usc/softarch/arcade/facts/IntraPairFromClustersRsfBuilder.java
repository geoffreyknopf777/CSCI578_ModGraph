package edu.usc.softarch.arcade.facts;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.facts.driver.RsfReader;

public class IntraPairFromClustersRsfBuilder {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(IntraPairFromClustersRsfBuilder.class);

	public static HashSet<HashSet<String>> buildIntraPairsFromClustersRsf(final File rsfFile) {
		final List<List<String>> facts = RsfReader.extractFactsFromRSF(rsfFile);
		final TreeMap<String, HashSet<String>> clusterMap = buildClusterMapFromRsfFile(facts);
		final TreeMap<String, HashSet<HashSet<String>>> clusterIntraPairsMap = buildClusterIntraPairsMap(clusterMap);
		final HashSet<HashSet<String>> allIntraPairs = buildAllIntraPairs(clusterIntraPairsMap);

		return allIntraPairs;
	}

	private static HashSet<HashSet<String>> buildAllIntraPairs(final TreeMap<String, HashSet<HashSet<String>>> clusterIntraPairsMap) {
		final HashSet<HashSet<String>> allIntraPairs = new HashSet<HashSet<String>>();
		for (final String clusterNumber : clusterIntraPairsMap.keySet()) {
			for (final HashSet<String> intraPair : clusterIntraPairsMap.get(clusterNumber)) {
				allIntraPairs.add(intraPair);
			}
		}
		return allIntraPairs;
	}

	private static TreeMap<String, HashSet<HashSet<String>>> buildClusterIntraPairsMap(final TreeMap<String, HashSet<String>> clusterMap) {
		final TreeMap<String, HashSet<HashSet<String>>> clusterIntraPairsMap = new TreeMap<String, HashSet<HashSet<String>>>();
		for (final String currClusterNumber : clusterMap.keySet()) {
			final HashSet<String> elements = clusterMap.get(currClusterNumber);

			for (final String element1 : elements) {
				for (final String element2 : elements) {
					/*
					 * if(element1.equals(element2)) { continue; }
					 */
					if (clusterIntraPairsMap.containsKey(currClusterNumber)) {
						final HashSet<HashSet<String>> intraPairs = clusterIntraPairsMap.get(currClusterNumber);
						final HashSet<String> intraPair = new HashSet<String>();
						intraPair.add(element1);
						intraPair.add(element2);
						// DebugUtil.checkIntraPairSize(intraPair,element1,element2);
						intraPairs.add(intraPair);
					} else {
						final HashSet<HashSet<String>> intraPairs = new HashSet<HashSet<String>>();
						final HashSet<String> intraPair = new HashSet<String>();
						intraPair.add(element1);
						intraPair.add(element2);
						intraPairs.add(intraPair);
						// DebugUtil.checkIntraPairSize(intraPair,element1,element2);
						clusterIntraPairsMap.put(currClusterNumber, intraPairs);
					}

				}
			}
		}

		logger.debug("Printing intrapairs for clusters from rsf file...");
		for (final String currClusterNumber : clusterMap.keySet()) {
			final HashSet<HashSet<String>> intraPairs = clusterIntraPairsMap.get(currClusterNumber);
			logger.debug("Intrapairs for cluster number: " + currClusterNumber);
			for (final HashSet<String> intraPair : intraPairs) {
				logger.debug("\t" + intraPair);
			}
		}

		return clusterIntraPairsMap;
	}

	private static TreeMap<String, HashSet<String>> buildClusterMapFromRsfFile(final List<List<String>> facts) {
		final TreeMap<String, HashSet<String>> clusterMap = new TreeMap<String, HashSet<String>>();

		for (final List<String> fact : facts) {
			final String currClusterId = fact.get(1);
			final String entity = fact.get(2);
			if (clusterMap.containsKey(currClusterId)) {
				final HashSet<String> elements = clusterMap.get(currClusterId);
				elements.add(entity);
			} else {
				final HashSet<String> elements = new HashSet<String>();
				elements.add(entity);
				clusterMap.put(currClusterId, elements);
			}
		}

		logger.debug("Printing clusters obtained from clusters rsf file...");
		for (final String currClusterNumber : clusterMap.keySet()) {
			final HashSet<String> elements = clusterMap.get(currClusterNumber);
			logger.debug("Current cluster number: " + currClusterNumber);
			for (final String element : elements) {
				logger.debug("\t" + element);
			}
		}
		return clusterMap;
	}
}
