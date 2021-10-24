VERSION = $(shell cat VERSION)
export JAVA_HOME = $(HOME)/var/graalvm-ce-java11-21.1.0/Contents/Home
export GRAALVM_HOME = $(JAVA_HOME)

run:
	clj -M:dev

uber:
	clojure -Srepro -e "(compile 'piu.main)"
	clojure -Srepro -M:uber

compile:
	clojure -Srepro -M:native

ancient:
	clojure -M:dev:ancient

upgrade:
	clojure -M:dev:ancient --upgrade
