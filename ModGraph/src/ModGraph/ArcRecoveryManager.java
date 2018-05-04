package ModGraph;

import java.io.File;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;

import edu.usc.softarch.arcade.archview.ArchitecturalView;
import edu.usc.softarch.arcade.config.Config;
import edu.usc.softarch.arcade.frontend.Controller;
import edu.usc.softarch.arcade.util.FileUtil;
import edu.usc.softarch.arcade.util.mallet.Import;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import javafx.event.Event;
import javafx.event.EventType;

public class ArcRecoveryManager extends Application{
	
	private static String sLang;
	
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

		FXMLLoader loader = new FXMLLoader(getClass().getResource("aRun.fxml"));
		final Parent root = loader.load();
		primaryStage.setTitle("ARCADE Runner");
		primaryStage.setScene(new Scene(root, 600, 800));
		primaryStage.setMinWidth(600);
		primaryStage.setMinHeight(800);
		//primaryStage.show();

		final ArchitecturalView av = new ArchitecturalView();
		Import.setImportAV(av);
		av.getConfig().setPerUserArcadeDir(FileUtil.checkDir(perUserArcadeDirName, true, true));

		// Check if the necessary directories exist
		FileUtil.checkDir("cfg", false, true);
		FileUtil.checkDir("res", false, true);
		FileUtil.checkDir("stoplists", false, true);
		
		//Get handle to controller class
		Controller controller = (Controller)loader.getController();
		
		//Create generic event to send to controller
		Event e = new Event(EventType.ROOT);
		
		//Set the language
		if(sLang.equals("java")){
			controller.radioJavaClicked(e);}
		else { //"c"
			controller.radioCClicked(e);
		}
		
		//Set the recovery method to ARC
		controller.methodListview.getSelectionModel().clearSelection(0);
		controller.methodListview.getSelectionModel().select(1);
		
		//Set subdir text blank
		controller.subDirText.setText("");
		
		//Simulate pushing the run button
		controller.runClicked(e);
	}	
	
	public static void main(final String[] args) {
		if(args.length < 2) {
			sLang = "java";
		}
		else {
			sLang = args[1]; //c or java
		}
		launch(args);
	}

}
