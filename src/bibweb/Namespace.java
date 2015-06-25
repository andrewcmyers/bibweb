package bibweb;

public interface Namespace {
	static class LookupFailure extends Exception {private static final long serialVersionUID = 1L;}

	String lookup(String name) throws LookupFailure;

}
