#!/bin/bash
# Builds both Java .jar and the libmv8.{so,dylib} shared library.
# See BUILDING.md for more info

set -e -x -u

V8_VERSION=8.6.211

V8_HOME="${V8_HOME:-"$(cd ~/git/v8 && pwd)"}"
V8_BUILD_RELEASE="${V8_BUILD_RELEASE:-"x64.release.sample"}"
JAVA_HOME="${JAVA_HOME:-"$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | grep -oE '\S+$')"}"

JAVAC="${JAVA_HOME}/bin/javac"
V8_INCLUDE="${V8_HOME}/include"
V8_OBJ="$V8_HOME/out.gn/${V8_BUILD_RELEASE}/obj"

mkdir -p bin
$JAVAC -cp "lib/*" $(find src -name "*.java") -d bin -h src/main/cpp;

if [[ "$OSTYPE" == "linux-gnu" ]]; then
	JAVA_INCLUDES="-I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
	OUTPUT_FILE=libmv8.so
elif [[ "$OSTYPE" == "darwin"* ]]; then
	JAVA_INCLUDES="-I$JAVA_HOME/include -I$JAVA_HOME/include/darwin"
	OUTPUT_FILE=libmv8.dylib
fi

# Note: Omitting V8_COMPRESS_POINTERS will lead to segfaults
# https://stackoverflow.com/questions/59533323/v8-quickisundefined-crushes-randomly-when-using-isconstructcall
g++ -shared -I"${V8_INCLUDE}" $JAVA_INCLUDES \
	-DV8_COMPRESS_POINTERS \
	src/main/cpp/mv8.cpp \
	-o $OUTPUT_FILE \
	-Wl,$V8_OBJ/libv8_monolith.a \
	-ldl -pthread -std=c++11 -fPIC
