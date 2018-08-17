package easyIO;

/**
 * A Recognizer recognizes certain input sequences of characters available on a
 * BacktrackScanner, and invokes a continuation for each recognized sequence.
 */
public interface Recognizer {
	/** The remainder of a computation that can be performed after recognizing input.
	 *  Throwing {@code Success} is used to stop the recognizers at the current position.
	 */
	interface Continuation {
		void check() throws Success;
	}
	
	/** Represents successful matching of the input stream by a recognizer. */
	@SuppressWarnings("serial")
	public class Success extends Exception {
		@Override public Throwable fillInStackTrace() {
			return this;
		}
	}
	/** For each possible way of recognizing the input, advance {@code inp}
	 *  past the point where the input is recognized and invoke {@code cont}.
	 *  If the continuation throws {@code Success}, leave the input position
	 *  where it is and throw the same exception to the caller.
	 */
	void recognize(BacktrackScanner inp, Continuation cont) throws Success;
}

