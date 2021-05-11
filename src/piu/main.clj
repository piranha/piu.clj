(ns piu.main
  (:gen-class)
  (:require [mount.core :as mount]
            [org.httpkit.server :as httpkit]

            [piu.log :as log]
            [piu.app :as app]))


(set! *warn-on-reflection* true)
(alter-var-root #'log/*logger (fn [_] (log/->Stdout)))


(defn port []
  (Integer/parseInt
    (or (System/getenv "PORT") "8000")))


(mount/defstate server
  :start (let [p (port)]
           (println "Opening port" p)
           (httpkit/run-server app/app {:port p}))
  :stop (server))


(defn -main [& args]
  (mount/start))
