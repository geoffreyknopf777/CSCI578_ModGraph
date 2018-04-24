/**
 *
 */
package edu.usc.softarch.arcade.util;

import java.util.ArrayList;

/**
 * @author daniellink
 *
 */
public class JobControlUtil {
	public static ArrayList<Job> jobs = new ArrayList<>();

	public static void submitJob(final String[] jobArgs) {
		final Job newJob = new Job(jobArgs);
		jobs.add(newJob);
		newJob.run();
	}
}
