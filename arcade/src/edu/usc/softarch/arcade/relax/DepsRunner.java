package edu.usc.softarch.arcade.relax;

import java.io.File;
import java.util.concurrent.Callable;

import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.facts.driver.DepsMaker;
import edu.usc.softarch.arcade.facts.driver.SourceToDepsBuilder;

public class DepsRunner implements Callable<SourceToDepsBuilder> {

	private SourceToDepsBuilder builder;
	private final String revisionNumber, classesDirName;
	private final File versionFolder, outputDir;

	public DepsRunner(final String revisionNumber, final File versionFolder, final File outputDir, final String classesDirName) {
		this.revisionNumber = revisionNumber;
		this.versionFolder = versionFolder;
		this.outputDir = outputDir;
		this.classesDirName = classesDirName;
	}

	@Override
	public SourceToDepsBuilder call() throws Exception {
		builder = DepsMaker.generate(revisionNumber, versionFolder, outputDir, classesDirName);

		if (builder.getEdges().size() == 0) {
			System.out.println("Builder has zero edges!");
			// return;
		}
		Config.getCurrentController().runPKGbatchPackager();
		return builder;
	}

}
