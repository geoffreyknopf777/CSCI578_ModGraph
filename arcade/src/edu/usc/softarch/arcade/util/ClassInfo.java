/**
 *
 */
package edu.usc.softarch.arcade.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;

/**
 * @author daniellink
 *
 */
public class ClassInfo {

	private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(ClassInfo.class);

	public static String getClassFilePackage(final String fileName) {
		String returnString = null;
		final ClassParser cParser = new ClassParser(fileName);
		try {
			final JavaClass jClass = cParser.parse();
			returnString = jClass.getClassName();
		} catch (final ClassFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		}
		return returnString;
	}

	public static String getJavaFilePackage(final Path file) {
		logger.traceEntry(file.toString());
		if (Files.isDirectory(file)) {
			logger.trace("File is directory, no package info: " + file);
			return null;
		}
		CompilationUnit cu = null;
		try {
			cu = JavaParser.parse(file.toFile());
		} catch (final ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (final IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (null == cu) {
			System.err.println("Could not get compilation unit from file: " + file.toAbsolutePath());
			System.exit(-1);
		}
		// System.out.println("### Compilation unit start ###");
		// System.out.println(cu.toString());
		// System.out.println("### Compilation unit end ###");
		final PackageDeclaration p = cu.getPackage();
		// If the package for a java file is null, call it "default"
		if (null == p) {
			System.err.println("Package is null for file " + file.toAbsolutePath());
			// System.exit(-1);
			logger.traceExit();
			return "unnamed_package";
		}
		final String packageName = p.getName().toString();
		logger.traceExit();
		return packageName;

	}

	// public static String getCanonicalName(final Path file){
	// String packageName = getJavaFilePackage(file);
	// Path basePath=file.getFileName();
	// String baseName = basePath.toString();
	// return(packageName+baseName);
	// }

	public static String getJavaFilePackage(final String fileName) {
		final Path javaFile = new File(fileName).toPath();
		if (!Files.isReadable(javaFile)) {
			System.err.println("File does not exist when trying to check packages: " + javaFile);
			System.exit(-1);
		}
		try {
			if (Files.size(javaFile) == 0) {
				System.err.println("File length zero when trying to check pagackages:" + javaFile);
				System.exit(-1);
			}
		} catch (final IOException e1) {
			System.err.println("Unable to check file size when trying to check packages:" + javaFile);
			e1.printStackTrace();
		}
		String packageString = null;
		try {
			packageString = getJavaFilePackage(javaFile);
		} catch (final NullPointerException e) {
			System.err.println("Cannot get package from file: " + fileName);
			System.exit(-1);
		}
		return packageString;
	}
}
