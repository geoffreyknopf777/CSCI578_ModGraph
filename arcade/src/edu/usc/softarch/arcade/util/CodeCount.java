package edu.usc.softarch.arcade.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.frontend.Controller;

public class CodeCount {

	static Config currentConfig = Controller.getCurrentView().getConfig();
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(CodeCount.class);

	File classesDir, outputDir, versionDir;
	String uccOutputDirName;
	Thread uccThread;
	boolean needToRun;

	static ArrayList<CodeCountEntity> entities = new ArrayList<>();
	static HashMap<String, CodeCountEntity> entitiesMap = new HashMap<>();

	public static ArrayList<CodeCountEntity> getEntities() {
		return entities;
	}

	public static void setEntities(final ArrayList<CodeCountEntity> entities) {
		CodeCount.entities = entities;
	}

	public static HashMap<String, CodeCountEntity> getEntitiesMap() {
		return entitiesMap;
	}

	public static void setEntitiesMap(final HashMap<String, CodeCountEntity> entitiesMap) {
		CodeCount.entitiesMap = entitiesMap;
	}

	public CodeCount(final File classesDir, final File outputDir, final File versionDir) {
		logger.entry(classesDir, outputDir, versionDir);
		this.classesDir = classesDir;
		this.outputDir = outputDir;
		this.versionDir = versionDir;
		uccOutputDirName = versionDir.getAbsolutePath() + ".UCC";
		if (currentConfig.isRunUCC()) {
			final String outputFilename = versionDir.getAbsolutePath() + ".UCC" + File.separatorChar + "TOTAL_outfile.txt";
			final Path outputFilePath = Paths.get(outputFilename);
			if (Files.exists(outputFilePath)) {
				System.out.println("UCC outfile already exists!");
				needToRun = false;
				return;
			}
			needToRun = true;
			final String[] UCC_args = { Config.getUccLoc(), "-threads", "128", "-nocomplex", "-nolinks", "-nodup", "-ascii", "-unified", "-outdir", uccOutputDirName, "-dir",
					classesDir.getAbsolutePath() };
			logger.trace("UCC args = " + UCC_args);
			final Path uccPath = Paths.get(Config.getUccLoc());
			if (!Files.isExecutable(uccPath)) {
				System.err.println("UCC binary is not executable, change permissions or deselect UCC");
				System.exit(-1);
			}
			final Runnable uccRunnable = () -> {
				RunExtCommUtil.run(UCC_args);
			};
			uccThread = new Thread(uccRunnable);
			uccThread.start();
		} else {
			logger.info("Not configured to run UCC");
		}
		logger.traceExit();
	}

	public void readTotalOutfile() {
		final String outputFilename = uccOutputDirName + File.separatorChar + "TOTAL_outfile.txt";
		final Path outputFilePath = Paths.get(outputFilename);
		if (Files.exists(outputFilePath)) {
			System.out.println("UCC outfile found!");
		} else {
			System.err.println("UCC outfile not found!");
			System.err.println("Location = " + outputFilePath);
			return;
		}
		Stream<String> uccStream;
		try {
			uccStream = Files.lines(outputFilePath);
			uccStream.forEach(s -> processOneLine(s));
		} catch (final IOException e) {
			System.out.println("Cannot read UCC outfile!");
			System.exit(-1);
		}
	}

	private void processOneLine(final String s) {
		final String st = s.trim();
		if (!st.contains("CODE") || !st.endsWith(".java")) {
			return;
		}
		final String sourceFileName = st.split("CODE")[1].trim();
		final Path sourceFile = Paths.get(sourceFileName);
		if (!Files.exists(sourceFile)) {
			return;
		}
		// System.out.println(st);
		final String[] chunks = st.split("\\|");
		final ArrayList<Integer> counts = new ArrayList<>();
		for (final String chunk : chunks) {
			// System.out.println(chunk);
			final String[] numbers = chunk.split("\\s+");
			for (final String n : numbers) {
				// System.out.println("n = " + n);
				try {
					final int number = Integer.parseInt(n.trim());
					counts.add(number);
				} catch (final NumberFormatException e) {
					continue;
				}
			}
		}
		// for (final int c : counts) {
		// System.out.print(c + " ");
		// }
		// System.out.println("\n---");
		final CodeCountEntity cce = new CodeCountEntity(counts.get(0), counts.get(1), counts.get(2), counts.get(3), counts.get(4), counts.get(5), counts.get(6), counts.get(7), counts.get(8), "CODE",
				sourceFileName);
		entities.add(cce);
		entitiesMap.put(sourceFileName, cce);
	}

	public void blockTilFinished() {
		if (!needToRun) {
			System.out.println("No need to run UCC");
			return;
		}
		try {
			System.out.println("UCC Thread info:");
			System.out.println(uccThread);
			uccThread.join();
		} catch (final InterruptedException e) {
			System.err.println("CodeCount thread interrupted - exiting");
			System.exit(-1);
		}
	}
}
