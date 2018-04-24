package edu.usc.softarch.arcade.facts.driver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

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
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.extractors.cda.odem.Container;
import edu.usc.softarch.extractors.cda.odem.Namespace;
import edu.usc.softarch.extractors.cda.odem.ODEM;
import edu.usc.softarch.extractors.cda.odem.Type;

public class ODEMReader {
	private static List<Type> allTypes = new ArrayList<Type>();

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ODEMReader.class);

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
				formatter.printHelp("ODEMReader", options);
				System.exit(0);
			}
		} catch (final ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
		}

		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		Config.initConfigFromFile(Config.getProjConfigFile());

		System.out.println("Reading in odem file " + Config.getOdemFile() + "...");

		setTypesFromODEMFile(Config.getOdemFile());

		// parseXmlFile(Config.getOdemFile());
	}

	public static void setTypesFromODEMFile(final String odemFile) {
		JAXBContext context;
		try {
			context = JAXBContext.newInstance(ODEM.class);
			final Unmarshaller u = context.createUnmarshaller();
			final ODEM odem = (ODEM) u.unmarshal(new File(odemFile));
			for (final Container container : odem.getContext().getContainer()) {
				for (final Namespace n : container.getNamespace()) {
					final List<Type> types = n.getType();
					allTypes.addAll(types);
				}
			}
			int typeCount = 0;
			for (final Type t : allTypes) {
				logger.debug(typeCount + ": " + t.getName());
				typeCount++;
			}
		} catch (final JAXBException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static List<Type> getAllTypes() {
		return allTypes;
	}

}
