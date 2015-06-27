package bibweb;

import java.util.ArrayList;
import java.util.HashMap;
//import java.util.InputMismatchException;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.Key;
import org.jbibtex.Value;

import bibweb.Parsing.ParseError;

// parsed from file
public class Publication {
	String key;
	Key bibkey;
	ArrayList<String> topics;
	Map<String, String> defns = new HashMap<>();
	Namespace bindings;
	BibTeXDatabase db;

	Pattern id_pat = Pattern.compile("[a-zA-Z0-9_]([a-zA-Z0-9_]|-)*");

	Publication(String k, LNScanner sc, BibTeXDatabase db) throws ParseError {
		this.db = db;
		key = k;
		bibkey = new Key(k);
		//System.out.println("Created pub " + k);
		topics = new ArrayList<String>();
		while (sc.hasNextLine()) {
			Parsing.AttrValue av = Parsing.parseAttribute(sc);
			switch (av.attribute) {
			case "topic":
				Scanner tops = new Scanner(av.value);
				try {
					while (tops.hasNext(id_pat)) {
						String t = tops.next(id_pat);
						//System.out.print(" " + t);
						topics.add(t);
					}
					//System.out.println();
				} finally {
					tops.close();
				}
				break;
			default:
				defns.put(av.attribute, av.value);
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
												// Latex2html
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
		case "inproceedings": return field("booktitle", BibTeXEntry.KEY_BOOKTITLE);
		case "article": return field("journal", BibTeXEntry.KEY_JOURNAL); 
		default: return "(unknown venue)";
		}
	}

	String[] authors() {
		ArrayList<String> auths = new ArrayList<String>();
		String a = author();
		if (a == null) return new String[0];
		Scanner sc = new Scanner(a);
		sc.useDelimiter("(\\s)(\\s)*and(\\s)(\\s)*");
		while (sc.hasNext()) {
			auths.add(sc.next());
		}
		sc.close();
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
