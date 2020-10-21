package com.mv8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class V8Isolate implements AutoCloseable {
	static Logger logger = LoggerFactory.getLogger(V8Isolate.class);

	final private static Map<Long, V8Isolate> refMap = new HashMap<>();

	protected static V8Isolate getIsolate(long ptr) {
		return refMap.get(ptr);
	}
	
	private long isolatePtr;
	private InspectorCallbacks inspectorCallbacks;
	
	V8Isolate() {
	}
	
	public void init(long isolatePtr) {
		this.isolatePtr = isolatePtr;
		refMap.put(this.isolatePtr, this);
	}

	public synchronized V8Context createContext(String contextName) {
		V8Context context = new V8Context(isolatePtr);
		context.init(_createContext(isolatePtr, context, contextName));
		return context;
	}

	public V8ObjectTemplate createObjectTemplate() {
		return new V8ObjectTemplate(isolatePtr, _createObjectTemplate(isolatePtr));
	}
	
	private static native long _createContext(long isolatePtr, V8Context javaInstance, String contextName);
	private static native long _createObjectTemplate(long isolatePtr);
	
	public void sendInspectorMessage(String message) {
		_sendInspectorMessage(isolatePtr, message);
	}
	
	public void runIfWaitingForDebugger() {
		inspectorCallbacks.runIfWaitingForDebugger();
	}
	
	public void quitMessageLoopOnPause() {
		logger.info("QUITMESSAGELOOPONPAUSE");
		if (inspectorCallbacks == null) {
			return;
		}
		try {
			inspectorCallbacks.quitMessageLoopOnPause();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void runMessageLoopOnPause() {
		logger.info("RUNMESSAGELOOPONPAUSE");
		if (inspectorCallbacks == null) {
			return;
		}
		try {
			inspectorCallbacks.runMessageLoopOnPause();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void handleInspectorMessage(String message) {
		if (inspectorCallbacks != null) {
			inspectorCallbacks.handleMessage(message);
		}
	}
	
	public void setInspectorCallbacks(InspectorCallbacks callbacks) {
		this.inspectorCallbacks = callbacks;
	}

	private static native void _sendInspectorMessage(long isolatePtr, String message);
	private static native void _dispose(long isolatePtr);

	@Override
	public void close() throws Exception {
		_dispose(isolatePtr);
		refMap.remove(isolatePtr);
	}
}
