package edu.usc.softarch.arcade.frontend;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.Label;

public class GUIService extends Service<Label> {
	private Label l;

	public Label getL() {
		return l;
	}

	public void setL(final Label l) {
		this.l = l;
	}

	@Override
	protected Task<Label> createTask() {
		final Label l = getL();
		return new Task<Label>() {
			@Override
			protected Label call() {
				l.setVisible(true);
				return l;
			}
		};
	}
}
