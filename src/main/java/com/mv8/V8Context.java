package com.mv8;

public class V8Context implements AutoCloseable {
	private long ptr;
	private long isolatePtr;
	private boolean closed = false;
	private JavaCallback callback = null;
	
	V8Context(long isolatePtr) {
		this.isolatePtr = isolatePtr;
	}
	
	public void init(long ptr) {
		this.ptr = ptr;
	}
	
	public void setCallback(JavaCallback cb) {
		this.callback = cb;
	}
	
	public String runScript(String script, String scriptName) {
		if (closed) {
			throw new RuntimeException("context closed");
		}
		return _runScript(isolatePtr, ptr, script, scriptName);
	}

	public String __calljava(String message) {
		if (callback == null) {
			throw new RuntimeException("No callback set");
		}
		return callback.call(message);
	}

	private static native String _runScript(long isolatePtr, long contextPtr, String script, String scriptName);
	private static native void _dispose(long isolatePtr, long contextPtr);

	@Override
	public void close() {
		closed = true;
		_dispose(isolatePtr, ptr);
	}


}
