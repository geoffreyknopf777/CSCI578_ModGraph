/**
 *
 */
package edu.usc.softarch.arcade.util;

/**
 * @author daniellink
 *
 */
public class Job {
	String[] args;
	boolean running;
	static int nextId = 0;
	int id;
	Thread thread;
	Runnable runnable;

	public Job(final String[] commandArgs) {
		args = commandArgs;
		id = nextId++;
	}

	public void run() {
		runnable = () -> {
			RunExtCommUtil.run(args);
		};
		thread = new Thread(runnable);
		thread.start();
	}
}
