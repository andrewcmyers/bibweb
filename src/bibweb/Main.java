package bibweb;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.ObjectResolutionException;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;

public class Main {
	String inputFile;
	BibTeXDatabase db;
	HashMap<String, Publication> pubs;

	String bibFile;
	Tex2HTML t2h = new Tex2HTML();

	public static String[] month_names = { "January", "February", "March",
			"April", "May", "June", "July", "August", "September", "October",
			"November", "December" };
	HashMap<String, Integer> months = new HashMap<>();
	{
		for (int i = 0; i < 12; i++) {
			months.put(month_names[i], i);
		}
	}

	Main(String inputFile) {
		this.inputFile = inputFile;
	}

	public static void usage() {
		System.err.println("Usage: bibhtml <input-file.bib>");
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			usage();
			System.exit(1);
		}
		Main me = new Main(args[0]);
		me.run();
	}

	void run() {
		try {
			Scanner sc = new Scanner(new FileReader(inputFile));
			try {
				runScript(sc);
			} finally {
				sc.close();
			}
		} catch (FileNotFoundException e1) {
			System.err.println("File not found: " + e1);
		} catch (TokenMgrException e) {
			System.err.println("Token manager exception: " + e);
		} catch (ParseException e) {
			System.err.println("Parse exception: " + e);
		} catch (ObjectResolutionException e) {
			System.err.println("Cannot find object named \"" + e.getMessage()
					+ "\", missing a @string defn?");
		} catch (IOException e) {
			System.err.println("IO Exception:");
			System.err.println("" + e);
		}
	}

	int lineno = 0;

	Pattern colon = Pattern.compile("\\s*:\\s*");

	private void runScript(Scanner sc) throws IOException, TokenMgrException,
			ParseException {
		while (sc.hasNextLine()) {
			lineno++;
			try {
				Parsing.AttrValue av = Parsing.parseAttribute(sc);
				runScriptLine(av.attribute, av.value);
			} catch (NoSuchElementException e) {
				System.err.println("No attribute value at line " + lineno);
			}
		}
	} // runScript

	private void readPublications(Scanner sc) throws FileNotFoundException,
			IOException {
		pubs = new HashMap<String, Publication>();
		while (sc.hasNextLine()) {
			lineno++;

			Parsing.AttrValue av = Parsing.parseAttribute(sc);
			Scanner pubsc = new Scanner(av.value);
			try {
				Publication p = new Publication(av.attribute, pubsc, db);
				pubs.put(p.key, p);
				// System.out.println("installing " + p.key);
			} finally {
				pubsc.close();
			}
		}
	}

	Pattern whitespace = Pattern.compile("[ \t\r\n][ \t\r\n]*");

	/**
	 * execute a line of the script with form attribute: value, where
	 * 'attribute' may be a command or an attribute to be defined.
	 */
	void runScriptLine(String attribute, String value)
			throws TokenMgrException, ParseException,
			ObjectResolutionException, IOException {

		switch (attribute) {
		case "bibfile":
			bibFile = value;
			BibTeXParser parser = new BibTeXParser();
			db = parser.parseFully(new FileReader(bibFile));
			System.out.println("Found " + db.getObjects().size()
					+ " objects in BibTeX file.");
			break;
		case "pubs":
			if (db == null) {
				System.err
						.println("Must read the bib file before reading the 'pubs' list at line "
								+ lineno);
				return;
			}
			try {
				readPublications(new Scanner(value));
				System.out.println("Found " + pubs.size()
						+ " publications in script.");
			} catch (NoSuchElementException e) {
				System.out.println("No publications in script at line "
						+ lineno);
			}
			break;
		case "generate":
			Scanner sc = new Scanner(value);
			try {
				generate(sc);
			} finally {
				sc.close();
			}
			break;
		default:
			t2h.addMacro(attribute, value);
		}
	}

	private void generateHeader(PrintWriter w) {
		w.print(expand("\\header"));
	}
	private void generateFooter(PrintWriter w) {
		w.print(expand("\\footer"));
	}

	String expand(String s, boolean b) {
		try {
			return t2h.convert(s, b);
		} catch (T2HErr e) {
			return "HTML conversion failed on " + s + " : " + e.getMessage();
		}
	}

	String expand(String s) {
		if (s == null) return "";
		return expand(s, false);
	}

	private String formattedAuthors(Publication p) {
		String[] authors = p.authors();
		StringBuilder w = new StringBuilder();
		switch (authors.length) {
		case 0:
			break;
		case 1:
			w.append(expand(authors[0], false));
			break;
		case 2:
			w.append(expand(authors[0], false));
			w.append(" and ");
			w.append(expand(authors[1], false));
			break;
		default:
			for (int j = 0; j < authors.length; j++) {
				if (j > 0)
					w.append(", ");
				if (j == authors.length - 1)
					w.append("and ");
				w.append(expand(authors[j], false));
			}
			break;
		}
		return w.toString();
	}

	static interface Filter {
		boolean select(Publication p);
	}

	class AllFilter implements Filter {
		public boolean select(Publication p) {
			return true;
		}
	}

	class NoneFilter implements Filter {
		public boolean select(Publication p) {
			return false;
		}
	}

	Filter createFilter(final Parsing.AttrValue selector) {
		switch (selector.attribute) {
		case "pubtype":
			return new Filter() {
				public boolean select(Publication p) {
					return p.pubType().equals(selector.value);
				}
			};
		case "topic":
			return new Filter() {
				public boolean select(Publication p) {
					return p.topics.contains(selector.value);
				}
			};
		case "author":
			return new Filter() {
				public boolean select(Publication p) {
					for (String a : p.authors()) {
						try {
							if (a.equals(t2h.lookup("author"))) return true;
						} catch (Context.LookupFailure e) { return false; }
					}
					return false;
				}
			};
		case "all":
			return new AllFilter();
		}
		return new AllFilter();
	}

	private void generate(Scanner sc) {
		t2h.push();

		String fname = null;
		PrintWriter w = null;
		try {
			while (sc.hasNextLine()) {
				Parsing.AttrValue av = Parsing.parseAttribute(sc);
				switch (av.attribute) {
				case "output":
					fname = av.value;
					System.out.println("Creating output file " + fname);
					if (w != null) {
						System.err
								.println("Cannot have two outputs defined for one bib generation: line "
										+ lineno);
						return;
					}
					try {
						w = new PrintWriter(fname);
					} catch (FileNotFoundException e) {
						System.err.println("Cannot write to " + fname);
						return;
					}
					generateHeader(w);
					w.flush();
					break;
				case "section":
					Scanner ssc = new Scanner(av.value);
					try {
						generateSection(w, ssc);
					} finally {
						ssc.close();
					}
				default:
					t2h.addMacro(av.attribute, av.value);
				}
			}
			generateFooter(w);

		} finally {
			sc.close();
			t2h.pop();
			w.close();
		}

	}

	private void generateSection(PrintWriter w, Scanner ssc) {
		Set<Publication> selected = new HashSet<>();
//		System.out.println("Starting new section");
		t2h.push();
		try {
			while (ssc.hasNextLine()) {
				Parsing.AttrValue av = Parsing.parseAttribute(ssc);
				switch (av.attribute) {
				case "select":
					Filter filter = new AllFilter();
					Scanner sssc = new Scanner(av.value);
					try {
						while (sssc.hasNextLine()) {
							final Parsing.AttrValue selector = Parsing
									.parseAttribute(sssc);
							filter = createFilter(selector);
							for (Publication p : pubs.values()) {
								if (filter.select(p))
									selected.add(p);
							}
						}
					} finally {
						sssc.close();
					}
					break;
				default:
					t2h.addMacro(av.attribute, av.value);
					break;
				}
			}
			Publication[] pa = selected.toArray(new Publication[0]);
//			System.out.println("Selected " + pa.length
//					+ " publications for this section.");
			Arrays.sort(pa, byYear);

			w.println(expand("\\intro"));
			w.println(expand("\\openpaperlist"));
			for (Publication p : pa) {
				w.println("<li>");
				generatePub(p, w);
				w.println("</li>");
			}
			w.println(expand("\\closepaperlist"));
			w.flush();
		} finally {
			t2h.pop();
		}
	}

	Comparator<Publication> byYear = new Comparator<Publication>() {
		@Override
		public int compare(Publication o1, Publication o2) {
			return o2.year() - o1.year();
		}
	};
	Comparator<Publication> byType = new Comparator<Publication>() {
		@Override
		public int compare(Publication o1, Publication o2) {
			return o1.pubType().compareTo(o2.pubType());
		}
	};

//	private void generateByType(String fname, String uri)
//			throws FileNotFoundException {
//		PrintWriter w = new PrintWriter(fname);
//		System.out.println("Generating " + fname + "...");
//		generateHeader(w);
//		ArrayList<Publication> ord = new ArrayList<>();
//		for (String e : pubs.keySet()) {
//			ord.add(pubs.get(e));
//		}
//
//		w.println("<ul class=pubs>");
//		Publication[] pa = ord.toArray(new Publication[0]);
//		Arrays.sort(pa, byYear);
//		Arrays.sort(pa, byType);
//		for (Publication p : pa) {
//			w.println("<li>");
//			generatePub(p, w);
//			w.println("</li>");
//		}
//		w.println("</ul>");
//		w.close();
//	}

	void generatePub(Publication p, PrintWriter w) {

		String url = p.url();
		{
			w.println("<span class=\"papertitle\">\r\n");
			if (url != null) {
				w.print("<a href=\"");
				w.print(url);
				w.println("\">");
			}
			w.print(expand(p.title(), true));
			if (url != null)
				w.print("</a>");
			w.print("</span>"); // papertitle
			w.println(".<br/>");

			switch (p.pubType()) {
			case "inproceedings":
				String s = p.venue();
				w.print("<span class=\"conferencename\">\r\n");
				if (p.venueURL() != null) {
					w.print("<a href=\"");
					w.print(p.venueURL());
					w.print("\">\r\n");
				}
				w.print(expand(s));
				if (p.venueURL() != null)
					w.print("</a>");

				w.print("</span>");

				String pp = p.pages();
				if (pp != null)
					w.print(",\r\npp. " + pp);
				break;
			case "article":
				w.print("<span class=journalname>");
				if (p.venueURL() != null) {
					w.print("<a href=\"");
					w.print(p.venueURL());
					w.print("\">");
				}
				w.print(expand(p.venue()));
				if (p.venueURL() != null)
					w.print("</a>");
				w.print("</span>");
				String volume = p.volume();
				String number = p.number();
				String pages = p.pages();
				if (volume != null) {
					w.print(", ");
					w.print(volume);
					if (number != null) {
						w.print("(");
						w.print(number);
						w.print(")");
					}
					if (pages != null) {
						w.print(":");
						w.print(pages);
					}
				}
				break;
			case "unpublished":
				w.print(expand(p.field("note", BibTeXEntry.KEY_NOTE)));
				break;
			case "software":
				w.print("Software release");
				break;
			default:
				w.print("<strong>(Unhandled publication type " + p.pubType()
						+ ")</strong>");
			}
			w.print(",\r\n");
			if (p.month() == 0) {
				w.print(p.year());
			} else {
				w.print(month_names[p.month() - 1]);
				w.print(" ");
				w.print(p.year());
			}
			w.print(".\r\n");
			w.print(formattedAuthors(p));
			w.println(".");
		}
	}
}
