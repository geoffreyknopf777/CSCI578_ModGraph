/**
 *
 */
package edu.usc.softarch.arcade.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * @author daniellink
 *
 */
public class RunExtCommUtil {

	/**
	 * @param args
	 */
	public static void run(final String[] args) { // Thread safe?
		String s = null;
		final Logger logger = LogManager.getLogger(RunExtCommUtil.class);
		logger.debug("Running external command: ");
		String commandString = "";

		for (final String st : args) {
			commandString += " " + st;
		}

		logger.debug(commandString);

		try {

			final Process p = Runtime.getRuntime().exec(args);

			final BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

			final BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			// read the output from the command
			logger.trace("Standard output of the command:\n");
			while ((s = stdInput.readLine()) != null) {
				logger.trace(s);
			}

			// read any errors from the attempted command
			logger.trace("Standard error of the command (if any):\n");
			while ((s = stdError.readLine()) != null) {
				logger.trace(s);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
