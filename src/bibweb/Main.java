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
import java.util.Scanner;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;

import bibweb.Parsing.ParseError;
import bibweb.PubInfoImpl.GetPubCtxt;

public class Main {
	protected String[] args;
	protected String inputFile;
	protected BibTeXDatabase db;
	protected HashMap<String, Publication> pubs;
	/** Records the generated namespace for each publication. */
	Map<Publication, Namespace> pub_defns = new HashMap<>();

	protected String bibFile;
	protected boolean generated = false;
	protected Tex2HTML t2h;

	public static String[] month_names = { "January", "February", "March",
			"April", "May", "June", "July", "August", "September", "October",
			"November", "December" };
	HashMap<String, Integer> months = new HashMap<>();
	{
		for (int i = 0; i < 12; i++)
			months.put(month_names[i], i);
	}

	protected Main(String[] args) {
		this.args = args;
		pubs = new HashMap<String, Publication>();
		PubInfo pub_access = new PubInfoImpl(pubs, new GetPubCtxt() {
			public Namespace get(Publication p) { return getPubCtxt(p); }
		});
		t2h = new Tex2HTML(pub_access);
	}

	protected static void usage() {
		System.err.println("Usage: bibweb [--help | --defns | <script-file> ]");
	}

	public static void main(String[] args) {
		Main me = new Main(args);
		me.run();
	}
	protected void parseArgs() {
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
	protected void help() {
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
	
	protected void dumpDefns() {
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
	
	protected void addEnvMacros() {
		t2h.addMacro("HOME", System.getenv("HOME"));
		t2h.addMacro("USER", System.getenv("USER"));
		t2h.addMacro("DATE", new Date().toString());
	}

	protected void run() {
		parseArgs();
		addEnvMacros();

		try {
			LNScanner sc = new LNScanner(new FileReader(inputFile));
			try {
				runScript(sc);
			} finally {
				sc.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + inputFile);
		}
	}

	protected void runScript(LNScanner sc) {
		while (sc.hasNextLine()) {
			int lineno = sc.lineNo();
			Parsing.AttrValue av;
			try {
				av = Parsing.parseAttribute(sc);
			} catch (ParseError e) {
				System.err.println("Parse error: " + e.getMessage());
				continue;
			}
			runScriptLine(av.attribute, av.value, lineno);

		}
		if (!generated) {
			System.out
					.println("No 'generate' command found, nothing generated.");
		}
	}

	protected void readPublications(LNScanner sc) throws Parsing.ParseError {
		while (sc.hasNextLine()) {
			Parsing.AttrValue av = Parsing.parseAttribute(sc);
			LNScanner pubsc = new LNScanner(av.value, sc.lineNo());
			try {
				Publication p = new Publication(av.attribute, pubsc, db);
				pubs.put(p.key, p);
			} finally {
				pubsc.close();
			}
		}
	}

	/**
	 * execute a line of the script with form 'attribute: value', where
	 * 'attribute' may be a command or an attribute to be defined.
	 */
	protected void runScriptLine(String attribute, String value, int lineno) {
		switch (attribute) {
		case "bibfile":
			bibFile = value;
			BibTeXParser parser;
			try {
				parser = new BibTeXParser();
			} catch (TokenMgrException e) {
				System.err.println("Failed reading bib file at line " + lineno + ": " + e.getMessage());
				return;
			} catch (ParseException e) {
				System.err.println("Failed reading bib file at line " + lineno + ": " + e.getMessage());
				return;
			}
			try {
				db = parser.parseFully(new FileReader(expand(bibFile)));
			} catch (IOException e) {
				System.err.println("IO exception parsing bib file at " + lineno);
			}
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
				readPublications(new LNScanner(value, lineno));

				System.out.println("Found " + pubs.size()
						+ " publications in script.");
			} catch (ParseError e) {
				System.out.println("No publications in script at line "
						+ lineno + ": " + e.getMessage());
			}
			break;
		case "generate":
			generated = true;
			LNScanner gsc = new LNScanner(value, lineno);
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

	protected void generateHeader(PrintWriter w) {
		w.print(expand("\\header"));
	}

	protected void generateFooter(PrintWriter w) {
		w.print(expand("\\footer"));
	}

	protected String expand(String s, boolean b) {
		try {
			return t2h.convert(s, b);
		} catch (T2HErr e) {
			return "HTML conversion failed on " + s + " : " + e.getMessage();
		}
	}

	protected String expand(String s) {
		if (s == null)
			return "";
		return expand(s, false);
	}
	
	protected String normalizeAuthor(String a) {
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

	protected String formattedAuthors(Publication p) {
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

	protected static interface Filter {
		boolean select(Publication p);
	}

	protected static class AllFilter implements Filter {
		public boolean select(Publication p) {
			return true;
		}
	}

	protected Filter createFilter(final Parsing.AttrValue selector) {
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
		default: // general selection on an attribute
			return new Filter() {
				public boolean select(Publication p) {
					String v = p.field(selector.attribute, new Key(selector.attribute));
					return (v != null && selector.value.equals(v));
				}
			};
		}
	}

	protected void generate(LNScanner gsc) {
		t2h.push();

		String fname = null;
		PrintWriter w = null;
		boolean sections = false;
		boolean header = false;
		try {
			while (gsc.hasNextLine()) {
				int lineno = gsc.lineNo();
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
					break;
				case "section":
					sections = true;
					if (w == null) {
						System.err.println("No output defined. Use 'output' subcommand first.");
						return;
					}
					if (!header) {
						generateHeader(w);
						header = true;
					}
					LNScanner ssc = new LNScanner(av.value, lineno);
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

		} catch (ParseError e) {
			System.err.println("Parse error: " + e.getMessage());
		}
		finally {
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

	protected void generateSection(PrintWriter w, LNScanner ssc) throws ParseError {
		Collection<Publication> selected = new HashSet<>();
		t2h.push();
		boolean any_select = false;
		
		try {
			while (ssc.hasNextLine()) {
				int ln = ssc.lineNo();
				Parsing.AttrValue av = Parsing.parseAttribute(ssc);
				switch (av.attribute) {
				case "select":
					any_select = true;
					LNScanner sssc = new LNScanner(av.value, ln);
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
			Arrays.sort(pa, byYear);

			w.println(expand("\\intro"));
			w.println(expand("\\openpaperlist"));
			for (Publication p : pa)
				generatePub(p, w);
			w.println(expand("\\closepaperlist"));
			w.flush();
		} finally {
			t2h.pop();
		}
	}

	protected Comparator<Publication> byYear = new Comparator<Publication>() {
		@Override
		public int compare(Publication o1, Publication o2) {
			return (o2.year() - o1.year()) * 12 + (o2.month() - o1.month());
		}
	};

	protected String wherePublished(Publication p) {
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
			b.append("<strong>(Unhandled publication type "
					+ p.pubType()
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
	
	protected Namespace getPubCtxt(Publication p) {
		if (pub_defns.containsKey(p)) {
			assert pub_defns.get(p) != null;
			return pub_defns.get(p);
		}

		String where_published = wherePublished(p);
		String authors = formattedAuthors(p);
		
		Context ctxt = new Context();
		if (p.title() != null) ctxt.add("title", p.title());
		else ctxt.add("title", "<em>No title</em>");
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

	protected void generatePub(Publication p, PrintWriter w) {
		t2h.push(getPubCtxt(p));
		try {
			w.println(expand("\\pubformat"));
		} finally {
			t2h.pop();
		}
	}
}
