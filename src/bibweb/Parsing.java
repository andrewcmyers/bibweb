package bibweb;

import java.util.NoSuchElementException;

import org.eclipse.jdt.annotation.NonNull;

import easyIO.Scanner;
import easyIO.UnexpectedInput;

public class Parsing {
	int lineno;
		
	@SuppressWarnings("serial")
	public static class ParseError extends Exception {
		public ParseError(String msg) {
			super(msg);
		}
	}

	static void skipCommentLines(@NonNull Scanner sc) {
		sc.whitespace();
		while (sc.peek() == '%') {
			try {
				sc.nextLine();
			} catch (UnexpectedInput e) {
				return; // hit end of input
			}
			sc.whitespace();
		}
	}
	
	/** Parse a single whitespace-terminated (or colon-terminated) attribute name that may contain escaped characters,
	 *  including escaped whitespace. Skips any leading whitespace. */
	public static String parseAttribute(Scanner sc) {
		skipCommentLines(sc);
		sc.whitespace();
		sc.mark();
		while (sc.hasNext() && !Character.isWhitespace(sc.peek()) && sc.peek() != ':') {
			if (sc.peek() == '\\') {
				sc.advance();
			}
			sc.advance();
		}
		String result = sc.getToken();
		sc.accept();
//		System.out.println("Parsed attribute: " + result);
		return result;
	}
	public static boolean isMultilineValue(Scanner sc) {
		sc.whitespace();
		int c = sc.peek();
		switch (c) {
		case '{':
			sc.advance();
			sc.trailingWhitespace();
			try {
				sc.newline();
			} catch (UnexpectedInput ei) {
				System.out.println("Warning: unexpected characters after closing brace at " + sc.location());
			}
			return true;
		case ':':
			sc.advance();
			sc.trailingWhitespace();
			return false;
		default:
			System.out.println("Expected colon (:) or open brace ({) at " + sc.location());
			return false;
		}
	}	
	
	public static NoSuchElementException none = new NoSuchElementException();
	

	public static class Attribute {
		static public enum Case {
			SINGLE_LINE, MULTILINE
		}
		public final String name;
		public final Case case_;
		public Attribute(String a, Case c) {
			name = a;
			case_ = c;
		}
	}
	
	public static String parseValue(Scanner sc) {
		String result = parseText(sc, isMultilineValue(sc));
//		System.out.println("parsed value: \"" + result + "\"");
		return result;
	}

	public static String parseText(Scanner sc, boolean multiline) {
		sc.mark();
		if (!multiline) {
			sc.eol();
			String result = sc.getToken();
			sc.accept();
			// sc.advance(); // don't go past the newline; might be needed to
			// close out a higher-level single-line attribute
			return result;
		}
		StringBuilder b = new StringBuilder();
		
		while (!rhsClosed(sc, true)) {
			try { b.append(sc.nextLine()); }
			catch (UnexpectedInput e) {
				System.out.println("Unterminated line at " + sc.location());
				sc.accept();
				return b.toString().trim();
			}
		}
		sc.accept();
		return b.toString().trim();
	}
	public static boolean rhsClosed(Scanner sc, boolean multiline) {
		sc.trailingWhitespace();
		if (!sc.hasNext()) return true;
		if (!multiline) {
			if (sc.peek() == '\n') {
				return true;
			} else {
				return false;
			}
		}
		sc.whitespace();
		if (sc.peek() != '}') return false;

		sc.mark();
		sc.advance(); // go past }
		sc.trailingWhitespace();
		if (sc.peek() == '\n' || sc.peek() == -1) {
			sc.accept();
			sc.advance();
			return true;
		} else {
			// not a real closing brace, roll back to it.
			sc.abort();
			return false;
		}

	}
}
