package bibweb;

import java.io.Reader;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

/** Read input a line at a time from an input, with the possibility to insert new
 *  input ahead of the current input. 
 */
public class LNScanner {
	static private class Input {
		Scanner scanner;
		String input_name;
		int lineno = 1;
		Input(String n, String s, int lineno) {
			input_name = n;
			this.scanner = new Scanner(s);
			this.lineno = lineno;
		}
		Input (String n, Scanner s) {
			input_name = n;
			this.scanner = s;
		}
		public String toString() {
			return "\"" + input_name + "\", line " + lineno;
		}
		public String nextLine() {
			lineno++;
			return scanner.nextLine();
		}
	}
	private ArrayList<Input> inputs = new ArrayList<Input>();

	/* Start scanning a string s that started at line lineno */
	LNScanner(String nm, String s, int lineno) {
		inputs.add(new Input(nm, s, lineno));
	}

	public LNScanner(String nm, Reader r) {
		inputs.add(new Input(nm, new Scanner(r)));
	}
	
	private Input input() {
		return inputs.get(inputs.size() - 1);
	}
	
	private Scanner scanner() {
		return input().scanner;
	}

	boolean hasNextLine() {
		while (!scanner().hasNextLine()) {
			if (inputs.size() == 1) return false;
			inputs.remove(inputs.size() - 1);
		}
		return true;
	}
	
	public int lineNo() {
		return input().lineno;
	}

	String nextLine() throws NoSuchElementException {
		if (!hasNextLine()) throw new NoSuchElementException();
		return input().nextLine();
	}

	public void close() {
		for (Input sc : inputs) {		
			sc.scanner.close();
		}
	}

	public void push(String nm, Scanner s) {
		inputs.add(new Input(nm, s));
	}

	public String inputName() {
		return input().input_name;
	}
	
	public String toString() {
		return input().toString();
	}
}
