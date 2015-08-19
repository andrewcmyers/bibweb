package bibweb;

import static bibweb.Parsing.isMultilineValue;
import static bibweb.Parsing.rhsClosed;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.Value;

import bibweb.Parsing.ParseError;
import easyIO.Recognizer;
import easyIO.Regex;
import easyIO.Scanner;

// A publication. Typically parsed from the input file and overlaid with information
// from the script.
public class Publication {
	protected String key;
	protected Key bibkey;
	protected ArrayList<String> topics;
	protected Map<String, String> defns = new HashMap<>();
	protected Namespace bindings;
	protected BibTeXDatabase db;

	Publication(String k, Scanner sc, BibTeXDatabase db) throws ParseError {
		this.db = db;
		key = k;
		bibkey = new Key(k);
		topics = new ArrayList<String>();
		boolean multiline = Parsing.isMultilineValue(sc);
		while (!rhsClosed(sc, multiline)) {
			String attribute = Parsing.parseAttribute(sc);
			switch (attribute) {
			case "topic":
				boolean ml_topic = isMultilineValue(sc);

				while (!rhsClosed(sc, ml_topic)) {
//					System.out.print("sc0: " + sc);
					topics.add(Regex.parseToPattern(sc, Regex.oneOrMore(Regex.whitespace())));
					
//					System.out.print("sc1: " + sc);
					sc.trailingWhitespace();
					
//					System.out.print("sc2: " +sc);
				}
//				System.out.println("topics are " + topics);
//				System.out.println("scanner: " + sc);
				break;
			default:
				String value = Parsing.parseValue(sc);
				defns.put(attribute, value);
			}
		}
	}

	public String toString() {
		return key;
	}
	
	BibTeXEntry entry() {
		return db.getEntries().get(bibkey);
	}

	String field(String override, Key k) {
		if (defns.containsKey(override)) return defns.get(override);
		BibTeXEntry entry = entry();
		if (entry == null) return null;
		Value v = entry.getField(k);
		if (v == null)
			return null;
		return v.toUserString();
	}

	String title() {
		return field("title", BibTeXEntry.KEY_TITLE);
	}

	String author() {
		return field("authors", BibTeXEntry.KEY_AUTHOR);
	}

	String url() {
		return field("url", BibTeXEntry.KEY_URL);
	}

	String pages() {
		String pp = field("pages", BibTeXEntry.KEY_PAGES);
		if (pp == null)
			return null;
		return pp.replaceAll("--", "&ndash;"); // this should be handled by
											   // TeX2HTML
	}

	static Key KEY_venueURL = new Key("venueurl");

	String venueURL() {
		return field("venueurl", KEY_venueURL);
	}

	String pubType() {
		if (defns.containsKey("pubtype")) return defns.get("pubtype");
		BibTeXEntry e = entry();
		if (e == null) return "unknown";
		else return e.getType().getValue().toLowerCase();
	}

	String venue() {
		switch (pubType()) {
		case "inproceedings":
			return field("booktitle", BibTeXEntry.KEY_BOOKTITLE);
		case "article":
			return field("journal", BibTeXEntry.KEY_JOURNAL);
		default:
			return "(unknown venue)";
		}
	}

	String[] authors() {
		ArrayList<String> auths = new ArrayList<String>();
		String a = author();
		if (a == null) return new String[0];
		Scanner s = new Scanner(new StringReader(a), a);
		Recognizer and_r = Regex.concat(Regex.oneOrMore(Regex.whitespace()),
				Regex.concat(Regex.constant("and"),
						Regex.oneOrMore(Regex.whitespace())));
		while (s.hasNext())
			auths.add(Regex.parseToDelimiter(s, and_r));
		return auths.toArray(new String[0]);
	}

	public String bibtexYear() {
		return field("year", BibTeXEntry.KEY_YEAR);
	}
	int year() {
		try {
			return Integer.parseInt(bibtexYear());
		} catch (NumberFormatException e) {
			System.err.println("Bad year in publication " + key);
			return 0;
		}
	}

	public String bibtexMonth() {
		return field("month", BibTeXEntry.KEY_MONTH);
	}
	int month() {
		String m = bibtexMonth();
		for (int i = 0; i < 12; i++)
			if (Main.month_names[i].equals(m))
				return i + 1;
		return 0;
	}
	
	String number() {
		return field("number", BibTeXEntry.KEY_NUMBER);
	}
	String volume() {
		return field("volume", BibTeXEntry.KEY_VOLUME);
	}

	public String institution() {
		return field("institution", BibTeXEntry.KEY_INSTITUTION);
	}

	public String school() {
		return field("school", BibTeXEntry.KEY_SCHOOL);
	}
}
