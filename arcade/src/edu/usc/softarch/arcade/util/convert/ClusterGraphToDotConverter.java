package edu.usc.softarch.arcade.util.convert;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.clustering.util.ClusterUtil;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.util.FileUtil;

public class ClusterGraphToDotConverter {

	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(ClusterGraphToDotConverter.class);

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String depsFilename = args[0];
		final String clustersFilename = args[1];
		final String dotFilename = args[2];
		runConversion(depsFilename, clustersFilename, dotFilename);
	}

	public static void mainNoSmells(final String[] args) {
		// PropertyConfigurator.configure(Config.getLoggingConfigFilename());

		final String depsFilename = args[0];
		final String clustersFilename = args[1];
		final String dotFilename = args[2];
		runConversionNoSmells(depsFilename, clustersFilename, dotFilename);
	}

	private static String getFontColor(final Set<String> s, final String clusterName, final String color,
			final String fontColor) {
		if (!CollectionUtils.isEmpty(s)) {
			if (s.contains(clusterName)) {
				return "fontcolor = " + color;
			}
		}
		return fontColor;
	}

	public static void runConversion(final String depsFilename, final String clustersFilename,
			final String dotFilename) {

		RsfReader.loadRsfDataFromFile(depsFilename);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile(clustersFilename);
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;

		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);

		final Set<List<String>> edges = ClusterUtil.buildClusterEdges(clusterMap, depFacts);

		final Config currentConfig = Controller.getCurrentView().getConfig();

		try {
			final FileWriter out = new FileWriter(dotFilename);
			// out.write("graph [mindist=0.2]);\n");
			out.write("digraph \"" + FileUtil.checkFile(dotFilename, false, true).getName() + "\"{\n");
			// out.write("mindist=\"0.01\";\n");
			out.write("graph [K=0.6];\n");
			out.write("node [shape=record];\n");

			final Set<String> bdcClusters = Controller.getCurrentView().getSmellClusters().get("bdc");
			final Set<String> bcoClusters = Controller.getCurrentView().getSmellClusters().get("bco");
			final Set<String> buoClusters = Controller.getCurrentView().getSmellClusters().get("buo");
			final Set<String> spfClusters = Controller.getCurrentView().getSmellClusters().get("spf");

			for (final String clusterName : clusterMap.keySet()) {
				final String lcsOfComponents = Controller.getCurrentView()
						.getEntitiesLongestCommonSubstring(clusterName);

				final String clusterLabel = clusterName + "_" + lcsOfComponents;

				String fontColor = "fontcolor = black";
				String smellString = "";

				fontColor = getFontColor(spfClusters, clusterName, "green", fontColor);
				if (fontColor.contains("green")) {
					smellString = smellString.concat(" spf");
				}
				fontColor = getFontColor(buoClusters, clusterName, "orange", fontColor);
				if (fontColor.contains("orange")) {
					smellString = smellString.concat(" buo");
				}
				fontColor = getFontColor(bcoClusters, clusterName, "blue", fontColor);
				if (fontColor.contains("blue")) {
					smellString = smellString.concat(" bco");
				}
				fontColor = getFontColor(bdcClusters, clusterName, "red", fontColor);
				if (fontColor.contains("red")) {
					smellString = smellString.concat(" bdc");
				}

				// smellString = smellString.concat(") ");

				// String clusterToolTip = "|||||| " + lcsOfComponents +
				// " |||||| " + smellString +
				// Controller.getCurrentView().getEntitiesByNameAsString(clusterName);
				String clusterToolTip = Controller.getCurrentView().getEntitiesByNameAsString(clusterName);
				if (!currentConfig.isShowSvgToolTips()) {
					clusterToolTip = "^_^";
				}
				// out.write("\t\"" + clusterLabel + "\" [ " + fontColor +
				// " tooltip = \"" + clusterToolTip + "\" label=\"<id> " +
				// clusterName + "|<lcs> " + lcsOfComponents + "|{"
				// +
				// Controller.getCurrentView().getEntitiesByNameAsString(clusterName).replaceAll(",",
				// "\n") + "}|<sm> " + smellString + "\"];\n");
				out.write("\t\"" + clusterLabel + "\" [" + fontColor + " tooltip = \"" + clusterToolTip
						+ "\" label=\"<id> " + clusterName + "|<lcs> " + lcsOfComponents + "|<sm> " + smellString
						+ "\"];\n");
				// out.write("\t\"" + clusterLabel + "_components\" [ " +
				// fontColor + " label=\"" +
				// Controller.getCurrentView().getEntitiesByNameAsString(clusterName)
				// + "\"];\n");
				// out.write("\t\"" +
				// "[label=\"<f0> left|<f1> middle|<f2> right\"];");
			}

			for (final List<String> edge : edges) {
				final String source = edge.get(0);
				final String target = edge.get(1);
				final String sourceString = source + "_"
						+ Controller.getCurrentView().getEntitiesLongestCommonSubstring(source);
				final String targetString = target + "_"
						+ Controller.getCurrentView().getEntitiesLongestCommonSubstring(target);
				if (bdcClusters.contains(source) && bdcClusters.contains(target)) {
					out.write("\t\"" + sourceString + "\" -> \"" + targetString + "\" [color = red];\n");
				} else {
					out.write("\t\"" + sourceString + "\" -> \"" + targetString + "\";\n");
				}
			}
			// out.write("}\n");
			out.write("}\n");

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void runConversionNoSmells(final String depsFilename, final String clustersFilename,
			final String dotFilename) {

		RsfReader.loadRsfDataFromFile(depsFilename);
		final List<List<String>> depFacts = RsfReader.unfilteredFacts;

		RsfReader.loadRsfDataFromFile(clustersFilename);
		final List<List<String>> clusterFacts = RsfReader.unfilteredFacts;

		final Map<String, Set<String>> clusterMap = ClusterUtil.buildClusterMap(clusterFacts);

		final Set<List<String>> edges = ClusterUtil.buildClusterEdges(clusterMap, depFacts);

		final Config currentConfig = Controller.getCurrentView().getConfig();

		try {
			final FileWriter out = new FileWriter(dotFilename);
			// out.write("graph [mindist=0.2]);\n");
			out.write("digraph \"" + FileUtil.checkFile(dotFilename, false, true).getName() + "\"{\n");
			// out.write("mindist=\"0.01\";\n");
			out.write("graph [K=0.6];\n");
			out.write("node [shape=record];\n");

			for (final String clusterName : clusterMap.keySet()) {
				final String lcsOfComponents = Controller.getCurrentView()
						.getEntitiesLongestCommonSubstring(clusterName);

				final String clusterLabel = clusterName + "_" + lcsOfComponents;

				final String fontColor = "fontcolor = black";
				final String smellString = "";

				// smellString = smellString.concat(") ");

				// String clusterToolTip = "|||||| " + lcsOfComponents +
				// " |||||| " + smellString +
				// Controller.getCurrentView().getEntitiesByNameAsString(clusterName);
				String clusterToolTip = Controller.getCurrentView().getEntitiesByNameAsString(clusterName);
				if (!currentConfig.isShowSvgToolTips()) {
					clusterToolTip = "^_^";
				}
				// out.write("\t\"" + clusterLabel + "\" [ " + fontColor +
				// " tooltip = \"" + clusterToolTip + "\" label=\"<id> " +
				// clusterName + "|<lcs> " + lcsOfComponents + "|{"
				// +
				// Controller.getCurrentView().getEntitiesByNameAsString(clusterName).replaceAll(",",
				// "\n") + "}|<sm> " + smellString + "\"];\n");
				out.write("\t\"" + clusterLabel + "\" [" + fontColor + " tooltip = \"" + clusterToolTip
						+ "\" label=\"<id> " + clusterName + "|<lcs> " + lcsOfComponents + "|<sm> " + smellString
						+ "\"];\n");
				// out.write("\t\"" + clusterLabel + "_components\" [ " +
				// fontColor + " label=\"" +
				// Controller.getCurrentView().getEntitiesByNameAsString(clusterName)
				// + "\"];\n");
				// out.write("\t\"" +
				// "[label=\"<f0> left|<f1> middle|<f2> right\"];");
			}

			for (final List<String> edge : edges) {
				final String source = edge.get(0);
				final String target = edge.get(1);
				final String sourceString = source + "_"
						+ Controller.getCurrentView().getEntitiesLongestCommonSubstring(source);
				final String targetString = target + "_"
						+ Controller.getCurrentView().getEntitiesLongestCommonSubstring(target);

				out.write("\t\"" + sourceString + "\" -> \"" + targetString + "\";\n");
			}
			// out.write("}\n");
			out.write("}\n");

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
