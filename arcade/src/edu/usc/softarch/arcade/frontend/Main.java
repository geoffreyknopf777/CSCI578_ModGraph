/**
 *
 */
/**
 * @author daniellink
 *
 */
package edu.usc.softarch.arcade.frontend;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;

import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.mallet.Import;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class Main extends Application {

	@FXML
	// fx:id="consoleText"
	private static TextArea consoleText; // Value injected by FXMLLoader

	@FXML
	// fx:id="errorText"
	private static TextArea errorText; // Value injected by FXMLLoader

	@Override
	public void start(final Stage primaryStage) throws Exception {

		System.out.println("Starting up...\n");
		System.out.println("### System Properties ###");
		final Properties p = System.getProperties();
		final Enumeration<Object> keys = p.keys();
		while (keys.hasMoreElements()) {
			final String key = (String) keys.nextElement();
			final String value = (String) p.get(key);
			System.out.println(key + ": " + value);
		}
		System.out.println("### System Properties End ###\n");

		final String relativeUCCFileName = "tools" + File.separator + "ucc" + File.separator + System.getProperty("os.name") + File.separator + "UCC";
		if (FileUtil.checkFile(relativeUCCFileName, false, false).exists()) {
			Config.setUccLoc(relativeUCCFileName);
			System.out.println("UCC location set to " + relativeUCCFileName);
		}

		// final String relativeDotLayoutFileName = "tools" + File.separator +
		// "graphviz" + File.separator + "circo" + File.separator +
		// System.getProperty("os.name") + File.separator + "circo";
		// if (FileUtil.checkFile(relativeDotLayoutFileName, false,
		// false).exists()) {
		// Config.setDotLayoutCommandLoc(relativeDotLayoutFileName);
		// System.out.println("Dot layout location set to " +
		// relativeDotLayoutFileName);
		// }

		final String homeDir = System.getProperty("user.home");
		System.out.println("Home dir = " + homeDir);
		final String perUserArcadeDirName = homeDir + File.separator + "arcade_userdir";
		System.out.println("Per-user config dir = " + perUserArcadeDirName);
		System.setProperty("log4j.configurationFile", "cfg/log4j2.xml");
		final String log4jConfigFileName = perUserArcadeDirName + File.separator + "log4j2.xml";
		System.out.println("log4j2 config file = " + log4jConfigFileName);
		System.setProperty("log4j.configurationFile", log4jConfigFileName);
		System.out.println("log4j2 config file set to " + System.getProperty("log4j.configurationFile"));
		System.setProperty("logFilename", perUserArcadeDirName + File.separator + "arcade.log");

		System.setProperty("Log4jContextSelector", "org.apache.logging.log4j.core.async.AsyncLoggerContextSelector");

		final org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
		ctx.reconfigure();

		final Parent root = FXMLLoader.load(getClass().getResource("aRun.fxml"));
		primaryStage.setTitle("ARCADE Runner");
		primaryStage.setScene(new Scene(root, 600, 800));
		primaryStage.setMinWidth(600);
		primaryStage.setMinHeight(800);
		primaryStage.show();

		final ArchitecturalView av = new ArchitecturalView();
		Import.setImportAV(av);
		av.getConfig().setPerUserArcadeDir(FileUtil.checkDir(perUserArcadeDirName, true, true));

		// Check if the necessary directories exist
		FileUtil.checkDir("cfg", false, true);
		FileUtil.checkDir("res", false, true);
		FileUtil.checkDir("stoplists", false, true);

		// final OutputStream r_stdout = new OutputStream() {
		// @Override
		// public void write(final int b) throws IOException {
		// appendConsoleText(String.valueOf((char) b));
		// }
		// };
		//
		// final OutputStream r_stderr = new OutputStream() {
		// @Override
		// public void write(final int b) throws IOException {
		// appendErrorText(String.valueOf((char) b));
		// }
		// };

		// System.setOut(new PrintStream(r_stdout, true));
		// System.setErr(new PrintStream(r_stderr, true));
	}

	public static void main(final String[] args) {

		launch(args);
		// try {
		// r_stdout.close();
		// r_stderr.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	// public static class Console extends OutputStream {
	//
	// private final TextArea output;
	//
	// public Console(final TextArea ta) {
	// output = ta;
	// }
	//
	// @Override
	// public void write(final int i) throws IOException {
	// output.appendText(String.valueOf((char) i));
	// }
	// }
	//
	// public static void appendConsoleText(final String str) {
	// final Runnable r = () -> {
	// consoleText.appendText(str);
	// };
	//
	// new Thread(r).start();
	// // Platform.runLater(() -> consoleText.appendText(str));
	// // consoleText.appendText(str);
	// }
	//
	// public static void appendErrorText(final String str) {
	// final Runnable r = () -> {
	// errorText.appendText(str);
	// };
	//
	// new Thread(r).start();
	// // Platform.runLater(() -> errorText.appendText(str));
	// // errorText.appendText(str);
	//
	// }
}
