package edu.usc.softarch.arcade.facts.driver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import edu.usc.softarch.arcade.util.FileUtil;

public class CleanPackageNameInRsfDepFacts {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final File depsFile = FileUtil.checkFile(args[0], false, false);
		final File cleanDepsFile = FileUtil.checkFile(args[1], false, false);
		final String stripBeforePackageName = args[2];

		RsfReader.loadRsfDataFromFile(depsFile);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		try {
			final FileWriter out = new FileWriter(cleanDepsFile);
			for (final List<String> fact : depFacts) {
				final String rel = fact.get(0);
				final String source = fact.get(1);
				final String target = fact.get(2);
				String cleanSource = source;
				String cleanTarget = target;

				if (source.contains(stripBeforePackageName)) {
					cleanSource = source.substring(source.indexOf(stripBeforePackageName), source.length());
				}
				if (target.contains(stripBeforePackageName)) {
					cleanTarget = target.substring(target.indexOf(stripBeforePackageName), target.length());
				}

				out.write(rel + " " + cleanSource + " " + cleanTarget + "\n");

			}

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
