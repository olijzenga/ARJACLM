#!/bin/sh

./compile_java8_tools.sh
mvn compile exec:java -Dexec.args="$*"
