/*******************************************************************************
* Copyright (c) 2014 EclipseSource and others.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    EclipseSource - initial API and implementation
******************************************************************************/
#include <jni.h>
#include "libplatform/libplatform.h"
#include <iostream>
#include <v8.h>
#include <string.h>
#include <v8-inspector.h>
#include <cstdlib>
#include "com_mv8_V8.h"
#include "com_mv8_V8Isolate.h"
#include "com_mv8_V8Context.h"
#include "com_mv8_V8Value.h"

using namespace std;
using namespace v8;

void getJNIEnv(JNIEnv *&env);

std::unique_ptr<v8::Platform> v8Platform;
JavaVM *jvm = NULL;

jclass v8ContextCls = NULL;
jmethodID v8CallJavaMethodID = NULL;

jclass v8IsolateCls = NULL;
jmethodID v8runIfWaitingForDebuggerMethodID = NULL;
jmethodID v8quitMessageLoopOnPauseMethodID = NULL;
jmethodID v8runMessageLoopOnPauseMethodID = NULL;
jmethodID v8handleInspectorMessageMethodID = NULL;

jclass v8ExceptionCls = NULL;
jmethodID v8ExceptionConstructorMethodID = NULL;

class InspectorClient;

class V8IsolateData
{
  public:
	Isolate *isolate;
	StartupData startupData;
	Persistent<ObjectTemplate> * globalObjectTemplate;
  	InspectorClient * inspector;
	jobject v8;
};

class InspectorFrontend final : public v8_inspector::V8Inspector::Channel
{
public:
	explicit InspectorFrontend(V8IsolateData *isolateData, jobject v8Isolate)
	{
		isolateData_ = isolateData;
		v8Isolate_ = v8Isolate;
	}

	virtual ~InspectorFrontend() = default;

private:
	void sendResponse(
		int callId,
		std::unique_ptr<v8_inspector::StringBuffer> message) override
	{
		Send(message->string());
	}
	void sendNotification(
		std::unique_ptr<v8_inspector::StringBuffer> message) override
	{
		Send(message->string());
	}
	void flushProtocolNotifications() override {}

	void Send(const v8_inspector::StringView &string)
	{
		// v8::Isolate::AllowJavascriptExecutionScope allow_script(isolateData_->isolate);
		int length = static_cast<int>(string.length());
		// DCHECK_LT(length, v8::String::kMaxLength);
		// v8::Local<v8::String> message =
		// 	(string.is8Bit()
		// 		 ? v8::String::NewFromOneByte(
		// 			   isolateData_->isolate,
		// 			   reinterpret_cast<const uint8_t *>(string.characters8()),
		// 			   v8::NewStringType::kNormal, length)
		// 		 : v8::String::NewFromTwoByte(
		// 			   isolateData_->isolate,
		// 			   reinterpret_cast<const uint16_t *>(string.characters16()),
		// 			   v8::NewStringType::kNormal, length))
		// 		.ToLocalChecked();

		JNIEnv *env;
		getJNIEnv(env);
		// v8::String::Value unicodeString(isolateData_->isolate, message);
		jstring javaString = 
			(string.is8Bit() ?
				env->NewStringUTF("henk")
				:
				env->NewString(string.characters16(),length)
			);
		env->CallVoidMethod(v8Isolate_, v8handleInspectorMessageMethodID, javaString);
	}

	V8IsolateData *isolateData_;
	jobject v8Isolate_;
};

class InspectorClient : public v8_inspector::V8InspectorClient
{
public:
	InspectorClient(V8IsolateData *isolateData, jobject v8Isolate)
	{
		isolate_ = isolateData->isolate;
		channel_.reset(new InspectorFrontend(isolateData, v8Isolate));
		inspector_ = v8_inspector::V8Inspector::create(isolate_, this);
		session_ = inspector_->connect(1, channel_.get(), v8_inspector::StringView());
		v8Isolate_ = v8Isolate;
	}

	void connectContext(Local<Context> context, v8_inspector::StringView name)
	{
		inspector_->contextCreated(v8_inspector::V8ContextInfo(context, kContextGroupId, name));
		context_.Reset(isolate_, context);
	}

	void disconnectContext(Local<Context> context)
	{
		inspector_->contextDestroyed(context);
	}

	void runMessageLoopOnPause(int contextGroupId) override
	{
		JNIEnv *env;
		getJNIEnv(env);
		env->CallVoidMethod(v8Isolate_, v8runMessageLoopOnPauseMethodID);
	}

	void runIfWaitingForDebugger(int contextGroupId) override {
		JNIEnv *env;
		getJNIEnv(env);
		env->CallVoidMethod(v8Isolate_, v8runIfWaitingForDebuggerMethodID);
	}

	void quitMessageLoopOnPause() override
	{
		JNIEnv *env;
		getJNIEnv(env);
		env->CallVoidMethod(v8Isolate_, v8quitMessageLoopOnPauseMethodID);
	}

	v8_inspector::V8InspectorSession *GetSession()
	{
		return session_.get();
	}

private:
	Local<Context> ensureDefaultContextInGroup(int group_id) override
	{
		return context_.Get(isolate_);
	}

	static const int kContextGroupId = 1;

	std::unique_ptr<v8_inspector::V8Inspector> inspector_;
	std::unique_ptr<v8_inspector::V8InspectorSession> session_;
	std::unique_ptr<v8_inspector::V8Inspector::Channel> channel_;
	bool is_paused = false;
	Global<Context> context_;
	Isolate *isolate_;
	jobject v8Isolate_;
};

class ShellArrayBufferAllocator : public v8::ArrayBuffer::Allocator
{
public:
	virtual void *Allocate(size_t length)
	{
		void *data = AllocateUninitialized(length);
		return data == NULL ? data : memset(data, 0, length);
	}
	virtual void *AllocateUninitialized(size_t length) { return malloc(length); }
	virtual void Free(void *data, size_t) { free(data); }
};

ShellArrayBufferAllocator array_buffer_allocator;
void runScriptInContext(v8::Isolate* isolate, v8::Local<v8::Context> context, const char* utf8_source, const char* name, Local<Value> * result, Local<Value> * exception);

static void javaCallback(const v8::FunctionCallbackInfo<v8::Value> &args)
{
	if (args.Length() < 1)
		return;

	Isolate *isolate = args.GetIsolate();
	Local<Context> context = isolate->GetCurrentContext();
	HandleScope scope(isolate);

	Local<External> data = Local<External>::Cast(context->GetEmbedderData(1));
	jobject javaInstance = reinterpret_cast<jobject>(data->Value());

	JNIEnv *env;
	int getEnvStat = jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if (jvm->AttachCurrentThread((void **)&env, NULL) != 0)
		{
			std::cout << "Failed to attach" << std::endl;
		}
	}
	else if (getEnvStat == JNI_OK)
	{
	}

	String::Value unicodeString(isolate, args[0]->ToString(context).ToLocalChecked());
	jstring javaString = env->NewString(*unicodeString, unicodeString.length());
	jobject result = env->CallObjectMethod(javaInstance, v8CallJavaMethodID, javaString);

	const uint16_t *resultString = env->GetStringChars((jstring)result, NULL);
	int length = env->GetStringLength((jstring)result);
	Local<String> str = String::NewFromTwoByte(isolate, resultString, NewStringType::kNormal, length).ToLocalChecked();
	env->ReleaseStringChars((jstring)result, resultString);
	args.GetReturnValue().Set(str);
}

JNIEXPORT jbyteArray JNICALL Java_com_mv8_V8__1createStartupDataBlob(JNIEnv * env, jclass v8, jstring scriptSource, jstring fileName) {
	jstring snapshotBlobGlobal = (jstring)env->NewGlobalRef(scriptSource);
	const char * nativeString = env->GetStringUTFChars(scriptSource, NULL); // Note: GetStringUTF8Chars does not support emoji's

	SnapshotCreator * snapshot_creator = new SnapshotCreator();
	v8::Isolate* isolate = snapshot_creator->GetIsolate();
	{
		v8::HandleScope scope(isolate);
		v8::Local<v8::Context> context = v8::Context::New(isolate);
		Local<Value> result;
		Local<Value> exception;
		runScriptInContext(isolate, context, nativeString, "<embedded>", &result, &exception);
		snapshot_creator->SetDefaultContext(context, NULL);
	}
	StartupData startupData = snapshot_creator->CreateBlob(v8::SnapshotCreator::FunctionCodeHandling::kClear);
	jbyteArray ret = env->NewByteArray(startupData.raw_size);
	env->SetByteArrayRegion(ret, 0, startupData.raw_size, (const jbyte *)startupData.data);
	return ret;
}


JNIEXPORT jlong JNICALL Java_com_mv8_V8__1createIsolate(JNIEnv *env, jclass V8, jobject V8Isolate, jbyteArray snapshotBlob)
{
	const char *nativeString;
	V8IsolateData *isolateData = new V8IsolateData();
	v8::Isolate::CreateParams create_params;

	if (snapshotBlob)
	{
		jbyteArray snapshotBlobGlobal = (jbyteArray)env->NewGlobalRef(snapshotBlob);
		const char* buf = (const char *)malloc(env->GetArrayLength(snapshotBlobGlobal));
		env->GetByteArrayRegion(snapshotBlobGlobal, 0, env->GetArrayLength(snapshotBlobGlobal), (jbyte *)buf);
		isolateData->startupData.data = buf;
		isolateData->startupData.raw_size = env->GetArrayLength(snapshotBlobGlobal);

		// isolateData->startupData = v8::V8::CreateSnapshotDataBlob(nativeString);
		create_params.snapshot_blob = &isolateData->startupData;
	}

	create_params.array_buffer_allocator = &array_buffer_allocator;
	isolateData->isolate = v8::Isolate::New(create_params);
	Isolate *isolate = isolateData->isolate;
	v8::Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);

	Handle<ObjectTemplate> globalObject = ObjectTemplate::New(isolate);
	globalObject->Set(String::NewFromUtf8(isolate, "__calljava", NewStringType::kNormal).ToLocalChecked(), FunctionTemplate::New(isolate, javaCallback));
	isolateData->globalObjectTemplate = new Persistent<ObjectTemplate>(isolate, globalObject);

	jobject instanceRef = env->NewGlobalRef(V8Isolate);
	Local<External> ext = External::New(isolate, instanceRef);
	isolateData->inspector = new InspectorClient(isolateData, instanceRef);

	return reinterpret_cast<jlong>(isolateData);
}

JNIEXPORT jlong JNICALL Java_com_mv8_V8Isolate__1createContext(JNIEnv *env, jclass, jlong isolatePtr, jobject javaInstance, jstring contextName)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);
	Isolate *isolate = isolateData->isolate;
	v8::Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);

	const uint16_t *contextNameString = env->GetStringChars(contextName, NULL);
	int length = env->GetStringLength(contextName);

	jobject instanceRef = env->NewGlobalRef(javaInstance);

	Local<Context> context = Context::New(isolate, NULL, isolateData->globalObjectTemplate->Get(isolate));
	v8::Context::Scope context_scope(context);
	context->SetEmbedderData(1, External::New(isolate, instanceRef));

	Persistent<Context> *persistentContext = new Persistent<Context>(isolate, context);
	isolateData->inspector->connectContext(context, v8_inspector::StringView(contextNameString, length));

	env->ReleaseStringChars(contextName, contextNameString);

	return reinterpret_cast<jlong>(persistentContext);
}

void runScriptInContext(v8::Isolate* isolate, v8::Local<v8::Context> context, const char* utf8_source, const char* name, Local<Value> *result, Local<Value> *exception) {
	v8::Context::Scope context_scope(context);
	TryCatch try_catch(isolate);

	Local<String> source_string = String::NewFromUtf8(isolate, utf8_source, NewStringType::kNormal).ToLocalChecked();
	Local<String> resource_name = String::NewFromUtf8(isolate, name, NewStringType::kNormal).ToLocalChecked();

	ScriptOrigin origin(resource_name);
	ScriptCompiler::Source source(source_string, origin);

	Local<Script> script;
	if (!ScriptCompiler::Compile(context, &source).ToLocal(&script)) {
		*exception = try_catch.Exception();
		return;
	}

	if (!script->Run(context).ToLocal(result)) {
		*exception = try_catch.Exception();
	}
}

JNIEXPORT jstring JNICALL Java_com_mv8_V8Context__1runScript(JNIEnv *env, jclass clz, jlong isolatePtr, jlong contextPtr, jstring scriptSource, jstring scriptName)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);
	Isolate *isolate = isolateData->isolate;
	Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);

	Persistent<Context> *persistentContext = reinterpret_cast<Persistent<Context> *>(contextPtr);
	Local<Context> context = persistentContext->Get(isolate);
	Context::Scope context_scope(context);

	const char *utf8SourceString = env->GetStringUTFChars(scriptSource, NULL);
	const char *utf8NameString = env->GetStringUTFChars(scriptName, NULL);
	Local<Value> result;
	Local<Value> exception;
	runScriptInContext(isolate, context, utf8SourceString, utf8NameString, &result, &exception);
	env->ReleaseStringUTFChars(scriptSource, utf8SourceString);
	env->ReleaseStringUTFChars(scriptName, utf8NameString);

	if (!exception.IsEmpty()) {
		v8::Object* object = v8::Object::Cast(*exception);


		MaybeLocal<Value> stack = object->Get(context, v8::String::NewFromOneByte(isolate, (const uint8_t *)"stack", v8::NewStringType::kNormal).ToLocalChecked());
		String::Value unicodeString(isolate, exception->ToString(context).ToLocalChecked());
		String::Value stackAsString(isolate, stack.ToLocalChecked());
		jobject javaException = env->NewObject(v8ExceptionCls, v8ExceptionConstructorMethodID, 
			env->NewString(*unicodeString, unicodeString.length()),
			env->NewString(*stackAsString, stackAsString.length()));
		env->Throw((jthrowable)javaException);
		return NULL;
	}

	if (result.IsEmpty()) {
		return NULL;
	}

	String::Value unicodeString(isolate, result->ToString(context).ToLocalChecked());
	return env->NewString(*unicodeString, unicodeString.length());
}

JNIEXPORT void JNICALL Java_com_mv8_V8Context__1dispose(JNIEnv *env, jclass, jlong isolatePtr, jlong contextPtr)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);
	Isolate *isolate = isolateData->isolate;
	Persistent<Context> *persistentContext = reinterpret_cast<Persistent<Context> *>(contextPtr);
	HandleScope handle_scope(isolate);

	Local<Context> context = persistentContext->Get(isolate);

	Local<External> data = Local<External>::Cast(context->GetEmbedderData(1));
	jobject javaInstance = reinterpret_cast<jobject>(data->Value());
	if (javaInstance) {
		env->DeleteGlobalRef(javaInstance);
	}

	isolateData->inspector->disconnectContext(context);
	persistentContext->Reset();
}

JNIEXPORT void JNICALL Java_com_mv8_V8Isolate__1dispose(JNIEnv *, jclass, jlong isolatePtr)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);
	Isolate *isolate = isolateData->isolate;
	isolateData->globalObjectTemplate->Reset();
	isolate->Dispose();
	if (isolateData->startupData.data) {
		free((void *)isolateData->startupData.data);
	}
}

JNIEXPORT jstring JNICALL Java_com_mv8_V8Value__1getStringValue(JNIEnv *env, jclass, jlong isolatePtr, jlong contextPtr, jlong valuePtr)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);
	Isolate *isolate = isolateData->isolate;
	Persistent<Context> *persistentContext = reinterpret_cast<Persistent<Context> *>(contextPtr);
	Persistent<Value> *persistentValue = reinterpret_cast<Persistent<Value> *>(valuePtr);
	Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);
	Local<Value> v = persistentValue->Get(isolate);
	if (v->IsString())
	{
		String::Value unicodeString(isolate, v->ToString(persistentContext->Get(isolate)).ToLocalChecked());
		return env->NewString(*unicodeString, unicodeString.length());
	}
	else
	{
		return env->NewStringUTF("Not set");
	}
}

JNIEXPORT jlong JNICALL Java_com_mv8_V8Isolate__1createObjectTemplate(JNIEnv *, jclass, jlong isolatePtr)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);
	Isolate *isolate = isolateData->isolate;
	Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);

	Handle<ObjectTemplate> objectTemplate = ObjectTemplate::New(isolate);
	Persistent<ObjectTemplate> *persistent = new Persistent<ObjectTemplate>(isolate, objectTemplate);
	return reinterpret_cast<jlong>(persistent);
}

JNIEXPORT void JNICALL Java_com_mv8_V8Isolate__1sendInspectorMessage(JNIEnv *env, jclass, jlong isolatePtr, jstring message)
{
	V8IsolateData *isolateData = reinterpret_cast<V8IsolateData *>(isolatePtr);

	const uint16_t *unicodeString = env->GetStringChars(message, NULL);
	int length = env->GetStringLength(message);
	std::unique_ptr<uint16_t[]> buffer(new uint16_t[length]);
	for (int i = 0; i < length; i++)
	{
		buffer[i] = unicodeString[i];
	}
	v8_inspector::StringView message_view(buffer.get(), length);
	v8_inspector::V8InspectorSession *session = isolateData->inspector->GetSession();
	session->dispatchProtocolMessage(message_view);

	env->ReleaseStringChars(message, unicodeString);
}

class MethodDescriptor
{
public:
	jlong methodID;
	jlong v8RuntimePtr;
};

void getJNIEnv(JNIEnv *&env)
{
	int getEnvStat = jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
	if (getEnvStat == JNI_EDETACHED)
	{
		if (jvm->AttachCurrentThread((void **)&env, NULL) != 0)
		{
			std::cout << "Failed to attach" << std::endl;
		}
	}
	else if (getEnvStat == JNI_OK)
	{
	}
	else if (getEnvStat == JNI_EVERSION)
	{
		std::cout << "GetEnv: version not supported" << std::endl;
	}
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	JNIEnv *env;
	jint onLoad_err = -1;
	if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK)
	{
		return onLoad_err;
	}
	if (env == NULL)
	{
		return onLoad_err;
	}

	v8::V8::InitializeICU();
	V8::InitializeExternalStartupData(".");

	v8Platform = v8::platform::NewDefaultPlatform();
	v8::V8::InitializePlatform(v8Platform.get());
	v8::V8::Initialize();

	jvm = vm;
	v8ContextCls = (jclass)env->NewGlobalRef((env)->FindClass("com/mv8/V8Context"));
	v8CallJavaMethodID = env->GetMethodID(v8ContextCls, "__calljava", "(Ljava/lang/String;)Ljava/lang/String;");

	v8IsolateCls = (jclass)env->NewGlobalRef((env)->FindClass("com/mv8/V8Isolate"));
	v8runIfWaitingForDebuggerMethodID = env->GetMethodID(v8IsolateCls, "runIfWaitingForDebugger", "()V");
	v8quitMessageLoopOnPauseMethodID = env->GetMethodID(v8IsolateCls, "quitMessageLoopOnPause", "()V");
	v8runMessageLoopOnPauseMethodID = env->GetMethodID(v8IsolateCls, "runMessageLoopOnPause", "()V");
	v8handleInspectorMessageMethodID = env->GetMethodID(v8IsolateCls, "handleInspectorMessage", "(Ljava/lang/String;)V");

	v8ExceptionCls = (jclass)env->NewGlobalRef((env)->FindClass("com/mv8/V8Exception"));
	v8ExceptionConstructorMethodID = env->GetMethodID(v8ExceptionCls, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");

	v8ExceptionCls = (jclass)env->NewGlobalRef((env)->FindClass("com/mv8/V8Exception"));

	return JNI_VERSION_1_6;
}