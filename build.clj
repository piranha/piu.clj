(ns build
  (:require [clojure.tools.build.api :as b]
            [clojure.java.io :as io]))


(def lib 'net.solovyov/piu)
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def version (str "1." (b/git-process {:git-args "rev-list HEAD --count"})))
(def uber-file "target/piu.jar")


(defn clean [_]
  (b/delete {:path "target"}))


(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs   ["src" "resources"]
               :target-dir class-dir})
  (spit (io/file class-dir "VERSION") version)
  (b/compile-clj {:basis     basis
                  :src-dirs  ["src"]
                  :class-dir class-dir})
  (b/uber {:lib       lib
           :class-dir class-dir
           :uber-file uber-file
           :basis     basis
           :main      'piu.main})
  (println "Created uberjar:" uber-file))
