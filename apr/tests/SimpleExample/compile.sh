alias javac=/usr/lib/jvm/java-8-openjdk/bin/javac
javac -cp $PWD/src:$PWD/lib/junit-4.12.jar:$PWD/lib/hamcrest-core-1.3.jar -d $PWD/apr/build src/packageSimpleExample/SimpleExample.java src/tests/SimpleExampleTestsPos.java src/tests/SimpleExampleTestsNeg.java
