package bibweb;

import java.util.Map;

import bibweb.Namespace.LookupFailure;

public class PubInfo implements ExtInfo {
	Map<Publication, Namespace> pub_ns;
	Map<String, Publication> pubs;
	GetPubCtxt getPubCtxt;
	
	public static interface GetPubCtxt {
		Namespace get(Publication p);
	}
	
	public PubInfo(Map<String, Publication> pubs, GetPubCtxt pubCtxt) {
		this.pubs = pubs;
		this.getPubCtxt = pubCtxt;
	}
	
	@Override
	public String lookup(String key, String field) throws LookupFailure 
	{
		Publication p = pubs.get(key);
		if (p == null) throw Context.lookupFailed;
		Namespace n = getPubCtxt.get(p);
		return n.lookup(field);
	}

}
