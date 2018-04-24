package edu.usc.softarch.arcade.relax;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileManagerTest {

	static FileManager fm = new FileManager();
	static String rootDir = File.separator + "topleveldir" + File.separator + "sourcedir" + File.separator + "projectdir";
	static String relativeFileName = "somefile";
	static String absoluteFileName = rootDir + File.separator + relativeFileName;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {

		fm.setRootDirectory(new File(File.separator + "topleveldir" + File.separator + "sourcedir" + File.separator + "projectdir"));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testRelativeFilenameToAbsoluteFilename() {
		final String expected = rootDir + File.separator + relativeFileName;
		Assert.assertEquals("Absolute version of somefile should be " + expected, expected, fm.relativeFilenameToAbsoluteFilename(relativeFileName));
	}

	@Test
	public void testAbsoluteFilenameToRelativeFilename() {
		final String expected = relativeFileName;
		Assert.assertEquals("Relative version of somefile should be " + expected, expected, fm.absoluteFilenameToRelativeFilename(absoluteFileName));
	}

}
