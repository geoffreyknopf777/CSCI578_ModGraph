package edu.usc.softarch.arcade.util.graph;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.functors.StringValueTransformer;

import edu.uci.ics.jung.algorithms.generators.random.BarabasiAlbertGenerator;
import edu.uci.ics.jung.graph.DirectedGraph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.io.GraphMLWriter;

public class SimpleGraphGenerator {

	static Factory<DirectedGraph<String, Integer>> graphFactory = () -> new DirectedSparseMultigraph<String, Integer>();

	static Factory<Integer> edgeFactory = new Factory<Integer>() {
		int i = 0;

		@Override
		public Integer create() {
			return i++;
		}
	};

	static Factory<String> vertexFactory = new Factory<String>() {
		int i = 0;

		@Override
		public String create() {
			return "V" + i++;
		}
	};

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final int numInitVertices = 1;
		final int numEdgesToAttach = 5;
		final int seed = 2;
		final Set<String> seedVertices = new HashSet<String>();
		final BarabasiAlbertGenerator bag = new BarabasiAlbertGenerator(graphFactory, vertexFactory, edgeFactory, numInitVertices, numEdgesToAttach, seed, seedVertices);
		/*
		 * for (int i=0;i<numInitVertices;i++) {
		 * seedVertices.add(vertexFactory.create()); }
		 */

		bag.evolveGraph(5);

		final Graph<?, ?> graph = bag.create();
		System.out.println(graph);

		try {
			final FileWriter fileWriter = new FileWriter("data" + File.separator + "generated" + File.separator + "s" + seed + "v" + numInitVertices + "e" + numEdgesToAttach + ".graphml");
			final GraphMLWriter gmlWriter = new GraphMLWriter();
			gmlWriter.setEdgeIDs(StringValueTransformer.getInstance());
			gmlWriter.save(graph, fileWriter);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);

		}
	}
}