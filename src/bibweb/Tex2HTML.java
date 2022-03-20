package bibweb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import bibweb.Namespace.LookupFailure;

public class Tex2HTML {
	Context context = new Context();
	private ExtInfo ext_info;
	private static final boolean showbraces = false;
	
	Tex2HTML(ExtInfo ext)
	{
		String[][] builtin_macros = BuiltinMacros.macros;
		for (int i = 0; i < builtin_macros.length; i++) {
			context.add(builtin_macros[i][0], builtin_macros[i][1]);
		}
		this.ext_info = ext;
	}

	public void push() {
		context.push();
	}

	public void pop() {
		context.pop();
	}

	public void addMacro(String from, String to) {
		if (to != null)
		context.add(from, to);
	}

	private static enum State {
		Start,         // beginning of string
		Normal,
		Whitespace,    // previous char was regular whitespace
		Backslash,     // just saw a backslash
		AlphMacroName, // in middle of (or at end of) alphabetic macro name
		FullMacro,     // seen a complete macro invocation, but possibly some arguments still to come
		ShortMacroArg, // seen a non-alphabetic macro name, looking for short or long argument
		LongMacroArg,  // in middle of parsing a long macro arg (in braces)
		EOF            // end of file seen
	}
	
	static Set<String> special_macros = new HashSet<>();
	{
		special_macros.add("ifdef");
		special_macros.add("ifndef");
		special_macros.add("ifeq");
		special_macros.add("ifne");
		special_macros.add("pubinfo");
		special_macros.add("setpubinfo");
		special_macros.add("def");
		special_macros.add("depth");
	}

	String convert(String s, boolean sentence_case) throws T2HErr {
		State state = State.Start;
		StringBuilder macro_name = null;
		List<String> macro_args = null;
		StringBuilder cur_arg = null;
		StringBuilder ret = new StringBuilder();
		final char eof = (char) -1;
		int brace_depth = 0, macro_depth = 0;
		try {
			context.push();

			Input inp = new StringInput(s);
			char c;
			while (state != State.EOF) {
				if (inp.hasNext())
					c = inp.next();
				else
					c = eof;
				switch (state) {
				case Normal:
				case Whitespace:
				case Start:
					switch (c) {
					case eof:
						state = State.EOF;
						break;
					case '\\':
						state = State.Backslash;
						break;
					case '{':
						brace_depth++;
						if (showbraces) System.out.println("incrementing brace depth to " + brace_depth + " at " + inp);
						context.push();
						state = State.Normal;
						break;
					case '}':
						if (brace_depth <= 0)
							throw new T2HErr(
									"More closing braces than opening ones: " + inp);
						brace_depth--;
						if (showbraces) System.out.println("decrementing brace depth to " + brace_depth + " at " + inp);
						context.pop();
						state = State.Normal;
						break;
					case '-':
						if (inp.peek() == '-') {
							inp.next();
							if (inp.peek() == '-') {
								inp.next();
								ret.append("&mdash;");
							} else {
								ret.append("&ndash;");
							}
						} else {
							ret.append("-");
						}
						state = State.Normal;
						break;
					case '~':
						ret.append("&nbsp;");
						state = State.Normal;
						break;
					case '\r':
						if (inp.peek() == '\n') {
							ret.append("\r\n");
							state = State.Whitespace;
						}
						break;
					default:
						if (Character.isAlphabetic(c) && sentence_case
								&& brace_depth == 0 && state != State.Start) {
							ret.append(Character.toLowerCase(c));
							state = State.Normal;
						} else if (Character.isWhitespace(c)
								&& state != State.Normal) {
							// consume extra whitespace
							state = State.Whitespace;
						} else {
							ret.append(c);
							if (Character.isWhitespace(c))
								state = State.Whitespace;
							else
								state = State.Normal;
						}
					}
					break;
				case Backslash:
					switch (c) {
					case '\\':
					case eof:
						ret.append('\\');
						state = (c == eof) ? State.EOF : State.Normal;
						break;
					/* Use of \ for verbatim characters. */
					case '{':
					case '}':
					case '-':
					case '&':
					case '#':
					case '_':
					case '%':
						ret.append(c);
						state = State.Normal;
						break;
                    case ' ':
                        ret.append("&nbsp;");
                        break;
					case '\'': // one-char macros
					case '`':
					case '"':
					case ',':
					case '~':
                    case '^':
						macro_name = new StringBuilder();
						macro_name.append(c);
						state = State.ShortMacroArg;
						cur_arg = null;
						break;
                    case '/':
					case '@': // ignore
                        state = State.Normal;
                        break;
					default:
						if (Character.isAlphabetic(c)) {
							macro_name = new StringBuilder();
							macro_name.append(c);
							state = State.AlphMacroName;
							break;
						} else {
							throw new T2HErr(
									"Unexpected char after backslash: " + c);
						}
					}
					break;
				case AlphMacroName:
					if (Character.isAlphabetic(c)) {
						assert macro_name != null;
						macro_name.append(c);
						// stay in state
					} else if (c == '{') {
						state = State.LongMacroArg;
						macro_args = new ArrayList<>();
						cur_arg = new StringBuilder();
						
						macro_depth = brace_depth;
						if (showbraces) System.out.println("incrementing brace depth in macro (1) to " + brace_depth + " at " + inp);
						brace_depth++;
					} else {
						if (c != eof)
							inp.push(Character.toString(c));
						assert macro_name != null;
						String name = macro_name.toString();
						if (special_macros.contains(name)) {
							throw new T2HErr("Unexpected character \'" + c
									+ "\': special macro \\" + name
									+ " expects argument in braces");
						} else {
							inp.push(expandMacro(name));
						}
						state = State.Normal;
					}
					break;
				case LongMacroArg: // in middle of argument held in braces
					if (c == '{') {
						assert cur_arg != null;
						cur_arg.append(c);
						brace_depth++;
						if (showbraces) System.out.println("incrementing brace depth in macro (2) to " + brace_depth + " at " + inp);
						// state = State.LongMacroArg;
					} else if (c == '}') {
						brace_depth--;
						if (showbraces) System.out.println("decrementing brace depth in macro to " + brace_depth + " at " + inp);
						assert brace_depth >= macro_depth;
						if (brace_depth == macro_depth) {
							state = State.FullMacro;
                            if (cur_arg == null) {
                                System.out.println("cur_arg is null");
                            } else if (macro_args == null) {
                                System.out.println("macro args are null, cur_arg = " + cur_arg);

                            } else {
                                macro_args.add(cur_arg.toString());
                            }
						} else {
							assert cur_arg != null;
							cur_arg.append(c);
						}
						// else stay in state
					} else if (c == eof) {
						throw new T2HErr("unexpected end to macro argument.");
					} else {
						assert cur_arg != null;
						cur_arg.append(c);
						// stay in state
					}
					break;
				case FullMacro:
					if (c == '{') {
						brace_depth++;
//						System.out.println("Incrementing brace depth at full macro " + inp);
						state = State.LongMacroArg;
						cur_arg = new StringBuilder();
					} else {
						assert macro_name != null && macro_args != null;
						String name = macro_name.toString();
						inp.push(Character.toString(c));
						if (special_macros.contains(name)) {
							handleSpecialMacro(inp, name, macro_args);
						} else {
							inp.push(expandMacro(name, macro_args));
						}
						state = State.Normal;
					}
					break;
				case ShortMacroArg:
					if (c == '{') {
						state = State.LongMacroArg;
						macro_depth = brace_depth;
						brace_depth++;
						cur_arg = new StringBuilder();
						macro_args = new ArrayList<>();
					} else if (c == eof) {
						assert macro_name != null;
						inp.push(expandMacro(macro_name.toString(),
								new ArrayList<String>()));
						state = State.Normal;
					} else if (c == '\\' && cur_arg == null) {
						List<String> args = new ArrayList<String>();
						cur_arg = new StringBuilder(c);
						// keep reading argument
					} else {
						List<String> args = new ArrayList<String>();
						if (cur_arg == null) {
							args.add(Character.toString(c));
						} else {
							cur_arg.append(c);
							if (Character.isAlphabetic(c)) {
								while (Character.isAlphabetic(inp.peek())) {
									cur_arg.append(inp.next());
								}
								args.add(expandMacro(cur_arg.toString()));
							} else {
								args.add(cur_arg.toString());
							}
						}
						assert macro_name != null;
						inp.push(expandMacro(macro_name.toString(), args));
						state = State.Normal;
					}
					break;
				default:
					throw new T2HErr("Unexpected state " + state);
				}
			}
			return ret.toString();
		} finally {
			context.pop();
		}
	}

	boolean inScope(String name) {
		try {
			context.lookup(name);
			return true;
		} catch (LookupFailure f) {
		}
		return false;
		}

	private void handleSpecialMacro(Input inp, String name, List<String> args) throws T2HErr {
//		 System.out.println("handling special macro \\" + name + args);
		switch (name) {
		case "ifdef":
		case "ifndef":
			if (args.size() != 2) throw new T2HErr("\\ifdef and \\ifndef expect 2 arguments (not " + args.size() + ")");
			String n = args.get(0);
			if (n.charAt(0) == '\\') n = n.substring(1);
			if (inScope(n) == name.equals("ifdef"))
				inp.push(args.get(1));
			break;
		case "ifeq":
		case "ifne":
			if (args.size() != 3) throw new T2HErr("\\ifeq and \\ifne expect 3 arguments (not " + args.size() + ")");
			String e1 = convert(args.get(0), false),
					e2 = convert(args.get(1), false);
			if (e1.equals(e2) == name.equals("ifeq"))
				inp.push(args.get(2));
			break;
		case "pubinfo": {
			if (args.size() != 2) throw new T2HErr("Usage: \\pubinfo{key}{field} (saw " + args.size() + "args)");

			String key = convert(args.get(0), false);
			String field = convert(args.get(1), false);
			try { inp.push(ext_info.lookup(key, field)); } catch (LookupFailure e) {}
			break;
		}
		case "setpubinfo": {
			if (args.size() != 3) throw new T2HErr("Usage: \\setpubinfo{key}{field}{value} (saw " + args.size() + "args)");
			String key = convert(args.get(0), false);
			String field = convert(args.get(1), false);
			String value = convert(args.get(2), false);
			try { ext_info.put(key, field, value); } catch (LookupFailure e) {
				throw new Error("Cannot set field " + field + " of nonexistent publication " + key);
			}
			break;
		}
		case "def":
			if (args.size() != 2) throw new T2HErr("Usage: \\def{macro}{expansion}");
			context.add(args.get(0), args.get(1));
			break;
		case "depth":
			inp.push("" + context.depth());
			break;
		default:
			throw new Error("Internal error: Not a special macro: " + name);
		}
	}

	private String expandMacro(String macro_name) {
//		System.out.println("expanding simple macro \\" + macro_name);
		try {
			return context.lookup(macro_name);
		} catch (LookupFailure e) {
			return "<em>Don't know how to expand parameterless macro "
							+ macro_name + "</em>";
		}
	}

	private String expandMacro(String macro_name, List<String> macro_argument) {
		assert macro_argument != null;
//	 System.out.println("handling macro \\" + macro_name + macro_argument);
		try {
			String result = context.lookup(macro_name);
			// XXX should watch for escaped # here.

			for (int i = 0; i < macro_argument.size(); i++) {
				result = result.replaceAll("#" + (i+1), macro_argument.get(i));
			}
			return result;
		} catch (LookupFailure e) { }

		try {
			if (macro_argument.size() > 0) {
				try {
					String arg = convert(macro_argument.get(0), false);
					return context.lookup(macro_name + arg);
				} catch (T2HErr e) {}
			}
		} catch (LookupFailure e) {
		}

		return "<em>don't know how to expand macro " + macro_name + "</em>";
	}

	public String lookup(String n) throws LookupFailure {
		return context.lookup(n);
	}

	public void push(Namespace n) {
		context.push(n);
	}
	
	@SuppressWarnings("serial")
	public static class T2HErr extends Exception {
		public T2HErr(String string) {
			super(string);
		}
	}
}
