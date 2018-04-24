package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Joiner;

import cc.mallet.classify.Classifier;
import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.util.CodeCount;
import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.convert.RsfToDotConverter;

/**
 * @author daniellink
 *
 */
public class TopLevel {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(TopLevel.class);
	static Config currentConfig = Controller.currentView.getConfig();
	static Classifier relaxClassifier;
	static ArrayList<CodeEntity> entities;
	private static FileManager fManager;
	private static File outputDir;
	private static String classesDirName;
	private static File versionFolder;
	private static Thread depsBuilderThread;
	private static String revisionNumber;
	// private static ArrayList<CodeCount> codeCounts;
	// private static HashMap<String, CodeCount> codeCountsMap;

	public static Thread getDepsBuilderThread() {
		return depsBuilderThread;
	}

	public static void setDepsBuilderThread(final Thread depsBuilderThread) {
		TopLevel.depsBuilderThread = depsBuilderThread;
	}

	public static File getVersionFolder() {
		return versionFolder;
	}

	public static void setVersionFolder(final File versionFolder) {
		TopLevel.versionFolder = versionFolder;
	}

	private static long relaxStartTime, relaxFinishTime;

	public static Config getCurrentConfig() {
		return currentConfig;
	}

	public static void setCurrentConfig(final Config currentConfig) {
		TopLevel.currentConfig = currentConfig;
	}

	public static File getOutputDir() {
		return outputDir;
	}

	public static void setOutputDir(final File outputDir) {
		TopLevel.outputDir = outputDir;
	}

	public static String getClassesDirName() {
		return classesDirName;
	}

	public static void setClassesDirName(final String classesDirName) {
		TopLevel.classesDirName = classesDirName;
	}

	/**
	 * Run RELAX on one or more versions of a system
	 *
	 * @param inputDir
	 *            - Root directory of the folder containing the different
	 *            versions
	 * @param outputDir
	 *            - Directory where the recovery output files should go
	 * @param classesDirName
	 *            - Subdirectory in each version that contains the source code
	 *            hierarchy to be examined
	 * @param language
	 *            - Programming language to check
	 * @param classifierFileName
	 *            - The prepared MALLET classifier file
	 */
	public static void run(final File inputDir, final File outputDir, final String classesDirName) {
		logger.entry(inputDir, outputDir, classesDirName);
		relaxStartTime = System.currentTimeMillis();
		setOutputDir(outputDir);
		setClassesDirName(classesDirName);
		initializeClassifier();
		// final File[] inputFiles = inputDir.listFiles(FileUtil.javaFilter);
		final File[] inputDirectories = inputDir.listFiles(FileUtil.saneDirectoryFilter);
		// inputFiles = inputDir.listFiles(null);
		final Set<File> fileSet = new TreeSet<>(Arrays.asList(inputDirectories));
		logger.trace("All files in {} :", inputDir);
		logger.trace(Joiner.on("\n").join(fileSet));
		boolean nothingtodo = true;
		for (final File f : fileSet) {
			if (f.isDirectory()) {
				if (f.getAbsolutePath().endsWith(".UCC")) {
					continue;
				}
				versionFolder = f;
				logger.trace("Identified directory: " + versionFolder.getName());
				logger.trace(FileListing.printDirectoryTree(versionFolder));
				nothingtodo = false;
				final ArchitecturalView curView = new ArchitecturalView(versionFolder.getAbsolutePath());
				Controller.setCurrentView(curView);
				// final Runnable r = () -> {
				// final Runtime rt = Runtime.getRuntime();
				processOneVersion();
				// };
				// new Thread(r).start();
			} else {
				logger.trace("Not a directory: " + f.getName());
			}
		}
		if (nothingtodo) {
			logger.debug("Nothing to do!");
		}
		// printEntities();
		relaxFinishTime = System.currentTimeMillis();
		logger.debug("### RELAX recovery time in seconds = " + (relaxFinishTime - relaxStartTime) / 1000);
		logger.traceExit();
	}

	/**
	 * Run one version of a system through RELAX
	 *
	 * @param versionFolder
	 *            - Directory the version resides in
	 */
	private static void processOneVersion() {
		final CodeCount cc;
		logger.entry(versionFolder);
		initializeFileManager(versionFolder);

		logger.trace(fManager);
		fManager.writeManifestToFile(outputDir + File.separator + versionFolder.getName() + "_relax_manifest.txt");

		// Uncomment the next two lines to create different slices of source
		// code in order to test RELAX composition.
		// While more or less any number of slices can be created, the Java
		// Executor will reject new threads after 16 threads are submitted and
		// throw a showstopper exception,
		// so 7 or 8 slices is probably a reasonable upper limit.

		// fManager.distribute(8);
		// System.exit(0);
		CodeEntity.setEntityCount(0);
		CodeEntity.setNoMatchCount(0);
		Clustering.resetClusterList();
		entities = new ArrayList<>();
		revisionNumber = versionFolder.getName();
		logger.debug("Processing revision directory: " + revisionNumber);
		final File classesDir = FileUtil.checkDir(versionFolder.getAbsolutePath() + File.separatorChar + classesDirName, false, true);

		// final Config currentConfig = Controller.getCurrentView().getConfig();

		/**
		 * If configured to, run UCC on the selected classes directory using
		 * another thread
		 */
		cc = new CodeCount(classesDir, outputDir, versionFolder);
		// codeCounts.add(cc);
		// codeCountsMap.put(versionFolder.getAbsolutePath(), cc);

		final ExecutorService es = Executors.newSingleThreadExecutor();
		final Future<SourceToDepsBuilder> builderOfTheFuture = es.submit(new DepsRunner(revisionNumber, versionFolder, outputDir, classesDirName));

		// The C SourceToDepsBuilder has issues that need to be fixed - for now
		// let's just do this when we're looking at Java code

		// if (Config.getSelectedLanguage().equals(Language.java)) {
		// depsBuilderThread.start();
		// }
		// try {
		// t.join();
		// } catch (final InterruptedException e1) {
		// // TODO Auto-generated catch block
		// e1.printStackTrace();
		// }

		final String fullSrcDirName = versionFolder.getAbsolutePath() + File.separatorChar + classesDirName;
		final File fullSrcDir = FileUtil.checkDir(fullSrcDirName, false, true);
		final File classifierFile = FileUtil.checkFile(currentConfig.getRelaxClassifierFileName(), false, true);
		new Classifications(fullSrcDir, outputDir, classifierFile, fManager);

		final String clusterFileClassesContents = Clustering.clusterListToContainString(versionFolder.getPath(), true);
		final String clusterFileName = outputDir + File.separator + revisionNumber + "_relax_clusters.rsf";
		logger.debug("Cluster file name with class names = " + clusterFileName);
		try {
			FileUtils.writeStringToFile(FileUtil.checkFile(clusterFileName, false, false), clusterFileClassesContents);
		} catch (final IOException e) {
			e.printStackTrace();
			logger.debug("Unable to write to File " + clusterFileName);
		}

		final String clusterFileFileNamesContents = Clustering.clusterListToContainString(versionFolder.getPath(), false);
		final String clusterFilewithFilesName = outputDir + File.separator + revisionNumber + "_relax_clusters_fn.rsf";
		logger.debug("Cluster file name with absolute file names = " + clusterFilewithFilesName);
		try {
			FileUtils.writeStringToFile(FileUtil.checkFile(clusterFilewithFilesName, false, false), clusterFileFileNamesContents);
		} catch (final IOException e) {
			e.printStackTrace();
			logger.debug("Unable to write to File " + clusterFilewithFilesName);
		}

		final String dotFileName = FilenameUtils.removeExtension(clusterFileName) + ".dot";
		// RsfToDotConverter.runConversionRELAX(clusterFileName, dotFileName);

		RsfToDotConverter.runConversionRELAXBase(clusterFileName, dotFileName, false);
		RsfToDotConverter.runConversionRELAXBase(clusterFileName, dotFileName, true);

		FileUtil.checkFile(clusterFileName, false, false);

		// if (Config.getSelectedLanguage().equals(Language.java)) {
		// DepsMaker.getDepsRsfFilename();
		// }
		// Clustering.printAllClusters();
		Clustering.printClusterInfo();

		String entityInfo = "";
		for (final CodeEntity c : entities) {
			entityInfo += c.infoString();
		}
		// logger.debug(entityInfo);
		final String entityFileName = outputDir + File.separator + revisionNumber + "_relax_entities.txt";
		try {
			FileUtils.writeStringToFile(FileUtil.checkDir(entityFileName, false, false), entityInfo);
		} catch (final IOException e) {
			System.err.println("Unable to write entity file!");
			System.exit(-1);
		}

		final String directoryDotFileName = outputDir + File.separator + versionFolder.getName() + "_directories.dot";
		SourceToDepsBuilder builder = null;
		try {
			builder = builderOfTheFuture.get();
		} catch (final InterruptedException e1) {
			System.err.println("depsRunner was interrupted!");
			e1.printStackTrace();
		} catch (final ExecutionException e1) {
			System.err.println("depsRunner had an exceution exception!");
			e1.printStackTrace();
		}
		printBuilderEdges(builder);
		relaxFilesAddDeps(builder);
		relaxFilesGetSLOC(cc);
		listAllRelaxFilesWithDeps();
		fManager.writeRelaxFilesDot(new File(directoryDotFileName), builder.getEdges());
		final String directoryFileName = outputDir + File.separator + versionFolder.getName() + "_directories.pdf";
		final String[] dotLayoutArgs = { currentConfig.getDotLayoutCommandDir() + File.separator + "dot", currentConfig.getGraphParams(), directoryDotFileName, "-o", directoryFileName };

		String commandString = "";
		for (final String s : dotLayoutArgs) {
			commandString += " " + s;
		}

		try {
			Runtime.getRuntime().exec(commandString);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		writeLegend();

		// final Runnable dotGraphRunnable = () -> {
		// RunExtCommUtil.run(dotLayoutArgs);
		// };
		// final Thread directoryGraph = new Thread(dotGraphRunnable);
		// directoryGraph.start();
		// try {
		// directoryGraph.join();
		// } catch (final InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		listEntitiesShuffled();
		logger.traceExit();
	}

	private static void relaxFilesGetSLOC(final CodeCount cc) {
		System.out.println("Waiting for UCC thread to finish...");
		cc.blockTilFinished();
		System.out.println("Getting SLOC...");
		cc.readTotalOutfile();

		// CodeCountSingleFile.readUCCResultsFile(uccFile);
	}

	private static String printBuilderEdges(final SourceToDepsBuilder s) {
		String out = "### Set of edges: ###\n";
		for (final Pair<String, String> e : s.getEdges()) {
			out += e.getLeft() + " --> " + e.getRight() + "\n";
		}
		out += "### End of edges ###\n";
		return out;
	}

	private static String listAllRelaxFilesWithDeps() {
		String out = "### All files with outgoing dependencies\n";
		for (final RelaxFile rf : fManager.getRelaxFiles().values()) {
			final String cn = rf.getCanonicalName();
			if (cn == null) {
				continue;
			}
			out += cn + "(" + rf.getBestColor() + ")\n";
			for (final String dep : rf.getOutgoingDependencies()) {
				final RelaxFile depRF = RelaxFile.getCanonicalToRelaxFile().get(dep);
				String bestColor = "---";
				if (depRF != null) {
					bestColor = depRF.getBestColor();
				}
				out += "-->" + dep + "(" + bestColor + ")\n";
			}
		}
		out += "######";
		return out;
	}

	private static String relaxFilesAddDeps(final SourceToDepsBuilder b) {
		for (final Pair<String, String> p : b.getEdges()) {
			final String left = p.getLeft();
			final String right = p.getRight();
			final RelaxFile rfLeft = RelaxFile.getCanonicalToRelaxFile().get(left);
			final RelaxFile rfRight = RelaxFile.getCanonicalToRelaxFile().get(right);
			if (rfLeft != null) {
				rfLeft.getOutgoingDependencies().add(right);
			}
			if (rfRight != null) {
				rfRight.getIncomingDependencies().add(left);
			}
		}
		return "";
	}

	private static void writeLegend() {
		final String legendDotFileName = outputDir + File.separator + versionFolder.getName() + "_legend.dot";

		String out = "digraph  { \n" + "mindist=0;\n" + "ranksep=0;\n" + "nodesep=0;\n" +

				"node[shape=box,margin=\"0,0\",width=1, height=0.5];\n" + "edge [style=invis];\n" +

				"Legend;\n";

		final String[] classNames = CodeEntity.getClassNames().toArray(new String[0]);
		ColorManager.getColorlist();
		int colorIndex = 0;
		for (final String s : classNames) {
			out += s + "[fontcolor=\"" + ColorManager.getColorByIndex(colorIndex++) + "\"];\n";
		}

		// for (final String s : colorNames) {
		// out += s + "[fontcolor=" + s + "];\n";
		// }

		out += "Legend -> " + classNames[0] + ";\n";
		// out += "Legend -> " + colorNames.get(0) + ";\n";

		for (int i = 0; i < classNames.length - 1; i++) {
			out += classNames[i] + "->" + classNames[i + 1] + ";\n";
			// out += colorNames.get(i) + "->" + colorNames.get(i + 1) + ";\n";
		}
		//
		out += "edge [constraint=false];\n";
		// for (int i = 0; i < classNames.length; i++) {
		// out += classNames[i] + "->" + colorNames.get(i) + ";\n";
		// }
		out += "}";
		try {
			FileUtils.writeStringToFile(new File(legendDotFileName), out);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		final String legendFileName = outputDir + File.separator + versionFolder.getName() + "_legend.pdf";
		final String[] dotLegendLayoutArgs = { currentConfig.getDotLayoutCommandDir() + File.separator + "dot", currentConfig.getGraphParams(), legendDotFileName, "-o", legendFileName };

		String commandString = "";

		for (final String s : dotLegendLayoutArgs) {
			commandString += " " + s;
		}

		try {
			Runtime.getRuntime().exec(commandString);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static String listEntitiesShuffled() {
		final StringBuilder outBuilder = new StringBuilder();
		outBuilder.append("### Randomized list of file names of all entities for checking:\n");
		final ArrayList<CodeEntity> randomList = new ArrayList<>(entities);
		Collections.shuffle(randomList);
		for (final CodeEntity ce : randomList) {
			outBuilder.append(ce.getBestLabel() + "\t");
			outBuilder.append(ce.getRankinString() + "\t");
			outBuilder.append(ce.getPasteableFileName() + "\n");
		}
		return outBuilder.toString();
	}

	public static CodeEntity getEntityByName(final String name) {
		for (final CodeEntity e : entities) {
			if (e.getClassName().equals(name)) {
				return e;
			}
		}
		return null;
	}

	public static Classifier getRelaxClassifier() {
		return relaxClassifier;
	}

	public static void setRelaxClassifier(final Classifier relaxClassifier) {
		TopLevel.relaxClassifier = relaxClassifier;
	}

	public static ArrayList<CodeEntity> getEntities() {
		return entities;
	}

	public static void setEntities(final ArrayList<CodeEntity> entities) {
		TopLevel.entities = entities;
	}

	public static FileManager getfManager() {
		return fManager;
	}

	public static void setfManager(final FileManager fManager) {
		TopLevel.fManager = fManager;
	}

	private static void initializeFileManager(final File directory) {
		final String effectiveDirName = directory + File.separator + classesDirName;
		final File effectiveDir = FileUtil.checkDir(effectiveDirName, false, true);
		if (Config.getSelectedLanguage() == Language.java) {
			fManager = new FileManager(effectiveDir, new RegexFileFilter("([^\\s]+(\\.(?i)(java))$)"));// Searchfor
																										// class
																										// files,
																										// return
																										// Java
																										// files
		} else if (Config.getSelectedLanguage() == Language.c) {
			fManager = new FileManager(effectiveDir, new RegexFileFilter("^.*\\.(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)$"));// ("^.*\\.(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)$"));
		}
		fManager.populateRelaxFiles();
	}

	private static void initializeClassifier() {

	}

	public static void printEntities() {
		for (final CodeEntity c : entities) {
			logger.debug(c);
		}
	}
}
