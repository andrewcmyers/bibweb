package bibweb;

public class BuiltinMacros {
	static String[][] macros = {
		{ "url", "<a href=\"#1\">#1</a>" },
		{ "'e", "&eacute;" },
		{ "'i", "&iacute;" },
		{ "\"u", "&uuml;" },
		{ "\"o", "&ouml;" },
		{ "\"a", "&auml;" },
		{ "\\{", "{" },
		{ "\\}", "}" },
		{ "--", "&ndash;" },
		{ "---", "&mdash;" },
		{ "~", "&nbsp;" },
		{ "textsuperscript", "<span class=ordinal>#1</span>" },
		{ "texttt", "<tt>#1</tt>" },
		{ "header",
			"\\doctype\r\n" +
			"<html>\r\n" +
			"<head>\r\n" +
			"  <title>\\title</title>\r\n" +
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
	    { "title",       "Publications by \\author" },
	    { "contenttype", "  <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"},
		{ "moremeta",    "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">" },
		{ "style",       "  <link href=\"\\stylesheet\" type=\"text/css\" rel=\"stylesheet\" />" },
		{ "morestyle",   "" },
		{ "uri",         "" },
		{ "urimeta",     "" },
		{ "opening",     "<div class=paperlist>" },
		{ "closing",     "</div>" },
		{ "intro",       "<h2>Publications</h2>" },
		{ "banner",      "<h1>Publications by \\author</h1>" },
		{ "stylesheet",  "default.css" },
		{ "author",      "John Doe" },
		{ "openpaperlist", "<ul class=pubs>" },
		{ "closepaperlist", "</ul>" },
		{ "pubformat",
			"\\authors. \\pubtitle.\r\n"
	      + "\\wherepublished.\r\n" },
	    { "\\authors", "(formatted author list)" },
	    { "\\pubtitle", "(title of this publication)" },
	    { "\\wherepublished", "(publication venue and pages)" },
	    { "\\year", "(year of this publication)" },
	    // ifdef
	    // ifndef
	    // true
	    // false
	};
}
