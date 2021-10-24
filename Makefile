VERSION = $(shell cat VERSION)
export JAVA_HOME = $(HOME)/var/graalvm-ce-java17-21.3.0/Contents/Home
export GRAALVM_HOME = $(JAVA_HOME)

run:
	clj -M:dev

uber:
	clojure -Srepro -e "(compile 'piu.main)"
	clojure -Srepro -M:uber

compile:
	clojure -Srepro -M:native

dump:
	clojure -Srepro -M:native:dump

ancient:
	clojure -M:dev:ancient

upgrade:
	clojure -M:dev:ancient --upgrade
