package edu.usc.softarch.arcade.antipattern.detection.interfacebased;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

import edu.usc.softarch.arcade.util.RecoveryParams;

public class BatchDepFinder {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(BatchDepFinder.class);
	static String DepExt = "C:\\DependencyFinder-1.2.1-beta4\\bin";
	//static String DepExt  		= System.getProperty("user.dir") + File.separator+ "DependencyFinder" + File.separator + "bin";
//	static String testInput 	= "F:\\continuum\\continuum_binary_2";
//	static String testOutput 	= "F:\\continuum\\DepFinder";
//	static String testClass		= "apps\\continuum\\WEB-INF\\classes";
//	static String testClass		= "apps";

//	nutch
	static String testInput 	= "F:\\nutch\\bin";
	static String testOutput 	= "F:\\nutch\\DepFinders";
	static String testClass		= "plugins";
	
//	cxf
//	static String testInput 	= "F:\\cxf_data\\cxf_binary";
//	static String testOutput 	= "F:\\cxf_data\\DepFinder";
//	static String testClass		= "lib";

//	static String testInput 	= "F:\\wicket_data\\src_3";
//	static String testOutput 	= "F:\\wicket_data\\extra_info\\DepFinders";
//	static String testClass		= "lib";

//	Camel
//	static String testInput 	= "F:\\camel_data\\New Folder";
//	static String testOutput 	= "F:\\camel_data\\extra_info\\";
//	static String testClass		= "lib";
	
//	Wicket
//	static String testInput 	= "F:\\wicket_data\\src_2";
//	static String testOutput 	= "F:\\wicket_data\\extra_info\\DepFinders";
//	static String testClass		= "";
	
	static String[] args	= new String[]{testInput, testOutput, testClass};
	
	public static void main(final String[] args1) throws IOException {
//		if (args.length < 3 || args.l ength > 4) {
//			System.out.println("Usage: InterfaceBasedSmellDetection <inputDirName> <outputDirName> <classesDir> [language]");
//			System.exit(-1);
//		}
		logger.info(System.getProperty("user.dir"));
		final RecoveryParams recParms = new RecoveryParams(args);
		final File[] files = recParms.getInputDir().listFiles();
		final Set<File> fileSet = new TreeSet<File>(Arrays.asList(files));
		logger.debug("All files in " + recParms.getInputDir() + ":");
		logger.debug(Joiner.on("\n").join(fileSet));
		boolean nothingtodo = true;
		for (final File versionFolder : fileSet) {
			if (versionFolder.isDirectory()) {
				logger.debug("Identified directory: " + versionFolder.getName());
				nothingtodo = false;
				single(versionFolder, recParms.getOutputDir(), recParms.getClassesDirName(), recParms.getLanguage());
			} else {
				logger.debug("Not a directory: " + versionFolder.getName());
			}
		}
		if (nothingtodo) {
			System.out.println("Nothing to do!");
		}
	}

	public static void single(final File versionFolder, final File outputDir, final String classesDirName, final String language) throws IOException {
		logger.debug("Processing directory: " + versionFolder.getName());
		// the revision number is really just the name of the subdirectory
		final String revisionNumber = versionFolder.getName();
		// depsRsfFilename is the file name of the dependencies rsf file (one is
		// created per subdirectory of dir)
		final String depsXMLFilename = outputDir.getAbsolutePath() + File.separatorChar + revisionNumber + "_deps.xml";
		final File depsXMLFile = new File(depsXMLFilename);
		if (!depsXMLFile.getParentFile().exists()) {
			depsXMLFile.getParentFile().mkdirs();
		}
		// classesDir is the directory in each subdirectory of the dir directory
		// that contains the compiled classes of the subdirectory

		final String absoluteClassesDir = versionFolder.getAbsolutePath() + File.separatorChar + classesDirName;
		final File classesDirFile = new File(absoluteClassesDir);
		if (!classesDirFile.exists()) {
			return;
		}

		logger.debug("Get deps for revision " + revisionNumber);

		// Run deps for a single folder
		Process p =  Runtime.getRuntime().exec("cmd /c DependencyExtractor.bat -xml -out "+ depsXMLFilename + " "
				+ versionFolder + File.separator + classesDirName, 
				null, new File(DepExt));
	}
}
