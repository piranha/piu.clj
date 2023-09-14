VERSION = $(shell cat VERSION)
export JAVA_HOME = $(HOME)/var/graalvm-community-openjdk-20.0.2+9.1/Contents/Home
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

highlight:
	ls -d highlight.js && \
		cd highlight.js && git pull || \
		git clone --depth=1 git@github.com:highlightjs/highlight.js.git
	cd highlight.js && bun i && bun tools/build.js -t browser
	cp highlight.js/build/highlight.js resources/
