package bibweb;

public class BuiltinMacros {
	static String[][] macros = {
		{ "url", "<a href=\"#1\">#1</a>" },
		{ "'a", "&aacute;" },
		{ "'e", "&eacute;" },
		{ "'i", "&iacute;" },
		{ "'\\i", "&iacute;" },
		{ "'o", "&oacute;" },
		{ "'u", "&uacute;" },
		{ "'c", "&cacute;" },
		{ "'A", "&Aacute;" },
		{ "'E", "&Eacute;" },
		{ "'I", "&Iacute;" },
		{ "'\\I", "&Iacute;" },
		{ "'O", "&Oacute;" },
		{ "'U", "&Uacute;" },
		{ "'C", "&Cacute;" },
		{ "`i", "&igrave;" },
		{ "`\\i", "&igrave;" },
		{ "`e", "&egrave;" },
        { "^a", "&acirc;" },
        { "^e", "&ecirc;" },
        { "^i", "&icirc;" },
		{ "^\\i", "&icirc;" },
		{ "^o", "&ocirc;" },
        { "^u", "&ucirc;" },
		{ "\"a", "&auml;" },
		{ "\"e", "&euml;" },
		{ "\"i", "&iuml;" },
		{ "\"\\i", "&iuml;" },
		{ "\"o", "&ouml;" },
		{ "\"u", "&uuml;" },
		{ "cc", "&ccedil;" },
		{ "cs", "&scedil;" },
		{ "~n", "&ntilde;" },
		{ "~a", "&atilde;" },
		{ "~e", "&etilde;" },
		{ "~i", "&itilde;" },
		{ "~\\i", "&itilde;" },
		{ "~o", "&otilde;" },
		{ "~u", "&utilde;" },
		{ "l", "&#322;" },
		{ "L", "&#321;" },
		{ "aa", "&angst;" },
		{ "AA", "&Angst;" },
		{ "ae", "&aelig;" },
		{ "AE", "&AElig;" },
		{ "ss", "&szlig;" },
		{ "o", "&oslash;" },
		{ "O", "&Oslash;" },
		{ "i", "&imath;" },
		{ "vC", "&Ccaron;" },
		{ "vc", "&ccaron;" },
		{ "vR", "&Rcaron;" },
		{ "vr", "&rcaron;" },
		{ "\\{", "{" },
		{ "\\}", "}" },
		{ "@", "" },
		{ "--", "&ndash;" },
		{ "---", "&mdash;" },
		{ "~", "&nbsp;" },
		{ "textsuperscript", "<span class=ordinal>#1</span>" },
		{ "texttt", "<tt>#1</tt>" },
		{ "textbf", "<b>#1</b>" },
		{ "textsc", "<sc>#1</sc>" },
		{ "footnotesize", "<small>#1</small>" },
		{ "alpha", "&alpha;" },
		{ "beta", "&beta;" },
		{ "delta", "&delta;" },
		{ "mu", "&mu;" },
		{ "lambda", "&lambda;" },
		{ "pi", "&pi;" },
		{ "rho", "&rho;" },
		{ "tau", "&rho;" },
		{ "lfloor", "&lfloor;" },
		{ "rfloor", "&rfloor;" },
		{ "TeX", "T<sub>E</sub>X" },
		{ "header",
			"\\doctype\r\n" +
			"<html>\r\n" +
			"<head>\r\n" +
			"  <title>\\pagetitle</title>\r\n" +
			"  \\urimeta\r\n" +
			"  \\contenttype\r\n" + 
			"  \\moremeta\r\n" +
			"  \\style\r\n" +
			"  \\morestyle\r\n" +
			"</head>\r\n" +
			"<body>\\opening\r\n" +
			"  \\banner\r\n"
		},
		{ "footer",
			"\\closing\r\n" +
		    "</body>\r\n" +
			"</html>" },
		{ "doctype",     "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\""
				       + " \"http://www.w3.org/TR/html4/loose.dtd\">"},
	    { "pagetitle",       "Publications by \\author" },
	    { "contenttype", "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"},
		{ "moremeta",    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" },
		{ "style",       "  <link href=\"\\stylesheet\" type=\"text/css\" rel=\"stylesheet\">" },
		{ "morestyle",   "" },
		{ "uri",         "" },
		{ "urimeta",     "" },
		{ "opening",     "<div class=paperlist>" },
		{ "closing",     "</div>\\credits" },
		{ "credits",     "<p class=credits>Generated by <a href=\"https://github.com/andrewcmyers/bibweb\">bibweb</a></p>" },
		{ "intro",       "<h2>Publications</h2>" },
		{ "banner",      "<h1>Publications by \\author</h1>" },
		{ "stylesheet",  "default.css" },
		{ "author",      "Unknown Author" },
		{ "openpaperlist", "<ul class=pubs>" },
		{ "closepaperlist", "</ul>" },
		{ "pubformat",
			"<li>\r\n\\authors. \\title.\r\n"
	      + "\\wherepublished.</li>\r\n" },
	    { "\\authors", "(formatted author list)" },
	    { "\\title", "(title of this publication)" },
	    { "\\wherepublished", "(publication venue and pages)" },
	    { "\\year", "(year of this publication)" },
	    { "\\ifdef", "(insert second argument if first is a defined macro)"},
	    { "\\ifndef", "(insert second argument if first is not a defined macro)"},
	    { "\\ifeq", "(insert third argument if first two are equal)"},
	    { "\\ifne", "(insert third argument if first two are not equal)"},
	    { "\\pubinfo", "(\\pubinfo{key}{attribute} looks up an attribute of publication with the given key)"},
	    { "\\setpubinfo", "(\\pubinfo{key}{attribute}{val} redefines an attribute of publication with the given key)"},
	    { "\\def", "(\\def{name}{expansion} defines a new macro. Arguments can be named as #1, #2, etc. in expansion"}
	};
}
