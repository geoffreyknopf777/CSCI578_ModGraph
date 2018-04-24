package edu.usc.softarch.arcade.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Recursive file listing under a specified directory.
 *
 * @author javapractices.com
 * @author Alex Wong
 * @author anonymous user
 */
public final class FileListing {

	/**
	 * Demonstrate use.
	 *
	 * @param aArgs
	 *            - <tt>aArgs[0]</tt> is the full name of an existing directory that can be read.
	 */
	public static void main(final String... aArgs) throws FileNotFoundException {
		final File startingDirectory = new File(aArgs[0]);
		final List<File> files = FileListing.getFileListing(startingDirectory);

		// print out all file names, in the the order of File.compareTo()
		for (final File file : files) {
			System.out.println(file);
		}
	}

	/**
	 * Recursively walk a directory tree and return a List of all Files found; the List is sorted using File.compareTo().
	 *
	 * @param aStartingDir
	 *            is a valid directory, which can be read.
	 */
	static public List<File> getFileListing(final File aStartingDir) throws FileNotFoundException {
		validateDirectory(aStartingDir);
		final List<File> result = getFileListingNoSort(aStartingDir, null);
		Collections.sort(result);
		return result;
	}

	/**
	 * Recursively walk a directory tree and return a List of files matching the FilenameFilter; the List is sorted using File.compareTo().
	 *
	 * @param aStartingDir
	 *            is a valid directory, which can be read.
	 */
	static public List<File> getFileListing(final File aStartingDir, final String extension) throws FileNotFoundException {
		validateDirectory(aStartingDir);
		final List<File> result = getFileListingNoSort(aStartingDir, extension);
		Collections.sort(result);
		return result;
	}

	// PRIVATE //
	// Crap code that finds directories when it should be finding files only - sad! :P
	// Use apache io commons fileutils instead
	static private List<File> getFileListingNoSort(final File aStartingDir, final String extension) throws FileNotFoundException {
		final List<File> result = new ArrayList<>();
		final File[] filesAndDirs = aStartingDir.listFiles();
		final List<File> filesDirs = Arrays.asList(filesAndDirs);
		for (final File file : filesDirs) {
			if (file.isHidden()) { // Do not find hidden files, such as DS_Store
				continue;
			}
			if (extension == null) {
				try {
					if (Files.isSymbolicLink(file.toPath())) { // if the file is
																// a symbolic
																// link
						if (file.getCanonicalFile().exists()) { // check if the
																// file exists
							result.add(file); // always add, even if directory
						} else {
							// don't add it
						}
					} else {
						result.add(file); // always add if not symbolic, even if
											// directory
					}
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			} else if (file.getName().endsWith(extension)) {
				result.add(file);
			}
			if (file.isDirectory()) {
				// must be a directory recursive call!
				final List<File> deeperList = getFileListingNoSort(file, extension);
				result.addAll(deeperList);
			}
		}
		return result;
	}

	/**
	 * Directory is valid if it exists, does not represent a file, and can be read.
	 */
	static private void validateDirectory(final File aDirectory) throws FileNotFoundException {
		if (aDirectory == null) {
			throw new IllegalArgumentException("Directory should not be null.");
		}
		if (!aDirectory.exists()) {
			throw new FileNotFoundException("Directory does not exist: " + aDirectory);
		}
		if (!aDirectory.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: " + aDirectory);
		}
		if (!aDirectory.canRead()) {
			throw new IllegalArgumentException("Directory cannot be read: " + aDirectory);
		}
	}

	/**
	 * Pretty print the directory tree and its file names.
	 *
	 * @param folder
	 *            must be a folder.
	 * @return
	 */
	public static String printDirectoryTree(final File folder) {
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("folder is not a Directory");
		}
		final int indent = 0;
		final StringBuilder sb = new StringBuilder();
		printDirectoryTree(folder, indent, sb);
		return sb.toString();
	}

	private static void printDirectoryTree(final File folder, final int indent, final StringBuilder sb) {
		if (!folder.isDirectory()) {
			throw new IllegalArgumentException("folder is not a Directory");
		}
		sb.append(getIndentString(indent));
		sb.append("+--");
		sb.append(folder.getName());
		sb.append("/");
		sb.append("\n");
		for (final File file : folder.listFiles()) {
			if (file.isDirectory()) {
				printDirectoryTree(file, indent + 1, sb);
			} else {
				printFile(file, indent + 1, sb);
			}
		}
	}

	private static void printFile(final File file, final int indent, final StringBuilder sb) {
		sb.append(getIndentString(indent));
		sb.append("+--");
		sb.append(file.getName());
		sb.append("\n");
	}

	private static String getIndentString(final int indent) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indent; i++) {
			sb.append("|  ");
		}
		return sb.toString();
	}
}
