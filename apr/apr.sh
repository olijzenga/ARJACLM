#!/bin/sh

mvn compile exec:java -Dexec.args="$*"
