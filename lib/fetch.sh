#!/usr/bin/env bash

ROOT=$(cd $(dirname $0) && pwd)

set -x
set -e

fetch() {
	fetch_maven_deps() {
		local dir=$(mktemp -d);

		(
			cd "$dir";
			cat > pom.xml <<-EOF
				<project>
					<modelVersion>4.0.0</modelVersion>
					<groupId>temp</groupId>
					<artifactId>temp</artifactId>
					<version>master</version>
					<repositories>
						<repository>
							<id>jitpack.io</id>
							<url>https://jitpack.io</url>
						</repository>
					</repositories>
					<dependencies>
			EOF

			for package in "$@"; do
				echo "$package" | \
					sed -E 's!([^:]+):([^:]+):([^:]+)!<dependency><groupId>\1</groupId><artifactId>\2</artifactId><version>\3</version></dependency>!g' \
					>> pom.xml
			done;

			cat >> pom.xml <<-EOF
					</dependencies>
				</project>
			EOF

			mvn dependency:copy-dependencies # dependency:sources
			mv target/dependency/* "$ROOT";
		)
		rm -rf "$dir";
	}

	if [ "${OSTYPE/win32}" != "$OSTYPE" ]; then
		j2v8_os=win32
	elif [ "${OSTYPE/linux}" != "$OSTYPE" ]; then
		j2v8_os=linux
	else
		j2v8_os=macosx
	fi;


	fetch_maven_deps \
		'junit:junit:4.12' \
		'org.slf4j:slf4j-log4j12:1.7.7' \
		'org.eclipse.jetty:jetty-server:9.4.5.v20170502' \
		'org.eclipse.jetty.websocket:websocket-api:9.4.5.v20170502' \
		'org.eclipse.jetty.websocket:websocket-server:9.4.5.v20170502' \
		'com.google.code.gson:gson:2.8.5' \
	;
}

clean() {
	rm -f *.jar;
}

if [[ "$@" == "" ]]; then
	echo "Usage: ./fetch.sh [clean] fetch";
fi

for fn in "$@"; do
	"$fn";
done;
