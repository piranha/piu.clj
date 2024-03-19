export JAVA_HOME ?= $(HOME)/var/graalvm-community-openjdk-21.0.2+13.1/Contents/Home
export GRAALVM_HOME ?= $(JAVA_HOME)

run:
	clj -M:dev

target/piu.jar: deps.edn $(shell find src -type f)
	clojure -Srepro -T:build uber

uber: target/piu.jar

native: uber
	$(GRAALVM_HOME)/bin/native-image \
	-jar target/piu.jar \
	-o piu \
	--report-unsupported-elements-at-runtime \
	-H:+UnlockExperimentalVMOptions \
	-H:ResourceConfigurationFiles=resource-config.json \
	-H:+ReportExceptionStackTraces \
	--no-fallback \
	--features=clj_easy.graal_build_time.InitClojureClasses

dump:
	clojure -Srepro -M:native:dump

ancient:
	clojure -M:dev:ancient

upgrade:
	clojure -M:dev:ancient --upgrade
