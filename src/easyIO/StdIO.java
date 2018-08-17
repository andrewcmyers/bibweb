package easyIO;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

/** Easy access to standard input and standard output, without fussing with exceptions. One
    easy way to use this is to make your class a subclass of {@code StdIO}. Or you can use a static
    import:
    <pre>
    import static easyIO.StdIO.*;
    ...
    </pre>
    
    */
public class StdIO {
	final static public BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
	final static public PrintWriter stdout = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
	
	protected StdIO() {}

	public static void print(String s) {
		stdout.print(s);
	}
	public static void println(String s) {
		stdout.println(s);
	}
	public static void println() {
		stdout.println();
	}
	public static void print(int x) {
		stdout.print(x);
	}
	public static void print(char c) {
		stdout.print(c);
	}
	public static void println(int x) {
		stdout.println(x);
	}
	public static String readln() {
		try {
			return stdin.readLine();
		} catch (IOException e) {
			throw new Error("IO exception on standard input");
		}
	}
}
