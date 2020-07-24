package jettyv8.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mv8.InspectorCallbacks;
import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;
import com.mv8.V8Value;

public class DebugServer {
	
	static Logger logger = LoggerFactory.getLogger(DebugServer.class);
	private Server server;
	private ServletContextHandler handler;
	private List<IsolateMetadata> isolatesMetaData = new ArrayList<>();
	private int port;
	
	class DebugSocketServlet implements InspectorCallbacks {
		private V8Isolate isolate;
		private LinkedBlockingQueue<String> messagesFromInspectorFrontEnd = new LinkedBlockingQueue<>();
		private boolean quitMessageLoop;
		
		private Session session;
		private boolean paused;
		
		private DebugSocketServlet(V8Isolate isolate, String urlPath) {
			this.isolate = isolate;
			
			handler.addServlet(new ServletHolder(new WebSocketServlet() {
				private static final long serialVersionUID = 1L;
				
				@Override
				public void configure(WebSocketServletFactory factory) {
					factory.setCreator((req, resp) -> new WebSocketAdapter() {
						
						@Override
						public void onWebSocketConnect(Session session) {
							super.onWebSocketConnect(session); 
							DebugSocketServlet.this.session = session;
						}
						
						@Override
						public void onWebSocketClose(int statusCode, String reason) {
							DebugSocketServlet.this.session = null;
							System.err.println("Close connection " + statusCode + ", " + reason);
							super.onWebSocketClose(statusCode, reason); 
						}
						
						@Override
						public void onWebSocketText(String message) {
							super.onWebSocketText(message);
//							isolate.sendInspectorMessage(message);
							messagesFromInspectorFrontEnd.offer(message);
						}
					});
				}
			}), urlPath);
			
			isolate.setInspectorCallbacks(this);
			
			new Thread(() -> {
				try {
					while (true) {
						if (!paused) {
							String message = messagesFromInspectorFrontEnd.poll();
							if (message != null) {
								logger.debug("Passing message: " + message);
								isolate.sendInspectorMessage(message);
							}
						}
						Thread.yield();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}
		
		public void runMessageLoop() throws Exception {
			logger.warn("Entering runMessageLoop");
			while (true) {
				String message = messagesFromInspectorFrontEnd.poll();
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
				paused = true;
				runMessageLoop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void handleMessage(String theMessage) {
	    	try {
	    		logger.debug("Passing reponse: " + theMessage);
	    		DebugSocketServlet.this.session.getRemote().sendString(theMessage);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void runIfWaitingForDebugger() {
			logger.info("runIfWaitingForDebugger called");
		}

	}
	
	
	public Server startServer(int port) throws Exception {
		Server server = new Server();
		this.port = port;
		
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		
		handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
		handler.setContextPath("/");
		server.setHandler(handler);
		
		ServletHolder metadataServletHolder = new ServletHolder(new MetadataServlet(() -> this.getIsolatesMetaData()));
		
		handler.addServlet(metadataServletHolder, "/json");
		handler.addServlet(metadataServletHolder, "/json/list");
		handler.addServlet(metadataServletHolder, "/json/version");
		
		server.start();
		
		return server;
	}
	
	private List<IsolateMetadata> getIsolatesMetaData() {
		return isolatesMetaData ;
	}

	private void join() throws InterruptedException {
		server.join();
	}

	public static DebugServer start(int port) throws Exception {
		DebugServer instance = new DebugServer();
		instance.server = instance.startServer(port);
		return instance;
	}

	public static void main(String[] args) throws Exception {
		DebugServer server = start(9999);
		
		byte[] startupData = V8.createStartupDataBlob(new String(Files.readAllBytes(Paths.get("js", "react.js")), StandardCharsets.UTF_8.name()), "<embedded> react.js");
		V8Isolate isolate = V8.createIsolate(startupData);
		
		server.attachIsolate(isolate);

		V8Context contextOne = isolate.createContext("one");

		Thread.sleep(5000);
		logger.info("running script");
		contextOne.runScript(
				  "debugIt = function() {\n"
				+ " const a = 6, b = 7;\n"
				+ " debugger;\n"
				+ " return 'did it' + (a * b);\n"
				+ "};\n", "");
		V8Value result = contextOne.runScript("debugIt()", "");
		System.out.println("RESULT: " + result.getStringValue());
		
////		V8Context contextTwo = isolate.createContext("two");
////		contextTwo.runScript(
////				  "debugIt = function() {\n"
////				+ " const a = 4, b = 5;\n"
////				+ " return 'did it' + (a * b);\n"
////				+ "};\n"
////				+ "debugIt();\n", "");
////
//		byte[] startupDataReact = V8.createStartupDataBlob(new String(Files.readAllBytes(Paths.get("js", "react.js")), StandardCharsets.UTF_8.name()), "<embedded>");
//
//		V8Isolate isolateTwo = V8.createIsolate(startupDataReact);
//		server.attachIsolate(isolateTwo);
//
//		V8Context i2 = isolateTwo.createContext("isolateTwo");
//		i2.runScript("function bla() {console.log(\"BLA!\");}", "");
//
		server.join();
	}

	public void attachIsolate(V8Isolate isolate) {
		String id = UUID.randomUUID().toString();
		String urlPath = "/" + id;

		IsolateMetadata metaData = IsolateMetadata.create("MV8", "localhost", port, id, urlPath);
		isolatesMetaData.add(metaData);
		new DebugSocketServlet(isolate, urlPath);
	}
}
