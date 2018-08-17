package easyIO;

/** A Recognizer that decides what other recognizer to use based on the lookahead character.
 */
public abstract class PredictiveRecognizer implements Recognizer {
	/**
	 * Provide another recognizer to use based on the next character in the
	 * input. Subclasses are expected to override this to provide the correct
	 * prediction.
	 * 
	 * @param c
	 *            is the next character on the input, or -1 to signify the end
	 *            of the input.
	 */
	abstract Recognizer predict(int c);
	
	public void recognize(BacktrackScanner inp, Continuation cont) throws Success {
		Recognizer r = predict(inp.peek());
		r.recognize(inp, cont);
	}
}
