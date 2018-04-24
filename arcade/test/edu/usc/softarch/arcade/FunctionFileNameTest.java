package edu.usc.softarch.arcade;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.logging.log4j.Logger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.RsfReader;

public class FunctionFileNameTest {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(FunctionFileNameTest.class);

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		final String projFile = "cfg/bash_concerns.cfg";
		Config.setProjConfigFile(projFile);
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		logger.debug("Logging from " + this.getClass());
		Config.initConfigFromFile(Config.getProjConfigFile());

		RsfReader.loadRsfDataForCurrProj();
		final String funcFileLocation = "/home/joshua/workspace/cpp_func_separator/output";
		final File funcFileDir = new File(funcFileLocation);

		final FileFilter funcFileFilter = file -> file.getName().endsWith(
				".func");

		logger.debug("Removing quotations from filtered routines...");
		final List<String> quotelessFilteredRoutines = new ArrayList<String>();
		for (String routine : RsfReader.filteredRoutines) {
			logger.debug("\t original routine: " + routine);
			routine = routine.replaceAll("\"", "");
			quotelessFilteredRoutines.add(routine);
			logger.debug("\t resulting routine: " + routine);
		}

		/*
		 * logger.debug("Checking if RsfReader.filteredRoutines has changed...");
		 * for (String routine : RsfReader.filteredRoutines) {
		 * logger.debug("\t current routine: " + routine); }
		 */

		final HashSet<String> quotelessFilteredRoutinesSet = Sets
				.newHashSet(quotelessFilteredRoutines);

		logger.debug("Processing func files...");
		for (final File funcFile : funcFileDir.listFiles(funcFileFilter)) {
			final String funcFileName = funcFile.getName();
			logger.debug(funcFileName);
			final String[] tokens = funcFileName.split("#");

			final int firstOccurrenceOfDotInFilename = funcFileName
					.indexOf(".func");
			final String filenamePrefix = funcFileName.substring(0,
					firstOccurrenceOfDotInFilename);

			final String funcNameAndExtension = tokens[2];
			logger.debug("function name and extension: " + funcNameAndExtension);
			final int firstOccurrenceOfDot = funcNameAndExtension.indexOf(".");
			final String functionNameOnly = funcNameAndExtension.substring(0,
					firstOccurrenceOfDot);
			if (firstOccurrenceOfDot > 0) {
				logger.debug("\tfunction name only: " + functionNameOnly);
			} else {
				logger.error("\tinvalid function name and extension: "
						+ funcNameAndExtension);
			}

			if (!quotelessFilteredRoutinesSet.contains(filenamePrefix)) {
				logger.error("\tfiltered routines does not filtered routine: "
						+ filenamePrefix);
			}
		}
	}

}
