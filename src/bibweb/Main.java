package bibweb;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.ObjectResolutionException;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;

import bibweb.PubInfoImpl.GetPubCtxt;

public class Main {
	String[] args;
	String inputFile;
	BibTeXDatabase db;
	HashMap<String, Publication> pubs;
	/** Records the generated namespace for each publication. */
	Map<Publication, Namespace> pub_defns = new HashMap<>();

	String bibFile;
	boolean generated = false;
	Tex2HTML t2h;

	public static String[] month_names = { "January", "February", "March",
			"April", "May", "June", "July", "August", "September", "October",
			"November", "December" };
	HashMap<String, Integer> months = new HashMap<>();
	{
		for (int i = 0; i < 12; i++) {
			months.put(month_names[i], i);
		}
	}

	Main(String[] args) {
		this.args = args;
		pubs = new HashMap<String, Publication>();
		t2h = new Tex2HTML(new PubInfoImpl(pubs, new GetPubCtxt() {
			public Namespace get(Publication p) {
				return getPubCtxt(p);
			}
		}));
	}

	public static void usage() {
		System.err.println("Usage: bibweb [--help | --defns | <script-file> ]");
	}

	public static void main(String[] args) {
		Main me = new Main(args);
		me.run();
	}
	public void parseArgs() {
		if (args.length != 1) {
			usage();
			System.exit(1);
		}

		if (args[0].equals("--help")) {
			help();
			System.exit(0);
		} else if (args[0].equals("--defns")) {
			dumpDefns();
			System.exit(0);
		} else {
			inputFile = args[0];
		}
	}
	public void help() {
		usage();
		System.out.println("\nScript commands:\r\n");
		System.out.println("  bibfile: <bibfile.bib>    % read a bibliography file");
		System.out.println("  pubs: <pubinfo>           % specify pubs to use (by key)");
		System.out.println("  .topic: <topics>          % specify publication topics");
		System.out.println("  generate: <subcommands>   % generate an HTML output file");
		System.out.println("  .output: <output.html>    % specify HTML output destination");
		System.out.println("  .section: <subcommands>   % create a list of publications");
		System.out.println("  ..select: <selectors>     % choose publications for current section");
		System.out.println("  ...pubtype: <type>        % choose pubs by type, e.g., 'inproceedings'");
		System.out.println("  ...topic: <type>          % choose pubs by topic");
		System.out.println("  .                         % end a multiline command");
		System.out.println("  ..                        % end a multiline subcommand (etc.)");
		
		System.out.println("\r\nMultiline commands and definitions are ended with a single period.");
		System.out.println("Initial periods are ignored in command lines.");
		

	}
	
	void dumpDefns() {
		System.out.println("Default definitions:\r\n");
		for (int i = 0; i < BuiltinMacros.macros.length; i++) {
			System.out.print(BuiltinMacros.macros[i][0]);
			System.out.print(": ");
			String value = BuiltinMacros.macros[i][1];
			if (value.contains("\n")) {
				System.out.println("\n" + value);
				System.out.println(".");
			} else {
				System.out.println(value);
			}
		}
	}
	
	void addEnvMacros() {
		t2h.addMacro("HOME", System.getenv("HOME"));
		t2h.addMacro("USER", System.getenv("USER"));
		t2h.addMacro("DATE", new Date().toString());
	}

	void run() {
		parseArgs();
		addEnvMacros();

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
		if (!generated) {
			System.out
					.println("No 'generate' command found, nothing generated.");
		}
	} // runScript
	
	int lineno;

	private void readPublications(Scanner sc) throws FileNotFoundException,
			IOException {

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
			db = parser.parseFully(new FileReader(expand(bibFile)));
			System.out.println("Found " + db.getObjects().size()
					+ " objects in BibTeX file.");
			break;
		case "pubs":
			if (db == null) {
				System.out.println("Warning: should read the bib file before reading the 'pubs' list at line "
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
			generated = true;
			Scanner gsc = new Scanner(value);
			try {
				generate(gsc);
			} finally {
				gsc.close();
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
		if (s == null)
			return "";
		return expand(s, false);
	}
	
	String normalizeAuthor(String a) {
		if (a.contains(", ")) {
			StringBuilder b = new StringBuilder();
			Scanner s = new Scanner(a);
			s.useDelimiter(", ");
			String surname = s.next();
			String firstname = s.next();
			b.append(firstname);
			b.append(" ");
			b.append(surname);
			if (s.hasNext()) {
				String jr = s.next();
				b.append(", "); b.append(jr);
			}
			s.close();
			return b.toString();
		}
		return a;
	}

	private String formattedAuthors(Publication p) {
		String[] authors = p.authors();
		StringBuilder w = new StringBuilder();
		switch (authors.length) {
		case 0:
			break;
		case 1:
			w.append(expand(normalizeAuthor(authors[0]), false));
			break;
		case 2:
			w.append(expand(normalizeAuthor(authors[0]), false));
			w.append(" and ");
			w.append(expand(normalizeAuthor(authors[1]), false));
			break;
		default:
			for (int j = 0; j < authors.length; j++) {
				if (j > 0)
					w.append(", ");
				if (j == authors.length - 1)
					w.append("and ");
				w.append(expand(normalizeAuthor(authors[j]), false));
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
						if (a.equals(selector.value))
							return true;
					}
					return false;
				}
			};
		case "all":
			return new AllFilter();
		default:
			return new Filter() {
				public boolean select(Publication p) {
					String v = p.field(selector.attribute, new Key(selector.attribute));
					return (v != null && selector.value.equals(v));
				}
			};
		}
	}

	private void generate(Scanner gsc) {
		t2h.push();

		String fname = null;
		PrintWriter w = null;
		boolean sections = false;
		try {
			while (gsc.hasNextLine()) {
				Parsing.AttrValue av = Parsing.parseAttribute(gsc);
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
					sections = true;
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
			if (w != null) {
				generateFooter(w);
			} else {
				System.out
						.println("No output file specified in 'generate', no output generated.");
			}

		} finally {
			if (sections == false) {
				System.out
						.println("No 'section' subcommand used in 'generate', no pubs generated in list.");
			}
			gsc.close();
			t2h.pop();
			if (w != null)
				w.close();
		}

	}

	private void generateSection(PrintWriter w, Scanner ssc) {
		Collection<Publication> selected = new HashSet<>();
		// System.out.println("Starting new section");
		t2h.push();
		boolean any_select = false;
		try {
			while (ssc.hasNextLine()) {
				Parsing.AttrValue av = Parsing.parseAttribute(ssc);
				switch (av.attribute) {
				case "select":
					any_select = true;
					Filter filter = new AllFilter();
					Scanner sssc = new Scanner(av.value);
					List<Filter> filters = new ArrayList<Filter>();
					try {
						while (sssc.hasNextLine()) {
							final Parsing.AttrValue selector = Parsing
									.parseAttribute(sssc);
							filters.add(createFilter(selector));
						}
					} finally {
						sssc.close();
					}
					nextpub: for (Publication p : pubs.values()) {
						for (Filter f : filters) {
							if (!f.select(p)) continue nextpub;
						}
						selected.add(p);
					}
					break;
				default:
					t2h.addMacro(av.attribute, av.value);
					break;
				}
			}
			if (!any_select)
				selected = pubs.values();
			
			Publication[] pa = selected.toArray(new Publication[0]);
			// System.out.println("Selected " + pa.length
			// + " publications for this section.");
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
			return (o2.year() - o1.year()) * 12 + (o2.month() - o1.month());
		}
	};
	Comparator<Publication> byType = new Comparator<Publication>() {
		@Override
		public int compare(Publication o1, Publication o2) {
			return o1.pubType().compareTo(o2.pubType());
		}
	};

	static String crlf = "\r\n";

	String wherePublished(Publication p) {
		StringBuilder b = new StringBuilder();
		switch (p.pubType()) {
		case "inproceedings":
			String s = p.venue();
			b.append("<span class=\"conferencename\">\r\n");
			if (p.venueURL() != null) {
				b.append("<a href=\"");
				b.append(p.venueURL());
				b.append("\">\r\n");
			}
			b.append(expand(s));
			if (p.venueURL() != null)
				b.append("</a>");

			b.append("</span>");

			String pp = p.pages();
			if (pp != null)
				b.append(",\r\npp. " + pp);
			break;
		case "article":
			b.append("<span class=journalname>");
			if (p.venueURL() != null) {
				b.append("<a href=\"");
				b.append(p.venueURL());
				b.append("\">");
			}
			b.append(expand(p.venue()));
			if (p.venueURL() != null)
				b.append("</a>");
			b.append("</span>");
			String volume = p.volume();
			String number = p.number();
			String pages = p.pages();
			if (volume != null) {
				b.append(", ");
				b.append(volume);
				if (number != null) {
					b.append("(");
					b.append(number);
					b.append(")");
				}
				if (pages != null) {
					b.append(":");
					b.append(pages);
				}
			}
			break;
		case "unpublished":
			b.append(expand(p.field("note", BibTeXEntry.KEY_NOTE)));
			break;
		case "software":
			b.append("Software release");
			break;
		case "techreport":
			b.append("Technical report ");
			b.append(p.number());
			b.append(", ");
			b.append(p.institution());
			break;
		case "phdthesis":
			b.append("Ph.D. dissertation, ");
			b.append(p.school());
			break;
		case "mastersthesis":
			b.append("Master's thesis, ");
			b.append(p.school());
			break;
		case "misc":
			b.append(p.field("howpublished", BibTeXEntry.KEY_HOWPUBLISHED));
			break;
		default:
			b.append("<strong>(Unhandled publication type " + p.pubType()
					+ ")</strong>");
		}
		b.append(",\r\n");
		if (p.month() == 0) {
			b.append(p.year());
		} else {
			b.append(month_names[p.month() - 1]);
			b.append(" ");
			b.append(p.year());
		}
		return b.toString();
	}
	
	Namespace getPubCtxt(Publication p) {
		//System.out.println("looking up pub " + p);
		if (pub_defns.containsKey(p)) {
			assert pub_defns.get(p) != null;
			return pub_defns.get(p);
		}

		String where_published = wherePublished(p);
		String authors = formattedAuthors(p);
		
		Context ctxt = new Context();
		ctxt.add("title", p.title());
		ctxt.add("wherepublished", where_published);
		ctxt.add("authors", authors);
		if (p.author() != null) ctxt.add("bibtexAuthors", p.author());
		ctxt.add("pubtype", p.pubType());
		if (p.url() != null) ctxt.add("paperurl", p.url());
		ctxt.add("venue", p.venue());
		ctxt.add("key", p.key);
		ctxt.add("year", p.bibtexYear());
		if (p.institution() != null) ctxt.add("institution", p.institution());
		if (p.volume() != null) ctxt.add("volume", p.volume());
		if (p.number() != null) ctxt.add("number", p.number());
		
		if (p.bibtexMonth() != null)
			ctxt.add("month", p.bibtexMonth());
		if (p.pages() != null)
			ctxt.add("pages", p.pages());
		for (String name : p.defns.keySet()) {
			ctxt.add(name,  p.defns.get(name));
		}
		pub_defns.put(p,  ctxt);
		return ctxt;
	}

	void generatePub(Publication p, PrintWriter w) {
		t2h.push(getPubCtxt(p));
		try {
			w.println(expand("\\pubformat"));
		} finally {
			t2h.pop();
		}
	}
}
