package bibweb;

import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Parsing {
	public static String readMultiline(Scanner sc) {
		StringBuilder b = new StringBuilder();
		while (sc.hasNextLine()) {
			String s = sc.nextLine();
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

	public static String readValue(String attribute, Scanner sc, Scanner lsc) {
		lsc.skip(colon);
		return lsc.hasNextLine() ? lsc.nextLine() : readMultiline(sc);
	}
	
	public static AttrValue parseAttribute(Scanner sc) throws NoSuchElementException {
		String line;
		do {
			line = sc.nextLine();
			if (line.equals("end")) throw none;
		} while (line.length() == 0 || line.charAt(0) == '%');

		Scanner lsc = new Scanner(line);
		try {
			lsc.useDelimiter(colon);
			String attribute = lsc.next();
			attribute = attribute.trim();
			// System.out.println("attribute = " + attribute);
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
//			System.out.println("Created attribute " + a + " = " + v);
		}
	}
}
