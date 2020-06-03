(ns piu.store.fstore
  (:import [java.nio.file Paths]
           [java.time Instant ZoneId ZonedDateTime]
           [java.util.zip GZIPOutputStream GZIPInputStream])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [com.brunobonacci.mulog :as u]

            [piu.store :as store]
            [piu.tnetstrings :as tn]))


(set! *warn-on-reflection* true)

(def utc (ZoneId/of "UTC"))


(defn itempath [path id]
  (let [prefix (if (> store/ID-LEN (count id))
                 "0"
                 (str (first id)))]
    (Paths/get path (into-array String [prefix (str id ".piu.gz")]))))


(defn maybe-parse-int [x]
  (if (string? x)
    (Long/parseLong x)
    x))


(defrecord FStore [path]
  store/Storage
  (has [this id]
    (.exists (io/file (itempath path id))))

  (read [this id]
    (let [f (io/file (itempath path id))]
      (when (.exists f)
        (with-open [in (-> f
                           io/input-stream
                           GZIPInputStream.)]
          (let [data    (tn/loads (slurp in))
                created (-> (maybe-parse-int (:date data))
                            (Instant/ofEpochSecond)
                            (.atZone utc))]
            (cond-> data
              (and (:html data)
                   (str/starts-with? (:html data) "<table class=\"highlighttable\""))
              (dissoc :html)

              (nil? (:id data)) (assoc :id id)
              true              (-> (assoc :created created)
                                    (dissoc :date))
              true              (store/fill-cache this)))))))

  (write [this {:keys [id raw lexer html created]}]
    (assert (and lexer raw html)
      "lexer, raw and html are required fields")
    (let [id      (or id (store/generate this))
          created (or (some-> ^ZonedDateTime created
                        .toInstant)
                      (Instant/now))
          data    {:id    id
                   :raw   raw
                   :lexer lexer
                   :html  html
                   :date  (.getEpochSecond created)}
          s       (tn/dumps data)
          f       (io/file (itempath path id))]
      (.mkdirs (.getParentFile f))
      (with-open [out (-> f
                          io/file
                          io/output-stream
                          GZIPOutputStream.)]
        (.write out (.getBytes s "UTF-8"))
        (u/log ::write :id id :length (count raw))
        id))))


(defn create [path]
  (->FStore path))
