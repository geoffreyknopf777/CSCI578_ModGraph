package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.clustering.FastFeatureVectors;
import edu.usc.softarch.arcade.clustering.FeatureVectorMap;
import edu.usc.softarch.arcade.functiongraph.TypedEdgeGraph;
import edu.usc.softarch.arcade.util.FileUtil;

public class CSourceToDepsBuilder implements SourceToDepsBuilder {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(CSourceToDepsBuilder.class);

	public Set<Pair<String, String>> edges;
	public static FastFeatureVectors ffVecs = null;
	public static int numSourceEntities = 0;

	@Override
	public Set<Pair<String, String>> getEdges() {
		logger.traceEntry();
		logger.traceExit();
		return edges;
	}

	@Override
	public int getNumSourceEntities() {
		logger.traceEntry();
		logger.traceExit();
		return numSourceEntities;
	}

	public static void main(final File inputClassesDir, final File depsRsfFile) {
		logger.entry(inputClassesDir, depsRsfFile);
		new CSourceToDepsBuilder().build(inputClassesDir, depsRsfFile);
		logger.traceExit();
	}

	@Override
	/**
	 * Get dependencies of a Java class directory
	 */
	public void build(final File inputClassesDir, final File depsRsfFile) {
		logger.entry(inputClassesDir, depsRsfFile);
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		// final File inputDir = FileUtil.checkDir(inputClassesDir[0], false);//
		// TODO: Compare with original source

		final String pwd = System.getProperty("user.dir");
		final String mkFilesCmdLoc = pwd + File.separator + "tools" + File.separator + "mkfiles.pl";
		final String mkDepCmdLoc = pwd + File.separator + "tools" + File.separator + "mkdep.pl";

		FileUtil.checkFile(mkFilesCmdLoc, false, true);
		FileUtil.checkFile(mkDepCmdLoc, false, true);

		final String mkFilesCmd = "perl " + mkFilesCmdLoc;
		final String mkDepCmd = "perl " + mkDepCmdLoc;

		final String[] cmds = { mkFilesCmd, mkDepCmd };
		for (final String cmd : cmds) {
			try {
				execCmd(cmd, inputClassesDir.getPath());
			} catch (final IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}

		final String makeDepFileLocation = inputClassesDir.getPath() + File.separator + "make.dep";
		final String[] makeDepReaderArgs = { makeDepFileLocation, depsRsfFile.getPath() };

		try {
			MakeDepReader.main(makeDepReaderArgs);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		RsfReader.loadRsfDataFromFile(depsRsfFile);

		numSourceEntities = RsfReader.unfilteredFacts.size();

		final TypedEdgeGraph typedEdgeGraph = new TypedEdgeGraph();
		edges = new LinkedHashSet<Pair<String, String>>();
		for (final List<String> fact : RsfReader.unfilteredFacts) {
			final String source = fact.get(1);
			final String target = fact.get(2);

			typedEdgeGraph.addEdge("depends", source, target);

			final Pair<String, String> edge = new ImmutablePair<String, String>(source, target);
			edges.add(edge);
		}

		final Set<String> sources = new HashSet<String>();
		for (final Pair<String, String> edge : edges) {
			sources.add(edge.getLeft());
		}
		numSourceEntities = sources.size();

		final FeatureVectorMap fvMap = new FeatureVectorMap(typedEdgeGraph);
		ffVecs = fvMap.convertToFastFeatureVectors();
		logger.traceExit();
	}

	private static void execCmd(final String cmd, final String inputDir) throws IOException {
		logger.entry(cmd, inputDir);
		System.out.println("Executing command: " + cmd);
		final Process process = Runtime.getRuntime().exec(cmd, null, new File(inputDir));

		String line;
		final BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
		while ((line = in.readLine()) != null) {
			System.out.println(line);
		}
		in.close();
		logger.traceExit();
	}

	@Override
	public FastFeatureVectors getFfVecs() {
		logger.traceEntry();
		logger.traceExit();
		return ffVecs;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		logger.traceEntry();
		logger.traceExit();
		return "JavaSourceToDepsBuilder [edges=" + edges + "\nFast feature vectors=" + ffVecs + "\nnumSourceEntities=" + numSourceEntities + "]";
	}
}
