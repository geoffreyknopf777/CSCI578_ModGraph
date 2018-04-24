package edu.usc.softarch.arcade.facts.driver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class BunchClusterToRsfClusterConverter {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		final String inBunchClusterFilename = args[0];
		final String outClusterRsfFilename = args[1];

		try {
			final BufferedWriter out = new BufferedWriter(new FileWriter(new File(outClusterRsfFilename)));
			final Scanner scanner = new Scanner(new File(inBunchClusterFilename));
			while (scanner.hasNextLine()) {
				final String line = scanner.nextLine();
				final String[] clusterLineTokens = line.split("=");
				String clusterName = clusterLineTokens[0];
				clusterName = clusterName.replaceFirst("SS\\(", "").replace(")", "");
				final String filesOfCluster = clusterLineTokens[1];

				final String[] filesTokens = filesOfCluster.split("\\s*,\\s*");
				for (final String file : filesTokens) {
					final String trimmedFile = file.trim();
					out.write("contain " + clusterName + " " + trimmedFile + "\n");

				}
				System.out.println(clusterName);
			}

			out.close();
			scanner.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

}
