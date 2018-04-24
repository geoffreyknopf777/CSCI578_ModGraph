package edu.usc.softarch.arcade.topics;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Iterator;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.CharSequenceLowercase;
import cc.mallet.pipe.CharSequenceReplace;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceRemoveStopwords;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.IDSorter;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelSequence;
import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;

public class TopicModelTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		// Begin by importing documents from text to feature sequences
		final ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

		// Pipes: alphanumeric only, camel case separation, lowercase, tokenize,
		// remove stopwords english, remove stopwords java, stem, map to
		// features
		pipeList.add(new CharSequenceReplace(Pattern.compile("[^A-Za-z]"), " "));
		pipeList.add(new CamelCaseSeparatorPipe());
		pipeList.add(new CharSequenceLowercase());
		pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")));
		pipeList.add(new TokenSequenceRemoveStopwords(new File("stoplists/en.txt"), "UTF-8", false, false, false));
		pipeList.add(new TokenSequenceRemoveStopwords(new File("res/javakeywords"), "UTF-8", false, false, false));
		pipeList.add(new StemmerPipe());
		pipeList.add(new TokenSequence2FeatureSequence());

		final InstanceList instances = new InstanceList(new SerialPipes(pipeList));

		final String testDir = "testdata/hadoop-0.19.0-src";
		for (final File file : FileListing.getFileListing(new File(testDir))) {
			System.out.println(file.getName());
			if (file.isFile() && file.getName().endsWith(".java")) {
				final String shortClassName = file.getName().replace(".java", "");
				final BufferedReader reader = new BufferedReader(new FileReader(file));
				String line = null;
				String fullClassName = "";
				while ((line = reader.readLine()) != null) {
					final String packageName = FileUtil.findPackageName(line);
					fullClassName = packageName + "." + shortClassName;
					if (packageName != null) {
						System.out.println("\t" + fullClassName);
					}
				}
				reader.close();
				final String data = FileUtil.readFile(file, Charset.defaultCharset());
				final Instance instance = new Instance(data, "X", fullClassName, file.getAbsolutePath());
				instances.addThruPipe(instance);
			}
		}

		/*
		 * Reader fileReader = new InputStreamReader(new FileInputStream(new
		 * File( args[0])), "UTF-8"); instances.addThruPipe(new
		 * CsvIterator(fileReader, Pattern
		 * .compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"), 3, 2, 1)); // data, //
		 * label, // name // fields
		 */
		// Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
		// Note that the first parameter is passed as the sum over topics, while
		// the second is
		final int numTopics = 40;
		final double alpha = (double) 50 / (double) numTopics;
		final double beta = .01;
		final ParallelTopicModel model = new ParallelTopicModel(numTopics, alpha, beta);

		model.addInstances(instances);

		// Use two parallel samplers, which each look at one half the corpus and
		// combine
		// statistics after every iteration.
		model.setNumThreads(4);

		// Run the model for 50 iterations and stop (this is for testing only,
		// for real applications, use 1000 to 2000 iterations)
		model.setNumIterations(100);
		model.estimate();

		// Show the words and topics in the first instance

		// The data alphabet maps word IDs to strings
		final Alphabet dataAlphabet = instances.getDataAlphabet();

		final FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
		final LabelSequence topics = model.getData().get(0).topicSequence;

		Formatter out = new Formatter(new StringBuilder(), Locale.US);
		for (int position = 0; position < tokens.getLength(); position++) {
			out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
		}
		System.out.println(out);

		// Estimate the topic distribution of the first instance,
		// given the current Gibbs state.
		final double[] topicDistribution = model.getTopicProbabilities(0);

		double sum = 0;
		for (final double prop : topicDistribution) {
			sum += prop;
		}
		System.out.println("sum: " + sum);

		// Get an array of sorted sets of word ID/count pairs
		final ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();

		// Show top 5 words in topics with proportions for the first document
		for (int topic = 0; topic < numTopics; topic++) {
			final Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();

			out = new Formatter(new StringBuilder(), Locale.US);
			out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
			int rank = 0;
			while (iterator.hasNext() && rank < 5) {
				final IDSorter idCountPair = iterator.next();
				out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
				rank++;
			}
			System.out.println(out);
		}

		// createNewInstancesExamples(instances, model,
		// dataAlphabet,topicSortedWords);
	}

	// private void createNewInstancesExamples(InstanceList instances,
	// ParallelTopicModel model, Alphabet dataAlphabet,
	// ArrayList<TreeSet<IDSorter>> topicSortedWords) {
	// // Create a new instance with high probability of topic 0
	// StringBuilder topicZeroText = new StringBuilder();
	// Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();
	//
	// int rank = 0;
	// while (iterator.hasNext() && rank < 5) {
	// IDSorter idCountPair = iterator.next();
	// topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID())
	// + " ");
	// rank++;
	// }
	//
	// // Create a new instance named "test instance" with empty target and
	// // source fields.
	// InstanceList testing = new InstanceList(instances.getPipe());
	// testing.addThruPipe(new Instance(topicZeroText.toString(), null,
	// "test instance", null));
	//
	// TopicInferencer inferencer = model.getInferencer();
	// double[] testProbabilities = inferencer.getSampledDistribution(
	// testing.get(0), 10, 1, 5);
	// System.out.println("0\t" + testProbabilities[0]);
	// }

}
