ifneq (,$(wildcard $(JAVA_HOME)/bin/native-image))
	GRAALVM_HOME ?= $(JAVA_HOME)
else ifneq (,$(shell which native-image))
else
	GRAALVM_HOME ?= $(HOME)/var/graalvm-community/Contents/Home
endif

run:
	clj -M:dev

target/piu.jar: deps.edn $(shell find src resources -type f)
	clojure -Srepro -T:build uber

uber: target/piu.jar build.clj

native: uber
	$(GRAALVM_HOME)/bin/native-image \
	-jar target/piu.jar \
	-o piu \
	-H:+UnlockExperimentalVMOptions \
	-H:+ReportExceptionStackTraces \
	--no-fallback \
	--features=clj_easy.graal_build_time.InitClojureClasses

dump:
	clojure -Srepro -M:native:dump

ancient:
	clojure -M:dev:ancient

upgrade:
	clojure -M:dev:ancient --upgrade
