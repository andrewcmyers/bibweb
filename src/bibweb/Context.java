package bibweb;

import java.util.HashMap;
import java.util.Map;

public class Context {

	Node bottom = new Node(null); // may not be null

	static class Node {
		Map<String, String> macros = new HashMap<>();
		Node up; // may be null
		
		Node(Node above) { up = above; }
	}
	static public class LookupFailure extends Exception {}
	static LookupFailure lookupFailed = new LookupFailure();
	

	public String lookup(String name) throws LookupFailure {
		Node n = bottom;
		while (n != null) {
			if (n.macros.containsKey(name))
				return n.macros.get(name);
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
	
	public void add(String name, String defn) {
		bottom.macros.put(name,  defn);
	}
}
