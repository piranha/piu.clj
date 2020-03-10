(ns piu.highlight
  (:import [org.graalvm.polyglot Context Value])
  (:require [mount.core :as mount]
            [clojure.java.io :as io]
            [clojure.string :as str]))


(mount/defstate ctx
  :start (let [ctx       (.build (Context/newBuilder (into-array ["js"])))
               highlight (slurp (io/resource "highlight.min.js"))]
           (.eval ^Context ctx "js" "var window = this;")
           (.eval ^Context ctx "js" highlight)
           ctx)
  :stop (.close ctx))


(defn eval-js [s]
  (.eval ^Context ctx "js" s))


(defn- execute [^Value execable & args] ;; just a little sugar
  (.execute execable (object-array args)))


(defmacro ^:private reify-ifn
  "Convenience macro for reifying IFn for executable polyglot Values."
  [v]
  (let [invoke-arity
        (fn [n]
          (let [args (map #(symbol (str "arg" (inc %))) (range n))]
            (if (seq args)
              `(~'invoke [this# ~@args] (value->clj (execute ~v ~@args)))
              `(~'invoke [this#] (value->clj (execute ~v))))))]
    `(reify clojure.lang.IFn
       ~@(map invoke-arity (range 22))
       (~'applyTo [this# args#] (value->clj (apply execute ~v args#))))))


(defn value->clj
  "Returns a Clojure (or Java) value for given polyglot Value if possible,
   otherwise throws."
  [^Value v]
  (cond
    (.isNull v) nil
    (.isHostObject v) (.asHostObject v)
    (.isBoolean v) (.asBoolean v)
    (.isString v) (.asString v)
    (.isNumber v) (.as v Number)
    (.canExecute v) (reify-ifn v)
    (.hasArrayElements v) (into []
                                (for [i (range (.getArraySize v))]
                                  (value->clj (.getArrayElement v i))))
    (.hasMembers v) (into {}
                          (for [k (.getMemberKeys v)]
                            [(keyword k) (value->clj (.getMember v k))]))
    :else (throw (Exception. "Unsupported value"))))


(def js (comp value->clj eval-js))


(def ALIASES {"text" "plaintext"})

(mount/defstate -hl
  :start (when ctx
           (js "(lang, s) => {
               if (lang) {
                 return {html: hljs.highlight(lang, s, true).value, lexer: lang};
               } else {
                 var res = hljs.highlightAuto(s);
                 return {html: res.value, lexer: res.language};
               }
             }")))


(defn hl [lang s]
  (let [lang (get ALIASES lang lang)]
    (if (= lang "guess")
      (-hl nil (str/trim s))
      (-hl lang (str/trim s)))))
