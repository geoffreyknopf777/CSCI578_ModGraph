package edu.usc.softarch.arcade.util.mallet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.CharSequenceReplace;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.PrintInputAndTarget;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceLowercase;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.pipe.iterator.FileIterator;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.relax.FileManager;
import edu.usc.softarch.arcade.topics.CamelCaseSeparatorPipe;
import edu.usc.softarch.arcade.topics.StemmerPipe;
import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;

public class Import {
	private static final Logger logger = LogManager.getLogger(Import.class);
	private static Pattern cLangPattern = Pattern.compile("\\.(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)$");
	public Pipe pipe;
	private static ArchitecturalView importAV;

	public static ArchitecturalView getImportAV() {
		return importAV;
	}

	public static void setImportAV(final ArchitecturalView importAV) {
		Import.importAV = importAV;
	}

	public Import() {

	}

	public static InstanceList buildInstances(final InstanceList instances, final File bodyDir,
			final boolean checkCFiles, final boolean checkJavaFiles) {
		// File bodyDir = FileUtil.checkDir(bodyDirName, false, true);
		try {
			for (final File file : FileListing.getFileListing(bodyDir)) {
				logger.trace("Should I add " + file.getName() + " to instances?");
				if (checkJavaFiles && file.isFile() && file.getName().endsWith(".java")) {
					final String shortClassName = file.getName().replace(".java", "");
					final BufferedReader reader = new BufferedReader(new FileReader(file));
					String line = null;
					String fullClassName = "";
					while ((line = reader.readLine()) != null) {
						final String packageName = FileUtil.findPackageName(line);
						if (packageName != null) {
							fullClassName = packageName + "." + shortClassName;
							logger.trace("\t I've identified the following full class name from analyzing files: "
									+ fullClassName);
						}
					}
					reader.close();
					addFileToInstances(file, instances, false, fullClassName);
				}
				// if we found a c or c++ file
				if (checkCFiles && cLangPattern.matcher(file.getName()).find()) {
					final String depsStyleFilename = file.getAbsolutePath().replace(bodyDir.getAbsolutePath(), "");
					System.out.println("depsStyleFilename = " + depsStyleFilename);
					addFileToInstances(file, instances, false, depsStyleFilename);
				} else {
					logger.trace("\t Nope");
				}
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		return instances;
	}

	public static InstanceList buildInstancesFromFileManager(final FileManager fileMan, final InstanceList instances) {
		// File bodyDir = FileUtil.checkDir(bodyDirName, false, true);
		try {
			for (final File currentFile : fileMan.getFiles()) {
				logger.trace("Should I add " + currentFile.getName() + " to instances?");
				if (currentFile.getName().endsWith(".java")) {
					final String shortClassName = currentFile.getName().replace(".java", "");
					final BufferedReader reader = new BufferedReader(new FileReader(currentFile));
					String line = null;
					String fullClassName = "";
					while ((line = reader.readLine()) != null) {
						final String packageName = FileUtil.findPackageName(line);
						if (packageName != null) {
							fullClassName = packageName + "." + shortClassName;
							logger.trace("\t I've identified the following full class name from analyzing files: "
									+ fullClassName);
						}
					}
					reader.close();
					addFileToInstances(currentFile, instances, false, fullClassName);
				}
				// if we found a c or c++ file
				if (cLangPattern.matcher(currentFile.getName()).find()) {
					final String depsStyleFilename = currentFile.getName();
					System.out.println("depsStyleFilename = " + depsStyleFilename);
					addFileToInstances(currentFile, instances, false, depsStyleFilename);
				} else {
					logger.trace("\t Nope");
				}
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		return instances;
	}

	public static InstanceList buildInstancesGeneralFiles(final InstanceList instances, final File bodyDir) {
		// File bodyDir = FileUtil.checkDir(bodyDirName, false, true);
		try {
			for (final File file : FileListing.getFileListing(bodyDir)) {
				logger.trace("Should I add " + file.getName() + " to instances?");
				if (file.isFile()) {
					addGeneralFileToInstancesUnfiltered(file, instances);
				} else {
					logger.trace("\t Nope");
				}
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
		return instances;
	}

	public ArrayList<Pipe> preparePipeList(final boolean checkCFiles, final boolean checkJavaFiles) {
		final ArrayList<Pipe> pipeList = new ArrayList<>();
		// Pipes: alphanumeric only, camel case separation, lowercase, tokenize,
		// remove stopwords english, remove stopwords java, stem, map to
		// features

		// Set up pipes to filter the text
		pipeList.add(new CharSequenceReplace(Pattern.compile("[^A-Za-z]"), " "));
		pipeList.add(new CamelCaseSeparatorPipe());
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
		pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("stoplists/en.txt", false, true), "UTF-8",
				false, false, false));

		if (checkCFiles) {
			pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("res/ckeywords", false, true), "UTF-8",
					false, false, false));
			pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("res/cppkeywords", false, true), "UTF-8",
					false, false, false));
		} else if (checkJavaFiles) {
			pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("res/javakeywords", false, true), "UTF-8",
					false, false, false));
		} else {
			logger.error("No stop list defined for current programming language!");
			System.exit(-1);
		}

		// Uses the snowball stemmer
		// Running this before the remaining stoplists so those can be
		// simplified
		pipeList.add(new StemmerPipe());

		// CS or programming related list of stopwords, e.g. "data"
		pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("stoplists/cs.txt", true, true), "UTF-8",
				false, false, false));
		// Miscellaneous stop words that don't belong in any other categories
		pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("stoplists/misc.txt", true, true), "UTF-8",
				false, false, false));

		// Stopwords related to the current project, e.g. the name of the
		// project or programmer
		importAV.setConfig(new Config());
		pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile(
				importAV.getConfig().getPerUserArcadeDir().getAbsolutePath() + File.separator + "project.txt", false,
				true), "UTF-8", false, false, false));

		pipeList.add(new TokenSequence2FeatureSequence());
		return pipeList;
	}

	public Pipe buildPipe() {
		final ArrayList<Pipe> pipeList = new ArrayList<>();

		// Read data from File objects
		pipeList.add(new Input2CharSequence("UTF-8"));

		// Regular expression for what constitutes a token.
		// This pattern includes Unicode letters, Unicode numbers,
		// and the underscore character. Alternatives:
		// "\\S+" (anything not whitespace)
		// "\\w+" ( A-Z, a-z, 0-9, _ )
		// "[\\p{L}\\p{N}_]+|[\\p{P}]+" (a group of only letters and numbers OR
		// a group of only punctuation marks)
		final Pattern tokenPattern = Pattern.compile("[\\p{L}\\p{N}_]+");

		// Tokenize raw strings
		pipeList.add(new CharSequence2TokenSequence(tokenPattern));

		// Normalize all tokens to all lowercase
		pipeList.add(new TokenSequenceLowercase());

		// Remove stopwords from a standard English stoplist.
		// options: [case sensitive] [mark deletions]
		pipeList.add(new TokenSequenceRemoveStopwords(false, false));

		// Rather than storing tokens as strings, convert
		// them to integers by looking them up in an alphabet.
		pipeList.add(new TokenSequence2FeatureSequence());

		// Do the same thing for the "target" field:
		// convert a class label string to a Label object,
		// which has an index in a Label alphabet.
		pipeList.add(new Target2Label());

		// Now convert the sequence of features to a sparse vector,
		// mapping feature IDs to counts.
		pipeList.add(new FeatureSequence2FeatureVector());

		// Print out the features and the label
		pipeList.add(new PrintInputAndTarget());
		// Stop words are not needed here because the lists are assumed to be
		// pre-filtered
		return new SerialPipes(pipeList);
	}

	/**
	 * Add a file to instances
	 *
	 * @param file
	 *            - The File
	 * @param instances
	 *            - The InstanceList to add the file to
	 * @param cLangFile
	 *            - Whether it's a C/C++ language file
	 * @param name
	 *            - Name to add it under
	 */
	public static void addFileToInstances(final File file, final InstanceList instances, final boolean cLangFile,
			final String name) {
		logger.entry(file, cLangFile, name);
		logger.trace("I'm going to add this file to instances: " + file);
		final String data = FileUtil.readFile(file, Charset.defaultCharset());
		// System.out.println("Data = " + data);
		final Instance instance = new Instance(data, "X", name, file.getAbsolutePath());
		instances.addThruPipe(instance);
		logger.traceExit();
	}

	public static void addGeneralFileToInstancesUnfiltered(final File file, final InstanceList instances) {
		logger.entry(file);
		logger.trace("I'm going to add this file to instances: " + file);
		final String data = FileUtil.readFile(file, Charset.defaultCharset());
		System.out.println("Data = " + data);
		final Instance instance = new Instance(data, "X", "noname", file.getAbsolutePath());
		// instances.addThruPipe(instance);
		instances.add(instance);
		logger.traceExit();
	}

	public InstanceList readDirectory(final File directory) {
		return readDirectories(new File[] { directory });
	}

	public InstanceList readDirectories(final File[] directories) {

		// Construct a file iterator, starting with the
		// specified directories, and recursing through subdirectories.
		// The second argument specifies a FileFilter to use to select
		// files within a directory.
		// The third argument is a Pattern that is applied to the
		// filename to produce a class label. In this case, I've
		// asked it to use the last directory name in the path.
		final FileIterator iterator = new FileIterator(directories, new DummyFilter(), FileIterator.LAST_DIRECTORY);

		// Construct a new instance list, passing it the pipe
		// we want to use to process instances.
		final InstanceList instances = new InstanceList(pipe);

		// Now process each instance provided by the iterator.
		instances.addThruPipe(iterator);

		return instances;
	}

	/** This class illustrates how to build a simple file filter */
	class TxtFilter implements FileFilter {

		/**
		 * Test whether the string representation of the file ends with the
		 * correct extension. Note that {@ref FileIterator} will only call this
		 * filter if the file is not a directory, so we do not need to test that
		 * it is a file.
		 */
		@Override
		public boolean accept(final File file) {
			return file.toString().endsWith(".txt");
		}
	}

	class DummyFilter implements FileFilter {

		/**
		 * Test whether the string representation of the file ends with the
		 * correct extension. Note that {@ref FileIterator} will only call this
		 * filter if the file is not a directory, so we do not need to test that
		 * it is a file.
		 */
		@Override
		public boolean accept(final File file) {
			return true;
		}
	}

}
