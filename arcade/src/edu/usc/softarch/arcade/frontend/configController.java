/**
 * Sample Skeleton for 'ConfigDiag.fxml' Controller Class
 */

package edu.usc.softarch.arcade.frontend;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger;

import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.StoppingCriterionConfig;
import edu.usc.softarch.arcade.metrics.BatchSystemEvo;
import edu.usc.softarch.arcade.util.FileUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Window;

public class configController {

	Config currentConfig;
	private final Logger logger = org.apache.logging.log4j.LogManager.getLogger(configController.class);

	@FXML
	// ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML
	// URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML
	// fx:id="uccLocText"
	private TextField uccLocText; // Value injected by FXMLLoader

	@FXML
	// fx:id="uccCheck"
	private CheckBox uccCheck; // Value injected by FXMLLoader

	@FXML
	// fx:id="svgLocText"
	private TextField svgLocText; // Value injected by FXMLLoader

	@FXML
	// fx:id="svgCheck"
	private CheckBox svgCheck; // Value injected by FXMLLoader

	@FXML
	// fx:id="saveButton"
	private Button saveButton; // Value injected by FXMLLoader

	@FXML
	// fx:id="statusLabel"
	private Label statusLabel; // Value injected by FXMLLoader

	@FXML
	// fx:id="cancelButton"
	private Button cancelButton; // Value injected by FXMLLoader

	@FXML
	// fx:id="svgTooltipsCheck"
	private CheckBox svgTooltipsCheck; // Value injected by FXMLLoader

	@FXML
	// fx:id="teText"
	private TextField teText; // Value injected by FXMLLoader

	@FXML
	// fx:id="cfcText"
	private TextField cfcText; // Value injected by FXMLLoader

	@FXML
	// fx:id="numTopicsText"
	private TextField numTopicsText; // Value injected by FXMLLoader

	@FXML
	// fx:id="numClusText"
	private TextField numClusText; // Value injected by FXMLLoader

	@FXML
	// fx:id="numTopicsCheck"
	private CheckBox numTopicsCheck; // Value injected by FXMLLoader

	@FXML
	// fx:id="numClusCheck"
	private CheckBox numClusCheck; // Value injected by FXMLLoader

	@FXML
	// fx:id="radioClustergain"
	private RadioButton radioClustergain; // Value injected by FXMLLoader

	@FXML
	// fx:id="numClusters"
	private ToggleGroup numClusters; // Value injected by FXMLLoader

	@FXML
	// fx:id="radioPreselected"
	private RadioButton radioPreselected; // Value injected by FXMLLoader

	@FXML
	// fx:id="mIterationsText"
	private TextField mIterationsText; // Value injected by FXMLLoader

	@FXML
	// fx:id="mSeedText"
	private TextField mSeedText; // Value injected by FXMLLoader

	@FXML
	// fx:id="mThreadsText"
	private TextField mThreadsText; // Value injected by FXMLLoader

	@FXML
	// fx:id="mBetaText"
	private TextField mBetaText; // Value injected by FXMLLoader

	@FXML
	// fx:id="mTopwordsText"
	private TextField mTopwordsText; // Value injected by FXMLLoader

	@FXML // fx:id="superModelDirText"
	private TextField superModelDirText; // Value injected by FXMLLoader

	@FXML // fx:id="superModelCheck"
	private CheckBox superModelCheck; // Value injected by FXMLLoader

	@FXML // fx:id="relInputDirText"
	private TextField relInputDirText; // Value injected by FXMLLoader

	@FXML // fx:id="relClassifierFileText"
	private TextField relClassifierFileNameText; // Value injected by FXMLLoader

	@FXML // fx:id="relTrainButton"
	private Button relTrainButton; // Value injected by FXMLLoader

	@FXML // fx:id="relaxTrainingPortionText"
	private TextField relaxTrainingPortionText; // Value injected by FXMLLoader

	@FXML // fx:id="relaxClassifierAlgorithmComboBox"
	private ComboBox<String> relaxClassifierAlgorithmComboBox; // Value injected
																// by FXMLLoader

	@FXML // fx:id="relRandomSeedText"
	private TextField relRandomSeedText; // Value injected by FXMLLoader

	@FXML // fx:id="relOutputDirText"
	private TextField relOutputDirText; // Value injected by FXMLLoader

	@FXML // fx:id="relTrialsText"
	private TextField relTrialsText; // Value injected by FXMLLoader

	@FXML // fx:id="relVerbosityText"
	private TextField relVerbosityText; // Value injected by FXMLLoader

	@FXML // fx:id="layoutCBox"
	private ComboBox<String> layoutCBox; // Value injected by FXMLLoader

	@FXML // fx:id="outputCBox"
	private ComboBox<String> outputCBox; // Value injected by FXMLLoader

	@FXML // fx:id="dotParmsListView"
	private ListView<String> dotParmsListView; // Value injected by FXMLLoader

	@FXML // fx:id="dotDirText"
	private TextField dotDirText; // Value injected by FXMLLoader

	@FXML // fx:id="dotCheck"
	private CheckBox dotCheck; // Value injected by FXMLLoader

	@FXML // fx:id="metricsDirTbx"
	private TextField metricsDirTbx; // Value injected by FXMLLoader

	@FXML // fx:id="metricsParamCbx"
	private ComboBox<String> metricsParamCbx; // Value injected by FXMLLoader

	@FXML // fx:id="metricsRunButton"
	private Button metricsRunButton; // Value injected by FXMLLoader

	@FXML // fx:id="matchConfidenceText"
	private TextField matchConfidenceText; // Value injected by FXMLLoader

	@FXML
	void uccCheckClicked(final MouseEvent event) {
		updateControls();
	}

	@FXML
	void dotheckClicked(final MouseEvent event) {
		updateControls();
	}

	@FXML
	void svgCheckClicked(final MouseEvent event) {
		updateControls();
	}

	@FXML
	void uccLocClicked(final MouseEvent mouseEvent) {
		final File f = JavaFXHelper.getFileFromDialog(mouseEvent);
		if (null != f) {
			uccLocText.setText(f.getAbsolutePath());
		}
		updateControls();
	}

	@FXML
	void dotDirClicked(final MouseEvent mouseEvent) {
		System.out.println("Mouse clicked in dotLoc field...");
		final File f = JavaFXHelper.getFileFromDialog(mouseEvent);
		if (null != f) {
			dotDirText.setText(f.getAbsolutePath());
			currentConfig.setDotLayoutCommandDir(f.getAbsolutePath());
		}
		updateControls();
	}

	@FXML
	void dotCheckClicked(final MouseEvent event) {
		currentConfig.setRunGraphs(!currentConfig.isRunGraphs());
	}

	@FXML
	void svgLocClicked(final MouseEvent mouseEvent) {
		System.out.println("Mouse clicked in svgLoc field...");
		final File f = JavaFXHelper.getFileFromDialog(mouseEvent);
		if (null != f) {
			svgLocText.setText(f.getAbsolutePath());
		}
		updateControls();
	}

	@FXML
	void numClusRadioClicked(final Event event) {
		updateControls();
	}

	@FXML
	void uccLocEntered(final Event event) {
		checkEnteredLoc(uccLocText);
		updateControls();
	}

	@FXML
	void dotDirEntered(final Event event) {
		checkEnteredLoc(dotDirText);
		updateControls();
	}

	@FXML
	void svgLocEntered(final Event event) {
		checkEnteredLoc(svgLocText);
		updateControls();
	}

	void checkEnteredLoc(final TextField t) {
		statusLabel.setVisible(false);
		if (!edu.usc.softarch.arcade.util.FileUtil.checkFile(t.getText(), false, false).exists()) {
			statusLabel.setText("File does not exist: " + t.getText());
			statusLabel.setVisible(true);
		}
	}

	@FXML
	void teTextEntered(final Event event) {
		final double d = getDoubleEntered(teText);
		if (d != -1.0) {
			currentConfig.setArcTopicsEntitiesFactor(d);
		}
		updateControls();
	}

	@FXML
	void cfcTextEntered(final Event event) {
		final double d = getDoubleEntered(cfcText);
		if (d != -1.0) {
			currentConfig.setArcNumClustersFastClustersFactor(d);
		}
		updateControls();
	}

	@FXML
	void numClustersTextEntered(final Event event) {
		updateControls();
	}

	@FXML
	void numTopicsTextEntered(final Event event) {
		updateControls();
	}

	@FXML
	void mBetaTextEntered(final Event event) {
		try {
			currentConfig.setmBeta(Double.parseDouble(mBetaText.getText()));
		} catch (final NumberFormatException e) {
			// Don't do anything
		}
	}

	@FXML
	void mIterationsTextEntered(final Event event) {
		try {
			currentConfig.setmIterations(Integer.parseInt(mIterationsText.getText()));
		} catch (final NumberFormatException e) {
			// Don't do anything
		}
	}

	@FXML
	void mRandomSeedTextEntered(final Event event) {
		try {
			currentConfig.setmRandomSeed(Integer.parseInt(mSeedText.getText()));
		} catch (final NumberFormatException e) {
			// Don't do anything
		}
	}

	@FXML
	void mThreadsTextEntered(final Event event) {
		try {
			currentConfig.setmThreads(Integer.parseInt(mThreadsText.getText()));
		} catch (final NumberFormatException e) {
			// Don't do anything
		}
	}

	@FXML
	void mTopwordsTextEntered(final Event event) {
		try {
			currentConfig.setmTopWords(Integer.parseInt(mTopwordsText.getText()));
		} catch (final NumberFormatException e) {
			// Don't do anything
		}
	}

	@FXML
	void dotOutputFormatChanged(final Event event) {
		currentConfig.setDotOutputFormat(outputCBox.getSelectionModel().getSelectedItem());
	}

	@FXML
	void dotProgChanged(final Event event) {
		currentConfig.setDotLayoutCommand(layoutCBox.getSelectionModel().getSelectedItem());
	}

	double getDoubleEntered(final TextField t) {
		double d;
		final String s = t.getText();
		try {
			d = Double.parseDouble(s);
		} catch (final Exception e) {
			return -1.0;
		}
		return d;
	}

	@FXML
	void numClusCheckClicked(final Event event) {
		// final Config currentConfig = Controller.getCurrentView().getConfig();
		// currentConfig.setUseARCAbsoluteClustersNumber(!currentConfig.isUseARCAbsoluteClustersNumber());
		updateControls();
	}

	@FXML
	void numTopicsCheckClicked(final Event event) {
		// final Config currentConfig = Controller.getCurrentView().getConfig();
		// currentConfig.setUseARCAbsoluteTopicsNumber(!currentConfig.isUseARCAbsoluteTopicsNumber());
		updateControls();
	}

	@FXML
	void relClassifierNameClicked(final Event mouseEvent) {
		// System.out.println("Relax classifier file clicked");
		// if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
		// if (mouseEvent.getClickCount() == 2) {
		// System.out.println("Double clicked");
		// final FileChooser fc = new FileChooser();
		// final Node n = (Node) mouseEvent.getTarget();
		// final Scene s = n.getScene();
		// final Window w = s.getWindow();
		// final File f = fc.showOpenDialog(w);
		// if (null != f) {
		// currentConfig.setRelaxClassifierFile(f);
		// relClassifierFileText.setText(f.getAbsolutePath());
		// }
		// }
		// }
		currentConfig.setRelaxClassifierFileName(relClassifierFileNameText.getText());
	}

	@FXML
	void relInputDirClicked(final Event event) {
		logger.entry(event);
		MouseEvent mouseEvent;
		if (event instanceof MouseEvent) {
			mouseEvent = (MouseEvent) event;
			System.out.println("Relax training input dir clicked");
			if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
				if (mouseEvent.getClickCount() == 2) {
					System.out.println("Double clicked");
					final DirectoryChooser dc = new DirectoryChooser();
					final Node n = (Node) mouseEvent.getTarget();
					final Scene s = n.getScene();
					final Window w = s.getWindow();
					final File f = dc.showDialog(w);
					if (null != f && f.exists()) {
						currentConfig.setRelaxTrainingDir(f);
						System.out.println("Path = " + f.getAbsolutePath());
						relInputDirText.setText(f.getAbsolutePath());
					}
				}
			}
		}
		logger.traceExit();
	}

	@FXML
	void relTrainClicked(final Event event) {
		// Time to distinguish trained results
		final DateFormat dateFormat = new SimpleDateFormat("MM/dd-HH:mm:ss");
		// get current date time with Date()
		final Date date = new Date();
		final String dt = dateFormat.format(date);
		System.out.println(dt);

		logger.entry(event);
		System.out.println("Relax training button clicked");
		// final DirectoryChooser dc = new DirectoryChooser();
		// final Node n = (Node) event.getTarget();
		// final Scene s = n.getScene();
		// final Window w = s.getWindow();
		// final File f = dc.showDialog(w);
		final File f = FileUtil.checkFile(relOutputDirText.getText(), false, true);
		if (null != f) {
			final File classifierFile = new File(f.getAbsolutePath() + File.separator + currentConfig.getRelaxClassifierFileName());
			logger.debug("Classifier File = " + classifierFile.getAbsolutePath());
			final String importFileName = f.getAbsolutePath() + File.separator + "relax.mallet";
			final File importFile = new File(importFileName);
			logger.debug("Import File = " + importFile.getAbsolutePath());
			logger.debug("*** Beginning Mallet Import ****");
			final String importCommandLine = currentConfig.getMalletExecutable().toString() + " import-dir --input " + currentConfig.getRelaxTrainingDirName() + File.separator + "* --output "
					+ importFileName + " --remove-stopwords";// --print-output
																// TRUE";//
																// --binary-features";
			logger.debug("Mallet command = " + importCommandLine);
			final Runtime rt = Runtime.getRuntime();
			try {
				final Process p = rt.exec(importCommandLine);
				logger.debug("*** stdout:");
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = in.readLine()) != null) {
					System.out.println(line);
				}
				logger.debug("*** stderr:");
				in = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				while ((line = in.readLine()) != null) {
					System.out.println(line);
				}

			} catch (final IOException e) {
				logger.debug("Cannot execute mallet import command: " + importCommandLine);
				System.exit(-1);
			}
			// instances = Import.buildInstancesGeneralFiles(instances, f);
			logger.debug("*** Finished Mallet Import ***");
			logger.debug("*** Beginning Mallet Training ***");
			final String trainingCommandLine = currentConfig.getMalletExecutable().toString() + " train-classifier --input " + importFileName + " --output-classifier " + classifierFile + " --trainer "
					+ relaxClassifierAlgorithmComboBox.getSelectionModel().getSelectedItem() + " --training-portion " + (double) currentConfig.getRelaxTrainingPortion() / 100 + " --random-seed "
					+ currentConfig.getRelaxRandomSeed() + " --verbosity " + currentConfig.getRelaxVerbosity() + " --num-trials " + currentConfig.getRelaxTrials();
			logger.debug("Mallet classifier command: " + trainingCommandLine);

			try {
				final Process p = rt.exec(trainingCommandLine);
				// outer.txt represents the values of accuracy of all trainers.
				// so delete a previously stored accuracy set before running
				// rt.exec("rm outer.txt");
				// final File newfile = new File("/home/prithvi/outer.txt");
				// outputfile is the output with statistics on best classifier
				// and its accuracy
				// final File outfile = new
				// File("/home/prithvi/outputFile.txt");
				logger.debug("*** stdout:");
				final BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				// input for regex expression is stored in regstr
				// String regStr = "";
				// final FileWriter fw = new FileWriter(newfile);
				while ((line = in.readLine()) != null) {
					logger.debug(line);

					// regStr += line;
				}
				// }
				// using regex to extract accuracy values of all trainers
				// final Pattern pat = Pattern.compile("test data accuracy=
				// (\\d+\\.\\d+)");
				// final Matcher m = pat.matcher(regStr);
				// int numTrials = 0;
				// // extracting and writing the the accuracy values to file
				// // outer.txt
				// while (m.find()) {
				//
				// fw.write(m.group(1));
				// fw.write("\n");
				// numTrials++;
				// }
				// String accu = null;
				//
				// fw.flush();
				// fw.close();
				// final FileReader fileReader = new FileReader(newfile);
				// final BufferedReader bufferedReader = new
				// BufferedReader(fileReader);
				//
				// final double[] arr = new double[numTrials];
				// int i = 0;
				// // reading for outer.txt and converting to double and storing
				// in
				// // array for finding max among accuracy values
				// while ((accu = bufferedReader.readLine()) != null) {
				// arr[i] = Double.parseDouble(accu);
				// i++;
				//
				// }
				// int j = 0;
				// double max = arr[0];
				// int maxind = 0;
				// final BufferedWriter bestTrainer = new BufferedWriter(new
				// FileWriter(outfile, true));
				// bestTrainer.write("\n");
				// bestTrainer.write("***** Training done at:" + dt + " *****");
				// bestTrainer.write("\n");
				// // for loop to iterate the accuracy array and find the max
				// // accuracy and the trainer index
				// for (j = 0; j < numTrials; j++) {
				// bestTrainer.write("Trainer " + j + " accuracy is :" +
				// arr[j]);
				// bestTrainer.write("\n");
				// if (max < arr[j]) {
				// maxind = j;
				// max = arr[j];
				// }
				//
				// }
				// bufferedReader.close();
				// // writing the output in outputfile.txt
				//
				// bestTrainer.write("Best Trainer is Trainer " + maxind + " and
				// it's accuracy is : " + max + "\n");
				// bestTrainer.write("*****END*****");
				// bestTrainer.write("\n");
				// bestTrainer.close();
				//
				// System.out.println("best accuracy is" + max + "trainer is" +
				// maxind);
				//
				// // Always close files.
				//
				// logger.debug("*** stderr:");
				// in = new BufferedReader(new
				// InputStreamReader(p.getErrorStream()));
				// while ((line = in.readLine()) != null) {
				// logger.debug(line);
				//
				// }

			} catch (final IOException e) {
				logger.debug("Cannot execute mallet training command: " + trainingCommandLine);
				System.exit(-1);
				// e.printStackTrace();
			}

			logger.debug("*** Finished Mallet Training ***");
		}
		logger.traceExit();
	}

	@FXML
	void relTraPoClicked(final KeyEvent event) {
		logger.entry(event);
		try {
			final int fractionPercentage = Integer.parseInt(relaxTrainingPortionText.getText());
			currentConfig.setRelaxTrainingPortion(fractionPercentage);
		} catch (final java.lang.NumberFormatException e) {
			// Do nothing
			logger.debug("Cannot parse Integer!");
		}
		logger.traceExit();
	}

	@FXML
	void relTrialsClicked(final Event event) {
		logger.entry(event);
		final String trialsString = relTrialsText.getText();
		if (!trialsString.isEmpty()) {
			final int trials = Integer.parseInt(trialsString);
			currentConfig.setRelaxTrials(trials);
		}
		logger.traceExit();
	}

	@FXML
	void relVerbosityClicked(final Event event) {
		logger.entry(event);
		final String verbosityString = relVerbosityText.getText();
		if (!verbosityString.isEmpty()) {
			final int verbosity = Integer.parseInt(verbosityString);
			currentConfig.setRelaxVerbosity(verbosity);
		}
		logger.traceExit();
	}

	@FXML
	void relOutputDirClicked(final Event event) {
		logger.entry(event);
		MouseEvent mouseEvent;
		if (event instanceof MouseEvent) {
			mouseEvent = (MouseEvent) event;
			System.out.println("Relax training output dir clicked");
			if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
				if (mouseEvent.getClickCount() == 2) {
					logger.debug("Double clicked");
					final DirectoryChooser dc = new DirectoryChooser();
					final Node n = (Node) event.getTarget();
					final Scene s = n.getScene();
					final Window w = s.getWindow();
					final File f = dc.showDialog(w);
					if (null != f && f.exists()) {
						currentConfig.setRelaxOutputDir(f);
						System.out.println("Path = " + f.getAbsolutePath());
						relOutputDirText.setText(f.getAbsolutePath());
					}
				}
			}
		}
		logger.traceExit();
	}

	@FXML
	void relRandomSeedClicked(final Event event) {
		logger.entry(event);
		final String seedString = relRandomSeedText.getText();
		if (!seedString.isEmpty()) {
			final int seed = Integer.parseInt(seedString);
			currentConfig.setRelaxRandomSeed(seed);
		}
		logger.traceExit();
	}

	@FXML
	void relaxAlgorithmChanged(final ScrollEvent event) {

	}

	@FXML
	void svgTooltipCheckClicked(final Event event) {
		updateControls();
	}

	@FXML
	void vDistChanged(final ScrollEvent event) {

	}

	@FXML
	void metricsDirClicked(final MouseEvent event) {
		logger.entry(event);

		if (event.getButton().equals(MouseButton.PRIMARY)) {
			if (event.getClickCount() == 2) {
				logger.debug("Double clicked");
				final DirectoryChooser dc = new DirectoryChooser();
				final Node n = (Node) event.getTarget();
				final Scene s = n.getScene();
				final Window w = s.getWindow();
				final File f = dc.showDialog(w);
				if (null != f && f.exists()) {
					currentConfig.setMetricsDir(f.getAbsolutePath());
					metricsDirTbx.setText(f.getAbsolutePath());
				}
			}
		}
		logger.traceExit();
	}

	@FXML
	void metricsRunClicked(final MouseEvent event) {
		final String[] commandArray = { "-distopt", metricsParamCbx.getSelectionModel().getSelectedItem(), metricsDirTbx.getText() };
		try {
			BatchSystemEvo.main(commandArray);
		} catch (final FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@FXML
	void superCheckClicked(final Event event) {
		currentConfig.setUseSuperModel(superModelCheck.isSelected());
		updateControls();
	}

	@FXML
	void selectedDirClicked(final MouseEvent mouseEvent) {
		System.out.println("Selected dir clicked");
		if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
			if (mouseEvent.getClickCount() == 2) {
				System.out.println("Double clicked");
				final DirectoryChooser dc = new DirectoryChooser();
				final Node n = (Node) mouseEvent.getTarget();
				final Scene s = n.getScene();
				final Window w = s.getWindow();
				final File f = dc.showDialog(w);
				if (null != f) {
					currentConfig.setSuperModelSelectedVersionsDir(f);
					superModelDirText.setText(f.getAbsolutePath());
				}
			}
		}
	}

	@FXML
	void matchConfidenceEntered(final Event event) {
		final Double newConfidence = Double.parseDouble(matchConfidenceText.getText());
		currentConfig.setMatchConfidence(newConfidence);
	}

	@FXML
	void cancelClicked(final Event event) {
		updateControls();
		Controller.configStage.close();
	}

	@FXML
	void saveClicked(final Event event) {
		updateControls();

		try {
			Config.setUccLoc(uccLocText.getText());
			currentConfig.setRunUCC(uccCheck.isSelected());
			currentConfig.setDotLayoutCommandDir(dotDirText.getText());
			currentConfig.setRunGraphs(dotCheck.isSelected());
			currentConfig.setGraphOpenerLoc(svgLocText.getText());
			currentConfig.setArcAbsoluteClustersNumber(Integer.parseInt(numClusText.getText()));
			currentConfig.setUseARCAbsoluteClustersNumber(numClusCheck.isSelected());
			currentConfig.setArcAbsoluteTopicsNumber(Integer.parseInt(numTopicsText.getText()));
			currentConfig.setUseARCAbsoluteTopicsNumber(numTopicsCheck.isSelected());
			currentConfig.setShowSvgToolTips(svgTooltipsCheck.isSelected());
			currentConfig.setDotLayoutCommand(layoutCBox.getSelectionModel().getSelectedItem());
			currentConfig.setDotOutputFormat(outputCBox.getSelectionModel().getSelectedItem());

			if (radioClustergain.isSelected()) {
				currentConfig.setInstanceStoppingCriterion(StoppingCriterionConfig.clustergain);
			}
			if (radioPreselected.isSelected()) {
				currentConfig.setInstanceStoppingCriterion(StoppingCriterionConfig.preselected);
			}
		} catch (final NullPointerException e) {
			System.out.println("Unable to read a config setting");
		}

		final ArchitecturalView cView = Controller.getCurrentView();
		final Config c = cView.getConfig();
		c.writeConfigToFile();
		Controller.configStage.close();
	}

	/**
	 * This must ONLY be done once, not after changes!
	 */
	void initializeControls() {
		currentConfig = Controller.getCurrentView().getConfig();
		uccLocText.setText(Config.getUccLoc());
		dotDirText.setText(currentConfig.getDotLayoutCommandDir());
		svgLocText.setText(currentConfig.getGraphOpenerLoc());
		uccCheck.setDisable(!currentConfig.isRunUCC());
		dotCheck.setDisable(!currentConfig.isRunGraphs());
		svgCheck.setDisable(!currentConfig.isViewGraphs());
		teText.setText(Double.toString(currentConfig.getArcTopicsEntitiesFactor()));
		cfcText.setText(Double.toString(currentConfig.getArcNumClustersFastClustersFactor()));
		numTopicsText.setText(Integer.toString(currentConfig.getArcAbsoluteTopicsNumber()));
		numTopicsText.setDisable(!currentConfig.isUseARCAbsoluteTopicsNumber());
		numTopicsCheck.setSelected(currentConfig.isUseARCAbsoluteTopicsNumber());
		numClusText.setText(Integer.toString(currentConfig.getArcAbsoluteClustersNumber()));
		numClusText.setDisable(!currentConfig.isUseARCAbsoluteClustersNumber());
		numClusCheck.setSelected(currentConfig.isUseARCAbsoluteClustersNumber());
		svgTooltipsCheck.setSelected(currentConfig.isShowSvgToolTips());
		radioClustergain.setSelected(currentConfig.getInstanceStoppingCriterion().equals(StoppingCriterionConfig.clustergain));
		radioPreselected.setSelected(currentConfig.getInstanceStoppingCriterion().equals(StoppingCriterionConfig.preselected));
		mIterationsText.setText(Integer.toString(currentConfig.getmIterations()));
		mSeedText.setText(Integer.toString(currentConfig.getmRandomSeed()));
		mThreadsText.setText(Integer.toString(currentConfig.getmThreads()));
		mBetaText.setText(Double.toString(currentConfig.getmBeta()));
		mTopwordsText.setText(Integer.toString(currentConfig.getmTopWords()));
		superModelCheck.setSelected(currentConfig.isUseSuperModel());
		superModelDirText.setDisable(!superModelCheck.isSelected());
		superModelDirText.setText(currentConfig.getSuperModelSelectedVersionsDir().toString());
		relClassifierFileNameText.setText(currentConfig.getRelaxClassifierFileName());
		relInputDirText.setText(currentConfig.getRelaxTrainingDirName());
		relaxTrainingPortionText.setText(Integer.toString(currentConfig.getRelaxTrainingPortion()));
		relOutputDirText.setText(currentConfig.getRelaxOutputDir().toString());
		relRandomSeedText.setText(Integer.toString(currentConfig.getRelaxRandomSeed()));
		relTrialsText.setText(Integer.toString(currentConfig.getRelaxTrials()));
		relVerbosityText.setText(Integer.toString(currentConfig.getRelaxVerbosity()));
		layoutCBox.getSelectionModel().select(currentConfig.getDotLayoutCommand());
		outputCBox.getSelectionModel().select(currentConfig.getDotOutputFormat());
		metricsDirTbx.setText(currentConfig.getMetricsDirName());
		metricsParamCbx.getSelectionModel().select(currentConfig.getvDist());
		matchConfidenceText.setText(Double.toString(currentConfig.getMatchConfidence()));

		relaxClassifierAlgorithmComboBox.getSelectionModel().select("NaiveBayes");

		// relaxClassifierAlgorithmComboBox.getItems().clear();

		// relaxClassifierAlgorithmComboBox.getItems().addAll("jacob.smith@example.com",
		// "isabella.johnson@example.com", "ethan.williams@example.com",
		// "emma.jones@example.com",
		// "michael.brown@example.com");

		final ArrayList<String> algorithmsList = new ArrayList<>();
		final String[] algorithmsArray = { "NaiveBayes", "MaxEnt", "C45", "DecisionTree" };
		for (final String s : algorithmsArray) {
			algorithmsList.add(s);
		}
		final ObservableList<String> items = FXCollections.observableArrayList(algorithmsList);
		relaxClassifierAlgorithmComboBox.setItems(items);

		final ArrayList<String> vDistList = new ArrayList<>();
		for (int i = 1; i < 4; i++) {
			vDistList.add(Integer.toString(i));
		}
		final ObservableList<String> vDistItems = FXCollections.observableArrayList(vDistList);
		metricsParamCbx.setItems(vDistItems);

		// relaxClassifierAlgorithmComboBox.getSelectionModel().select(0);

		final ObservableList<String> dotPrograms = FXCollections.observableArrayList(Arrays.asList("dot", "neato", "sfdp", "fdp", "twopi", "circo"));

		final ObservableList<String> dotOutputFormats = FXCollections
				.observableArrayList(Arrays.asList("bmp", "canon", "cgimage", "cmap", "cmapx", "cmapx_np", "dot", "eps", "exr", "fig", "gif", "gv", "icns", "ico", "imap", "imap_np", "ismap", "jp2",
						"jpe", "jpeg", "jpg", "pct", "pdf", "pic", "pict", "plain", "plain-ext", "png", "pov", "ps", "ps2", "psd", "sgi", "svg", "svgz", "tga", "tif", "tiff", "tk", "vml"));
		// vmlz xdot xdot1.2 xdot1.4"))

		layoutCBox.setItems(dotPrograms);
		// layoutCBox.getSelectionModel().select("neato");
		outputCBox.setItems(dotOutputFormats);
		// outputCBox.getSelectionModel().select("pdf");
		// relaxClassifierAlgorithmComboBox = new ComboBox<>(options);

		// this.relaxClassifierAlgorithmComboBox=new ComboBox();

		// relaxClassifierAlgorithmComboBox.getItems().addAll("Option4",
		// "Option5", "Option6");

		// System.out.println(this.relaxClassifierAlgorithmComboBox.getItems().);

		// Have controls be consistent with what is enabled
		updateControls();
	}

	/**
	 * Update controls for consistency
	 */
	void updateControls() {

		// First Tab
		uccLocText.setDisable(!uccCheck.isSelected());
		dotDirText.setDisable(!dotCheck.isSelected());
		svgLocText.setDisable(!svgCheck.isSelected());

		// Second Tab

		if (radioClustergain.isSelected()) {
			numClusCheck.setDisable(true);
			numClusText.setDisable(true);
			cfcText.setDisable(true);
		}
		if (radioPreselected.isSelected()) {
			numClusCheck.setDisable(false);
			numClusText.setDisable(false);
			cfcText.setDisable(false);
			numClusText.setDisable(!numClusCheck.isSelected());
			cfcText.setDisable(numClusCheck.isSelected());
		}

		numTopicsText.setDisable(!numTopicsCheck.isSelected());
		teText.setDisable(numTopicsCheck.isSelected());

		superModelDirText.setDisable(!superModelCheck.isSelected());

	}

	@FXML // This method is called by the FXMLLoader when initialization is
			// complete
	void initialize() {
		assert uccLocText != null : "fx:id=\"uccLocText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert uccCheck != null : "fx:id=\"uccCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert dotDirText != null : "fx:id=\"dotDirText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert dotCheck != null : "fx:id=\"dotCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert svgLocText != null : "fx:id=\"svgLocText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert svgCheck != null : "fx:id=\"svgCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert statusLabel != null : "fx:id=\"statusLabel\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert svgTooltipsCheck != null : "fx:id=\"svgTooltipsCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert layoutCBox != null : "fx:id=\"layoutCBox\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert outputCBox != null : "fx:id=\"outputCBox\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert dotParmsListView != null : "fx:id=\"dotParmsListView\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert teText != null : "fx:id=\"teText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert cfcText != null : "fx:id=\"cfcText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert numTopicsText != null : "fx:id=\"numTopicsText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert numClusText != null : "fx:id=\"numClusText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert numTopicsCheck != null : "fx:id=\"numTopicsCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert numClusCheck != null : "fx:id=\"numClusCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert radioClustergain != null : "fx:id=\"radioClustergain\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert numClusters != null : "fx:id=\"numClusters\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert radioPreselected != null : "fx:id=\"radioPreselected\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert superModelDirText != null : "fx:id=\"superModelDirText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert superModelCheck != null : "fx:id=\"superModelCheck\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert mIterationsText != null : "fx:id=\"mIterationsText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert mSeedText != null : "fx:id=\"mSeedText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert mThreadsText != null : "fx:id=\"mThreadsText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert mBetaText != null : "fx:id=\"mBetaText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert mTopwordsText != null : "fx:id=\"mTopwordsText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relInputDirText != null : "fx:id=\"relInputDirText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relClassifierFileNameText != null : "fx:id=\"relClassifierFileNameText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relTrainButton != null : "fx:id=\"relTrainButton\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relaxClassifierAlgorithmComboBox != null : "fx:id=\"relaxClassifierAlgorithmComboBox\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relaxTrainingPortionText != null : "fx:id=\"relaxTrainingPortionText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relRandomSeedText != null : "fx:id=\"relRandomSeedText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relOutputDirText != null : "fx:id=\"relOutputDirText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relVerbosityText != null : "fx:id=\"relVerbosityText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert relTrialsText != null : "fx:id=\"relTrialsText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert saveButton != null : "fx:id=\"saveButton\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert cancelButton != null : "fx:id=\"cancelButton\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert metricsDirTbx != null : "fx:id=\"metricsDirTbx\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert metricsParamCbx != null : "fx:id=\"metricsParamCbx\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert metricsRunButton != null : "fx:id=\"metricsRunButton\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		assert matchConfidenceText != null : "fx:id=\"matchConfidenceText\" was not injected: check your FXML file 'ConfigDiag.fxml'.";
		initializeControls();
	}
}
