package bibweb;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.jdt.annotation.NonNull;

public class Maybe<T> {
	private static class Some<T> extends Maybe<T> {
		T value;
		Some(@NonNull T v) { value = v; }
		public T get() { return value; }
		public boolean hasValue() { return true; }
	}

	public boolean hasValue() { return false; }
	private static Maybe<Object> none = new Maybe<Object>();
	@SuppressWarnings("unchecked")
	public static <T> Maybe<T> none() {
		return (Maybe<T>) none;
	}
	public static <T> Maybe<T> some(@NonNull T x) {
		return new Some<T>(x);
	}
	public T get() {
		throw new Error("tried to get an empty maybe");
	}
	public <R> R match(Function<T, R> some_case, Supplier<R> none_case) {
		if (hasValue()) {
			return some_case.apply(get());
		} else {
			return none_case.get();
		}
	}
	
	public void ifsome(Consumer<T> some_case) {
		if (hasValue()) some_case.accept(get());
	}
	public void ifnone(Runnable none_case) {
		if (hasValue()) none_case.run();
	}
	public void if_(Consumer<T> some_case, Runnable none_case) {
		if (hasValue()) some_case.accept(get());
		else none_case.run();
	}

}
