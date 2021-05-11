(ns piu.tnetstrings
  "From https://github.com/alexgartrell/tnetstrings-clj")

(set! *warn-on-reflection* true)


(defn explode-tnetstring [^String s]
  (assert (seq s) "Cannot parse empty string")
  (let [idx (.indexOf s ":")
        _   (when (neg? idx)
              (throw (ex-info "Cannot find : in string" {:data s})))

        size  (Long/parseUnsignedLong (.substring s 0 idx))
        start (inc idx)
        end   (+ start size)

        data    (.substring s start end)
        type    (.charAt    s end)
        remains (.substring s (inc end))]
    [data type remains]))


(declare load-item)


(defn- load-int [^String s]
  (try
    (Long/parseLong s)
    (catch java.lang.NumberFormatException e
      (throw (ex-info (str "Invalid integer: " s) {:data s} e)))))


(defn- load-bool [s]
  (case s
    "true"  true
    "false" false
    (throw (ex-info (str "Invalid boolean: " s) {:data s}))))


(defn- load-vector [data]
  (loop [data  data
         accum []]
    (if (seq data)
      (let [[data type remains] (explode-tnetstring data)
            item                (load-item data type)]
        (recur remains
               (conj accum item)))
      accum)))


(defn maybe-keyword [s]
  (if (string? s)
    (keyword s)
    s))


(defn- load-map [data]
  (loop [data  data
         accum {}]
    (if (seq data)
      (let [[kdata ktype remains] (explode-tnetstring data)
            [vdata vtype remains] (explode-tnetstring remains)
            key                   (maybe-keyword (load-item kdata ktype))
            val                   (load-item vdata vtype)]
        (recur remains (assoc accum key val)))
      accum)))


(defn- load-item [^String data type]
  (case type
    \~ nil
    \, data
    \# (load-int data)
    \! (load-bool data)
    \] (load-vector data)
    \} (load-map data)))


;;; Helper functions for dumps

(defn -dump-item [^Character type ^String value]
  (str (.length value) \: value type))


(defprotocol Dump
  (dump-item [this]))


(extend-protocol Dump
  nil
  (dump-item [_]
    "0:~")

  String
  (dump-item [s]
    (-dump-item \, s))

  clojure.lang.Keyword
  (dump-item [k]
    (-dump-item \, (name k)))

  Number
  (dump-item [x]
    (-dump-item \# (str x)))

  Boolean
  (dump-item [b]
    (if b "4:true!" "5:false!"))

  clojure.lang.Sequential
  (dump-item [l]
    (let [sb (StringBuilder.)]
      (run! #(.append sb (dump-item %)) l)
      (-dump-item \] (str sb))))

  clojure.lang.IPersistentMap
  (dump-item [m]
    (let [sb (StringBuilder.)]
      (run! (fn [e]
              (.append sb (dump-item (key e)))
              (.append sb (dump-item (val e))))
        m)
      (-dump-item \} (str sb)))))


;;; Public Functions
(defn loads [^String s]
  (let [[data type _] (explode-tnetstring s)]
    (load-item data type)))

(defn dumps ^String [data]
  (dump-item data))
