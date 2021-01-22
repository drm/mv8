package mv8;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CallbackTest {
	@Test
	public void doTest() throws Exception {
		try (V8Isolate isolate = V8.createIsolate();
			 V8Context context = isolate.createContext("foo")
		) {
			context.runScript("__print('Hello world');", "");
		}
	}

	@Test
	public void testStartupBlob() throws Exception {
		try (V8Isolate isolate = V8.createIsolate(V8.createStartupDataBlob("const foo = \"Hello world\";", ""));
			 V8Context context = isolate.createContext("foo")
		) {
			context.runScript("__print(foo);", "");
		}
	}


	@Test
	public void testException() throws Exception {
		try (V8Isolate isolate = V8.createIsolate();
			 V8Context context = isolate.createContext("foo")
		) {
			context.setCallback((ctx, str) -> {
				throw new RuntimeException("Foo!");
			});

			context.runScript("__calljava(\"\");", "");
		} catch (RuntimeException e) {
			Assert.assertEquals("Foo!", e.getMessage());
			return;
		}
		Assert.fail("Exception should have been thrown");
	}


	@Test
	public void testPingPong() throws Exception {
		try (V8Isolate isolate = V8.createIsolate();
			 V8Context context = isolate.createContext("foo")
		) {
			// repeat to check scoping problems.
			for (int j = 0; j < 2; j ++) {
				final AtomicInteger n = new AtomicInteger(0);
				final List<String> s = new LinkedList<>();

				context.setCallback((ctx, str) -> {
					s.add(str);
					if (str.equals("")) {
						for (int i = 0; i < 10; i ++) {
							ctx.runScript("__iterator(" + n.getAndIncrement() + ")", "<iterator>");
						}
					}

					return "";
				});

				context.runScript("function __iterator(n) { " +
					"		__calljava(\"\" + n); " +
					"	};", "");
				context.runScript(
					"(() => { " +
						"	__calljava(\"\");" +
						"})();",
					""
				);

				Assert.assertEquals(11, s.size());
				Assert.assertEquals("", s.get(0));
				Assert.assertEquals("0", s.get(1));
				Assert.assertEquals("9", s.get(10));
			}
		}
	}

	@Test
	public void testEncoding() throws Exception {
		try (V8Isolate isolate = V8.createIsolate();
			 V8Context context = isolate.createContext("foo")
		) {
			Assert.assertEquals("hello world", context.runScript("\"hello\" + \" \" + \"world\";", "<test>"));
			Assert.assertEquals("𝓈𝓁", context.runScript("\"𝓈𝓁\"", "<test>"));
		}
	}
}
