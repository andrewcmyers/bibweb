package bibweb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.Nullable;

/** A namespace that supports pushing and popping other namespaces.
 *  The current namespace can be either immutable or mutable. The initial
 *  state is a single-level mutable namespace to which new bindings can be
 *  added.
 */
public class Context implements Namespace {
	
	interface Node {
		String get(String name) throws LookupFailure;
		void put(String name, String value);
	}
	
	static class FixedNode implements Node {
		Namespace fixed_mappings; // may be null

		public FixedNode(Namespace n) {
			fixed_mappings = n;
		}

		@Override
		public String get(String name) throws LookupFailure {
			return fixed_mappings.lookup(name);
		}

		@Override
		public void put(String name, String value) {
			throw new UnsupportedOperationException();
		}
	}
	
	static class MutableNode implements Node {
		Map<String, String> mappings = new HashMap<>();

		@Override
		public String get(String name) throws LookupFailure {
			if (!mappings.containsKey(name))
				throw lookupFailed;
			return mappings.get(name);
		}

		@Override
		public void put(String name, String value) {
			mappings.put(name, value);			
		}
	}

	private List<Node> nodes;
	{
		nodes = new ArrayList<Node>();
		nodes.add(new MutableNode());
	}

	static public LookupFailure lookupFailed = new LookupFailure();
	
	public String lookup(String name) throws LookupFailure {
		for (int i = nodes.size() - 1; i >= 0; i--) {
			Node n = nodes.get(i);
			try {
				return n.get(name);
			} catch (LookupFailure e) {
				// try the next node up the stack
			}
		}
		throw lookupFailed;		
	}

	public void push() {
		nodes.add(new MutableNode());
	}
	
	public void push(Namespace n) {
		nodes.add(new FixedNode(n));
	}

	public void pop() {
		nodes.remove(nodes.size()-1);
	}
	
	public void add(String name, @Nullable String defn) {
		if (defn == null) {
			System.err.println("Warning: " + name + " bound to null");
			return;
		}
		nodes.get(nodes.size()-1).put(name,  defn);
	}

	public int depth() {
		return nodes.size() - 1;
	}
}
