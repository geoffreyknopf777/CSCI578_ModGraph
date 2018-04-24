package edu.usc.softarch.arcade.relax;

import java.util.ArrayList;
import java.util.HashMap;

public class ColorManager {
	private static String noMatchColor = "black";

	public static String getNoMatchColor() {
		return noMatchColor;
	}

	public static void setNoMatchColor(final String noMatchColor) {
		ColorManager.noMatchColor = noMatchColor;
	}

	// The default for GraphViz is to follow the X11 color scheme, so let's go
	// with that
	// Black is for no_match
	// private static final String[] masterColorList = { "red", "green", "blue",
	// "yellow", "chocolate2", "aquamarine", "brown", "burlywood", "chartreuse",
	// "coral", "crimson", "cyan", "gold", "indigo",
	// "khaki", "lime", "magenta", "maroon", "navy", "orange", "orchid", "peru",
	// "plum", "purple", "salmon", "seagreen", "sienna", "tan", "thistle",
	// "teal", "tomato", "turquoise", "violet",
	// "wheat" };

	// More visible colors courtesy of ColorBrewer 2.0 (colorbrewer2.org)
	// After the first 12 colors, use old colors which may not be optimal

	private static final String[] masterColorList = { "#a6cee3", "#1f78b4", "#b2df8a", "#33a02c", "#fb9a99", "#e31a1c", "#fdbf6f", "#ff7f00", "#cab2d6", "#6a3d9a", "#ffff99", "#b15928", "red",
			"green", "blue", "yellow", "chocolate2", "aquamarine", "brown", "burlywood", "chartreuse", "coral", "crimson", "cyan", "gold", "indigo", "khaki", "lime", "magenta", "maroon", "navy",
			"orange", "orchid", "peru", "plum", "purple", "salmon", "seagreen", "sienna", "tan", "thistle", "teal", "tomato", "turquoise", "violet", "wheat" };
	private static ArrayList<String> usedColors = new ArrayList<>();
	private static HashMap<String, String> labelColors = new HashMap<>();
	private static int currentLabelID = 0;

	public static int getCurrentLabelID() {
		return currentLabelID;
	}

	public static void setCurrentLabelID(final int currentLabelID) {
		ColorManager.currentLabelID = currentLabelID;
	}

	// public static HashMap<String, String> getLabelColors() {
	// return labelColors;
	// }

	public static void setLabelColors(final HashMap<String, String> labelColors) {
		ColorManager.labelColors = labelColors;
		ColorManager.labelColors.replace("no_match", noMatchColor);
	}

	public static void addLabel(final String l) {
		if (l == "no_match") {
			labelColors.put("no_match", noMatchColor);
			usedColors.add(noMatchColor);
			return;
		}
		labelColors.put(l, masterColorList[currentLabelID]);
		usedColors.add(masterColorList[currentLabelID++]);
	}

	public static ArrayList<String> getColorlist() {
		return usedColors;
	}

	/**
	 *
	 * @param index
	 *            - Index of color to be returned. If the number is too high,
	 *            start again from the beginning
	 * @return
	 */
	public static String getColorByIndex(final int index) {
		final int effectiveIndex = index % usedColors.size();
		return usedColors.get(effectiveIndex);
	}

	public static String getColorByName(final String label) {
		if (label == "no_match") {
			return noMatchColor;
		}
		return labelColors.get(label);
	}
}
