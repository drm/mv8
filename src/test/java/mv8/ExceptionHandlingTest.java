package mv8;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Exception;
import com.mv8.V8Isolate;

import jettyv8.server.DebugServer;

public class ExceptionHandlingTest {

	static Logger logger = LoggerFactory.getLogger(DebugServer.class);

	@Test
	public void testStackTrace() throws Exception {
		try (	V8Isolate isolate = V8.createIsolate(null); 
				V8Context context = isolate.createContext("default")) {
			try {
				context.runScript("function doit() {\n"
						+ "throw new Error('hello');\n"
						+ "}\n"
						+ "\n"
						+ "doit();", "myscript.js");
				Assert.fail();
			} catch (V8Exception expected) {
				Assert.assertEquals("Error: hello", expected.getMessage());
				Assert.assertEquals("Error: hello\n" + 
						"    at doit (myscript.js:2:7)\n" + 
						"    at myscript.js:5:1", expected.getV8StackTrace());
			}
		}
	}
	
	@Test
	public void testThrowingAnError() throws Exception {
		try (	V8Isolate isolate = V8.createIsolate(null); 
				V8Context context = isolate.createContext("default")) {
			try {
				context.runScript("throw new Error('hello')", "");
				Assert.fail();
			} catch (V8Exception expected) {
				Assert.assertEquals("Error: hello", expected.getMessage());
				Assert.assertEquals("Error: hello\n" + 
						"    at <anonymous>:1:7", expected.getV8StackTrace());
			}
		}
	}

	@Test
	public void testParseErrors() throws Exception {
		try (	V8Isolate isolate = V8.createIsolate(null); 
				V8Context context = isolate.createContext("default")) {
			try {
				context.runScript("1;\n2;\nu n p a r s e a b l e\n", "myscript.js");
				Assert.fail();
			} catch (V8Exception expected) {
				Assert.assertEquals("SyntaxError: Unexpected identifier", expected.getV8StackTrace());
			}
		}
	}
}
