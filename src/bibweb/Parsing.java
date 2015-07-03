package bibweb;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Parsing {
	int lineno;
	
	public static String readMultiline(LNScanner sc) {
		StringBuilder b = new StringBuilder();

		while (sc.hasNextLine()) {
			String s = sc.nextLine();
			if (s.length() > 0 && s.charAt(0) == '%') continue;
			if (s.length() > 0 && s.charAt(0) == '.') {
				s = s.substring(1);
				if (s.length() == 0)
					break;
			}
			b.append(s).append("\r\n");
		}
		return b.toString();
	}
	
	static Pattern colon = Pattern.compile("\\s*:\\s*");

	public static String readValue(String attribute, LNScanner sc, Scanner lsc)
			throws ParseError {
		try {
			lsc.skip(colon);
			return lsc.hasNextLine() ? lsc.nextLine() : readMultiline(sc);
		} catch (NoSuchElementException e) {
			throw new ParseError("No attribute value found at " + sc);
		}
	}
	
	public static class ParseError extends Exception {
		public ParseError(String msg) {
			super(msg);
		}

		private static final long serialVersionUID = 1L;}
	
	public static AttrValue parseAttribute(LNScanner sc) throws ParseError {
		String line;
		do {
			line = sc.nextLine();
		} while (line.length() == 0 || line.charAt(0) == '%');

		Scanner lsc = new Scanner(line);
		try {
			lsc.useDelimiter(colon);
			String attribute;
			try {
				attribute = lsc.next();
			} catch (NoSuchElementException e) {
				throw new ParseError("No attribute found at line " + sc.lineNo());
			}
			attribute = attribute.trim();
			String value = Parsing.readValue(attribute, sc, lsc);
			return new AttrValue(attribute, value);
		} finally {
			lsc.close();
		}
	}
	
	public static NoSuchElementException none = new NoSuchElementException();
	
	public static class AttrValue {
		public final String attribute;
		public final String value;
		public AttrValue(String a, String v) {
			attribute = a;
			value = v;
		}
	}
}
