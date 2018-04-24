package edu.usc.softarch.arcade.facts;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.util.FileUtil;

public class SymmetricRsfFactsTransformer {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(SymmetricRsfFactsTransformer.class);

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String filename = "/home/joshua/recovery/RSFs/linuxRel.rsf";
		RsfReader.loadRsfDataFromFile(FileUtil.checkFile(filename, false, false));

		final List<List<String>> symmetricFacts = Lists.newArrayList(RsfReader.filteredRoutineFacts);

		for (final List<String> fact : RsfReader.filteredRoutineFacts) {
			final List<String> symmetricFact = new ArrayList<String>();
			final String type = fact.get(0);
			final String source = fact.get(1);
			final String target = fact.get(2);
			symmetricFact.add(type);
			symmetricFact.add(target);
			symmetricFact.add(source);
			symmetricFacts.add(symmetricFact);
		}

		final String extension = filename.substring(filename.lastIndexOf("."), filename.length());
		final String prefix = filename.substring(0, filename.lastIndexOf("."));

		final String outputFilename = prefix + ".symmetric" + extension;

		try {
			final FileWriter fw = new FileWriter(outputFilename);
			final BufferedWriter out = new BufferedWriter(fw);
			for (final List<String> fact : symmetricFacts) {
				out.write(fact.get(0) + " " + fact.get(1) + " " + fact.get(2) + "\n");
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
