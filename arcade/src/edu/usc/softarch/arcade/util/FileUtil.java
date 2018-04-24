package edu.usc.softarch.arcade.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.poi.util.IOUtils;

public class FileUtil {
	static Logger logger = org.apache.logging.log4j.LogManager.getLogger(FileUtil.class);

	private static final class ForwardVersionComparator implements Comparator<File> {
		@Override
		public int compare(final File o1, final File o2) {
			logger.entry(o1, o2);
			if (o1.isDirectory() || o2.isDirectory()) {// In case that we're supposed to directories, throw an
				throw new IllegalArgumentException("Cannot compare directories, files expected");
			}
			final String version1 = extractVersion(o1.getName());
			final String[] parts1 = version1.split("\\.");

			final String version2 = extractVersion(o2.getName());
			final String[] parts2 = version2.split("\\.");

			final int minLength = parts1.length > parts2.length ? parts2.length : parts1.length;
			for (int i = 0; i < minLength; i++) {
				try {
					final Integer part1 = Integer.parseInt(parts1[i]);
					final Integer part2 = Integer.parseInt(parts2[i]);
					final int compareToVal = part1.compareTo(part2);
					if (compareToVal != 0) {
						logger.debug("compareTo " + version1 + " to " + version2 + ": " + compareToVal);
						logger.traceExit();
						return compareToVal;
					}
				} catch (final NumberFormatException e) {
					logger.debug("Invalid part using string comparison for " + version1 + " to " + version2 + ": " + version1.compareTo(version2));
					logger.traceExit();
					return version1.compareTo(version2);
				}
			}
			logger.traceExit();
			return version1.compareTo(version2);
		}
	}

	// private static final class ReverseVersionComparator implements
	// Comparator<File> {
	// @Override
	// public int compare(File o1, File o2) {
	// String version1 = extractVersion(o1.getName());
	// String[] parts1 = version1.split("\\.");
	//
	// String version2 = extractVersion(o2.getName());
	// String[] parts2 = version2.split("\\.");
	//
	// int minLength = parts1.length > parts2.length ? parts2.length
	// : parts1.length;
	// for (int i = 0; i < minLength; i++) {
	// try {
	// Integer part1 = Integer.parseInt(parts1[i]);
	// Integer part2 = Integer.parseInt(parts2[i]);
	// int compareToVal = part1.compareTo(part2);
	// if (compareToVal != 0) {
	// System.out.println("compareTo " + version1 + " to "
	// + version2 + ": " + compareToVal);
	// return compareToVal;
	// }
	// } catch (NumberFormatException e) {
	// System.out
	// .println("Invalid part using string comparison for "
	// + version1
	// + " to "
	// + version2
	// + ": "
	// + version2.compareTo(version1));
	// return version2.compareTo(version1);
	// }
	// }
	// return version2.compareTo(version1);
	// }
	// }

	public static String extractFilenamePrefix(final String fileName) {
		logger.entry(fileName);
		logger.traceExit();
		return extractFilenamePrefix(FileUtil.checkFile(fileName, false, false));
	}

	public static String extractFilenamePrefix(final File theFile) {
		logger.entry(theFile);
		logger.traceExit();
		return FilenameUtils.getBaseName(theFile.getName());
	}

	public static String extractFilenameSuffix(final File theFile) {
		logger.entry(theFile);
		logger.traceExit();
		return FilenameUtils.getExtension(theFile.getName());
	}

	public static String readFile(final File file, final Charset encoding) {
		logger.entry(file, encoding);
		try {
			logger.traceExit();
			return FileUtils.readFileToString(file, encoding);
		} catch (final IOException e) {
			System.out.println("Uable to read " + file.getPath() + " into string - exiting");
			System.exit(-1);
		}
		// Not reachable but whatever - the syntax check demands it :)
		return null;
	}

	public static String getPackageNameFromJavaFile(final String filename) throws IOException {
		logger.entry(filename);
		final FileReader fr = new FileReader(filename);
		final BufferedReader reader = new BufferedReader(fr);

		String line = null;
		while ((line = reader.readLine()) != null) {
			final String packageName = findPackageName(line);
			reader.close();
			logger.traceExit();
			return packageName;
		}
		reader.close();
		logger.traceExit();
		return null;
	}

	public static String findPackageName(final String test1) {
		logger.entry(test1);
		final Pattern pattern = Pattern.compile("\\s*package\\s+(.+)\\s*;\\s*");
		final Matcher matcher = pattern.matcher(test1);
		if (matcher.find()) {
			logger.traceExit();
			return matcher.group(1).trim();
		}
		logger.traceExit();
		return null;
	}

	public static List<File> sortFileListByVersion(final List<File> inList) {
		logger.entry(inList);
		final List<File> outList = new ArrayList<>(inList);
		Collections.sort(outList, new ForwardVersionComparator());
		logger.traceExit();
		return outList;
	}

	public static String extractVersion(final String name) {
		logger.entry(name);
		final Pattern p = Pattern.compile("[0-9]+\\.[0-9]+(\\.[0-9]+)*");
		final Matcher m = p.matcher(name);
		if (m.find()) {
			logger.traceExit();
			return m.group(0);
		}
		logger.traceExit();
		return null;
	}

	public static String extractVersionPretty(final String name) {
		logger.entry(name);
		final Pattern p = Pattern.compile("[0-9]+\\.[0-9]+(\\.[0-9]+)*+(-(RC|ALPHA|BETA|M|Rc|Alpha|Beta|rc|alpha|beta|deb|b|a|final|Final|FINAL)[0-9]+)*");
		final Matcher m = p.matcher(name);
		if (m.find()) {
			logger.traceExit();
			return m.group(0);
		}
		logger.traceExit();
		return null;
	}

	public static String extractVersionFromFilename(final String versionSchemeExpr, final String filename) {
		logger.entry(versionSchemeExpr, filename);
		String version = "";
		final Pattern p = Pattern.compile(versionSchemeExpr);
		final Matcher m = p.matcher(filename);
		if (m.find()) {
			version = m.group(0);
			logger.trace(version + " is the version of " + filename);
		}
		logger.traceExit();
		return version;
	}

	/**
	 * Check if a directory exists
	 *
	 * @param dirName
	 *            - the directory
	 * @param create
	 *            - whether the directory should be created if it doesn't exist
	 * @param exitOnNoExist
	 *            - whether nonexistence of the directory should stop the show
	 * @return - the directory
	 */
	public static File checkDir(final String dirName, final boolean create, final boolean exitOnNoExist) {
		logger.entry(dirName, create, exitOnNoExist);
		final File f = new File(dirName);
		if (!f.isDirectory()) {
			logger.trace(dirName + " is not a directory - ");
			if (exitOnNoExist) {
				System.out.println("### Directory that must exist does not exist: " + dirName);
				System.out.println("Exiting");
				logger.trace("exiting");
				logger.traceExit();
				System.exit(-1);
			}
			if (create) {
				logger.trace("making - ");
				if (f.mkdirs()) {
					logger.trace(" succeeded");
				} else {
					logger.trace(" failed");
					logger.traceExit();
					System.out.println("### Could not create directory: " + dirName);
					System.out.println("Exiting");
					System.exit(-1);
				}
			}
		}
		logger.traceExit();
		return f;
	}

	/**
	 *
	 * @param fileName
	 *            - The name of the file
	 * @param create
	 *            - Create the file if it doesn't exist yet
	 * @param exitOnNoExist
	 *            - Exit if the file doesn't exist
	 * @return
	 */
	public static File checkFile(final String fileName, final boolean create, final boolean exitOnNoExist) {
		logger.entry(fileName, create, exitOnNoExist);
		final File f = new File(fileName);
		if (!f.exists()) {
			logger.trace(fileName + " does not exist");
			if (create) {
				logger.trace(" - making - ");
				try {
					if (f.createNewFile()) {
						logger.trace(" succeeded");
					} else {
						logger.trace(" failed ");
						logger.traceExit();
						System.out.println("### Could not create file: " + fileName);
						System.out.println("Exiting");
						System.exit(-1);
					}
				} catch (final IOException e) {
					logger.trace(" failed due to IOException");
					logger.traceExit();
					System.exit(-1);
				}
			} else if (exitOnNoExist) {
				logger.trace(" - exiting");
				System.out.println("### File that must exist does not exist: " + fileName);
				System.out.println("Exiting");
				System.exit(-1);
			}
		}
		logger.traceExit();
		return f;
	}

	public static FilenameFilter saneDirectoryFilter = (dir, name) -> {
		logger.traceEntry();
		final String lowercaseName = name.toLowerCase();
		if (lowercaseName.endsWith(".ds_store")) {
			logger.traceExit();
			return false;
		} else {
			logger.traceExit();
			return true;
		}
	};

	/**
	 * A class that implements the C FileFilter interface.
	 */
	public static class CFileFilter implements FileFilter {
		private final String[] okFileExtensions = new String[] { "c", "cpp", "cc", "s", "h", "hpp", "icc", "ia", "tbl", "p" };

		@Override
		public boolean accept(final File file) {
			logger.entry(file);
			for (final String extension : okFileExtensions) {
				if (file.getName().toLowerCase().endsWith("." + extension)) {
					logger.traceExit();
					return true;
				}
			}
			logger.traceExit();
			return false;
		}
	}

	public static String getMD5sum(final File inputFile) {
		String checksumString = null;
		FileInputStream inStream = null;
		try {
			inStream = new FileInputStream(inputFile);
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			checksumString = DigestUtils.md5Hex(IOUtils.toByteArray(inStream));
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return checksumString;
	}

	public static String getClassFileNameFromJavaSourceFileName(final String fileName) {
		return fileName.replace(".java", ".class");
	}
}
