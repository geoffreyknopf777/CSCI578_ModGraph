package edu.usc.softarch.arcade.relax;

//import java.io.File;
//
//import cc.mallet.classify.Classifier;
//import edu.usc.softarch.arcade.util.mallet.Import;

public class Test {
	// static Classifier c;
	// static File cFile;
	// static String cFileName = "relax.classifier";
	// static String trainFileDir = "/Users/daniellink/Copy/mallet_training/java";
	// static Import i = new Import();

	// public static void main(final String[] args) {
	// cFile = FileUtil.checkFile("relax.classifier", false, false);
	// System.out.println("Trying to load classifier file " + cFile.getAbsolutePath());
	// try {
	// c = ClassifierUtils.loadClassifier(cFile);
	// } catch (final FileNotFoundException e) {
	// makeClassifier();
	// } catch (final ClassNotFoundException e) {
	// e.printStackTrace();
	// System.exit(-1);
	// } catch (final IOException e) {
	// e.printStackTrace();
	// System.exit(-1);
	// }
	// System.out.println("Loaded classifier successfully");
	// final ArchitecturalView av = new ArchitecturalView();
	// Import.setImportAV(av);
	// InstanceList instances = new InstanceList(new SerialPipes(i.preparePipeList(false, true)));
	// instances = Import.buildInstances(instances, FileUtil.checkDir("/Users/daniellink/Copy/RecProjects/chukwa_releases", false, true), false, true);
	// // System.out.println(instances);
	// // for (final Instance i : instances) {
	// // // System.out.println(i.getLabeling());
	// // System.out.println(i.getName());
	// // // System.out.println(i.getData());
	// // // labeling = c.classify(i).getLabeling();
	// // // for (int rank = 0; rank < labeling.numLocations(); rank++)
	// // {
	// // // System.out.print(labeling.getLabelAtRank(rank) + ":" +
	// // // labeling.getValueAtRank(rank) + " ");
	// // // }
	// // }
	//
	// // while (instances.hasNext()) {
	// // final Labeling labeling =
	// // classifier.classify(instances.next()).getLabeling();
	//
	// // print the labels with their weights in descending order (ie best
	// // first)
	// ClassifierUtils.printLabelingsDir(c, FileUtil.checkDir("/Users/daniellink/Copy/RecProjects/chukwa_releases", false, true));
	// System.out.println();
	// }

	// public static void makeClassifier() {
	// final Import i = new Import();
	// i.pipe = i.buildPipe();
	// final File trainingDirectory = FileUtil.checkDir(trainFileDir, false, true);
	// final InstanceList il = i.readDirectory(trainingDirectory);
	// final Classifier c = ClassifierUtils.trainClassifier(il);
	// try {
	// ClassifierUtils.saveClassifier(c, cFile);
	// } catch (final IOException e) {
	// System.out.println("Cannot save classifier to file " + cFile.getAbsolutePath());
	// System.exit(-1);
	// }
	// System.out.println("Saved classifier to file " + cFile.getAbsolutePath());
	// }
}
