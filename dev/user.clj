(ns user
  (:require [mount.core :as mount]
            [piu.main]))

(def start mount/start)
(def stop mount/stop)

(comment
  (add-lib '[com.brunobonacci/mulog "0.2.0"]))
