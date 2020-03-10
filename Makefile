VERSION = $(shell cat VERSION)

run: export JAVA_HOME = $(HOME)/var/graalvm-ce-java11-20.0.0/Contents/Home
run:
	clj -A:dev

compile: export GRAALVM_HOME = $(HOME)/var/graalvm-ce-java11-20.0.0/Contents/Home
compile: export JAVA_HOME = $(HOME)/var/graalvm-ce-java11-20.0.0/Contents/Home
compile:
	clj -A:native
