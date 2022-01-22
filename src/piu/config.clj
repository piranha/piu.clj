(ns piu.config)


(set! *warn-on-reflection* true)


(defn PORT []
  (Integer/parseInt
    (or (System/getenv "PORT") "8000")))


(defn DBPATH []
  (or (System/getenv "DBPATH")
      "store"))


(defn SECRET []
  (if-let [s (System/getenv "SECRET")]
    (.getBytes s "UTF-8")
    (binding [*out* *err*]
      (print "\nWARNING: set 'SECRET' env variable to be secure\n\n")
      (.getBytes "epic-secret" "UTF-8"))))
