package edu.usc.softarch.arcade.frontend;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.thoughtworks.xstream.XStream;

import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.config.Config.Language;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.RecoveryParams;
import edu.usc.softarch.arcade.util.RunExtCommUtil;
import edu.usc.softarch.arcade.util.StringUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Created by daniellink on 3/12/15.
 */
public class Controller {
	public static Logger logger = LogManager.getLogger(Controller.class);
	public OutputStream r_stdout, r_stderr;
	public final PrintStream std_stdout = System.out;
	public final PrintStream std_stderr = System.err;
	public formData currentForm;
	public  final String[] Rec_methods = { "relax", "arc", "acdc", "pkg", "bunch", "limbo", "wca" };
	public  final ArrayList<String> recoveryMethodsList = new ArrayList<>();
	// private final ArrayList<ArchitecturalView> views = new
	// ArrayList<ArchitecturalView>();
	public static ArchitecturalView currentView;
	public static Stage configStage;
	public  Config currentConfig;
	public  static File formDataFile;
	public  Thread pkgThread;
	public  String currentOutputDirName = "";

	// @FXML
	// ResourceBundle that was given to the FXMLLoader
	// private ResourceBundle resources;

	// @FXML
	// URL location of the FXML file that was given to the FXMLLoader
	// private URL location;

	@FXML
	// fx:id="subDirText"
	public TextField subDirText; // Value injected by FXMLLoader

	@FXML
	// fx:id="stdoutLabel"
	public Label stdoutLabel; // Value injected by FXMLLoader

	@FXML
	// fx:id="inputDirText"
	public TextField inputDirText; // Value injected by FXMLLoader

	@FXML
	// fx:id="radioC"
	public RadioButton radioC; // Value injected by FXMLLoader

	@FXML
	// fx:id="captureOuputCheck"
	public CheckBox captureOuputCheck; // Value injected by FXMLLoader

	@FXML
	// fx:id="runButton"
	public Button runButton; // Value injected by FXMLLoader

	@FXML
	// fx:id="consoleText"
	public TextArea consoleText; // Value injected by FXMLLoader

	@FXML
	// fx:id="logoView"
	public ImageView logoView; // Value injected by FXMLLoader

	@FXML
	// fx:id="substringText"
	public TextField substringText; // Value injected by FXMLLoader

	@FXML
	// fx:id="outputDirText"
	public TextField outputDirText; // Value injected by FXMLLoader

	@FXML
	// fx:id="statusLabel"
	public Label statusLabel; // Value injected by FXMLLoader

	@FXML
	// fx:id="errorText"
	public TextArea errorText; // Value injected by FXMLLoader

	@FXML
	// fx:id="stderrLabel"
	public Label stderrLabel; // Value injected by FXMLLoader

	@FXML
	// fx:id="closeButton"
	public MenuItem closeButton; // Value injected by FXMLLoader

	@FXML
	// fx:id="recoveryLanguages"
	public ToggleGroup recoveryLanguages; // Value injected by FXMLLoader

	@FXML
	// fx:id="radioJava"
	public RadioButton radioJava; // Value injected by FXMLLoader

	@FXML
	// fx:id="mainPane"
	public Pane mainPane; // Value injected by FXMLLoader

	@FXML
	// fx:id="fileMenu"
	public Menu fileMenu; // Value injected by FXMLLoader

	@FXML
	// fx:id="methodListview"
	public ListView<String> methodListview; // Value injected by FXMLLoader

	@FXML
	// fx:id="mainMenuBar"
	public MenuBar mainMenuBar; // Value injected by FXMLLoader

	@FXML // fx:id="relClassifierText"
	public TextField relClassifierText; // Value injected by FXMLLoader

	@FXML // fx:id="prefMenu"
	public MenuItem prefMenu; // Value injected by FXMLLoader

	@FXML // fx:id="recLabel"
	public Label recLabel; // Value injected by FXMLLoader

	@FXML // fx:id="secondLabel"
	public Label secondLabel; // Value injected by FXMLLoader

	@FXML // fx:id="visLabel"
	public Label visLabel; // Value injected by FXMLLoader

	@FXML // fx:id="auxLabel"
	public Label auxLabel; // Value injected by FXMLLoader

	@FXML // fx:id="devButton"
	public Button devButton; // Value injected by FXMLLoader

	public Controller() {
		logger.traceEntry();
		Config.setCurrentController(this);
		logger.traceExit();
	}

	public class formData { // implements Serializable {
		/**
		 *
		 */
		// private static final long serialVersionUID = -5419437480641919771L;

		private File inputDir;
		private String subDir;// String because it's a basename

		public String getSubDir() {
			return subDir;
		}

		public void setSubDir(final String subDir) {
			this.subDir = subDir;
		}

		private File outputDir;
		private String language;
		private ObservableList<String> methods;
		private File relaxClassifierFile;

		public File getRelaxClassifierFile() {
			return relaxClassifierFile;
		}

		public void setRelaxClassifierFile(final File relaxClassifierFile) {
			this.relaxClassifierFile = relaxClassifierFile;
		}

		public ObservableList<String> getMethods() {
			return methods;
		}

		public void setMethods(final ObservableList<String> methods) {
			this.methods = methods;
		}

		public formData() {
		}

		public formData(final String lang) {
			language = lang;
		}

		public File getInputDir() {
			return inputDir;
		}

		public void setInputDir(final File inputDir) {
			this.inputDir = inputDir;
		}

		public File getOutputDir() {
			return outputDir;
		}

		public void setOutputDir(final File outputDir) {
			this.outputDir = outputDir;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(final String language) {
			this.language = language;
		}

		public void writeToFile() {
			logger.traceEntry();
			final XStream xstream = new XStream();
			xstream.omitField(formData.class, "this$0");
			final String xml = xstream.toXML(this);
			// System.out.println(xml);
			try {
				org.apache.commons.io.FileUtils.writeStringToFile(Controller.formDataFile, xml);
			} catch (final IOException e) {
				System.out.println("Unable to save form data!");
			}
			logger.traceExit();
		}
	}

	@FXML
	public void inputDirEntered(final Event event) {
		System.out.println("Input dir text action...");
		System.out.println(inputDirText.getText());
		checkFolder(inputDirText.getText(), "Bad input Dir!");
	}

	@FXML
	public void inputDirClicked(final MouseEvent mouseEvent) {
		System.out.println("Mouse clicked in inputDir field...");
		if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
			if (mouseEvent.getClickCount() == 2) {
				System.out.println("Double clicked");
				final DirectoryChooser dc = new DirectoryChooser();
				final Node n = (Node) mouseEvent.getTarget();
				final Scene s = n.getScene();
				final Window w = s.getWindow();
				final File f = dc.showDialog(w);
				if (null != f) {
					currentForm.setInputDir(f);
					final String selectedDir = currentForm.getInputDir().getPath();
					// .getPath();
					//
					inputDirText.setText(selectedDir);
					substringText.setText(edu.usc.softarch.arcade.util.StringUtil.longestCommonSubstring(currentForm.getInputDir().list()));
					subDirText.setVisible(true);
				}
			}
		}
	}

	@FXML
	public void outputDirEntered(final Event event) {
		System.out.println("Output dir text action...");
		checkFolder(outputDirText.getText(), "Bad out dir!");
	}

	@FXML
	public void outputDirClicked(final MouseEvent mouseEvent) {
		System.out.println("Mouse clicked in outputDir field...");
		if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
			if (mouseEvent.getClickCount() == 2) {
				System.out.println("Double clicked");
				final DirectoryChooser dc = new DirectoryChooser();
				final Node n = (Node) mouseEvent.getTarget();
				final Scene s = n.getScene();
				final Window w = s.getWindow();
				final File f = dc.showDialog(w);
				if (null != f) {
					String selectedDir = null;
					currentForm.setOutputDir(f);
					// System.out.println("Selected input directory: "+
					// inputDir.getPath());
					try {
						selectedDir = currentForm.getOutputDir().getPath();
					} catch (final NullPointerException e) {
						System.out.println("Didn't select any output directory");
					}
					if (!selectedDir.equals(null)) {
						outputDirText.setText(selectedDir);
						subDirText.setVisible(true);
					}
				}
			}
		}
	}

	@FXML
	public void subDirClicked(final MouseEvent mouseEvent) {
		System.out.println("Mouse clicked in subDir field...");
		if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
			if (mouseEvent.getClickCount() == 2) {
				System.out.println("Double clicked");
				final DirectoryChooser dc = new DirectoryChooser();
				dc.setInitialDirectory(currentForm.getInputDir());
				final Node n = (Node) mouseEvent.getTarget();
				final Scene s = n.getScene();
				final Window w = s.getWindow();
				final File subDir = dc.showDialog(w);
				if (subDir == null) {
					return;
				}
				final String str1 = subDir.getPath();
				final String str2 = currentForm.getInputDir().getPath();
				subDirText.setText("");
				final int index = str1.lastIndexOf(str2);
				if (index > -1) {
					String diff = str1.substring(str2.length());
					String result;
					// Get rid of the first character if it's the separator
					if (diff.startsWith(File.separator)) {
						diff = diff.substring(1);
					}
					if (diff.contains(File.separator)) {
						result = diff.substring(diff.indexOf(File.separator) + 1);
						subDirText.setText(result);
					}
				}
			}
		}
		currentForm.setSubDir(subDirText.getText());
		checkClassDirs();
	}

	@FXML
	public void subDirTyped(final Event event) {
		System.out.println("Typed something in subdir");

		currentForm.setSubDir(subDirText.getText());
		checkClassDirs();
	}

	@FXML
	public void relClassifierEdited(final Event event) {
		System.out.println("Manual editing not supported yet!");
	}

	@FXML
	public void relClassifierClicked(final MouseEvent mouseEvent) {
		System.out.println("Mouse clicked in classifier file field...");
		final File f = JavaFXHelper.getFileFromDialog(mouseEvent);
		if (null != f) {
			relClassifierText.setText(f.getAbsolutePath());
			final File r = FileUtil.checkFile(relClassifierText.getText(), false, true);
			currentForm.setRelaxClassifierFile(r);
			currentConfig.setRelaxClassifierFile(f.getName());
		}
	}

	public boolean checkClassDirs() {
		if (!RecoveryParams.checkClassesDirs(currentForm.getInputDir(), currentForm.getSubDir())) {
			runButton.setDisable(true);
			statusLabel.setText("Bad classes dir");
			statusLabel.setVisible(true);
			return false;
		} else {
			runButton.setDisable(false);
			statusLabel.setVisible(false);
			return true;
		}
	}

	public boolean checkFolder(final String folderName, final String message) {
		if (!FileUtil.checkDir(folderName, false, false).exists()) {
			runButton.setDisable(true);
			statusLabel.setText(message);
			statusLabel.setVisible(true);
			return false;
		} else {
			runButton.setDisable(false);
			statusLabel.setVisible(false);
			return true;
		}
	}

	@FXML
	public void exitOnClose(final ActionEvent event) {
		System.out.println("Exiting on close from Menu");
		System.exit(0);
	}

	@FXML
	public void prefsSelected(final Event event) {
		System.out.println("Preferences selected");
		if (null != configStage && configStage.isShowing()) {
			System.out.println("Config dialog is already open");
			return;
		}
		final FXMLLoader curtLoder = new FXMLLoader(getClass().getResource("ConfigDiag.fxml"));
		try {
			final Parent root1 = (Parent) curtLoder.load();
			configStage = new Stage();
			configStage.setScene(new Scene(root1));
			configStage.show();
		} catch (final IOException e) {
			System.out.println("Unable to load config dialog!");
			e.printStackTrace();
		}

	}

	@FXML
	public void uccCheckClicked(final ActionEvent event) {
		System.out.println("UCC use box clicked");
	}

	@FXML
	public void runClicked(final Event event) {
		// labelSetText(recLabel, "ABC");
		// final GUIService gs = new GUIService();
		// gs.setL(recLabel);
		// final Task<Label> gst = gs.createTask();Ehigw
		// gst.call();
		logger.entry(event);
		System.out.println("Run was clicked");

		if (!checkClassDirs()) {
			return;
		}
		currentForm.writeToFile();
		File currentOutputDir;

		if (methodListview.getSelectionModel().getSelectedItems().contains("relax")) {
			final String classifierName = relClassifierText.getText().trim();
			if (classifierName.isEmpty()) {
				logger.fatal("No classifier set - aborting recovery");
				return;
			}
			currentConfig.setRelaxClassifierFile(classifierName);
			prepareRecovery("RELAX");
			currentOutputDir = FileUtil.checkDir(currentOutputDirName, true, false);
			final Runnable r = () -> {
				edu.usc.softarch.arcade.relax.TopLevel.run(currentForm.getInputDir(), currentOutputDir, subDirText.getText());
			};
			final Thread t = new Thread(r);
			t.start();
			System.out.println("### RELAX recovery thread started! ###");
		}
		if (methodListview.getSelectionModel().getSelectedItems().contains("arc")) {
			prepareRecovery("ARC");

			final String[] runArgs = { currentForm.getInputDir().getPath(), currentOutputDirName, subDirText.getText(), currentForm.getLanguage() };
			// final Runnable r = () -> {
			edu.usc.softarch.arcade.clustering.BatchClusteringEngine.main(runArgs);
			// };
			// new Thread(r).start();
			generateDotGraphs(FileUtil.checkDir(currentOutputDirName, false, false));
		}
		if (methodListview.getSelectionModel().getSelectedItems().contains("acdc")) {
			System.out.println("Running ACDC...");
			prepareRecovery("ACDC");

			final String[] runArgs = { currentForm.getInputDir().getPath(), currentOutputDirName, subDirText.getText(), currentForm.getLanguage() };
			edu.usc.softarch.arcade.AcdcWithSmellDetection.main(runArgs);
			generateDotGraphs(FileUtil.checkDir(currentOutputDirName, false, false));
		}
		if (methodListview.getSelectionModel().getSelectedItems().contains("bunch")) {
			System.out.println("Bunch runner not implemented yet");
		}
		if (methodListview.getSelectionModel().getSelectedItems().contains("limbo")) {
			System.out.println("Limbo runner not implemented yet");
		}

		if (methodListview.getSelectionModel().getSelectedItems().contains("wca")) {
			System.out.println("WCA runner not implemented yet");
		}

		if (methodListview.getSelectionModel().getSelectedItems().contains("pkg") && methodListview.getSelectionModel().getSelectedItems().size() == 1) {
			prepareRecovery("PKG");
			runPKGmain();
		}

		System.out.println("### All recoveries started or finished successfully! ###");
		// recLabel.setVisible(false);
		logger.traceExit();
	}

	// private void labelSetText(final Label l, final String s) {
	// l.setText(s);
	// }

	public  void prepareRecovery(final String method) {
		final String methodUpper = method.toUpperCase();
		System.out.println("Runnning " + methodUpper);
		Config.setSelectedRecoveryMethodName(methodUpper);
		currentOutputDirName = generateResultsDirname(methodUpper);
		if (methodListview.getSelectionModel().getSelectedItems().contains("pkg")) {
			runPKGbatchPackager();
		}
	}

	public void runPKGbatchPackager() {
		/* Piggyback PKG */
		if (!methodListview.getSelectionModel().getSelectedItems().contains("pkg")) {
			return;
		}
		if (currentOutputDirName.isEmpty()) {
			currentOutputDirName = generateResultsDirname("PKG");
		}
		final String[] PKG_BP_args = { currentConfig.getPythonloc(), currentConfig.getBatchPackagerLoc(), "--startdir", FileUtil.checkFile(currentOutputDirName, false, true).getAbsolutePath(),
				"--pkgprefixes", currentConfig.getPkgPrefixes() };
		logger.debug("PKG Batch Packager args = " + StringUtil.printStringArray(PKG_BP_args));
		final Runnable pkgBPRunnable = () -> {
			RunExtCommUtil.run(PKG_BP_args);
		};
		pkgThread = new Thread(pkgBPRunnable);
		pkgThread.run();
		// pkgThread.join();
	}

	public void runPKGmain() {
		try {
			logger.debug("Waiting for Batch Packager to finish...");
			pkgThread.join();
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Running PKG...");
		final String[] runArgs = { "-c ", currentOutputDirName, "-d ", currentOutputDirName, "-s ", currentOutputDirName };
		edu.usc.softarch.arcade.PkgsWithSmellDetection.main(runArgs);
	}

	public  String generateResultsDirname(final String methodName) {
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss_SSS");
		final String date_time = sdf.format(Calendar.getInstance().getTime());
		final String effective_outputDir = currentForm.getOutputDir().getPath() + System.getProperty("file.separator") + substringText.getText() + "_" + methodName + "_" + Config.getSelectedLanguage()
				+ "_" + date_time;
		FileUtil.checkDir(effective_outputDir, true, false);
		System.setProperty("logFilename", effective_outputDir + System.getProperty("file.separator") + "arcade.log");
		final org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();
		return effective_outputDir;
	}

	public  void generateDotGraphs(final File directory) {
		final String[] files = directory.list(new SuffixFileFilter(".rsf"));
		for (final String f : files) {
			final String inputFileName = directory.getPath() + System.getProperty("file.separator") + f;
			final String outputFileName = directory.getPath() + System.getProperty("file.separator") + FilenameUtils.removeExtension(f) + ".dot";
			if (new File(outputFileName).exists()) {
				logger.info("Dot file " + outputFileName + " exists already!");
				continue;
			}
			final String[] runArgs = { inputFileName, outputFileName };
			edu.usc.softarch.arcade.util.convert.RsfToDotConverter.main(runArgs);
		}
	}

	@FXML
	public void radioJavaClicked(final Event event) {
		radioJava.setSelected(true);
		System.out.println("Java selected");
		radioC.setSelected(false);
		currentForm.setLanguage("java");
		Config.setSelectedLanguage(Language.java);
	}

	@FXML
	public void radioCClicked(final Event event) {
		radioC.setSelected(true);
		System.out.println("C selected");
		radioJava.setSelected(false);
		currentForm.setLanguage("c");
		Config.setSelectedLanguage(Language.c);
	}

	public static ArchitecturalView getCurrentView() {
		return currentView;
	}

	public static void setCurrentView(final ArchitecturalView currentView) {
		Controller.currentView = currentView;
	}

	@FXML
	public void captureOutputChecked(final ActionEvent event) {
		if (captureOuputCheck.isSelected()) {
			System.out.println("Capture selected");

			consoleText.setDisable(false);
			errorText.setDisable(false);
			System.setOut(new PrintStream(r_stdout, true));
			System.setErr(new PrintStream(r_stderr, true));
		} else {
			System.out.println("Capture deselected");

			consoleText.setDisable(true);
			errorText.setDisable(true);
			System.setOut(std_stdout);
			System.setErr(std_stderr);
		}
		// For debugging
		errorText.setDisable(false);
	}

	public void appendConsoleText(final String str) {
		Platform.runLater(() -> consoleText.appendText(str));
		// consoleText.appendText(str);
	}

	public void appendErrorText(final String str) {
		Platform.runLater(() -> errorText.appendText(str));
		// errorText.appendText(str);

	}

	@FXML
	public void methodListClicked(final MouseEvent mouseEvent) {
		System.out.println("Method list clicked");
		System.out.println(methodListview.getSelectionModel().getSelectedItems());
	}

	@FXML
	public void devClicked(final ActionEvent event) {
		recLabel.setVisible(!recLabel.isVisible());
	}

	@FXML // This method is called by the FXMLLoader when initialization is
			// complete
	public void initialize() {
		assert mainPane != null : "fx:id=\"mainPane\" was not injected: check your FXML file 'aRun.fxml'.";
		assert inputDirText != null : "fx:id=\"inputDirText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert outputDirText != null : "fx:id=\"outputDirText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert subDirText != null : "fx:id=\"subDirText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert relClassifierText != null : "fx:id=\"relClassifierText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert radioJava != null : "fx:id=\"radioJava\" was not injected: check your FXML file 'aRun.fxml'.";
		assert recoveryLanguages != null : "fx:id=\"recoveryLanguages\" was not injected: check your FXML file 'aRun.fxml'.";
		assert radioC != null : "fx:id=\"radioC\" was not injected: check your FXML file 'aRun.fxml'.";
		assert runButton != null : "fx:id=\"runButton\" was not injected: check your FXML file 'aRun.fxml'.";
		assert mainMenuBar != null : "fx:id=\"mainMenuBar\" was not injected: check your FXML file 'aRun.fxml'.";
		assert fileMenu != null : "fx:id=\"fileMenu\" was not injected: check your FXML file 'aRun.fxml'.";
		assert closeButton != null : "fx:id=\"closeButton\" was not injected: check your FXML file 'aRun.fxml'.";
		assert prefMenu != null : "fx:id=\"prefMenu\" was not injected: check your FXML file 'aRun.fxml'.";
		assert consoleText != null : "fx:id=\"consoleText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert logoView != null : "fx:id=\"logoView\" was not injected: check your FXML file 'aRun.fxml'.";
		assert errorText != null : "fx:id=\"errorText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert captureOuputCheck != null : "fx:id=\"captureOuputCheck\" was not injected: check your FXML file 'aRun.fxml'.";
		assert stdoutLabel != null : "fx:id=\"stdoutLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert stderrLabel != null : "fx:id=\"stderrLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert substringText != null : "fx:id=\"substringText\" was not injected: check your FXML file 'aRun.fxml'.";
		assert methodListview != null : "fx:id=\"methodListview\" was not injected: check your FXML file 'aRun.fxml'.";
		assert statusLabel != null : "fx:id=\"statusLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert recLabel != null : "fx:id=\"recLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert secondLabel != null : "fx:id=\"secondLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert visLabel != null : "fx:id=\"visLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert auxLabel != null : "fx:id=\"auxLabel\" was not injected: check your FXML file 'aRun.fxml'.";
		assert devButton != null : "fx:id=\"devButton\" was not injected: check your FXML file 'aRun.fxml'.";

		// r_stdout = new OutputStream() {
		// @Override
		// public void write(final int b) throws IOException {
		// appendConsoleText(String.valueOf((char) b));
		// }
		// };
		// r_stderr = new OutputStream() {
		// @Override
		// public void write(final int b) throws IOException {
		// appendErrorText(String.valueOf((char) b));
		// }
		// };

		TextAreaAppender.setTextArea(errorText);

		currentView = new ArchitecturalView("dummy");
		currentView.readConfigFromFile();
		currentConfig = Controller.getCurrentView().getConfig();

		if (readFormDataFromFile()) {
			populateForm(currentForm);
		} else {
			currentForm = new formData("java");
		}
		for (final String s : Rec_methods) {
			recoveryMethodsList.add(s);
		}

		final ObservableList<String> items = FXCollections.observableArrayList(recoveryMethodsList);

		methodListview.setItems(items);
		methodListview.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		// Should be last part of initialization so that exceptions occurring
		// during startup will be readable
		// System.setOut(new PrintStream(r_stdout, true));
		// System.setErr(new PrintStream(r_stderr, true));
		methodListview.getSelectionModel().select(0);

		// Turn output capturing off since it's not the best option for
		// debugging
		captureOuputCheck.setSelected(false);
		captureOutputChecked(null);

		logger.traceExit();
	}

	public  void populateForm(final formData fd) {
		logger.entry(fd);
		if (fd.getInputDir().exists()) {
			inputDirText.setText(fd.getInputDir().getPath());
			subDirText.setVisible(true);
			subDirText.setText(fd.getSubDir());
			substringText.setText(edu.usc.softarch.arcade.util.StringUtil.longestCommonSubstring(currentForm.getInputDir().list()));
		}
		if (fd.getOutputDir().exists()) {
			outputDirText.setText(fd.getOutputDir().getPath());
		}
		if (fd.getRelaxClassifierFile().exists()) {
			relClassifierText.setText(fd.getRelaxClassifierFile().getAbsolutePath());
		}
		logger.traceExit();
	}

	public  boolean readFormDataFromFile() {
		logger.traceEntry();
		formDataFile = FileUtil.checkFile(currentConfig.getFormDataFileName(), false, false);
		System.out.println("Trying to read form data from file " + formDataFile.getAbsolutePath());
		if (formDataFile.exists()) {
			final XStream xstream = new XStream();
			try {
				currentForm = (formData) xstream.fromXML(formDataFile);
				logger.traceExit(true);
				return true;
			} catch (final Exception e) {
				System.out.println("Unable to read config file, please delete file " + formDataFile.getAbsolutePath());
				logger.traceExit(false);
				return false;
			}
		}
		System.out.println("No form data file found");
		logger.traceExit(false);
		return false;
	}
}
