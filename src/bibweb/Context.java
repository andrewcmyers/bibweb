package bibweb;

import java.util.HashMap;
import java.util.Map;

public class Context {

	Node bottom = new Node(null); // may not be null

	static class Expansion {
		String rhs;
		int num_args;

		Expansion(String s, int n) {
			rhs = s;
			num_args = n;
		}

		Expansion(String s) {
			this(s, 0);
		}
	}

	static class Node {
		Map<String, String> macros = new HashMap<>();
		Node up; // may be null
		
		Node(Node above) { up = above; }
	}
	static public class LookupFailure extends Exception {
		private static final long serialVersionUID = 1L;}
	static LookupFailure lookupFailed = new LookupFailure();
	

	public String lookup(String name) throws LookupFailure {
		Node n = bottom;
		while (n != null) {
			if (n.macros.containsKey(name)) {
				String result = n.macros.get(name);
				assert result != null;
				return result;
			}
			n = n.up;
		}
		throw lookupFailed;		
	}

	public void push() {
		Node n = new Node(bottom);
		bottom = n;
	}

	public void pop() {
		bottom = bottom.up;
	}
	
	public void add(String name, String defn, int num_args) {
		assert defn != null;
		bottom.macros.put(name,  defn);
	}
}
