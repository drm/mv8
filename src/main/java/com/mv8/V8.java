package com.mv8;

public class V8 {
	static {
		System.loadLibrary("mv8");
	}

	public static V8Isolate createIsolate() {
		return createIsolate(null);
	}
	
	public static V8Isolate createIsolate(byte[] snapshotBlob) {
		V8Isolate isolate = new V8Isolate();
		isolate.init(_createIsolate(isolate, snapshotBlob));
		return isolate;
	}
	
	public static byte[] createStartupDataBlob(String source, String scriptName) {
		return _createStartupDataBlob(source, scriptName);
	}

	private static native long _createIsolate(V8Isolate isolate, byte[] snapshotBlob);
	private static native byte[] _createStartupDataBlob(String source, String scriptName);
}
