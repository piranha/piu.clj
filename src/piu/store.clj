(ns piu.store
  (:refer-clojure :exclude [read])
  (:require [piu.highlight :as hl]
            [piu.config :as config]))


(set! *warn-on-reflection* true)


(defprotocol Storage
  (has [this id])
  (read [this id])
  (write [this data]))


;;; utils

(defn fill-cache [data db]
  (if-not (and (:html data)
               (> 500000 (count (:raw data))))
    (try
      (let [res  (hl/hl (:lexer data) (:raw data))
            data (assoc data :html (:html res))]
        (write db data)
        data)
      (catch Exception e
        (let [res (if (re-find #"Unknown language" (.getMessage e))
                    (hl/hl "guess" (:raw data))
                    (throw e))]
          (assoc data :lexer (:lexer res) :html (:html res)))))
    data))


(def ALPHABET "0123456789abcdefghjiklmnopqrstuvwxyz")
(def IDLEN (config/IDLEN))


(defn -generate []
  (let [cnt (count ALPHABET)]
    (apply str
      (repeatedly IDLEN
        (fn []
          (get ALPHABET (Math/floor (* cnt (Math/random)))))))))


(defn generate [store]
  (loop [i 9999]
    (if (zero? i)
      (throw (ex-info "Could not generate unique id" {}))
      (let [id (-generate)]
        (if (has store id)
          (recur (dec i))
          id)))))
