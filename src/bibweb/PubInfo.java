package bibweb;

import java.util.Map;

import bibweb.Namespace.LookupFailure;

// A PubInfo supports a mutable key/value lookup based on 
// underlying Publication objects, plus an additional namespace
// containing some derived values.
public class PubInfo implements ExtInfo {
	Map<String, Publication> pubs;
	GetPubCtxt getPubCtxt;

	public static interface GetPubCtxt {
		Namespace get(Publication p);
	}

	public PubInfo(Map<String, Publication> pubs, GetPubCtxt pubCtxt) {
		this.pubs = pubs;
		this.getPubCtxt = pubCtxt;
	}

	@Override public String lookup(String key, String field)
			throws LookupFailure {
		if (!pubs.containsKey(key)) throw Context.lookupFailed;
		Publication p = pubs.get(key);
		Namespace n = getPubCtxt.get(p);
		return n.lookup(field);
	}

	@Override public void put(String key, String field, String value)
			throws LookupFailure {
		if (!pubs.containsKey(key)) throw Context.lookupFailed;
		Publication p = pubs.get(key);
		p.put(field, value);
	}
}
