(ns piu.store.sql
  (:import [java.time Instant ZonedDateTime LocalDateTime ZoneId ZoneRegion]
           [java.time.format DateTimeFormatter])
  #_(:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]

            [piu.store :as store]))


(set! *warn-on-reflection* true)

(def sqlite-dt (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))
(def utc (ZoneId/of "UTC"))


#_(defrecord SQL [ds]
  store/Storage
  (has [this id]
    (some? (jdbc/execute-one! ds
             ["select id from piu where id = ?" id])))

  (read [this id]
    (-> (jdbc/execute-one! ds
          ["select id, created, lexer, raw, html from piu where id = ?" id]
          {:builder-fn rs/as-unqualified-lower-maps})
        (update :created #(-> (LocalDateTime/parse % sqlite-dt)
                              (.atZone ^ZoneRegion utc)))
        (store/fill-cache this)))

  (write [this {:keys [id raw lexer html]
                :or   {id (store/generate this)}}]
    (assert (and lexer raw html)
      "lexer, raw and html are required fields")
    (let [now (-> (ZonedDateTime/now)
                  (.withZoneSameInstant ^ZoneRegion utc)
                  (.format sqlite-dt))]
      (jdbc/execute-one! ds
        ["INSERT INTO piu (id, lexer, raw, html, created)
          VALUES (?, ?, ?, ?, ?)
          ON CONFLICT (id) DO UPDATE SET
          lexer = excluded.lexer, raw = excluded.raw, html = excluded.html"
         id lexer raw html now]
        {:builder-fn rs/as-unqualified-lower-maps})
      id)))


#_(defn create [ds]
  (->SQL (jdbc/get-datasource ds)))
