(ns piu.main
  (:gen-class)
  (:import [org.eclipse.jetty.server Server])
  (:require [mount.core :as mount]
            [ring.adapter.jetty :as jetty]

            [piu.app :as app]))


(set! *warn-on-reflection* true)


(mount/defstate server
  :start (jetty/run-jetty app/app {:join? false :port 8000})
  :stop  (.stop ^Server server))


(defn -main [& args]
  (println "Starting...")
  (mount/start))
