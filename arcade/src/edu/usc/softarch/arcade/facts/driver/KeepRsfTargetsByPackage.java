package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;


import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

public class KeepRsfTargetsByPackage {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(KeepRsfTargetsByPackage.class);

	public static void main(String[] args) {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		String inputRsfFilename = args[0];
		String outputRsfFilename = args[1];
		String packageToKeep = args[2];

		RsfReader.loadRsfDataFromFile(inputRsfFilename);
		List<List<String>> inputFacts = Lists
				.newArrayList(RsfReader.filteredRoutineFacts);
		logger.debug("Input facts:");
		logger.debug(Joiner.on("\n").join(inputFacts));

		List<List<String>> filteredFacts = new ArrayList<List<String>>();

		for (List<String> fact : inputFacts) {
			String target = fact.get(2);
			if (target.startsWith(packageToKeep)) {
				filteredFacts.add(fact);
			}

		}

		try {
			FileWriter fw = new FileWriter(outputRsfFilename);
			BufferedWriter out = new BufferedWriter(fw);
			System.out.println("Writing to file " + outputRsfFilename + "...");
			for (List<String> fact : filteredFacts) {
				out.write(fact.get(0) + " " + fact.get(1) + " " + fact.get(2)
						+ "\n");
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();System.exit(-1);
		}

	}

}
