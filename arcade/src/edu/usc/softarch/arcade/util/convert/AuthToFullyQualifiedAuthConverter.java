package edu.usc.softarch.arcade.util.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.util.FileUtil;

public class AuthToFullyQualifiedAuthConverter {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		final File clustersFile = FileUtil.checkFile(args[0], false, false);
		final File depsRsfFile = FileUtil.checkFile(args[1], false, false);
		final String fullyQualifiedGroundTruthFilename = args[2];

		RsfReader.loadRsfDataFromFile(depsRsfFile);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile(clustersFile);
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;

		final Map<String, Set<String>> clusterMap = new HashMap<String, Set<String>>();

		for (final List<String> fact : clusterFacts) {
			final String clusterName = fact.get(1);
			final String entity = fact.get(2);
			if (clusterMap.get(clusterName) == null) {
				final Set<String> entities = new HashSet<String>();
				entities.add(entity);
				clusterMap.put(clusterName, entities);
			} else {
				final Set<String> entities = clusterMap.get(clusterName);
				entities.add(entity);
				clusterMap.put(clusterName, entities);
			}
		}

		// maps an entity to all of the possible classes it may match
		final Map<String, Set<String>> matchingClassesMap = new HashMap<String, Set<String>>();

		findMatchingClasses(depFacts, clusterMap, matchingClassesMap);

		System.out.println(Joiner.on("\n").withKeyValueSeparator(":").join(matchingClassesMap));

		final Set<String> unmatchedEntities = new HashSet<String>();

		for (final String clusterName : clusterMap.keySet()) {
			for (final String entity : clusterMap.get(clusterName)) {
				if (matchingClassesMap.get(entity) == null) {
					unmatchedEntities.add(entity);
				}
			}
		}
		System.out.println("List of entities not a source or target:");
		System.out.println(Joiner.on("\n").join(unmatchedEntities));

		try {
			final FileWriter out = new FileWriter(fullyQualifiedGroundTruthFilename);
			for (final String clusterName : clusterMap.keySet()) {
				for (final String entity : clusterMap.get(clusterName)) {
					for (final String matchingClass : matchingClassesMap.get(entity)) {
						out.write("contain " + clusterName + " " + matchingClass + "\n");
					}
				}
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	/**
	 *
	 * Fills {@code matchingClassesMap} with all the classes that the entity key
	 * may refer to
	 *
	 * @param depFacts
	 * @param clusterMap
	 * @param matchingClassesMap
	 */
	private static void findMatchingClasses(List<List<String>> depFacts, Map<String, Set<String>> clusterMap, Map<String, Set<String>> matchingClassesMap) {
		for (final String clusterName : clusterMap.keySet()) {
			for (final String entity : clusterMap.get(clusterName)) {
				for (final List<String> depFact : depFacts) {
					final String source = depFact.get(1).trim();
					final String target = depFact.get(2).trim();
					final String sourceClassNameOnly = source.substring(source.lastIndexOf(".") + 1).split("\\$")[0].trim();
					final String targetClassNameOnly = target.substring(target.lastIndexOf(".") + 1).split("\\$")[0].trim();

					if (entity.trim().equals(sourceClassNameOnly) || entity.trim().equals(targetClassNameOnly)) {
						String matchingClass = null;
						if (entity.trim().equals(sourceClassNameOnly)) {
							matchingClass = source;
						} else {
							matchingClass = target;
						}

						if (matchingClassesMap.get(entity) == null) {
							final Set<String> classes = new HashSet<String>();
							classes.add(matchingClass);
							matchingClassesMap.put(entity.trim(), classes);
						} else {
							final Set<String> classes = matchingClassesMap.get(entity);
							classes.add(matchingClass);
							matchingClassesMap.put(entity.trim(), classes);
						}

					}
				}
			}
		}
	}

}
