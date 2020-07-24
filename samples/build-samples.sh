#!/bin/bash
set -e -x

V8_VERSION=8.0.426.17
V8_BASE="$(cd ~/git/v8 ; pwd)"
V8_INCLUDE="$V8_BASE/include"
V8_OBJ="$V8_BASE/out.gn/libv8/obj"

# for sample in hello-world threads 
for sample in hello-world 
do
	g++ -DV8_COMPRESS_POINTERS -I$V8_INCLUDE -O0 \
		$sample.cc \
		-o $sample \
		-Wl,$V8_OBJ/libv8_monolith.a \
		-ldl -pthread -std=c++11
done