package mv8;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;
import org.junit.Test;

public class HeapStatsTest {
	@Test
	public void doTest() throws Exception {
		V8.setFlags("--expose-gc --trace_gc --trace_gc_nvp  --trace_gc_ignore_scavenger");

		String[] scripts = {
			// a inside function
			"(function() {\n" +
				"	let a = \"\";\n" +
				"	for (let j = 0; j < 1000000; j ++) {\n" +
				"		a += \"a\";\n" +
				"	}\n" +
				"})();\n",
//
//			// a outside function
//			"let a = \"\";\n" +
//			"(function() {\n" +
//				"	for (let j = 0; j < 1000000; j ++) {\n" +
//				"		a += \"a\";\n" +
//				"	}\n" +
//				"})();\n",
//
//			"(function() {\n" +
//			"const fn = function() {\n" +
//			"	const r = {str: \"a\"};\n" +
//			"	return r;\n" +
//			"};" +
//			"let a = \"\";" +
//			"for (let j = 0; j < 1000000; j ++) {\n" +
//			"		a += fn().a;\n" +
//			"}\n" +
//			"})();\n"
		};

		for (String s : scripts) {
			System.out.println(s);

			try (V8Isolate isolate = V8.createIsolate();
				 V8Context context = isolate.createContext("foo")
			) {
				for (int i = 0; i < 10; i ++) {
					context.runScript(s, "");
					System.err.println("i=" + i);
					context.runScript("__print(\"before gc: \" + JSON.stringify(__heapstats(), undefined, 4));", "");
					context.runScript("gc();", "");
					context.runScript("__print(\"after gc: \" + JSON.stringify(__heapstats(), undefined, 4));", "");
				}
			}
		}
	}
}
