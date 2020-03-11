(ns piu.main
  (:gen-class)
  (:require [mount.core :as mount]
            [org.httpkit.server :as httpkit]

            [piu.app :as app]))


(set! *warn-on-reflection* true)


(def port (or (System/getenv "PORT") "8000"))


(mount/defstate server
  :start (httpkit/run-server app/app {:port (Integer/parseInt port)})
  :stop (server))


(defn -main [& args]
  (println "Starting...")
  (mount/start)
  (println "Started on port" port))
