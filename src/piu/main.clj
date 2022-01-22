(ns piu.main
  (:gen-class)
  (:require [mount.core :as mount]
            [org.httpkit.server :as httpkit]

            [piu.config :as config]
            [piu.log :as log]
            [piu.app :as app]))


(set! *warn-on-reflection* true)
(alter-var-root #'log/*logger (fn [_] (log/->Stdout)))


(mount/defstate server
  :start (let [p (config/PORT)]
           (println "Opening port" p)
           (httpkit/run-server app/app {:port p}))
  :stop (server))


(defn -main [& args]
  (mount/start))
