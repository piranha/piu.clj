VERSION = $(shell cat VERSION)
export JAVA_HOME ?= $(HOME)/var/graalvm-ce-java11-20.0.0/Contents/Home
#GRAALVM_HOME ?= $(HOME)/var/graalvm-ce-java11-20.0.0/Contents/Home

run:
	clj -A:dev

compile:
	clojure -A:native
