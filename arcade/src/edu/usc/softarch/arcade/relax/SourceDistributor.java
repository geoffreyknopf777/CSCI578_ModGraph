/**
 *
 */
package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.FileUtils;

import edu.usc.softarch.arcade.util.FileUtil;

/**
 * @author daniellink
 *
 */
public class SourceDistributor {
	File originalDirectory;
	ArrayList<File> newRootDirs;
	LinkedHashMap<File, Integer> fileDistribution;
	int sections;
	static String sliceIndicator = "_SLICE_";
	Logger logger = org.apache.logging.log4j.LogManager.getLogger(SourceDistributor.class);

	public SourceDistributor(final File originalRoot, final ArrayList<String> fileList, final int numberOfDirs) {
		fileDistribution = new LinkedHashMap<File, Integer>();
		originalDirectory = originalRoot;
		sections = numberOfDirs;
		final Random rand = new Random();
		for (final String s : fileList) {
			logger.debug(s);
			final int randomNumber = rand.nextInt(numberOfDirs);
			logger.debug(randomNumber);
			fileDistribution.put(FileUtil.checkFile(s, false, true), randomNumber);
		}
		makeDirs();
	}

	public void makeDirs() {
		for (int i = 0; i < sections; i++) {
			final File newDir = new File(originalDirectory + sliceIndicator + i);
			try {
				FileUtils.mkdir(newDir, true);
			} catch (final IOException e) {
				e.printStackTrace();
			}
			try {
				org.apache.commons.io.FileUtils.copyDirectory(originalDirectory, newDir, new DistributionFilter(i), true);
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	public class DistributionFilter implements FileFilter {
		int currentSection;

		public DistributionFilter(final int section) {
			currentSection = section;
		}

		@Override
		public boolean accept(final File pathname) {
			if (!fileDistribution.containsKey(pathname)) {
				return true;
			}
			if (fileDistribution.get(pathname) == currentSection) {
				return true;
			}
			return false;
		}

	}

}
