/**
 *
 */
package edu.usc.softarch.arcade.util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import edu.usc.softarch.arcade.relax.TopLevel;

/**
 * @author daniellink
 *
 */
public class CodeCountSingleFile {

	private static HashMap<File, Integer> logicalSLOCMap;

	public static int getLogicalSLOC(final File f) {
		if (null == logicalSLOCMap) {
			// readUCCResultsFile(new File("/Users/daniellink/Desktop/TOTAL_outfile.txt"));// remove
			// hard
			// coding
		}
		// Make a file list with just this file
		// Run UCC on that file list
		// Get the SLOC out of that file
		// Delete UCC output files
		final Integer sloc = 999;
		// final Integer sloc = logicalSLOCMap.get(f);
		// if (null == sloc) {
		// return 0;
		// } else {
		return sloc;
		// }
	}

	public static void readUCCResultsFile(final File f) {
		logicalSLOCMap = new HashMap<File, Integer>();
		List<String> lines = null;
		try {
			lines = FileUtils.readLines(f, "UTF-8");
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (final String line : lines) {
			if (!line.contains("CODE")) {
				continue;
			}
			final String fileName = line.split("CODE")[1].trim();
			if (!new File(fileName).exists()) {
				continue;
			}
			final File sourceFile = new File(TopLevel.getfManager().absoluteFilenameToRelativeFilename(fileName));
			final String chunk = line.split("\\|")[3].trim();
			final String slocString = chunk.split("\\s+")[1].trim();
			final int logSLOC = Integer.parseInt(slocString);
			logicalSLOCMap.put(sourceFile, logSLOC);
		}
	}
}
