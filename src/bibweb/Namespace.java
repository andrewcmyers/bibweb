package bibweb;

public interface Namespace {
	@SuppressWarnings("serial")
	static class LookupFailure extends Exception {}

	String lookup(String name) throws LookupFailure;

}