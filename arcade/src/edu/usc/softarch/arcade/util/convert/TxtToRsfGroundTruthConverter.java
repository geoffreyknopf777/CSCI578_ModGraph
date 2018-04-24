package edu.usc.softarch.arcade.util.convert;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import edu.usc.softarch.arcade.facts.ConcernCluster;
import edu.usc.softarch.arcade.facts.GroundTruthFileParser;

public class TxtToRsfGroundTruthConverter {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// //PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String txtFilename = args[0];
		final String rsfFilename = args[1];

		GroundTruthFileParser.parseBashStyle(txtFilename);

		// Set<ConcernCluster> clusters = GroundTruthFileParser.getClusters();

		try {
			final FileWriter out = new FileWriter(rsfFilename);

			final Map<String, ConcernCluster> clusterMap = GroundTruthFileParser.getClusterMap();

			for (final String clusterName : clusterMap.keySet()) {
				final ConcernCluster cluster = clusterMap.get(clusterName);
				for (final String entity : cluster.getEntities()) {
					out.write("contain " + cluster.getName() + " " + entity + "\n");
				}
			}

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
