package x.mvmn.aircndctrl.gui.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class LangUtils {

	public static final Integer INT_ONE = Integer.valueOf(1);

	public static <T> T call(Callable<T> callable, Consumer<Throwable> exceptionHandler) {
		try {
			return callable.call();
		} catch (Throwable t) {
			exceptionHandler.accept(t);
		}
		return null;
	}

	public static SafeCaller safeCaller(Consumer<Throwable> exceptionHandler) {
		return new SafeCaller(exceptionHandler);
	}

	public static class SafeCaller {
		protected final Consumer<Throwable> exceptionHandler;

		public SafeCaller(Consumer<Throwable> exceptionHandler) {
			super();
			this.exceptionHandler = exceptionHandler;
		}

		public <T> T callSafe(Callable<T> t) {
			try {
				return t.call();
			} catch (Exception e) {
				exceptionHandler.accept(e);
				return null;
			}
		}
	}
}
