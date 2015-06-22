package bibweb;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import bibweb.Context.LookupFailure;

public class Tex2HTML {
	Context context = new Context();
	{
		String[][] builtin_macros = BuiltinMacros.macros;
		for (int i = 0; i < builtin_macros.length; i++) {
			String rhs = builtin_macros[i][1];
			int n = rhs.contains("#1") ? 1 : 0;
			context.add(builtin_macros[i][0], builtin_macros[i][1], n);
		}
	}

	public void push() {
		context.push();
	}

	public void pop() {
		context.pop();
	}

	public void addMacro(String from, String to) {
		context.add(from, to, 0);
	}

	public void addMacro(String from, String to, int n) {
		context.add(from, to, n);
	}

	static class StateX {
		public final State tag;

		StateX(State normal) {
			tag = normal;
		}
	}

	class Normal extends StateX {
		Normal() {
			super(State.Normal);
		}
	}

	class Backslash extends StateX {
		Backslash() {
			super(State.Backslash);
		}
	}

	class LongMacroArg extends StateX {

		LongMacroArg() {
			super(State.LongMacroArg);
		}
	}

	class EOF extends StateX {
		EOF() {
			super(State.EOF);
		}
	}

	private static enum State {
		Normal, Backslash, AlphMacroName, ShortMacroArg, LongMacroArg, EOF, Start, Whitespace
	}
	
	static Set<String> special_macros = new HashSet<>();
	{
		special_macros.add("ifdef");
		special_macros.add("ifndef");
		special_macros.add("true");
		special_macros.add("false");
	}

	String convert(String s, boolean sentence_case) throws T2HErr {
		State state = State.Start;
		StringBuilder macro_name = null;
		StringBuilder macro_argument = null;
		StringBuilder ret = new StringBuilder();
		final char eof = (char) -1;
		int bracedepth = 0;

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
					state = State.Normal;
					break;
				case '}':
					if (bracedepth <= 0)
						throw new T2HErr("More closing braces than opening ones");
					bracedepth--;
					state = State.Normal;
					break;
				case '-':
					if (inp.lookahead() == '-') {
						inp.next();
						if (inp.lookahead() == '-') {
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
					if (inp.lookahead() == '\n') {
						ret.append("\r\n");
						state = State.Whitespace;
					}
				default:
					if (Character.isAlphabetic(c) && sentence_case && bracedepth == 0 && state != State.Start) {
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
					} else
						throw new T2HErr("Unexpected char after backslash: "
								+ c);
				}
				break;
			case AlphMacroName:
				if (Character.isAlphabetic(c)) {
					macro_name.append(c);
					// stay in state
				} else if (c == '{') {
					state = State.LongMacroArg;
					macro_argument = new StringBuilder();
					bracedepth = 1;
				} else {
					if (c != eof) inp.push(Character.toString(c));
					String name = macro_name.toString();
					if (special_macros.contains(name)) {
						throw new T2HErr("Special macro \\" + name + " expects argument in braces");
					} else {
						inp.push(expandMacro(name));
					}
					state = State.Normal;
				}
				break;
			case LongMacroArg: // in middle of argument held in braces
				if (c == '{') {
					macro_argument.append(c);
					bracedepth++;
					// state = State.LongMacroArg;
				} else if (c == '}') {
					bracedepth--;
					assert bracedepth >= 0;
					if (bracedepth == 0) {
						String name = macro_name.toString(),
								arg = macro_argument.toString();
						if (special_macros.contains(name)) {
							handleSpecialMacro(inp, name, arg);
						} else {
							inp.push(expandMacro(name, arg));
						}
						state = State.Normal;
					}
					// else stay in state
				} else if (c == eof) {
					throw new T2HErr("unexpected end to macro argument.");
				} else {
					macro_argument.append(c);
					// stay in state
				}
				break;
			case ShortMacroArg:
				if (c == '{') {
					state = State.LongMacroArg;
					bracedepth = 1;
					macro_argument = new StringBuilder();
				} else if (c == eof) {
					inp.push(expandMacro(macro_name.toString(), ""));
					state = State.Normal;
				} else {
					inp.push(expandMacro(macro_name.toString(), Character.toString(c)));
					state = State.Normal;
				}
				break;
			default:
				throw new T2HErr("Unexpected state " + state);
			}
		}
		return ret.toString();
	}

	boolean inScope(String name) {
		try {
			context.lookup(name);
			return true;
		} catch (LookupFailure f) {
		}
		return false;
		}

	private void handleSpecialMacro(Input inp, String name, String arg) {
		switch (name) {
		case "true": inp.push(arg); break;
		case "false": break;
		case "ifdef":
			if (arg.charAt(0) == '\\') arg = arg.substring(1);
			inp.push(inScope(arg) ? "\\true" : "\\false");
			break;
		case "ifndef":
			if (arg.charAt(0) == '\\') arg = arg.substring(1);
			inp.push(inScope(arg) ? "\\false" : "\\true");
			break;
		default:
			throw new Error("Not a special macro: " + name);
		}
	}

	private String expandMacro(String macro_name) {
		// System.out.println("expanding macro \\" + macro_name);
		try {
			return context.lookup(macro_name);
		} catch (LookupFailure e) {
			return "<em>Don't know how to expand parameterless macro "
							+ macro_name + "</em>";
		}
	}

	private String expandMacro(String macro_name, String macro_argument) {
		assert macro_argument != null;
		// System.out.println("handling macro \\" + macro_name + "{"
		// + macro_argument + "}");
		try {
			String result = context.lookup(macro_name);
			return result.replaceAll("#1", macro_argument);
		} catch (LookupFailure e) {
		}

		try {
			return context.lookup(macro_name + macro_argument);
		} catch (LookupFailure e) {
		}

		return "<em>don't know how to expand macro " + macro_name + "</em>";
	}

	public String lookup(String n) throws LookupFailure {
		return context.lookup(n);
	}
}
