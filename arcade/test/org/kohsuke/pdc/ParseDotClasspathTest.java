package org.kohsuke.pdc;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ParseDotClasspathTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testPushPull() {
		final String[] pushPullClasspath = { "/home/joshua/workspace/pushpull-0.2/.classpath" };

		try {
			ParseDotClasspath.main(pushPullClasspath);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Test
	public void testHadoopCore() {
		final String[] hadoopClasspath = { "/home/joshua/workspace/hadoop-0.20.2-core/.classpath" };
		try {
			ParseDotClasspath.main(hadoopClasspath);
		} catch (final Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
