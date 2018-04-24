package org.tartarus.snowball;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class TestApp {
	private static void usage() {
		System.err
				.println("Usage: TestApp <algorithm> <input file> [-o <output file>]");
	}

	public static void main(final String[] args) throws Throwable {
		if (args.length < 2) {
			usage();
			return;
		}

		final Class<?> stemClass = Class.forName("org.tartarus.snowball.ext."
				+ args[0] + "Stemmer");
		final SnowballStemmer stemmer = (SnowballStemmer) stemClass
				.newInstance();

		Reader reader;
		reader = new InputStreamReader(new FileInputStream(args[1]));
		reader = new BufferedReader(reader);

		final StringBuffer input = new StringBuffer();

		OutputStream outstream;

		if (args.length > 2) {
			if (args.length >= 4 && args[2].equals("-o")) {
				outstream = new FileOutputStream(args[3]);
			} else {
				usage();
				reader.close();
				return;
			}
		} else {
			outstream = System.out;
		}
		Writer output = new OutputStreamWriter(outstream);
		output = new BufferedWriter(output);

		int repeat = 1;
		if (args.length > 4) {
			repeat = Integer.parseInt(args[4]);
		}

		// Object [] emptyArgs = new Object[0];
		int character;
		while ((character = reader.read()) != -1) {
			final char ch = (char) character;
			if (Character.isWhitespace(ch)) {
				if (input.length() > 0) {
					stemmer.setCurrent(input.toString());
					for (int i = repeat; i != 0; i--) {
						stemmer.stem();
					}
					output.write(stemmer.getCurrent());
					output.write('\n');
					input.delete(0, input.length());
				}
			} else {
				input.append(Character.toLowerCase(ch));
			}
		}
		output.flush();
		output.close();
		reader.close();
	}
}