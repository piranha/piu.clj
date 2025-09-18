run:
	clj -M:dev

target/piu.jar: deps.edn $(shell find src resources -type f)
	clojure -Srepro -T:build uber

uber: target/piu.jar build.clj

native: uber
	@which sdk && sdk env || echo 'no sdkman'
	native-image \
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
