package edu.usc.softarch.arcade.facts.driver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.Logger;

import classycle.Analyser;
import classycle.ClassAttributes;
import classycle.graph.AtomicVertex;
import edu.usc.softarch.arcade.clustering.FastFeatureVectors;
import edu.usc.softarch.arcade.clustering.FeatureVectorMap;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.functiongraph.TypedEdgeGraph;
import edu.usc.softarch.arcade.relax.RelaxFile;

public class JavaSourceToDepsBuilder implements SourceToDepsBuilder {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(JavaSourceToDepsBuilder.class);

	private final String[] negativeList = { "java" };

	private Set<Pair<String, String>> edges;
	private static FastFeatureVectors ffVecs = null;
	private int numSourceEntities = 0;

	@Override
	public Set<Pair<String, String>> getEdges() {
		return edges;
	}

	@Override
	public int getNumSourceEntities() {
		return numSourceEntities;
	}

	public static void main(final File inputClassesDir, final File depsRsfFile) {
		new JavaSourceToDepsBuilder().build(inputClassesDir, depsRsfFile);
	}

	@Override
	/**
	 * Get dependencies of a Java class directory
	 */
	public void build(final File inputClassesDir, final File depsRsfFile) {
		logger.entry(inputClassesDir, depsRsfFile);
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String[] analyser_args = { inputClassesDir.getPath() };
		final Analyser analyzer = new Analyser(analyser_args);
		try {
			analyzer.readAndAnalyse(false);
		} catch (final IOException e) {
			System.out.println("Unable to read class files while determining dependencies");
			System.exit(-1);
		}
		// analyzer.printRaw(new PrintWriter(System.out));

		PrintStream out = null;
		try {
			out = new PrintStream(depsRsfFile);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		final PrintWriter writer = new PrintWriter(out);
		final AtomicVertex[] graph = analyzer.getClassGraph();

		edges = new LinkedHashSet<>();
		for (final AtomicVertex vertex : graph) {
			final ClassAttributes sourceAttributes = (ClassAttributes) vertex.getAttributes();
			// writer.println(sourceAttributes.getType() + " " +
			// sourceAttributes.getName());
			for (int j = 0, n = vertex.getNumberOfOutgoingArcs(); j < n; j++) {
				final ClassAttributes targetAttributes = (ClassAttributes) vertex.getHeadVertex(j).getAttributes();
				// writer.println(" " + targetAttributes.getType() + " " +
				// targetAttributes.getName());
				final Pair<String, String> edge = new ImmutablePair<>(sourceAttributes.getName(), targetAttributes.getName());

				// Exclude dependencies that are part of a negative list, e.g. those that start with "java."
				boolean addEdge = true;
				for (final String s : negativeList) {
					if (edge.getKey().startsWith(s) || edge.getValue().startsWith(s)) {
						addEdge = false;
					}
				}
				if (addEdge) {
					edges.add(edge);
					if (Config.getSelectedRecoveryMethodName().equals("RELAX")) {
						addRelaxFileOutgoing(sourceAttributes.getName(), targetAttributes.getName());
					}
				}
			}
		}

		for (final Pair<String, String> edge : edges) {
			writer.println("depends " + edge.getLeft() + " " + edge.getRight());
		}
		writer.close();

		final Set<String> sources = new HashSet<>();
		for (final Pair<String, String> edge : edges) {
			sources.add(edge.getLeft());
		}
		numSourceEntities = sources.size();

		final TypedEdgeGraph typedEdgeGraph = new TypedEdgeGraph();
		for (final Pair<String, String> edge : edges) {
			typedEdgeGraph.addEdge("depends", edge.getLeft(), edge.getRight());
		}

		final FeatureVectorMap fvMap = new FeatureVectorMap(typedEdgeGraph);
		ffVecs = fvMap.convertToFastFeatureVectors();
		logger.traceExit();
	}

	private void addRelaxFileOutgoing(final String canonicalName, final String outDep) {
		final RelaxFile rf = RelaxFile.getCanonicalToRelaxFile().get(canonicalName);
		if (rf == null) {
			System.out.println("RelaxFile is null when adding outgoing dependency: " + canonicalName + " --> " + outDep);
			return;
		}
		rf.getOutgoingDependencies().add(outDep);
	}

	@Override
	public FastFeatureVectors getFfVecs() {
		return JavaSourceToDepsBuilder.ffVecs;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "JavaSourceToDepsBuilder [edges=" + edges + "\nFast feature vectors=" + ffVecs + "\nnumSourceEntities=" + numSourceEntities + "]";
	}
}
