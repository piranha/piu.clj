(ns piu.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [mount.core :as mount]
            [ring.adapter.jetty :as jetty]
            [piu.app :as app]))


(mount/defstate server
  :start (jetty/run-jetty app/app {:join? false :port 8000})
  :stop  (.stop server))


(defn -main [& args]
  (println "Starting...")
  (mount/start))
