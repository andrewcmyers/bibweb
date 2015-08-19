package bibweb;

import static bibweb.Parsing.isMultilineValue;
import static java.lang.System.out;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;

import bibweb.Parsing.ParseError;
import bibweb.PubInfo.GetPubCtxt;
import bibweb.Tex2HTML.T2HErr;
import easyIO.Recognizer;
import easyIO.Regex;
import easyIO.Scanner;
import easyIO.UnexpectedInput;
import static bibweb.Parsing.rhsClosed;

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
	Scanner input;

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
		ExtInfo pub_access = new PubInfo(pubs, new GetPubCtxt() {
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
	final String[] usage_msg = {
			"",
			"Script commands:",
			"  bibfile: <bibfile.bib>    % read a bibliography file",
			"  pubs: <pubinfo>           % specify pubs to use (by key)",
			"  .topic: <topics>          % specify publication topics",
			"  include <script>          % read input from another script",
			"  generate: <subcommands>   % generate an HTML output file",
			"  .output: <output.html>    % specify HTML output destination",
			"  .section: <subcommands>   % create a list of publications",
			"  ..select: <selectors>     % choose publications for current section",
			"  ...pubtype: <type>        % choose pubs by type, e.g., 'inproceedings'",
			"  ...topic: <type>          % choose pubs by topic",
			"  .                         % end a multiline command",
			"  ..                        % end a multiline subcommand (etc.)",
			"",
			"Multiline commands and definitions are ended with a single period.",
			"Initial periods are ignored in command lines. New macro definitions can be",
			"added anywhere."
	};
	protected void help() {
		usage();
		for (String ln : usage_msg)
			out.println(ln);
	}
	
	protected void dumpDefns() {
		out.println("Default definitions:\r\n");
		for (int i = 0; i < BuiltinMacros.macros.length; i++) {
			out.print(BuiltinMacros.macros[i][0]);
			out.print(": ");
			String value = BuiltinMacros.macros[i][1];
			if (value.contains("\n")) {
				out.println("\n" + value);
				out.println(".");
			} else {
				out.println(value);
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
			input = new Scanner(new FileReader(inputFile), inputFile);
			try {
				runScript(input);
			} finally {
				input.close();
			}
		} catch (FileNotFoundException e) {
			System.err.println("File not found: " + inputFile);
		} catch (IOException e) {
			System.err.println("IO Exception in " + inputFile + ": " + e.getMessage());
		}
	}

	protected void runScript(Scanner sc) {
		while (sc.hasNext()) {
			runScriptLine(sc);
		}
		if (!generated) {
			out.println("No 'generate' command found, nothing generated.");
		}
	}

	
	private void readPublications(Scanner sc) {
		boolean multiline = isMultilineValue(sc);
		if (!multiline) {
			out.println("Publication list must use braces ({}) at " + sc.location());
			return;
		}
		while (!rhsClosed(sc, multiline)) {
			String pubname = Parsing.parseAttribute(sc);
			try {
				Publication p = new Publication(pubname, sc, db);
				pubs.put(pubname,  p);
			} catch (ParseError e) {
				out.println("Parse error " + e.getMessage() + " at " + sc.location());
				return;
			}
		}
	}

	/**
	 * execute a line of the script with form 'attribute: value', where
	 * 'attribute' may be a command or an attribute to be defined.
	 */
	protected void runScriptLine(Scanner sc) {		
		String attribute = Parsing.parseAttribute(sc);

		switch (attribute) {
		case "bibfile":
			bibFile = Parsing.parseValue(sc);
			BibTeXParser parser;
			try {
				parser = new BibTeXParser();
			} catch (TokenMgrException e) {
				System.err.println("Failed reading bib file at " + sc.location());
				return;
			} catch (ParseException e) {
				System.err.println("Failed reading bib file at " + sc.location());
				return;
			}
			try {
				db = parser.parseFully(new FileReader(expand(bibFile)));
			} catch (IOException e) {
				System.err.println("IO exception parsing bib file at " + sc.location());
			}
			if (db != null)
				out.println("Found " + db.getObjects().size()
					+ " objects in BibTeX file.");
			break;
		case "pubs":
			if (db == null) {
				out.println("Warning: should read the bib file before reading the 'pubs' list at "
						+ sc.location());
				return;
			}
			readPublications(sc);

			System.out.println("Found " + pubs.size()
					+ " publications in script.");
			break;
		case "generate":
			generated = true;
			generate(sc);
			break;
		case "include":
			Reader r = null;
			File inpf = new File(inputFile);
			String fname = Parsing.parseValue(sc);
			File inc = new File(inpf.getParent(), fname);
			try {
				r = new FileReader(inc);
				sc.includeSource(r, fname);
			} catch (FileNotFoundException e) {
				System.out.println(fname + ":" + e.getMessage());
			}
			break;
		default:
			t2h.addMacro(attribute, Parsing.parseValue(sc));
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
			Scanner s = new Scanner(new StringReader(a), a);
			Recognizer r = Regex.constant(", ");
			String surname = Regex.parseToPattern(s, r);
			try { Regex.scanPattern(s, r); } catch (UnexpectedInput e) {}
			String firstname = Regex.parseToPattern(s, r);
			b.append(firstname);
			b.append(" ");
			b.append(surname);
			if (s.hasNext()) {
				try { Regex.scanPattern(s, r); } catch (UnexpectedInput e) {}
				String jr = Regex.parseToPattern(s, r);
				b.append(", "); b.append(jr);
			}
			try { s.close(); } catch (IOException e) {}
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

	protected Filter createFilter(final String selector, final String value) {
		switch (selector) {
		case "pubtype":
			return new Filter() {
				public boolean select(Publication p) {
					return p.pubType().equals(value);
				}
			};
		case "topic":
			return new Filter() {
				public boolean select(Publication p) {
					return p.topics.contains(value);
				}
			};
		case "author":
			return new Filter() {
				public boolean select(Publication p) {
					for (String a : p.authors()) {
						if (a.equals(value))
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
					String v = p.field(selector, new Key(selector));
					return (v != null && value.equals(v));
				}
			};
		}
	}

	protected void generate(Scanner sc) {
		t2h.push();

		String fname = null;
		PrintWriter w = null;
		boolean sections = false;
		boolean header = false;
		boolean multiline = Parsing.isMultilineValue(sc);
		if (!multiline) {
			System.out.println("generate command requires braces {} : " + sc.location());
			return;
		}
		try {
			while (!rhsClosed(sc, multiline)) {
				String attribute = Parsing.parseAttribute(sc);
				switch (attribute) {
				case "output":
					fname = Parsing.parseValue(sc);
					System.out.println("Creating output file " + fname);
					if (w != null) {
						System.err
								.println("Cannot have two outputs defined for one bib generation: " + sc);
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
					generateSection(w, sc);
					break;
				default:
					t2h.addMacro(attribute, Parsing.parseValue(sc));
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
			t2h.pop();
			if (w != null)
				w.close();
		}
	}

	protected void generateSection(PrintWriter w, Scanner sc) throws ParseError {
		Collection<Publication> selected = new HashSet<>();
		t2h.push();
		boolean any_select = false;
		boolean multiline = isMultilineValue(sc);
		
		try {
			while (!rhsClosed(sc, multiline)) {
				String attribute = Parsing.parseAttribute(sc);
				
				switch (attribute) {
				case "select":
					any_select = true;
					
					boolean ml_select = isMultilineValue(sc);
					List<Filter> filters = new ArrayList<Filter>();
					while (!rhsClosed(sc, ml_select)) {
						String selector = Parsing.parseAttribute(sc);
						String value = Parsing.parseValue(sc);
						filters.add(createFilter(selector, value));
					}

					nextpub: for (Publication p : pubs.values()) {
						for (Filter f : filters) {
							if (!f.select(p)) continue nextpub;
						}
						selected.add(p);
					}
					break;
				default:
					t2h.addMacro(attribute, Parsing.parseValue(sc));
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
		if (p.title() != null) ctxt.add("title", expand(p.title(), true));
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
