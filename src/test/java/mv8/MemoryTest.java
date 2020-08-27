package mv8;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

public class MemoryTest {
	@Test
	public void test() throws Exception {
		V8.setFlags("--max_old_space_size=1024 --max_semi_space_size=512 --trace-gc-object-stats --trace-gc --trace-gc-nvp");

		if (Runtime.getRuntime().maxMemory() > 512 * 1024 * 1024) {
			throw new AssertionError("You will need to set -Xmx512M to run this test effectively");
		}

		for (int i = 0; i < 3; i ++) {
			try (
				V8Isolate isolate = V8.createIsolate();
				V8Context ctx = isolate.createContext("");
			) {
				ctx.runScript("const str = \"Hello world\".repeat(1_000_000);", "");
				AtomicInteger n = new AtomicInteger(0);
				final int ii = i;
				ctx.setCallback((str) -> {
					try {
						if (str.charAt(0) != 'H') {
							throw new AssertionError("Expected first char to be 'H'");
						}
					} catch (IndexOutOfBoundsException e) {
						throw new AssertionError("Expected first char to be 'H'");
					}
					System.out.printf("i = %d, n = %d, free = %.2f, max = %.2f\n", ii, n.incrementAndGet(), Runtime.getRuntime().freeMemory() / 1048576f, Runtime.getRuntime().maxMemory() / 1048576f);
					System.gc();
					return str;
				});
				ctx.runScript("for (let n = 0; n < 1000; n ++) { JSON.stringify(__calljava(str)); }", "");
			}
		}
	}
}
