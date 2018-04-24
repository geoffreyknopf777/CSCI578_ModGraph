package edu.usc.softarch.arcade.classgraphs;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import soot.PackManager;
import soot.Transform;

/**
 * @author joshua
 *
 */
public class ModifiedCallGraphExample {
	static HashSet<String> traversedMethodSet = new HashSet<String>();
	static final boolean DEBUG = false;
	static ClassGraph clg = new ClassGraph();

	public static void main(String[] args) {
		final List<String> argsList = new ArrayList<String>(Arrays.asList(args));
		argsList.addAll(Arrays.asList(new String[] { "-p", "cg", "verbose:true", "-w", "-main-class", "MTSGenerator.Generator_app",// main-class
				"MTSGenerator.Generator_app",// argument classes
				"DataTypes.Event",
				// "MTSGenerator.algorithm.FinalMTS_Generator" //
		}));

		final String stdOutFilename = "output.txt";
		final String stdErrFilename = "error.txt";

		redirectSystemOut(stdOutFilename);
		redirectSystemErr(stdErrFilename);

		// SootSetupTool.setupSootClassPath();

		PackManager.v().getPack("wjtp").add(new Transform("wjtp.myTrans", new ClassGraphTransformer()));

		args = argsList.toArray(new String[0]);

		soot.Main.main(args);
	}

	private static void redirectSystemErr(final String stdErrFilename) {
		try {
			System.setErr(new PrintStream(new FileOutputStream(stdErrFilename)));
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}

	}

	private static void redirectSystemOut(final String stdOutFilename) {
		try {
			System.setOut(new PrintStream(new FileOutputStream(stdOutFilename)));
		} catch (final FileNotFoundException e1) {
			e1.printStackTrace();
			System.exit(-1);
		}
	}

}
