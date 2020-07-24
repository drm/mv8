package mv8;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mv8.InspectorCallbacks;
import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;

import jettyv8.server.DebugServer;

public class InspectorTest {

	static Logger logger = LoggerFactory.getLogger(DebugServer.class);
	
	static class TestFrontEnd implements InspectorCallbacks {
		private boolean paused = false;
		private boolean quitMessageLoop = false;
		private V8Isolate isolate;
		private LinkedBlockingQueue<String> messagesToSend = new LinkedBlockingQueue<>();
		
		public TestFrontEnd(V8Isolate isolate) {
			this.isolate = isolate;
		}

		public void runMessageLoop() throws Exception {
			logger.warn("Entering runMessageLoop");
			while (true) {
				String message = messagesToSend.poll();
				if (message != null) {
					logger.debug("Passing message: " + message);
					isolate.sendInspectorMessage(message);
				}
				if (quitMessageLoop) {
					quitMessageLoop = false;
					paused = false;
					logger.debug("quitting!");
					return;
				} else {
					Thread.yield();
				}
			}
		}
		
		public void quitMessageLoopOnPause() {
			logger.info("Quit message loop");
			quitMessageLoop = true;
		}
		
		public void runMessageLoopOnPause() {
			try {
				paused  = true;
				runMessageLoop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		public void handleMessage(String theMessage) {
			logger.debug("Got response: " + theMessage);
		}

		@Override
		public void runIfWaitingForDebugger() {
			
		}
		
		public void sendCommand(String command) {
			logger.debug("Sending command: " + command);
			messagesToSend.add(command);
		}
	}

	@Test
	public void hello() throws Exception {
		
		ExecutorService worker = Executors.newSingleThreadExecutor();
		
		V8Isolate isolate = V8.createIsolate(null);
		TestFrontEnd frontEnd = new TestFrontEnd(isolate);
		
		isolate.setInspectorCallbacks(frontEnd);
		
		try (V8Context context = isolate.createContext("default")) {
			String result = context.runScript("debugger; 'henk'", "");
			worker.execute(() -> {
				frontEnd.sendCommand("{\"id\":1,\"method\":\"Runtime.enable\"}");
				frontEnd.sendCommand("{\"id\":3,\"method\":\"Debugger.enable\",\"params\":{\"maxScriptsCacheSize\":100000000}}");
				frontEnd.sendCommand("{\"id\":13,\"method\":\"Runtime.evaluate\",\"params\":{\"expression\":\"3 + 9\",\"includeCommandLineAPI\":true,\"contextId\":1,\"generatePreview\":true,\"userGesture\":false,\"awaitPromise\":false,\"throwOnSideEffect\":true,\"timeout\":500,\"disableBreaks\":true}}");
 			});
			
			frontEnd.runMessageLoop();
			
		}
	}

}
