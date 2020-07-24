package com.mv8;

public class V8Exception extends RuntimeException {
	private static final long serialVersionUID = 1L;
	private final String v8StackTrace;
	
	public String getV8StackTrace() {
		return v8StackTrace;
	}

	public V8Exception(String message, String v8StackTrace) {
		super(message);
		this.v8StackTrace = v8StackTrace;
	}
}
