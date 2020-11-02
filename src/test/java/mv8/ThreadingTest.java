package mv8;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadingTest {
	AtomicInteger n = new AtomicInteger(0);
	ExecutorService s = Executors.newFixedThreadPool(10, (r) -> {
		var t =  new Thread(r);
		t.setName("executor " + n.getAndIncrement());
		t.setDaemon(false);
		return t;
	});

	@Test
	public void testSerial() throws Exception {
		Thread.currentThread().setName("MAIN");
		V8Isolate isolate = V8.createIsolate();
		for (int i = 0; i < 10; i ++) {
			try (V8Context c = isolate.createContext(Thread.currentThread().getName())) {
				c.runScript("__print(\"Hello from " + Thread.currentThread().getName() + "\")", "");
			}
		}
		isolate.close();
	}


	@Test
	public void testParallelCreation() throws Exception {
		Thread.currentThread().setName("MAIN");
		V8Isolate isolate = V8.createIsolate();
		for (int i = 0; i < 10; i ++) {
			s.submit(() -> {
				try (V8Context c = isolate.createContext(Thread.currentThread().getName())) {
					c.runScript("__print(\"Hello from " + Thread.currentThread().getName() + "\")", "");
				}
			});
		}

		s.awaitTermination(1, TimeUnit.SECONDS);
		isolate.close();
	}


	@Test
	public void testParallelExecutionOnce() throws Exception {
		Thread.currentThread().setName("MAIN");
		V8Isolate isolate = V8.createIsolate();
		s.submit(() -> {
			try (V8Context c = isolate.createContext(Thread.currentThread().getName())) {
				c.runScript("__print(\"Hello from " + Thread.currentThread().getName() + "\")", "");
			}
		});

		s.awaitTermination(1, TimeUnit.SECONDS);
		isolate.close();
	}

	@Test
	public void testParallelExecutionMultiple() throws Exception {
		Thread.currentThread().setName("MAIN");
		V8Isolate isolate = V8.createIsolate();
		for (int i = 0; i < 10; i ++) {
			s.submit(() -> {
				try (V8Context c = isolate.createContext(Thread.currentThread().getName())) {
					c.runScript("__print(\"Hello from " + Thread.currentThread().getName() + "\")", "");
				}
			});
		}

		s.awaitTermination(1, TimeUnit.SECONDS);
		isolate.close();
	}
}
