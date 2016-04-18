package bibweb;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.annotation.Nullable;

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

	public void addMacro(String from, @Nullable String to) {
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
		int bracedepth = 0, macrodepth = 0;
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
						bracedepth++;
						if (showbraces) System.out.println("incrementing brace depth to " + bracedepth + " at " + inp);
						context.push();
						state = State.Normal;
						break;
					case '}':
						if (bracedepth <= 0)
							throw new T2HErr(
									"More closing braces than opening ones: " + inp);
						bracedepth--;
						if (showbraces) System.out.println("decrementing brace depth to " + bracedepth + " at " + inp);
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
					default:
						if (Character.isAlphabetic(c) && sentence_case
								&& bracedepth == 0 && state != State.Start) {
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
					case '{':
					case '}':
					case '-':
					case '&':
					case '#':
					case '~':
						ret.append(c);
						state = State.Normal;
						break;
					case '\'':
					case '"':
						macro_name = new StringBuilder();
						macro_name.append(c);
						state = State.ShortMacroArg;
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
						macro_name.append(c);
						// stay in state
					} else if (c == '{') {
						state = State.LongMacroArg;
						macro_args = new ArrayList<>();
						cur_arg = new StringBuilder();
						
						macrodepth = bracedepth;
						if (showbraces) System.out.println("incrementing brace depth in macro (1) to " + bracedepth + " at " + inp);
						bracedepth++;
					} else {
						if (c != eof)
							inp.push(Character.toString(c));
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
						cur_arg.append(c);
						bracedepth++;
						if (showbraces) System.out.println("incrementing brace depth in macro (2) to " + bracedepth + " at " + inp);
						// state = State.LongMacroArg;
					} else if (c == '}') {
						bracedepth--;
						if (showbraces) System.out.println("decrementing brace depth in macro to " + bracedepth + " at " + inp);
						assert bracedepth >= macrodepth;
						if (bracedepth == macrodepth) {
							state = State.FullMacro;
							macro_args.add(cur_arg.toString());
						} else {
							cur_arg.append(c);
						}
						// else stay in state
					} else if (c == eof) {
						throw new T2HErr("unexpected end to macro argument.");
					} else {
						cur_arg.append(c);
						// stay in state
					}
					break;
				case FullMacro:
					if (c == '{') {
						bracedepth++;
//						System.out.println("Incrementing brace depth at full macro " + inp);
						state = State.LongMacroArg;
						cur_arg = new StringBuilder();
					} else {
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
						bracedepth = 1;
						cur_arg = new StringBuilder();
					} else if (c == eof) {
						inp.push(expandMacro(macro_name.toString(),
								new ArrayList<String>()));
						state = State.Normal;
					} else {
						List<String> args = new ArrayList<String>();
						args.add(Character.toString(c));
						cur_arg = new StringBuilder();
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
			if (args.size() != 2) throw new T2HErr("\\ifdef and \\ifndef expect 2 arguments");
			String n = args.get(0);
			if (n.charAt(0) == '\\') n = n.substring(1);
			if (inScope(n) == name.equals("ifdef"))
				inp.push(args.get(1));
			break;
		case "ifeq":
		case "ifne":
			if (args.size() != 3) throw new T2HErr("\\ifeq and \\ifne expect 3 arguments");
			String e1 = convert(args.get(0), false),
					e2 = convert(args.get(1), false);
			if (e1.equals(e2) == name.equals("ifeq"))
				inp.push(args.get(2));
			break;
		case "pubinfo":
			if (args.size() != 2) throw new T2HErr("Usage: \\pubinfo{key}{field}");

			String key = convert(args.get(0), false);
			String field = convert(args.get(1), false);
			try { inp.push(ext_info.lookup(key, field)); } catch (LookupFailure e) {}
			break;
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
//		System.out.println("expanding macro \\" + macro_name);
		try {
			return context.lookup(macro_name);
		} catch (LookupFailure e) {
			return "<em>Don't know how to expand parameterless macro "
							+ macro_name + "</em>";
		}
	}

	private String expandMacro(String macro_name, List<String> macro_argument) {
		assert macro_argument != null;
//		 System.out.println("handling macro \\" + macro_name 
//		 + macro_argument);
		try {
			String result = context.lookup(macro_name);
			// XXX should watch for escaped # here.
			for (int i = 0; i < macro_argument.size(); i++) {
				result = result.replaceAll("#" + (i+1), macro_argument.get(i));
			}
			return result;
		} catch (LookupFailure e) {
		}

		try {
			if (macro_argument.size() > 0)
				return context.lookup(macro_name + macro_argument.get(0));
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
