package easyIO;

import easyIO.Recognizer.Continuation;
import easyIO.Recognizer.Success;

/** Regular expression support for {@code easyIO.Scanner}
 * @see easyIO.Scanner easyIO.Scanner */
public class Regex {
	
	protected Regex() {}
	
	final static Success success = new Success();

	static final UnexpectedInput uinp = new UnexpectedInput();

	/**
     * Match the input characters starting at the current position against the
     * pattern r and advance the scanner position beyond the matched
     * characters.  Throw {@code UnexpectedInput} if there is no way to match
     * the input characters against the pattern, and leave the scanner
     * position unchanged.
	 */
	public static void scanPattern(BacktrackScanner sc, Recognizer r) throws UnexpectedInput {
		sc.mark();
		try {
			r.recognize(sc, new Continuation() {
				@Override public void check() throws Success {
					throw success;
				}
			});
			sc.abort();
			throw uinp;
		} catch (Success e) {
			sc.accept();
		}
	}
	

	/**
	 * Match the input characters starting at the current position against the
	 * pattern r and return the characters matched. Throw UnexpectedInput if
	 * there is no way to match the input characters against the pattern, and
	 * leave the scanner position unchanged.
	 */
	public static String parsePattern(BacktrackScanner sc, Recognizer r) throws UnexpectedInput {
		sc.mark();
		try {
			scanPattern(sc, r);
			String result = sc.getToken();
			sc.accept();
			return result;
		} catch (UnexpectedInput e) {
			sc.abort();
			throw e;
		}
	}

	/**
     * Return all the characters from the current scanner position up
     * to the first occurrence of the delimiter pattern {@code delim},
     * leaving the scanner position either immediately after that
     * delimiter pattern or at the end of the string if the delimiter
     * pattern was never found.
	 */
	public static String parseToDelimiter(BacktrackScanner sc, Recognizer delim) {
		String result = Regex.parseToPattern(sc, delim);
		if (!sc.hasNext()) return result;
		try { Regex.scanPattern(sc, delim); } catch (UnexpectedInput e) { assert false; }
		return result;
	}
	
	/** Whether the next characters on the input can be matched by r. */
	public static boolean hasPattern(BacktrackScanner sc, Recognizer r) {
		sc.mark();
		try {
			scanPattern(sc, r);
			sc.abort();
			return true;
		} catch (UnexpectedInput e) {
			sc.abort();
			return false;
		}
	}
	
	/**
     * Advance the scanner position to the point where the pattern
     * recognized by {@code r} is found, or all the way to the end of
     * the input if it is not found.
	 */
	public static void advanceToPattern(BacktrackScanner sc, Recognizer r) {
		while (sc.hasNext() && !hasPattern(sc, r))
			sc.advance();
	}	
	
	/** Return all the text between the current scanner position and the
	 *  first occurrence of the pattern recognized by r.
	 */
	public static String parseToPattern(BacktrackScanner sc, Recognizer r) {
		sc.mark();
		advanceToPattern(sc, r);
		String result = sc.getToken(); 
		sc.accept();
		return result;
	}
	
	/** Recognizes a followed by b (regular expression ab) */
	public static Recognizer concat(final Recognizer a, final Recognizer b) {
		return new Recognizer() {
			@Override public void recognize(final BacktrackScanner s, final Continuation k) throws Success {
				a.recognize(s, new Continuation() {
					@Override public void check() throws Success {
						b.recognize(s, k);
					}
				});		
			}
		};
	}

	/** Recognizes either a or b (regular expression a|b) */
	public static Recognizer alt(final Recognizer a, final Recognizer b) {
		return new Recognizer() {
			@Override
			public void recognize(BacktrackScanner s, Continuation k) throws Success {
				s.mark();
				try {
					a.recognize(s, k);
				} catch (Success e) {
					s.accept();
					throw e;
				}
				s.abort();
				s.mark();
				try {
					b.recognize(s, k);
				} catch (Success e) {
					s.accept();
					throw e;
				}
				s.abort();
			}
		};
	}
	
	/** Recognizes 0 or 1 instance of a (regular expression a?) */
	public static Recognizer opt(final Recognizer a) {
		return new Recognizer() {
			@Override public void recognize(BacktrackScanner s, Continuation k) throws Success {
				s.mark();
				try {
					a.recognize(s, k);
				} catch (Success e) {
					s.accept();
					throw e;
				}
				s.abort();
				k.check();
			}
		};
	}
	
	/** Recognizes 0 or more instances of a (regular expression a*) */
	public static Recognizer repeat(final Recognizer a) {
		return new Recognizer() {
			@Override public void recognize(final BacktrackScanner s, final Continuation k) throws Success {
				s.mark();
				try {
					a.recognize(s, new Continuation() {
						@Override public void check() throws Success {
							recognize(s, k);
						}
					});
				} catch (Success e) {
					s.accept();
					throw e;
				}
				s.abort();
				k.check();
			}
		};
	}
	/** Recognizes 1 or more instances of a (a+) */
	public static Recognizer oneOrMore(final Recognizer a) {
		return concat(a, repeat(a));
	}
	
    /** Recognizes all of the characters in c, in sequence. */
	public static Recognizer constant(final String c) {
		return new Recognizer() {
			@Override
			public void recognize(BacktrackScanner s, Continuation k) throws Success {
				try {
					s.string(c);
					k.check();
				} catch (UnexpectedInput e) {
					return;
				}
			}
		};
	}
	
	/** Recognizer that matches any single character in the string {@code c} */
	public static Recognizer anyChar(final String c) {
		return new Recognizer() {
			@Override
			public void recognize(BacktrackScanner s, Continuation k) throws Success {
				if (c.indexOf(s.peek()) != -1) {
					s.advance();
					k.check();
				}
			}
		};
	}
	
	/** Recognizer that matches any single character <em>not</em> in the string {@code c}. */
	public static Recognizer notChar(final String c) {
		return new Recognizer() {
			@Override
			public void recognize(BacktrackScanner s, Continuation k) throws Success {
				if (s.peek() != -1 && c.indexOf(s.peek()) == -1) {
					s.advance();
					k.check();
				}
			}
		};
	}
	
	/** Recognizer that matches a single whitespace character. */
	public static Recognizer whitespace() {
		return anyChar(" \t\r\n\f");
	}
}
