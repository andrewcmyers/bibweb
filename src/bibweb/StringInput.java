package bibweb;

import java.util.NoSuchElementException;

public class StringInput implements Input {

	StringChunk first; // may be null if no more input
	int depth = 1;

	static class StringChunk {
		String data;
		int cur; // invariant: 0 <= cur < data.length
		StringChunk next; // may be null

		public String firstn(int n) {
			String s = data.substring(cur);
			if (n < s.length())
				s = s.substring(0, n);
			else if (next != null)
				s += next.firstn(n - s.length() - cur);
			return s;
		}
	}

	StringInput(String s) {
		first = new StringChunk();
		if (s.length() > 0) {
			first.data = s;
			first.cur = 0;
			first.next = null;
		} else {
			first = null;
		}
	}

	public String toString() {
		if (first == null)
			return "\"\"";
		return "input/\"" + first.firstn(80) + "\"";
	}

	@Override
	public boolean hasNext() {
		return first != null;
	}

	static NoSuchElementException empty = new NoSuchElementException();

	@Override
	public char next() throws NoSuchElementException {
		if (first == null)
			throw empty;
		char result = first.data.charAt(first.cur);
		first.cur++;
		if (first.cur == first.data.length()) {
			first = first.next;
			depth--;
		}
		return result;
	}

	@Override
	public char lookahead() throws NoSuchElementException {
		if (first == null)
			throw empty;
		return first.data.charAt(first.cur);
	}

	@Override
	public void push(String s) {
		if (s.length() == 0) return;
		StringChunk c = new StringChunk();
		c.next = first;
		c.cur = 0;
		c.data = s;
		first = c;
		depth++;
		if (depth > 20) throw new Error("recursively expanding too much");
	}
}
