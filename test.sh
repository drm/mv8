#!/usr/bin/env bash

JAVA_HOME="${JAVA_HOME:-"$(java -XshowSettings:properties -version 2>&1 > /dev/null | grep 'java.home' | grep -oE '\S+$')"}"
JAVA="${JAVA_HOME}/bin/java"

set -e -x -u

if [[ "$@" == "" ]]; then
	tests="$(
		cd bin && \
		find . -name "*Test.class" \
			| while read f; do echo "$f"| sed 's!^./\|\.class$!!g' | sed 's!/!.!g'; done;
	)"
else
	tests="$@"
fi

$JAVA \
	-Xmx512M -Xms512M \
	-cp "lib/*:bin/:src/" \
	-Djava.library.path="." \
	org.junit.runner.JUnitCore \
	$@
