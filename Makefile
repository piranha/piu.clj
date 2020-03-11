VERSION = $(shell cat VERSION)
export JAVA_HOME ?= $(HOME)/var/graalvm-ce-java11-20.0.0/Contents/Home
export GRAALVM_HOME ?= $(JAVA_HOME)

run:
	clj -A:dev

uber:
	clojure -Srepro -A:uber

native:
	$(GRAALVM_HOME)/bin/native-image -jar target/piu.jar \
		-H:name=piu \
		-H:+ReportExceptionStackTraces \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		--initialize-at-build-time \
		-H:Log=registerResource: \
		-H:EnableURLProtocols=http,https \
		--enable-all-security-services \
		--verbose \
		--no-server \
		--no-fallback \
		--report-unsupported-elements-at-runtime

compile:
	clojure -Srepro -A:native
