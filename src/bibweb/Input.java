package bibweb;

import java.util.NoSuchElementException;

import org.eclipse.jdt.annotation.NonNull;

/** An input stream of characters that allows new strings of
 *  characters to be pushed into the head of the input.
 */
interface Input {
	/** Whether there is a next character to be read. */
	boolean hasNext();

	/** Read the next character from the stream. */
	char next() throws NoSuchElementException;

	/** The next character that will be read by next(). Unlike next(),
	 *  does not consume the next character. */
	char peek() throws NoSuchElementException;

	/** Put all the characters in s at the head of the input stream. */
	void push(@NonNull String s);
}
