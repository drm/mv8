// Copyright 2015 the V8 project authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "libplatform/libplatform.h"
#include "v8.h"

using namespace v8;

void appendFileToString(std::string * str, const char * fileName) {
    std::string line;
    std::ifstream in(fileName);
    while (std::getline(in, line))
    {
      *str += line + "\n";
    }
}

int main(int argc, char *argv[])
{
  // Initialize V8.
  // V8::InitializeICUDefaultLocation(argv[0]);
  V8::InitializeExternalStartupData(argv[0]);

  Platform *platform = platform::CreateDefaultPlatform();
  V8::InitializePlatform(platform);
  V8::Initialize();

  std::string text;
  appendFileToString(&text, "test/jettyv8/react.min.js");
  appendFileToString(&text, "test/jettyv8/react-dom.min.js");
  appendFileToString(&text, "test/jettyv8/react-dom-server.min.js");
  appendFileToString(&text, "test/jettyv8/testlib.js");
  const char *data = text.c_str();

  v8::StartupData data1 = v8::V8::CreateSnapshotDataBlob(data);
  v8::StartupData data2 = v8::V8::WarmUpSnapshotDataBlob(data1, "warmup();\n");

  printf("After warming up..\n");
  fflush(stdout);

  // Create a new Isolate and make it the current one.
  Isolate::CreateParams create_params;
  create_params.snapshot_blob = &data1;
  ResourceConstraints constraints;
  constraints.ConfigureDefaults(512000000LL, 512000000LL);
  constraints.set_max_old_space_size(10);
  create_params.constraints = constraints;

  create_params.array_buffer_allocator = v8::ArrayBuffer::Allocator::NewDefaultAllocator();

  Isolate *isolate = Isolate::New(create_params);
  {
    Isolate::Scope isolate_scope(isolate);

    // Create a stack-allocated handle scope.
    HandleScope handle_scope(isolate);

    Local<Value> resultA;
    Local<Value> resultB;

    Local<String> source = String::NewFromUtf8(isolate, "doit();", NewStringType::kNormal).ToLocalChecked();
    printf("After source..\n");fflush(stdout);

    for(int i = 0; i < 100; i++) 
    {
      Local<Context> context = Context::New(isolate);

      Context::Scope context_scope(context);
      Local<Script> script = Script::Compile(context, source).ToLocalChecked();
      resultA = script->Run(context).ToLocalChecked();
      String::Utf8Value utf8(resultA);
      printf("%s\n", *utf8);
    }

    // {
    //   Local<Context> context = Context::New(isolate);
    //   Context::Scope context_scope(context);
    //   Local<Script> script = Script::Compile(context, source).ToLocalChecked();
    //   resultB = script->Run(context).ToLocalChecked();
    // }

    // // Convert the result to an UTF8 string and print it.
    // String::Utf8Value utf8B(resultB);
    // printf("%s\n", *utf8B);
  }

  // Dispose the isolate and tear down V8.
  isolate->Dispose();
  V8::Dispose();
  V8::ShutdownPlatform();
  delete platform;
  delete create_params.array_buffer_allocator;
  return 0;
}
