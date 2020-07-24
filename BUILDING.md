# Building mv8

There is a single build script which can be run on Linux and MacOSX to build
both the shared library (`libmv8.so` on Linux or `libmv8.dylib` on MacOSX).

## Prerequisites

* v8 compiled as a monolith static library (see 
  [build instructions](https://v8.dev/docs/build) or get the lib from some 
  online repo)
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

If you wish to override these, run the script as follows:

```
JAVA_HOME=/path/a \
V8_HOME=/path/b \
V8_BUILD_RELEASE=other-release-name \
./build.sh
```
