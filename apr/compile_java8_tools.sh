#!/bin/bash

# Compile JUnitTestRunner under Java 8
mkdir -p target/java8
/usr/lib/jvm/java-8-openjdk/bin/javac -d target/java8 -cp "lib/junit-4.12.jar" -Xlint:deprecation src/nl/oebelelijzenga/arjaclm/execution/java8/JUnitTestRunner.java
/usr/lib/jvm/java-8-openjdk/bin/javac -d target/java8 -cp "lib/junit-4.12.jar" -Xlint:deprecation src/nl/oebelelijzenga/arjaclm/execution/java8/TestMethodResolver.java
