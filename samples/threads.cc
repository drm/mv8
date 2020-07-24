// Copyright 2015 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <iostream>
#include <thread>

#include "libplatform/libplatform.h"
#include "v8.h"
#include <unistd.h>
#include <sys/time.h>

using namespace v8;

std::mutex g_display_mutex;

Isolate * isolate;
void runInIsolate(const char * script);
Persistent<ObjectTemplate> * global;

void doit(const char * script) {
	runInIsolate(script);
}

long microtime() {
	struct timeval time;
	gettimeofday(&time, NULL);
	return ((unsigned long long)time.tv_sec * 1000000) + time.tv_usec;
}

// get called from js, switch context, do something, switch context back, continue
// switch scope, wait for result in different thread

static void LogCallback(const v8::FunctionCallbackInfo<v8::Value>& args) {
	if (args.Length() < 1) return;

	Isolate::Scope isolate_scope(args.GetIsolate());
	HandleScope scope(args.GetIsolate());
	Local<Value> arg = args[0];
	String::Utf8Value value(args.GetIsolate(), arg);
	printf("log: %s\n", *value);
	long start = microtime();
	isolate->Exit();
	v8::Unlocker unlock(isolate);
	// printf("Unlock took %ld\n", microtime() - start);fflush(stdout);

	std::thread::id this_id = std::this_thread::get_id();

	// g_display_mutex.lock();
	std::cout << "thread " << this_id << " sleeping...\n";
	// g_display_mutex.unlock();

	usleep(500000);
	std::cout << "thread " << this_id << " waking up...\n";
	{
		long start = microtime();
		v8::Locker lock(isolate);
		isolate->Enter();
		// printf("Lock took %ld\n", microtime() - start);fflush(stdout);
	}
}

const char * henk = "henk";

void runInIsolate(const char * scriptSource) {
	v8::Locker locker(isolate);

	Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);

	Local<String> source = String::NewFromUtf8(isolate, scriptSource, NewStringType::kNormal).ToLocalChecked();

	Local<Context> context = Context::New(isolate, NULL, global->Get(isolate));
	Context::Scope context_scope(context);

	// Local<String> string = String::NewFromUtf8(isolate, henk).ToLocalChecked();
	Local<External> ext = External::New(isolate, (void *)henk);
	context->SetEmbedderData(2, ext);
	context->GetEmbedderData(2);

	Local<Script> script = Script::Compile(context, source).ToLocalChecked();
	Local<Value> result = script->Run(context).ToLocalChecked();
	String::Utf8Value utf8(isolate, result);
	printf("%s\n", *utf8);
}

int main(int argc, char *argv[])
{

	// Initialize V8.
	// V8::InitializeICUDefaultLocation(argv[0]);
	V8::InitializeExternalStartupData(argv[0]);

	std::unique_ptr<v8::Platform> v8Platform = platform::NewDefaultPlatform();
	V8::InitializePlatform(v8Platform.get());
	V8::Initialize();

	// Create a new Isolate and make it the current one.
	Isolate::CreateParams create_params;
	create_params.array_buffer_allocator = v8::ArrayBuffer::Allocator::NewDefaultAllocator();
	ResourceConstraints constraints;
	constraints.ConfigureDefaults(512000LL, 512000LL);
	constraints.set_max_old_space_size(10);
	create_params.constraints = constraints;

	isolate = Isolate::New(create_params);
	Isolate::Scope isolate_scope(isolate);
	HandleScope handle_scope(isolate);

	global = new Persistent<ObjectTemplate>();
	global->Reset(isolate, ObjectTemplate::New(isolate));
	global->Get(isolate)->Set(String::NewFromUtf8(isolate, "log", NewStringType::kNormal).ToLocalChecked(), FunctionTemplate::New(isolate, LogCallback));

	// std::thread t1(doit, "for(let i = 0; i < 10; i++) { log('looping' + i);}");
	std::thread t1(doit, "log('t1: one'); log('t1: two');");

	usleep(100000);

	std::thread t2(doit, "log('t2: one'); log('t2: two');");

	usleep(100000);
	
	std::thread t3(doit, "log('t3: one'); log('t3: two');");

	usleep(100000);

	runInIsolate("log('main: one'); log('main: two');");

	t1.join();
	t2.join();
	t3.join();
	printf("Did it");

	V8::Dispose();
	V8::ShutdownPlatform();
	delete create_params.array_buffer_allocator;
	return 0;
}
