package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

//import org.apache.log4j.Appender;
//import org.apache.log4j.FileAppender;
import org.apache.logging.log4j.Logger;
//import org.apache.logging.log4j.core.Appender;
//import org.apache.logging.log4j.core.appender.FileAppender;

public class MakeDepReader {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(MakeDepReader.class);

	public static void main(final String[] args) throws FileNotFoundException {
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		logger.debug("Running from " + MakeDepReader.class.getName());

		// I don't think this should be used - Daniel

		// Config.initConfigFromFile(Config.getProjConfigFile());

		// final Enumeration<?> enumeration = logger.getAllAppenders();
		// while (enumeration.hasMoreElements()) {
		// final Appender app = (Appender) enumeration.nextElement();
		// if (app instanceof FileAppender) {
		// // I'll dump the file names to the console for now
		// // ... you do what you like with them
		// System.out.println("Appended File=" + ((FileAppender)
		// app).getFile());
		// }
		// }

		// String filename =
		// "/home/joshua/recovery/subject_systems/linux/make.dep";
		final String filename = args[0];
		final Scanner scanner = new Scanner(new FileInputStream(filename));
		final Map<String, List<String>> depMap = new HashMap<String, List<String>>();
		String currDotCFile = "";

		while (scanner.hasNextLine()) {
			final String line = scanner.nextLine();
			final String[] tokens = line.split("\\s");

			for (final String token : tokens) {
				String trimmedToken = token.trim();

				if (trimmedToken.endsWith(".c") || trimmedToken.endsWith(".h")) {
					trimmedToken = trimmedToken.substring(0, trimmedToken.length());
					List<String> deps = null;
					if (depMap.containsKey(currDotCFile)) {
						deps = depMap.get(currDotCFile);
					} else {
						deps = new ArrayList<String>();
					}
					deps.add(trimmedToken);
					depMap.put(currDotCFile, deps);
				}
				if (trimmedToken.endsWith(".o:")) {
					trimmedToken = trimmedToken.substring(0, trimmedToken.length() - 1);
					currDotCFile = trimmedToken.replace(".o", ".c");
				}
				logger.debug(trimmedToken + "");

			}
			logger.debug("\n");
		}

		final Set<String> cFiles = depMap.keySet();
		for (final String cFile : cFiles) {
			logger.debug(cFile + " has dependencies to ");
			final List<String> deps = depMap.get(cFile);
			for (final String dep : deps) {
				logger.debug("\t" + dep);
			}
		}

		// String outRsfFile = "linux_facts.rsf";
		final String outRsfFile = args[1];
		try {
			final FileWriter fstream = new FileWriter(outRsfFile);
			final BufferedWriter out = new BufferedWriter(fstream);
			for (final String cFile : cFiles) {
				final List<String> deps = depMap.get(cFile);
				for (final String dep : deps) {
					out.write("depends " + cFile + " " + dep + "\n");
				}
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
