#!/bin/bash
set -e -x

source $(cd $(dirname "$0") && pwd)/../build_vars.sh

# for sample in hello-world threads 
for sample in *.cc
do
	g++ -DV8_COMPRESS_POINTERS -I$V8_INCLUDE -O0 \
		$sample \
		-o bin/$(basename "$sample" .cc) \
		-Wl,$V8_MONOLITH \
		-ldl -pthread -std=c++11
done
