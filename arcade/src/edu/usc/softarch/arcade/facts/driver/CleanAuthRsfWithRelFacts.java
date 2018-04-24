package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import edu.usc.softarch.arcade.util.FileUtil;

/**
 * @author joshua
 *
 */
public class CleanAuthRsfWithRelFacts {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(CleanAuthRsfWithRelFacts.class);

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final File authRsfFile = FileUtil.checkFile(args[0], false, false);
		final File depsRsfFile = FileUtil.checkFile(args[1], false, false);
		final File outputFile = FileUtil.checkFile(args[2], false, false);

		RsfReader.loadRsfDataFromFile(authRsfFile);
		final List<List<String>> authFacts = Lists.newArrayList(RsfReader.filteredRoutineFacts);
		System.out.println("number of facts in flat authoritative recovery: " + authFacts.size());

		RsfReader.loadRsfDataFromFile(depsRsfFile);
		final Set<String> allRelFactNodesSet = new HashSet<String>(RsfReader.allNodesSet);

		final List<List<String>> factsToRemove = new ArrayList<List<String>>();

		for (final List<String> fact : authFacts) { // for each fact in the flat
			// authoritative recovery
			final String target = fact.get(2);
			if (!allRelFactNodesSet.contains(target)) { // if the target of the
				// fact is not in the
				// relation facts nodes
				// set
				factsToRemove.add(fact); // mark the fact for removal
			}

		}
		System.out.println("number of facts to remove: " + factsToRemove.size());
		logger.debug("Facts to remove:");
		logger.debug(Joiner.on("\n").join(factsToRemove));

		final Set<List<String>> cleanAuthFacts = Sets.newHashSet(authFacts);
		for (final List<String> factToRemove : factsToRemove) {
			cleanAuthFacts.remove(factToRemove);
		}
		System.out.println("number of facts in clean flat authoritative recovery: " + cleanAuthFacts.size());

		// String extension =
		// authRsfFilename.substring(authRsfFilename.lastIndexOf("."),authRsfFilename.length());
		// String prefix =
		// authRsfFilename.substring(0,authRsfFilename.lastIndexOf("."));

		// String outputFilename = prefix + ".clean" + extension;

		try {
			final FileWriter fw = new FileWriter(outputFile);
			final BufferedWriter out = new BufferedWriter(fw);
			System.out.println("Writing to file " + outputFile + "...");
			for (final List<String> fact : cleanAuthFacts) {
				out.write(fact.get(0) + " " + fact.get(1) + " " + fact.get(2) + "\n");
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
