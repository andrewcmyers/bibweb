package bibweb;

import static bibweb.Parsing.isMultilineValue;
import static bibweb.Parsing.rhsClosed;
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

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.Key;
import org.jbibtex.ParseException;
import org.jbibtex.TokenMgrException;

import bibweb.Parsing.ParseError;
import bibweb.Tex2HTML.T2HErr;
import easyIO.Recognizer;
import easyIO.Regex;
import easyIO.Scanner;
import easyIO.UnexpectedInput;

public class Main {
	protected String @NonNull [] args;
    protected Maybe<String> inputFile;
	protected Maybe<BibTeXDatabase> db;
	protected HashMap<String, Publication> pubs;
	/** Records the generated namespace for each publication. */
	Map<Publication, Namespace> pub_defns = new HashMap<>();

	protected String bibFile;
	protected boolean generated = false;
	protected Tex2HTML t2h;

	public static final String @NonNull [] month_names = { "January", "February", "March",
			"April", "May", "June", "July", "August", "September", "October",
			"November", "December" };
	
	final HashMap<String, Integer> months = new HashMap<>();
	{
		for (int i = 0; i < 12; i++) {
			String n = month_names[i]; // why does this warn?
			months.put(n, i);
		}
	}

	protected Main(String[] args) {
		this.args = args;
		pubs = new HashMap<String, Publication>();
		bibFile = "";
		ExtInfo pub_access = new PubInfo(pubs, p -> new PubContext(p));
		t2h = new Tex2HTML(pub_access);
		db = Maybe.none();
		inputFile = Maybe.none();
		addEnvMacros();
		parseArgs();
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
			String a = args[0];
			inputFile = a == null ? Maybe.none() : Maybe.some(a);
		}
	}
	final String[] usage_msg = {
			"",
			"Script commands:",
			"  bibfile: <bibfile.bib>    % read a bibliography file",
			"  include: <script>         % read input from another bibweb script",
			"  pubs: <publist>           % specify pubs to use (by key)",
			"    <key>: <attributes>     % add attributes to publication",
			"      <attr_name>: <val>    % add an attribute with value <val>",
			"      topic: <topics>       % specify publication topics",
			"  generate: <subcommands>   % generate an HTML output file",
			"    output: <output.html>   % specify HTML output destination",
			"    section: <subcommands>  % create a list of publications",
			"    select: <selectors>     % choose publications for current section",
			"      author: <name>        % choose pubs by author",
			"      pubtype: <type>       % choose pubs by type, e.g., 'inproceedings'",
			"      topic: <type>         % choose pubs by topic",
			"      newer: <pub>          % choose pubs newer than <pub>",
			"      <attr>: <value>       % select on other paper attribute",
			"",
			"Multiline commands and definitions use open brace ({) instead of a colon (:)",
			"  and are closed by a closing brace on a line by itself.",
			"New macro definitions can be added anywhere."
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
		try {
			Scanner input = new Scanner(inputFile.get());
			runScript(input);
			input.close();
		} catch (FileNotFoundException e1) {
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
		assert db.hasValue();
		boolean multiline = isMultilineValue(sc);
		if (!multiline) {
			out.println("Publication list must use braces ({}) at " + sc.location());
			return;
		}
		while (!rhsClosed(sc, multiline)) {
			String pubname = Parsing.parseAttribute(sc);
			try {
				Publication p = new Publication(pubname, sc, db.get());
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
				Reader r = new FileReader(expand(bibFile));
				BibTeXDatabase b = parser.parseFully(r);
				r.close();
				db = Maybe.some(b);
			} catch (IOException e) {
				System.err.println("IO exception parsing bib file at " + sc.location());
			}
			
			db.ifsome(db2 ->
				{ out.println("Found " + db2.getObjects().size() + " objects in BibTeX file.");});
			break;
		case "pubs":
			db.if_(db1 -> {
				readPublications(sc);

				System.out.println("Found " + pubs.size()
				+ " publications in script.");
				return;
			},
			() -> out.println("Warning: should read the bib file before reading the 'pubs' list at "
					+ sc.location()));
			break;
		case "generate":
			generated = true;
			generate(sc);
			break;
		case "include":
			Reader r = null;
			File inpf = new File(inputFile.get());
			String fname = expand(Parsing.parseValue(sc));
			File inc = inpf.isAbsolute()
					? new File(fname)
					: new File(inpf.getParent(), fname);
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

	protected String expand(@Nullable String s, boolean b) {
		if (s == null) return "";
		try {
			return t2h.convert(s, b);
		} catch (T2HErr e) {
			return "HTML conversion failed on " + s + " : " + e.getMessage();
		}
	}

	protected String expand(@Nullable String s) {
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
			return (p -> p.pubType().equals(value));
		case "topic":
			return (p -> p.topics.contains(value));
		case "author":
			return (p-> {
					for (String a : p.authors()) {
						if (a.equals(value))
							return true;
					}
					return false;
				});
		case "newer":
			Publication pv = pubs.get(value);
			if (pv == null) {
				System.err.println("Can't find publication in `newer`: " + value);
				return new AllFilter();
			}
			return p -> (compareDates(pv, p) <= 0);
		case "all":
			return new AllFilter();
		default: // general selection on an attribute
			return (p -> {
					String v = p.field(selector, new Key(selector));
					return (v != null && value.equals(v));
				});
		}
	}
	
	static int compareDates(Publication p1, Publication p2) {
		int y1 = p1.year(), y2 = p2.year();
		if (y1 == y2) return p1.month() - p2.month();
		return y1 - y2;
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

	static final protected Comparator<Publication> byYear = (o1, o2) ->
			 (o2.year() - o1.year()) * 12 + (o2.month() - o1.month());

	/** HTML description of where a publication was published. */
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
	
	// A namespace containing various derived attributes of a publication in
	// string form. The namespace is cached for later use, and regenerated if
	// the underlying publication changes. Note that some attributes come
	// "directly" from the publication, but these are numeric attributes.
	class PubContext implements Namespace {
		Publication pub;
		Namespace context = new Context();
		
		public PubContext(Publication p) {
			pub = p;
			if (pub_defns.containsKey(p)) {
				assert pub_defns.get(p) != null;
				context = pub_defns.get(p);
				return;
			}
			init(p);
			pub_defns.put(p, context);
			pub.registerObserver(pub2 -> refresh());
		}
		
		public void refresh() {
			init(pub);
			pub_defns.put(pub, context);
		}
		
		void init(Publication p) {
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
				ctxt.add(name, p.defns.get(name));
			}
			context = ctxt;	
		}

		@Override public String lookup(String name) throws LookupFailure {
			return context.lookup(name);
		}
	}

	protected void generatePub(Publication p, PrintWriter w) {
		t2h.push(new PubContext(p));
		try {
			w.println(expand("\\pubformat"));
		} finally {
			t2h.pop();
		}
	}
}
