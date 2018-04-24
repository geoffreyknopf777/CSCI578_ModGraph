package edu.usc.softarch.arcade.facts.driver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import edu.usc.softarch.arcade.util.FileUtil;

public class DepsRsfToBunchMdg {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final File inDepsFilename = FileUtil.checkFile(args[0], false, false);
		final File outBunchDepsFile = FileUtil.checkFile(args[1], false, false);

		RsfReader.loadRsfDataFromFile(inDepsFilename);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		try {
			final FileWriter out = new FileWriter(outBunchDepsFile);

			// Set<Pair<String,String>> mdgPairs = new
			// HashSet<Pair<String,String>>();
			for (final List<String> depFact : depFacts) {
				// String relType = depFact.get(0);
				final String source = depFact.get(1);
				final String target = depFact.get(2);
				// System.out.println(relType + " " + source + " " + target);

				out.write(source + " " + target + "\n");

			}

			out.close();

		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
