(ns piu.main
  (:gen-class)
  (:import [org.eclipse.jetty.server Server])
  (:require [mount.core :as mount]
            [ring.adapter.jetty :as jetty]

            [piu.app :as app]))


(set! *warn-on-reflection* true)


(def port (or (System/getenv "PORT") "8000"))


(mount/defstate server
  :start (jetty/run-jetty app/app {:join? false
                                   :port  (Integer/parseInt port)})
  :stop  (.stop ^Server server))


(defn -main [& args]
  (println "Starting...")
  (mount/start)
  (println "Started on port" port))
