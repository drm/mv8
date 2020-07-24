# Building mv8

There is a single build script which can be run on Linux and MacOSX to build
both the shared library (`libmv8.so` on Linux or `libmv8.dylib` on MacOSX).

## Prerequisites

* v8 compiled as a monolith static library (see 
  [embed instructions](https://v8.dev/docs/embed) or get the lib from some 
  online repo). See `grep V8_VERSION build.sh` for the currently implemented
  version, use `V8_MONOLITH=/path/to/libv8_monolith.a build.sh` to override
  default.
* `g++`
* `java`, `javac`

## Invocation

Running the script should be fine:
```
./build.sh
```
This script assumes the following:

* `V8_HOME=~/git/v8`, where the v8 source code checkout is.
* `JAVA_HOME=/the/path/to/your/java/home`, assumes you want to use your 
  global `java`'s JAVA_HOME
* `V8_BUILD_RELEASE=x64.release.sample`, the v8 build release name
* `V8_MONOLITH=$V8_HOME/out.gn/$V8_BUILD_RELEASE/obj/libv8_monolith.a"`, 
   the v8 static monolith build (as per [embed instructions](https://v8.dev/docs/embed)).

If you wish to override these, run the script as follows:

```
JAVA_HOME=/path/a \
V8_HOME=/path/b \
V8_BUILD_RELEASE=other-release-name \
V8_MONOLITH=/path/c/libv8_monolith.a \
./build.sh
```
