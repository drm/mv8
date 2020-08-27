package mv8;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;
import org.junit.Test;

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
			context.setCallback((str) -> {
				throw new RuntimeException("Foo!");
			});

			context.runScript("__calljava(\"\");", "");
		}
	}
}
