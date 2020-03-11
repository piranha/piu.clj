(ns piu.store
  (:refer-clojure :exclude [read])
  (:import [java.time Instant ZonedDateTime LocalDateTime ZoneId ZoneRegion]
           [java.time.format DateTimeFormatter])
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [mount.core :as mount]

            [piu.highlight :as hl]))


(set! *warn-on-reflection* true)


(declare fill-cache generate)
(def sqlite-dt (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))
(def gmt (ZoneId/of "GMT"))


(defprotocol Storage
  (read [this id])
  (write [this data]))


(defrecord SQL [ds]
  Storage
  (read [this id]
    (-> (jdbc/execute-one! ds
          ["select id, created, lexer, raw, html from piu where id = ?" id]
          {:builder-fn rs/as-unqualified-lower-maps})
        (update :created #(-> (LocalDateTime/parse % sqlite-dt)
                              (.atZone ^ZoneRegion gmt)))
        (fill-cache this)))
  (write [this data]
    (assert (and (:lexer data) (:raw data))
      "lexer and raw are required fields")
    (jdbc/with-transaction [tx ds]
      (let [id                   (or (:id data) (generate tx))
            raw                  (:raw data)
            {:keys [lexer html]} (if (:html data)
                                   data
                                   (hl/hl (:lexer data) raw))
            now                  (-> (ZonedDateTime/now)
                                     (.withZoneSameInstant ^ZoneRegion gmt)
                                     (.format sqlite-dt))]
        (jdbc/execute-one! tx
          ["INSERT INTO piu (id, lexer, raw, html, created)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
            lexer = excluded.lexer, raw = excluded.raw, html = excluded.html"
           id lexer raw html now]
          {:builder-fn rs/as-unqualified-lower-maps})
        id))))


(def SQLITE-PATH (or (System/getenv "DBPATH")
                     "piu.sqlite"))

(mount/defstate db
  :start (->SQL (jdbc/get-datasource {:dbtype "sqlite"
                                      :dbname SQLITE-PATH})))


;;; utils

(defn fill-cache [data db]
  (if-not (and (:html data)
               (> 500000 (count (:raw data))))
    (try
      (let [res (hl/hl (:lexer data) (:raw data))
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
(def ID-LEN 4)


(defn -generate []
  (let [cnt (count ALPHABET)]
    (apply str
      (repeatedly ID-LEN
        (fn []
          (get ALPHABET (Math/floor (* cnt (Math/random)))))))))


(defn generate [ds]
  (loop [i 9999]
    (if (zero? i)
      (throw (ex-info "Could not generate unique id" {}))
      (let [id (-generate)]
        (if (nil? (jdbc/execute-one! ds
                    ["select id from piu where id = ?" id]))
          id
          (recur (dec i)))))))
