(ns user
  (:require [mount.core :as mount]
            [cemerick.pomegranate :as pomegranate]
            [cemerick.pomegranate.aether]

            [piu.main]))


(def start mount/start)
(def stop mount/stop)


(defn add-deps [& coordinates]
  (let [thread (Thread/currentThread)
        cl     (.getContextClassLoader thread)]
    (when-not (instance? clojure.lang.DynamicClassLoader cl)
      (.setContextClassLoader thread (clojure.lang.DynamicClassLoader. cl))))
  (pomegranate/add-dependencies
    :coordinates coordinates
    :repositories (assoc cemerick.pomegranate.aether/maven-central
                    "clojars" "https://clojars.org/repo")))


(comment
  (add-deps '[com.brunobonacci/mulog "0.2.0"]))
