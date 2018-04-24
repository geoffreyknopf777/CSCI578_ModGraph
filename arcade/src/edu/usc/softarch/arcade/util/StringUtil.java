package edu.usc.softarch.arcade.util;

import java.util.ArrayList;

/**
 *
 * From http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/
 * Longest_common_substring
 *
 */
public class StringUtil {
	public static String longestCommonSubstring(final String S1, final String S2) {
		int Start = 0;
		int Max = 0;
		for (int i = 0; i < S1.length(); i++) {
			for (int j = 0; j < S2.length(); j++) {
				int x = 0;
				while (S1.charAt(i + x) == S2.charAt(j + x)) {
					x++;
					if (i + x >= S1.length() || j + x >= S2.length()) {
						break;
					}
				}
				if (x > Max) {
					Max = x;
					Start = i;
				}
			}
		}
		return S1.substring(Start, Start + Max);
	}

	public static String longestCommonSubstring(final String[] stringArray) {
		final String[] effectiveStrings = removeDotNames(stringArray);
		String currentString = effectiveStrings[0];
		if (effectiveStrings.length == 1) {
			return currentString;
		}
		for (int i = 1; i < effectiveStrings.length; i++) {
			currentString = longestCommonSubstring(currentString, effectiveStrings[i]);
		}
		return currentString;
	}

	private static String[] removeDotNames(final String[] inArray) {
		final ArrayList<String> goodOnes = new ArrayList<String>();
		for (final String s : inArray) {
			if (!s.startsWith(".")) {
				goodOnes.add(s);
			}
		}
		// String[] outArray = new String[goodOnes.size()];
		// final String[] outArray = (String[]) goodOnes.toArray();

		// Needing this contrived construct is exhibit A that JAva is crap
		final String[] outArray = goodOnes.toArray(new String[goodOnes.size()]);
		return outArray;
	}

	public static String printStringArray(String[] a) {
		String result = "";
		for (final String s : a) {
			result += s + ",";
		}
		if (result.length() < 2) {
			return result;
		}
		return result.substring(0, result.length() - 2);
	}
}
