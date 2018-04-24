package edu.usc.softarch.arcade.frontend;

import java.io.File;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class JavaFXHelper {

	public static File getFileFromDialog(final MouseEvent mouseEvent) {
		if (mouseEvent.getButton().equals(MouseButton.PRIMARY)) {
			if (mouseEvent.getClickCount() == 2) {
				System.out.println("Double clicked");
				final FileChooser fc = new FileChooser();
				final Node n = (Node) mouseEvent.getTarget();
				final Scene s = n.getScene();
				final Window w = s.getWindow();
				return fc.showOpenDialog(w);
			}
		}
		return null;
	}
}
