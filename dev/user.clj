(ns user
  (:require [mount.core :as mount]
            [cemerick.pomegranate :as pomegranate]
            [piu.main]))


(comment
  (let [cl (.getContextClassLoader (Thread/currentThread))]
    (.setContextClassLoader (Thread/currentThread) (clojure.lang.DynamicClassLoader. cl))
    (add-dependencies
      :coordinates '[[com.brunobonacci/mulog "0.2.0"]]
      :repositories (merge cemerick.pomegranate.aether/maven-central
                      {"clojars" "https://clojars.org/repo"}))))
