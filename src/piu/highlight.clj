(ns piu.highlight
  (:require [clojure.string :as str]
            [clojure.java.shell :as sh]))

(set! *warn-on-reflection* true)

;;; Split HTML in lines with correct open/close tags

(def TAG-RE #"</?[^>]+?>")

(defn make-closing-tag [^String tag]
  (if-let [i (str/index-of tag " ")]
    (str "</" (.substring tag 1 i) ">")
    (str "</" (.substring tag 1))))


(defn make-lines [html]
  (let [lines (.split ^String html "\n")
        *open (atom '())]
    (vec (for [line lines
               :let [line-open (str/join (reverse @*open))
                     _ (doseq [tag (re-seq TAG-RE line)]
                         ;; if a closing tag, throw away the last tag
                         ;; discovered, in other case add to a stack
                         (if (str/starts-with? tag "</")
                           (swap! *open rest)
                           (swap! *open conj tag)))
                     line-close (str/join
                                  (map make-closing-tag (reverse @*open)))]]
           (str line-open line line-close)))))


;;; Highlighting itself

(def ALIASES {"text" "plaintext"})


(defn hl [lang s]
  (let [lang        (get ALIASES lang lang)
        s           (str/trim s)
        res         (:out (sh/sh "sh" "-c" (format "./highlight.exe --lang %s" lang) :in s))
        [lang html] (str/split res #"\n" 2)]
    {:lexer lang
     :html  html
     :lines (make-lines html)}))
