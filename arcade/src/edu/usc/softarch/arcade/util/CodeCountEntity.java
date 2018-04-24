package edu.usc.softarch.arcade.util;

/**
 * @author daniellink
 *
 */
public class CodeCountEntity {
	int totalLines;
	int blankLines;
	int wholeComments;
	int embeddedComments;
	int compilerDirectives;
	int dataDeclarations;
	int executableInstructions;
	int logicalSLOC;
	int physicalSLOC;
	String fileType;
	String moduleName;

	public int getTotalLines() {
		return totalLines;
	}

	public void setTotalLines(final int totalLines) {
		this.totalLines = totalLines;
	}

	public int getBlankLines() {
		return blankLines;
	}

	public void setBlankLines(final int blankLines) {
		this.blankLines = blankLines;
	}

	public int getWholeComments() {
		return wholeComments;
	}

	public void setWholeComments(final int wholeComments) {
		this.wholeComments = wholeComments;
	}

	public int getEmbeddedComments() {
		return embeddedComments;
	}

	public void setEmbeddedComments(final int embeddedComments) {
		this.embeddedComments = embeddedComments;
	}

	public int getCompilerDirectives() {
		return compilerDirectives;
	}

	public void setCompilerDirectives(final int compilerDirectives) {
		this.compilerDirectives = compilerDirectives;
	}

	public int getDataDeclarations() {
		return dataDeclarations;
	}

	public void setDataDeclarations(final int dataDeclarations) {
		this.dataDeclarations = dataDeclarations;
	}

	public int getExecutableInstructions() {
		return executableInstructions;
	}

	public void setExecutableInstructions(final int executableInstructions) {
		this.executableInstructions = executableInstructions;
	}

	public int getLogicalSLOC() {
		return logicalSLOC;
	}

	public void setLogicalSLOC(final int logicalSLOC) {
		this.logicalSLOC = logicalSLOC;
	}

	public int getPhysicalSLOC() {
		return physicalSLOC;
	}

	public void setPhysicalSLOC(final int physicalSLOC) {
		this.physicalSLOC = physicalSLOC;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(final String fileType) {
		this.fileType = fileType;
	}

	public String getModuleName() {
		return moduleName;
	}

	public void setModuleName(final String moduleName) {
		this.moduleName = moduleName;
	}

	public CodeCountEntity(final int totalLines, final int blankLines, final int wholeComments, final int embeddedComments, final int compilerDirectives, final int dataDeclarations,
			final int executableInstructions, final int logicalSLOC, final int physicalSLOC, final String fileType, final String moduleName) {
		this.totalLines = totalLines;
		this.blankLines = blankLines;
		this.wholeComments = wholeComments;
		this.embeddedComments = embeddedComments;
		this.compilerDirectives = compilerDirectives;
		this.dataDeclarations = dataDeclarations;
		this.executableInstructions = executableInstructions;
		this.logicalSLOC = logicalSLOC;
		this.physicalSLOC = physicalSLOC;
		this.fileType = fileType;
		this.moduleName = moduleName;
	}

}
