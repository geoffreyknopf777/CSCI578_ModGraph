package edu.usc.softarch.arcade.antipattern.detection;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;
import org.apache.logging.log4j.Logger;


import com.google.common.base.Joiner;
import com.thoughtworks.xstream.XStream;

import edu.usc.softarch.arcade.util.FileListing;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.LogUtil;
import edu.usc.softarch.arcade.util.MapUtil;

public class SmellToIssuesCorrelation {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(SmellToIssuesCorrelation.class);

	public static void main(final String[] args) throws FileNotFoundException {
		//PropertyConfigurator.configure(Config.getLoggingConfigFilename());
		LogUtil.printLogFiles();

		// inputDirFilename is the directory containing the .ser files which
		// contain detected smells
		// final String inputDirFilename = args[0];

		// location of the version2issuecountmap.obj file
		final String issuesCountMapFilename = args[1];

		final List<File> fileList = FileListing.getFileListing(FileUtil.checkDir(args[0], false, false));
		final Set<File> orderedSerFiles = new TreeSet<File>();
		for (final File file : fileList) {
			if (file.getName().endsWith(".ser")) {
				orderedSerFiles.add(file);
			}
		}

		// key: version, value: smells counts for the version
		Map<String, Integer> versionToSmellCount = new LinkedHashMap<String, Integer>();
		for (final File file : orderedSerFiles) {
			logger.debug(file.getName());
			final Set<Smell> smells = SmellUtil.deserializeDetectedSmells(file.getAbsolutePath());
			logger.debug("\tcontains " + smells.size() + " smells");

			logger.debug("\tListing detected smells for file" + file.getName() + ": ");
			for (final Smell smell : smells) {
				logger.debug("\t" + SmellUtil.getSmellAbbreviation(smell) + " " + smell);
			}

			// You may need to change the regular expression below to match the
			// versioning scheme of your project
			final Pattern p = Pattern.compile("[0-9]+\\.[0-9]+(\\.[0-9]+)*");
			final Matcher m = p.matcher(file.getName());
			String currentVersion = "";
			if (m.find()) {
				currentVersion = m.group(0);
			}
			versionToSmellCount.put(currentVersion, smells.size());
		}

		versionToSmellCount = MapUtil.sortByKeyVersion(versionToSmellCount);
		System.out.println("Smell counts for versions:");
		System.out.println(Joiner.on("\n").withKeyValueSeparator("=").join(versionToSmellCount));

		final List<Integer> smellCounts = new ArrayList<Integer>(versionToSmellCount.values());
		System.out.println("Smell counts only:");
		System.out.println(Joiner.on(",").join(smellCounts));
		final double[] smellCountsArr = new double[smellCounts.size()];
		for (int i = 0; i < smellCounts.size(); i++) {
			smellCountsArr[i] = smellCounts.get(i);
		}

		final XStream xstream = new XStream();
		final Map<String, Integer> issuesCountMap = extracted(FileUtil.checkFile(issuesCountMapFilename, false, false), xstream);
		System.out.println("Number of issues for each version:");
		System.out.println(Joiner.on("\n").withKeyValueSeparator("=").join(issuesCountMap));

		System.out.println("Keys of smell count map: ");
		System.out.println(Joiner.on("\n").join(versionToSmellCount.keySet()));

		final List<String> versions = new ArrayList<String>(versionToSmellCount.keySet());
		final double[] issueCountsArr = new double[smellCounts.size()];
		for (int i = 0; i < smellCounts.size(); i++) {
			issueCountsArr[i] = 0;
			if (versions.get(i).endsWith(".0")) {
				String currentVersion = versions.get(i);
				currentVersion = currentVersion.substring(0, currentVersion.lastIndexOf(".0"));
				if (issuesCountMap.get(currentVersion) != null) {
					issueCountsArr[i] += (double) issuesCountMap.get(currentVersion);
				}
			}
			if (issuesCountMap.get(versions.get(i)) != null) {
				issueCountsArr[i] += (double) issuesCountMap.get(versions.get(i));
			}
		}

		System.out.println("version, smell count, issue count");
		for (int i = 0; i < smellCounts.size(); i++) {
			System.out.println(versions.get(i) + ", " + smellCountsArr[i] + ", " + issueCountsArr[i]);
		}

		final PearsonsCorrelation pearsons = new PearsonsCorrelation();
		System.out.println("Pearson's correlation: " + pearsons.correlation(smellCountsArr, issueCountsArr));

		final SpearmansCorrelation spearmans = new SpearmansCorrelation();
		System.out.println("Spearman's correlation: " + spearmans.correlation(smellCountsArr, issueCountsArr));
	}

	private static Map<String, Integer> extracted(final File issuesCountMapFile, final XStream xstream) {
		return (Map<String, Integer>) xstream.fromXML(issuesCountMapFile);
	}
}
