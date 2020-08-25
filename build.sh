#!/bin/bash

# Builds both Java .jar and the libmv8.{so,dylib} shared library.
# See BUILDING.md for more info

VERSION=$(git describe)

set -e -x -u

source $(cd $(dirname "$0") && pwd)/build_vars.sh

if ! [ -f "${V8_MONOLITH}" ]; then
	read -p "${V8_MONOLITH} not found. Try building it? [y/N] " CONT
	[ "$CONT" == "y" ] || [ "$CONT" == "Y" ] || echo "Aborting..." && exit
	(
		#
		cd $V8_HOME
		git checkout "$V8_VERSION"
		tools/dev/v8gen.py "${V8_BUILD_RELEASE}"
		ninja -C out.gn/${V8_BUILD_RELEASE} v8_monolith
	)
fi

mkdir -p bin out
$JAVAC -cp "lib/*" $(find src -name "*.java") -d bin -h src/main/cpp

if [[ "$OSTYPE" == "linux-gnu" ]]; then
	JAVA_INCLUDES="-I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
	OUTPUT_FILE=out/libmv8-$VERSION.so
elif [[ "$OSTYPE" == "darwin"* ]]; then
	JAVA_INCLUDES="-I$JAVA_HOME/include -I$JAVA_HOME/include/darwin"
	OUTPUT_FILE=out/libmv8-$VERSION.dylib
fi

# Note: Omitting V8_COMPRESS_POINTERS will lead to segfaults
# https://stackoverflow.com/questions/59533323/v8-quickisundefined-crushes-randomly-when-using-isconstructcall
g++ -shared -I"${V8_INCLUDE}" $JAVA_INCLUDES \
	-DV8_COMPRESS_POINTERS \
	src/main/cpp/mv8.cpp \
	-o $OUTPUT_FILE \
	-Wl,$V8_MONOLITH \
	-ldl -pthread -std=c++11 -fPIC

(cd bin && jar cf ../out/mv8-$VERSION.jar $(find . -name "*.class"))

echo -e "\n\nDone.\nDon't forget to run \`./test.sh\` to see if everything works.\n"
