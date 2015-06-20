package bibweb;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface Input {
	boolean hasNext();
	char next() throws NoSuchElementException;
	char lookahead();
	void push(String s);
}
