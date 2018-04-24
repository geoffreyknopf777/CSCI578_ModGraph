package edu.usc.softarch.arcade.topics;

import java.io.Serializable;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Instance;
import cc.mallet.types.Token;
import cc.mallet.types.TokenSequence;

// code from Miroslav Batchkarov and posted http://comments.gmane.org/gmane.comp.ai.mallet.devel/1724
public class StemmerPipe extends Pipe implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Instance pipe(final Instance carrier) {
		final SnowballStemmer stemmer = new englishStemmer();
		final TokenSequence in = (TokenSequence) carrier.getData();

		for (final Token token : in) {
			stemmer.setCurrent(token.getText());
			stemmer.stem();
			token.setText(stemmer.getCurrent());
		}

		return carrier;
	}
}
