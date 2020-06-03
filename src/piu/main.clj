(ns piu.main
  (:gen-class)
  (:require [mount.core :as mount]
            [org.httpkit.server :as httpkit]
            [com.brunobonacci.mulog :as u]

            [piu.app :as app]))


(set! *warn-on-reflection* true)


(mount/defstate logging
  :start (u/start-publisher! {:type :console})
  :stop (logging))


(defn port []
  (Integer/parseInt
    (or (System/getenv "PORT") "8000")))


(mount/defstate server
  :start (httpkit/run-server app/app {:port (port)})
  :stop (server))


(defn -main [& args]
  (println "Starting...")
  (mount/start)
  (println "Started on port" (port)))
