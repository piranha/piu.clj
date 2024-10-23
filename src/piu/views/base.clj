(ns piu.views.base
  (:import [java.util.zip Adler32])
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)


(defn adler32 [s]
  (-> (doto (Adler32.)
        (.update (.getBytes ^String s "UTF-8")))
      .getValue))


(def get-hash
  (memoize
    (fn [filename]
      (some-> filename io/resource slurp adler32 str))))


(defn static [path]
  (str "/static/" path "?v=" (get-hash (str "public/" path))))


(defn head []
  (hi/html
    [:head
     [:meta {:charset "utf-8"}]
     [:title "paste.in.ua"]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]

     [:link {:rel "icon" :href "data:image/svg+xml,<svg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22><text y=%22.9em%22 font-size=%2290%22>💾</text></svg>"}]
     [:link {:rel "stylesheet" :href (static "main.css")}]
     [:link {:rel "stylesheet" :href (static "xcode.css")}]
     [:script {:src (static "all.js")}]]))


(defn wrap [content]
  (hi/html
    (:html5 doctype)
    [:html
     (head)
     [:body.flexv
      [:header
       [:h1.ib [:a {:href "/"} "paste.in.ua"]]
       [:span.ml5 "(" [:a {:href "/about/"} "about"] ")"]]

      [:div#content.flexv.unit content]]]))


(defn lexer-select [lexer lexers & [{:keys [onchange]}]]
  (hi/html
    [:input#lexers {:list     "lexers-list"
                    :name     "lexer"
                    :value    lexer
                    :onfocus  "this.select()"
                    :onchange onchange
                    :tabindex 1}]
    [:datalist#lexers-list
     [:option {:value "guess"} "Guess type"]
     (for [item lexers]
       [:option {:value (:lexer item)}
        (:name item)])]))
