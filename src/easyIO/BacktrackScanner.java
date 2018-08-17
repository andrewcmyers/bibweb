package easyIO;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * A scanner class that, unlike {@code java.util.Scanner}, supports arbitrary
 * lookahead and backtracking. The caller can use {@code mark()} to set some
 * number of marks in the input stream, then {@code accept()} to erase the
 * previous mark or {@code abort()} to roll back to (and erase) the previous
 * mark. The marks function as a stack of previous points in the input.
 *
 * The class also allows reading a stream that is spread across multiple input
 * sources, and keeps track of the current source, current line number, and
 * current position within the line.
 * 
 * Arbitrary lookahead is allowed, but the space consumed by a scanner is
 * proportional to the number of chars between the first mark and last
 * lookahead position.
 * 
 * @see easyIO.Scanner easyIO.Scanner
 */
public class BacktrackScanner {
	 private static class Source {
		String name;
		Reader reader;
		int lineno = 1;
		int charpos = 0;
		Source(Reader r, String n) {
			name = n;
			reader = r;
		}
		public String toString() {
			return "\"" + name + "\", line " + lineno + ", character " + charpos;
		}

		/** Return the next character location or null if eof is reached. */
		Location read() throws IOException {
			int c = reader.read();
			if (c == -1) return null;
			if (c == '\n') {
				lineno++;
				charpos = 0;
			} else {
				charpos++;
			}
			return new Location(this, lineno, charpos, (char) c);
		}
		public void close() throws IOException {
			reader.close();
		}
	}

	/**
	 * An input character along with information about the source of the
	 * character, and its line number and position within the line.
	 */
	public static class Location {
		public Location(Source i, int l, int c, char ch) {
			input = i;
			lineno = l;
			charpos = c;
			character = ch;
		}
		Source input;
		int lineno;
		int charpos;
		char character;
		public String toString() {
			StringBuilder s = new StringBuilder();
			s.append('"');
			s.append(input.name);
			s.append("\", line ");
			s.append(lineno);
			s.append(", char ");
			s.append(charpos);
			s.append(" (");
			s.append(character);
			s.append(")");
			return s.toString();
		}
	}
	
	LinkedList<Source> inputs = new LinkedList<>();
	Location[] buffer;
	int pos; // current input position (always in the deepest region)
	int end; // marks end of characters actually read in buffer (last is at end-1)
	
	/** The stack of regions represented by their positions within prefix. Indices
	 *  must increase monotonically.
	 *       prefix            unread input (from linked list)
	 *  [...[....[...^....     .........$
	 *  0   1    2   pos  len           eof
	 *  marks
	 */ 
	int[] marks;
	int nmarks;
	static final int INITIAL_SIZE = 1;

	public boolean invariant() {
		assert nmarks >= 0;
		assert end >= 0 && end <= buffer.length;
		assert nmarks == 0 || pos >= marks[nmarks-1] && pos <= end;
		for (int i = 0; i < nmarks; i++) {
			assert marks[i] <= pos;
			if (i > 0) assert marks[i] >= marks[i-1];
		}
		return true;
	}
	/** Dump the state of the scanner to w in a human-readable form. */
	public void dump(StringBuilder w) {
		w.append("[Scanner\nbuffer length = " + buffer.length);
		w.append(" \npos = " + pos);
		w.append(" \nend = " + end);
		w.append(" \nnmarks = " + nmarks);
		if (pos < end) w.append("\ncurrent = " + buffer[pos]);
		w.append(" \nCurrent source: " + source() + "\n");
		
		int m = 0;
		int start = nmarks > 0 ? marks[0] : pos;
		start = (start >= 5) ? start - 5 : 0;
		for (int i = start; i < end; i++) {
			while (m < nmarks && marks[m] == i) {
				m++;
				w.append("[");
			}
			if (i == pos) w.append("^");
			w.append(buffer[i].character);
		}
		if (pos == end) w.append("^");
		w.append("...\n]\n");
	}
	
	public BacktrackScanner() {
		buffer = new Location[INITIAL_SIZE];
		end = 0;
		marks = new int[INITIAL_SIZE];
		nmarks = 0;
	}

	public void close() throws IOException {
		for (Source i : inputs) {
			i.close();
		}
	}
	public String source() {
		return inputs.getFirst().name;
	}
	public int lineNo() {
		return inputs.getFirst().lineno;
	}
	public int charPos() {
		return inputs.getFirst().charpos;
	}
	
	/** Add r to the input stream ahead of any existing inputs.*/
	public void includeSource(Reader r, String name) {
		Source i = new Source(r, name);
		inputs.addFirst(i);
	}
	/** Add r to the input stream after existing inputs. */
	public void appendSource(Reader r, String name) {
		Source i = new Source(r, name);
		inputs.addLast(i);
	}
	
	/** Whether there are characters already read ahead of the current position. */
	boolean charsAhead() {
		return (end - pos > 0);
	}

	/** Whether there is a character ahead in input. */
	public boolean hasNext() {
		return (peek() != -1);
	}

	static final EOF eof = new EOF();
	static final UnexpectedInput uinp = new UnexpectedInput();

	/** The next character ahead in the input. Equivalent to begin(); c = next(); abort(); return c; */
	public int peek() {
		if (charsAhead())
			return buffer[pos].character;
		Location c;
		try {
			c = inputs.getFirst().read();
		} catch (IOException e) {
			c = null;
		} catch (NoSuchElementException e) {
			return -1;
		}

		if (c == null) {
			Source fst = inputs.removeFirst();
			try {
				fst.close();
			} catch (IOException e) {
				// XXX throw an exception here?
			}
			if (inputs.size() == 0) return -1;
			return peek();
		}
		
		append(c);
		assert invariant();
		return c.character;
	}
	
	private void append(Location loc) {
		int n = buffer.length;
		assert end <= n;
		if (end == n) {
			grow();
		}
		buffer[end++] = loc;
		assert invariant();
	}

	/**
	 * Allocate a new prefix array at least twice as big as what is known to be
	 * needed, and copy all active input to that array. Anything before the
	 * first mark (or the current position if there is no mark) is discarded to
	 * save space.
	 */
	private void grow() {
		int start = pos;
		if (nmarks != 0) start = marks[0];
		int newlen = end - start;
		Location[] np;
		if (newlen * 2 < buffer.length) {
			np = buffer;
		} else {
			np = new Location[newlen * 2];
		}
		System.arraycopy(buffer, start, np, 0, newlen);
		buffer = np;
		for (int i = 0; i < nmarks; i++) {
			marks[i] -= start;
		}
		pos -= start;
		end = newlen;
	}
	
	/** Location in input source of the current position. */
	public Location location() {
		return buffer[pos];
	}
	/** Location in input source of the last mark. */
	public Location getMarkLocation() {
		return buffer[marks[nmarks-1]];
	}

	/** Add a mark at the current position. */
	public void mark() {
		assert invariant();
		if (nmarks == marks.length) {
			int[] rs2 = new int[nmarks*2];
			System.arraycopy(marks,  0,  rs2,  0,  nmarks);
			marks = rs2;
		}
		marks[nmarks++] = pos;
	}

	/** Effect: Erase the previous mark from the input, effectively
	 *  accepting all input up to the current position. */
	public void accept() {
		assert nmarks > 0 && invariant();
		nmarks--;
	}
	/** The current number of marks. Exposed for use in assertions, so
	 * client code can check that matching mark()...accept() calls occur
	 * at the same depth.
	 */
	public int depth() {
		return nmarks;
	}
	/** Return a string containing the characters from the most recent mark to the current position. */
	public String getToken() {
		assert nmarks > 0 && invariant();
		StringBuilder r = new StringBuilder();
		int s = marks[nmarks-1];
		for (int j = s; j < pos; j++)
			r.append(buffer[j].character);
		return r.toString();
	}

	/**
	 * Roll the input position back to the most recent mark, and erase the mark,
	 * effectively restarting scanning from that position.
	 */
	public void abort() {
		assert nmarks > 0;
		pos = marks[nmarks-1];
		nmarks--;
	}
	
	/** Advance past the next character, if any. Do nothing if at end of input. */
	public void advance() {
		try {
			next();
		} catch (EOF e) {
		}
	}

	/** Read the next character from the stream. */
	public char next() throws EOF {
		if (charsAhead())
			return buffer[pos++].character;
		try {
			if (inputs.size() == 0) throw eof;
			Location c = inputs.getFirst().read();
			if (c == null) {
				inputs.removeFirst();
				if (inputs.size() > 0) return next();
				throw eof;
			}
			return c.character;
		} catch (IOException e) {
			throw eof;
		}
	}

	/** Scan the characters of string s from the input.
	 * @throws UnexpectedInput if something other than the expected characters are encountered.
	 */
	public void string(String s) throws UnexpectedInput {
		mark();
		for (int i = 0; i < s.length(); i++) {
			if (peek() == s.charAt(i)) {
				advance();
			} else {
				abort();
				throw uinp;
			}
		}
		accept();
		return;
	}
	
	@Override public String toString() {
		StringBuilder b = new StringBuilder();
		dump(b);
		return b.toString();
	}
}
