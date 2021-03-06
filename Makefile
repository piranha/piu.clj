VERSION = $(shell cat VERSION)
export JAVA_HOME = $(HOME)/var/graalvm-ce-java11-21.1.0/Contents/Home
export GRAALVM_HOME = $(JAVA_HOME)

run:
	clj -M:dev

uber:
	clojure -Srepro -e "(compile 'piu.main)"
	clojure -Srepro -M:uber

native:
	$(GRAALVM_HOME)/bin/native-image -jar target/piu.jar \
		-H:Name=piu \
		-H:+ReportExceptionStackTraces \
		-H:+RemoveSaturatedTypeFlows \
		-J-Dclojure.spec.skip-macros=true \
		-J-Dclojure.compiler.direct-linking=true \
		--initialize-at-build-time \
		-H:Log=registerResource: \
		-H:EnableURLProtocols=http,https \
		-H:ResourceConfigurationFiles=resource-config.json \
		--enable-all-security-services \
		--no-server \
		--no-fallback \
		--report-unsupported-elements-at-runtime \
		--language:js \
		-H:JNIConfigurationFiles=jni.json

compile:
#	$(JAVA_HOME)/bin/javac -cp $(JAVA_HOME)/jre/lib/svm/builder/svm.jar resources/CutOffCoreServicesDependencies.java
	clojure -Srepro -M:native

ancient:
	clojure -M:dev:ancient

upgrade:
	clojure -M:dev:ancient --upgrade
