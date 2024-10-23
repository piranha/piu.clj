ifneq (,$(wildcard $(JAVA_HOME)/bin/native-image))
	GRAALVM_HOME ?= $(JAVA_HOME)
else ifneq (,$(shell which native-image))
else
	GRAALVM_HOME ?= $(HOME)/var/graalvm-community-openjdk-23.0.1+11.1/Contents/Home
endif

run:
	clj -M:dev

target/piu.jar: deps.edn $(shell find src -type f)
	clojure -Srepro -T:build uber

uber: target/piu.jar build.clj

native: uber
	$(GRAALVM_HOME)/bin/native-image \
	-jar target/piu.jar \
	-o piu \
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
