package jettyv8.server;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mv8.V8;
import com.mv8.V8Context;
import com.mv8.V8Isolate;

import jettyv8.server.DebugServer.InspectableIsolate;

public class WebServer {
	static Logger logger = LoggerFactory.getLogger(WebServer.class);
	
	static ExecutorService worker = Executors.newSingleThreadExecutor();

	private static byte[] startupData;

	private static DebugServer debugServer;
	
	public static class HelloWorldServlet extends DefaultServlet {
		
		private static final long serialVersionUID = 1L;

		@Override
		protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			response.setContentType("text/html");
			response.setCharacterEncoding(StandardCharsets.UTF_8.name());
			
			final String contextName = "webserver-" + Thread.currentThread().getName();
			
			try (V8Isolate isolate = V8.createIsolate(startupData);
					InspectableIsolate socket = debugServer.attachIsolate(isolate);
					V8Context context = isolate.createContext(contextName);) {
				
				context.setCallback((payload) -> {
					logger.debug("Got callback with payload: " + payload);
					return "";
				});
				
				
				socket.runMessageLoop();
				
				context.runScript(readJsFiles(Paths.get("typescript", "build.js")), "typescript/build.js");
				
				String html = context.runScript("renderHTML()", "");
				response.setStatus(200);
				response.getWriter().println(html);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	public static void main(String[] args) throws Exception {
		debugServer = DebugServer.start(9999);
		
		String reactJs = readJsFiles(
				Paths.get("js", "react.js"), 
				Paths.get("js", "react-dom.js"), 
				Paths.get("js", "react-dom-server.js"));
		
		startupData = V8.createStartupDataBlob("process = {pid: 12345, version: '8.3.14', arch: 'darwin'};\n\n" + reactJs, "<embedded>");
		
		worker.execute(() -> {
			try {
				
				
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);
        server.addConnector(connector);
        
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        handler.setContextPath("/");
        server.setHandler(handler);
        
		handler.addServlet(HelloWorldServlet.class, "/");
		
		server.start();
		
		server.join();
	}

	private static String readJsFiles(Path... paths) throws Exception {
		StringBuilder result = new StringBuilder();
		for (Path p : paths) {
			if (result.length() > 0) {
				result.append("\n\n");
			}
			result.append(new String(Files.readAllBytes(p), StandardCharsets.UTF_8.name()));
		}
		return result.toString();
	}
}
