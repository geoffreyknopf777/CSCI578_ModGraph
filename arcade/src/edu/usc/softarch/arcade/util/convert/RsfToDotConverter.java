package edu.usc.softarch.arcade.util.convert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.RsfReader;
import edu.usc.softarch.arcade.relax.Clustering;
import edu.usc.softarch.arcade.relax.ColorManager;
import edu.usc.softarch.arcade.relax.TopLevel;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.JobControlUtil;

public class RsfToDotConverter {
	static Config currentConfig = TopLevel.getCurrentConfig();

	public static void main(final String[] args) {
		final String rsfFilename = args[0];
		final String dotFilename = args[1];
		runConversion(rsfFilename, dotFilename);
	}

	private static void runConversion(final String rsfFilename, final String dotFilename) {
		RsfReader.loadRsfDataFromFile(rsfFilename);
		final List<List<String>> facts = RsfReader.unfilteredFacts;

		try {
			final FileWriter out = new FileWriter(dotFilename);
			out.write("digraph G {\n");

			for (final List<String> fact : facts) {
				final String source = fact.get(1);
				final String target = fact.get(2);
				out.write("\t\"" + source + "\" -> \"" + target + "\";\n");
			}

			out.write("}\n");

			out.close();
		} catch (final IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	public static void runConversionRELAX(final String rsfFilename, final String dotFileName) {
		RsfReader.loadRsfDataFromFile(rsfFilename);
		final List<List<String>> facts = RsfReader.unfilteredFacts;
		String out = "";
		out += "digraph G {\n";
		// out += "node [color=Red]\n";
		for (final List<String> fact : facts) {
			final String fact1 = fact.get(1);
			final String source = Clustering.getClusterMap().get(Integer.parseInt(fact1)).getName();
			final String target = fact.get(2);
			out += "\t\"" + source + "\"";
			out += "->";
			out += "\"" + target + "\";\n";

		}
		out += "}\n";
		try {
			FileUtils.writeStringToFile(FileUtil.checkFile(dotFileName, false, false), out);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void runConversionRELAXBase(final String rsfFilename, final String dotFileName,
			final Boolean showConcerns) {

		RsfReader.loadRsfDataFromFile(rsfFilename);
		final ArrayList<String> classNames = new ArrayList<>();
		List<List<String>> facts = RsfReader.unfilteredFacts;
		String header = "digraph G {\n";
		header += "graph [ fontsize = 36,\nlabel = \"" + dotFileName + "\"];\n";
		String out = header;
		String[] lines;
		int linesIndex;
		if (showConcerns) {
			linesIndex = 0;
			lines = new String[facts.size()];
			for (final List<String> fact : facts) {
				final String fact1 = fact.get(1);
				// String source =
				// Clustering.getClusterMap().get(Integer.parseInt(fact1)).getName();
				String source = fact1;
				source = FilenameUtils.getBaseName(source);
				String target = fact.get(2);
				target = FilenameUtils.getBaseName(target);
				classNames.add(source);
				classNames.add(target);
				String currentLine = "";
				currentLine += "\t ";
				currentLine += "\"" + source;
				currentLine += "\"";
				currentLine += " -> ";
				currentLine += "\"";
				currentLine += target;
				currentLine += "\"";
				// currentLine += " [color=\"" +
				// ColorManager.getColorByIndex(Clustering.getClusterID(source))
				// + "\"]";
				currentLine += " [color=\"" + ColorManager.getColorByName(fact1) + "\"]";
				currentLine += ";\n";
				lines[linesIndex++] = currentLine;
			}

			Arrays.sort(lines);
			for (final String l : lines) {
				out += l;
			}
		}

		final String depsRsfFileName = rsfFilename.replaceAll("relax_clusters", "deps");
		RsfReader.loadRsfDataFromFile(depsRsfFileName);
		facts = RsfReader.unfilteredFacts;
		final ArrayList<String> linesList = new ArrayList<>();
		lines = new String[facts.size()];
		linesIndex = 0;
		for (final List<String> fact : facts) {
			final String source = FilenameUtils.getExtension(fact.get(1));
			final String target = FilenameUtils.getExtension(fact.get(2));
			// if (classNames.contains(source) || classNames.contains(target)) {
			String currentLine = "";
			currentLine += "\t ";
			currentLine += "\"" + source;
			currentLine += "\"";
			currentLine += " -> ";
			currentLine += "\"";
			currentLine += target;
			currentLine += "\"";
			// final CodeEntity c = TopLevel.getEntityByName(target);
			// if (c != null) {
			// final String bestLabel = c.getBestLabel();
			// final int id = Clustering.getClusterID(bestLabel);
			//
			// currentLine += " [color=\"" + colorList[id] + "\"]";
			// }

			// final CodeEntity c = TopLevel.getEntityByName(target);
			// if (c != null) {
			// final String bestLabel = c.getBestLabel();
			// final int id = Clustering.getClusterID(bestLabel);
			//
			// currentLine += " [color=\"" + colorList[id] + "\"]";
			// }

			final Integer id = Clustering.getEntityNameBestLabelIDMap().get(target);
			if (id != null) {
				currentLine += " [color=\"" + ColorManager.getColorByIndex(id) + "\"]";
			}
			currentLine += ";\n";

			// Replace all '$' characters with '#' characters so the dot
			// interpreter doesn't think we are trying to have hex numbers in
			// here
			currentLine = currentLine.replaceAll("\\$", "#");
			linesList.add(currentLine);
			// }
		}
		// lines = new String[linesList.size()];
		final Object[] tmp = linesList.toArray();
		Arrays.sort(tmp);
		for (final Object o : tmp) {
			out += (String) o;
		}
		out += "}\n";
		try {
			FileUtils.writeStringToFile(FileUtil.checkFile(dotFileName, false, false), out);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (currentConfig.isRunGraphs()) {
			final String clusterDepImageFileName = FilenameUtils.removeExtension(dotFileName)
					+ (showConcerns ? "_Concerns" : "") + "." + TopLevel.getCurrentConfig().getDotOutputFormat();

			// Kludgy experimental code, should be made general

			final String[] dotLayoutArgs2 = {
					currentConfig.getDotLayoutCommandDir() + File.separator + currentConfig.getDotLayoutCommand(), "-x",
					"-Goverlap=scale", "-T" + currentConfig.getDotOutputFormat(), dotFileName, "-o",
					clusterDepImageFileName };
			JobControlUtil.submitJob(dotLayoutArgs2);
		}
	}
}
