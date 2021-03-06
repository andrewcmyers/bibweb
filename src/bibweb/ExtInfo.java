package bibweb;

import bibweb.Namespace.LookupFailure;

/** This interface exists to allow the TeX2HTML converter to look up publication
 * information without coupling it to Main.
 */
public interface ExtInfo {
	String lookup(String key, String field) throws LookupFailure;
	
	void put(String key, String field, String value) throws LookupFailure;
}
