package bibweb;

import static bibweb.Parsing.isMultilineValue;
import static bibweb.Parsing.rhsClosed;
import static easyIO.Regex.concat;
import static easyIO.Regex.constant;
import static easyIO.Regex.oneOrMore;
import static easyIO.Regex.parseToDelimiter;
import static easyIO.Regex.parseToPattern;
import static easyIO.Regex.whitespace;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.Value;

import bibweb.Parsing.ParseError;
import easyIO.Recognizer;
import easyIO.Scanner;

// A publication. Typically parsed from the input file and overlaid with information
// from a bibweb script.
public class Publication {
	protected String key;
	protected Key bibkey;
	protected ArrayList<String> topics;
	
	/** Additional definitions overlaid by the script.*/
	protected Map<String, String> defns = new HashMap<>();

	/** The entry in the bibtex file it was found in, if any. */
    protected Optional<BibTeXEntry> entry;

	Publication(String k, Scanner sc, Optional<BibTeXEntry> entry) throws ParseError {
		this.entry = entry;
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
					topics.add(parseToPattern(sc, oneOrMore(whitespace())));
					
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

    Publication(String k, BibTeXEntry entry) {
        this.entry = Optional.of(entry);
        key = k;
        bibkey = new Key(k);
    }

	public String toString() {
		return key;
	}
	
	Optional<BibTeXEntry> entry() {
		return entry;
	}

	// may return null
	String field(String override, Key k) {
		if (defns.containsKey(override)) return defns.get(override);
		Optional<BibTeXEntry> entry = entry();
		if (entry.isEmpty()) return null;
		Value v = entry.get().getField(k);
		if (v == null)
			return null;
		return v.toUserString();
	}

    // may return null
	String title() {
		return field("title", BibTeXEntry.KEY_TITLE);
	}

    // may return null
	String author() {
		return field("authors", BibTeXEntry.KEY_AUTHOR);
	}

    // may return null
	String url() {
		return field("url", BibTeXEntry.KEY_URL);
	}

    // may return null
	String pages() {
		String pp = field("pages", BibTeXEntry.KEY_PAGES);
		if (pp == null)
			return null;
		return pp.replaceAll("--", "&ndash;"); // this should be handled by
											   // TeX2HTML
	}

	static Key KEY_venueURL = new Key("venueurl");

    // may return null
	String venueURL() {
		return field("venueurl", KEY_venueURL);
	}

	String pubType() {
		if (defns.containsKey("pubtype")) return defns.get("pubtype");
		Optional<BibTeXEntry> e = entry();
        if (e.isEmpty()) return "unknown";
		else return e.get().getType().getValue().toLowerCase();
	}

    // may return null
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
		Recognizer and = concat(oneOrMore(whitespace()),
						 concat(constant("and"),
						        oneOrMore(whitespace())));
		while (s.hasNext())
			auths.add(parseToDelimiter(s, and));
		return auths.toArray(new String[0]);
	}

    // may return null
	public String bibtexYear() {
		return field("year", BibTeXEntry.KEY_YEAR);
	}
	int year() {
		try {
			String y = bibtexYear();
			if (y == null) {
				System.err.println("No year given for publication " + key);
				return 0;
			}
			return strToYear(y);
		} catch (NumberFormatException e) {
			System.err.println("Bad year in publication " + key + ": " +
					bibtexYear());
			return 0;
		}
	}

	/* Parse a bibtex field as a year, in a permissive way.
	*  @throws NumberFormatException if a year cannot be found. */
	int strToYear(String y) throws NumberFormatException {
		String[] fields = y.split("\s+");
		return Integer.parseInt(y.split("\s+")[fields.length - 1]);
	}

    // may return null
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
	
    // may return null
	String number() {
		return field("number", BibTeXEntry.KEY_NUMBER);
	}

    // may return null
	String volume() {
		return field("volume", BibTeXEntry.KEY_VOLUME);
	}

    // may return null
	public String institution() {
		return field("institution", BibTeXEntry.KEY_INSTITUTION);
	}

    // may return null
	public String school() {
		return field("school", BibTeXEntry.KEY_SCHOOL);
	}
	
	interface Observer {
		public void update(Publication p);
	}
	
	Collection<Observer> observers = new LinkedList<Observer>();
	
	void registerObserver(Observer o) {
		observers.add(o);
	}
	void notifyObservers() {
		for (Observer o : observers) {
			o.update(this);
		}
	}

	public void put(String k, String value) {
		defns.put(k, value);
		notifyObservers();
	}
}
