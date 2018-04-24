package edu.usc.softarch.arcade.util.ldasupport;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Logger;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.CharSequenceReplace;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.topics.CamelCaseSeparatorPipe;
import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;

public class PipeExtractor {

	private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(PipeExtractor.class);

	// private static final long serialVersionUID = 3815392533685324954L;

	public static void main(final String[] args) throws FileNotFoundException, IOException {
		logger.entry((Object[]) args);
		// logger.trace(serialVersionUID);// so it doesn't get deleted
		// automatically
		final ArrayList<Pipe> pipeList = new ArrayList<>();
		final boolean checkJavaFiles = Config.getSelectedLanguage().equals(Config.Language.java);
		final boolean checkCFiles = Config.getSelectedLanguage().equals(Config.Language.c);
		// numTopics =99;
		// Pipes: alphanumeric only, camel case separation, lowercase, tokenize,
		// remove stopwords english, remove stopwords java, stem, map to
		// features
		pipeList.add(new CharSequenceReplace(Pattern.compile("[^A-Za-z]"), " "));
		pipeList.add(new CamelCaseSeparatorPipe());
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
		// See
		// http://stackoverflow.com/questions/26977605/in-mallet-java-api-why-cant-the-input2charsequence-pipe-feed-into-the-charsequ
		//
		// pipeList.add(new TokenSequenceLowercase());
		pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false));

		if (checkCFiles) {
			pipeList.add(new TokenSequenceRemoveStopwords(new File("res/ckeywords"), "UTF-8", false, false, false));
			pipeList.add(new TokenSequenceRemoveStopwords(new File("res/cppkeywords"), "UTF-8", false, false, false));
		} else if (checkJavaFiles) {
			pipeList.add(new TokenSequenceRemoveStopwords(new File("res/javakeywords"), "UTF-8", false, false, false));
		} else {
			logger.error("No stop list defined for current programming language!");
			logger.traceExit();
			System.exit(-1);
		}

		// CS or programming related list of stopwords, e.g. "data"
		pipeList.add(new TokenSequenceRemoveStopwords(FileUtil.checkFile("stoplists/cs.txt", true, true), "UTF-8",
				false, false, false));

		// Stopwords related to the current project, e.g. the name of the
		// project or programmer
		// pipeList.add(new
		// TokenSequenceRemoveStopwords(FileUtil.checkFile(Config.getPerUserArcadeDir().getAbsolutePath()
		// + File.separator + "project.txt", false, true), "UTF-8", false,
		// false, false));

		pipeList.add(new TokenSequence2FeatureSequence());

		final InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		final String testDir = args[0]; // srcDir;
		for (final File file : FileListing.getFileListing(new File(testDir))) {
			if (file.isFile() && file.getName().endsWith(".java")) {
				final String shortClassName = file.getName().replace(".java", "");
				final BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				String fullClassName = "";
				while ((line = reader.readLine()) != null) {
					final String packageName = FileUtil.findPackageName(line);
					if (packageName != null) {
						fullClassName = packageName + "." + shortClassName;
					}
				}
				reader.close();
				final String data = FileUtil.readFile(file, Charset.defaultCharset());
				final Instance instance = new Instance(data, "X", fullClassName, file.getAbsolutePath());
				instances.addThruPipe(instance);
			}
			final Pattern p = Pattern.compile("\\.(c|cpp|cc|s|h|hpp|icc|ia|tbl|p)$");
			// if we found a c or c++ file
			if (p.matcher(file.getName()).find()) {
				final String depsStyleFilename = file.getAbsolutePath().replace(testDir, "");
				final String data = FileUtil.readFile(file, Charset.defaultCharset());
				final Instance instance = new Instance(data, "X", depsStyleFilename, file.getAbsolutePath());
				instances.addThruPipe(instance);
			}
		}

		// //save for next time
		instances.save(new File(args[1], "output.pipe"));
		logger.traceExit();
	}
}
