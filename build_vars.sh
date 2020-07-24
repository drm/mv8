V8_VERSION=8.6.211
V8_HOME="${V8_HOME:-"$(cd ~/git/v8 && pwd)"}"
V8_BUILD_RELEASE="${V8_BUILD_RELEASE:-"x64.release.sample"}"
JAVA_HOME="${JAVA_HOME:-"$(java -XshowSettings:properties -version 2>&1 >/dev/null | grep 'java.home' | grep -oE '\S+$')"}"
V8_MONOLITH="${V8_MONOLITH:-"$V8_HOME/out.gn/${V8_BUILD_RELEASE}/obj/libv8_monolith.a"}"
JAVAC="${JAVA_HOME}/bin/javac"
V8_INCLUDE="${V8_HOME}/include"
