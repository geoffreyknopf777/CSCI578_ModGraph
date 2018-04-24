package edu.usc.softarch.arcade.util.convert;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.MetricsDriver;
import edu.usc.softarch.arcade.facts.driver.ODEMReader;
import edu.usc.softarch.extractors.cda.odem.DependsOn;
import edu.usc.softarch.extractors.cda.odem.Type;

public class OdemToRsfConverter {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(OdemToRsfConverter.class);

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		String odemFileStr = "/home/joshua/cda/hadoop-0.19.odem";
		String rsfFileStr = "/home/joshua/workspace/MyExtractors/data/hadoop-0.19/hadoop-0.19-odem-facts.rsf";

		final Options options = new Options();

		final Option help = new Option("help", "print this message");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("ODEM file to be converted");
		final Option odemFile = OptionBuilder.create("odemFile");
		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("resulting RSF file");
		final Option rsfFileOption = OptionBuilder.create("rsfFile");

		options.addOption(help);
		options.addOption(odemFile);
		options.addOption(rsfFileOption);

		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(MetricsDriver.class.getName(), options);
				System.exit(0);
			}
			if (line.hasOption("odemFile")) {
				odemFileStr = line.getOptionValue("odemFile");
			}
			if (line.hasOption("odemFile")) {
				rsfFileStr = line.getOptionValue("rsfFile");
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		ODEMReader.setTypesFromODEMFile(odemFileStr);
		final List<Type> allTypes = ODEMReader.getAllTypes();
		final HashMap<String, Type> typeMap = new HashMap<String, Type>();
		for (final Type t : allTypes) {
			typeMap.put(t.getName().trim(), t);
		}

		final String convertMsg = "Writing dependencies from ODEM file to RSF file...";
		System.out.println(convertMsg);
		logger.debug(convertMsg);
		try {
			final File rsfFile = new File(rsfFileStr);
			if (!rsfFile.getParentFile().exists()) {
				rsfFile.getParentFile().mkdirs();
			}
			final FileWriter fw = new FileWriter(rsfFileStr);
			final BufferedWriter out = new BufferedWriter(fw);
			for (final String typeKey : typeMap.keySet()) {
				final Type t = typeMap.get(typeKey);
				for (final DependsOn dependency : t.getDependencies().getDependsOn()) {
					final String rsfLine = dependency.getClassification() + " " + t.getName() + " " + dependency.getName();
					logger.debug(rsfLine);
					out.write(rsfLine + "\n");
				}
			}
			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
