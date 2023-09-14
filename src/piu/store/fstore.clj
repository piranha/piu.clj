(ns piu.store.fstore
  (:import [java.io File FileNotFoundException]
           [java.nio.file Path Paths]
           [java.time Instant ZoneId ZonedDateTime]
           [java.util.zip GZIPOutputStream GZIPInputStream])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]

            [piu.log :as log]
            [piu.store :as store]
            [piu.tnetstrings :as tn]))


(set! *warn-on-reflection* true)

(def utc (ZoneId/of "UTC"))


(extend-protocol io/Coercions
  Path
  (as-file [this] (.toFile this))
  (as-url [this] (.. this (toFile) (toURL))))


(defn item-file ^File [path id]
  (let [prefix (if (< (count id) 4)
                 "0"
                 (str (first id)))]
    (io/file
      (Paths/get path (into-array String [prefix (str id ".piu.gz")])))))


(defn item-value ^String [path id]
  (try
    (with-open [in (-> (item-file path id)
                       io/input-stream
                       GZIPInputStream.)]
      (slurp in))
    (catch FileNotFoundException _
      nil)))


(defn maybe-parse-int [x]
  (if (string? x)
    (Long/parseLong x)
    x))


(defn compat-loads [^String id ^String s]
  (if (every? #(Character/isDigit ^Character %) id)
    ;; old python tnetstrings counted bytes instead of codepoints
    (tn/loads (.getBytes s "UTF-8"))
    (tn/loads s)))


(defrecord FStore [path]
  store/Storage
  (has [this id]
    (.exists (item-file path id)))

  (read [this id]
    (when-let [s (item-value path id)]
      (let [data    (tn/loads s)
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
          true              (store/fill-cache this)))))

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
          f       (item-file path id)]
      (.mkdirs (.getParentFile f))
      (with-open [out (-> f
                          io/file
                          io/output-stream
                          GZIPOutputStream.)]
        (.write out (.getBytes s "UTF-8"))
        (log/info ::write {:id id :length (count raw)})
        id))))


(defn create [path]
  (->FStore path))
