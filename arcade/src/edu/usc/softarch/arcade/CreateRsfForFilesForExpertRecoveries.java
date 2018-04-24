package edu.usc.softarch.arcade;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Logger;


import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.ExpertDecompositionBuilder;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.util.FileUtil;

public class CreateRsfForFilesForExpertRecoveries {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(CreateRsfForFilesForExpertRecoveries.class);

	public static void main(final String[] args) {
		final Options options = new Options();

		final Option help = new Option("help", "print this message");

		OptionBuilder.withArgName("file");
		OptionBuilder.hasArg();
		OptionBuilder.withDescription("project configuration file");
		final Option projFile = OptionBuilder.create("projfile");

		options.addOption(help);
		options.addOption(projFile);

		// create the parser
		final CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			final CommandLine line = parser.parse(options, args);

			if (line.hasOption("projfile")) {
				Config.setProjConfigFile(FileUtil.checkFile(line.getOptionValue("projfile"), false, false));
			}
			if (line.hasOption("help")) {
				// automatically generate the help statement
				final HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(CreateRsfForFilesForExpertRecoveries.class.getName(), options);
				System.exit(0);
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		logger.debug("Running from " + CreateRsfForFilesForExpertRecoveries.class.getName());
		Config.initConfigFromFile(Config.getProjConfigFile());
		System.out.println("Creating expert decomposition...");
		System.out.println("Reading in rsf file...");
		RsfReader.loadRsfDataForCurrProj();
		// RsfReader.performPreClusteringTasks();
		ExpertDecompositionBuilder.readInExpertDecomposition("/home/joshua/recovery/Expert Decompositions/Bash expert decompositions.txt");
		ExpertDecompositionBuilder.buildExpertDecompositionClusters(RsfReader.startNodesSet);
		ExpertDecompositionBuilder.findMissingElementsInExpertDecomposition();
		ExpertDecompositionBuilder.buildMojoTargetFilesForFiles();
		// ExpertDecompositionBuilder.buildDocTopicsForClusters();
	}

}
