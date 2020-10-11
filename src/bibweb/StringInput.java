package bibweb;

import java.util.NoSuchElementException;

public class StringInput implements Input {

	StringChunk first; // may be null if no more input
	int depth = 1;

	static class StringChunk {
		String data;
		int cur; // invariant: 0 <= cur < data.length
		StringChunk next; // may be null
		
		public StringChunk(String d, int c) {
			this(d, 0, null);
		}
        // n may be null
		public StringChunk(String d, int c, StringChunk n) {
			data = d;
			cur = 0;
			next = n;
		}
	
		boolean invariant() {
			return 0 <= cur && cur < data.length();
		}

		public String firstn(int n) {
			assert invariant();
			StringBuilder s = new StringBuilder();
			if (n > 0) {
				s.append(data.substring(cur));
				n -= s.length();
			}
			StringChunk c = next;
			while (n > 0 && c != null) {
				 s.append(c.data.substring(c.cur));
				 n -= s.length();
				 c = c.next;
			}
			return s.toString();
		}
	}

	StringInput(String s) {

		if (s.length() > 0) {
			first = new StringChunk(s, 0);
		} else {
			first = null;
		}
	}

	public String toString() {
		StringChunk f = first;
		if (f == null)
			return "\"\"";
		else
			return "input/\"" + f.firstn(80) + "\"";
	}

	@Override
	public boolean hasNext() {
		return first != null;
	}

	static NoSuchElementException empty = new NoSuchElementException();

	@Override
	public char next() throws NoSuchElementException {
		StringChunk f = first;
		assert f == null || f.invariant();
		if (f == null)
			throw empty;
		char result = f.data.charAt(f.cur);
		f.cur++;
		if (f.cur == f.data.length()) {
			first = f = f.next;
			depth--;
		}
		assert f == null || f.invariant();
		return result;
	}

	@Override
	public char peek() throws NoSuchElementException {
		StringChunk f = first;
		if (f == null)
			throw empty;
		return f.data.charAt(f.cur);
	}

	@Override
	public void push(String s) {
		if (s.length() == 0) return;
		first = new StringChunk(s, 0, first);
		depth++;
		if (depth > 20) throw new Error("recursively expanding too much");
	}
}
