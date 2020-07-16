(ns piu.views.render
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]]
            [clojure.string :as str]
            [markdown.core :as markdown]

            [piu.views.base :as base]))


(set! *warn-on-reflection* true)


(defn t [html]
  (hi/html
    (:html5 doctype)
    [:html
     [:head
      [:meta {:charset "utf-8"}]
      [:title "paste.in.ua"]
      [:link {:rel  "stylesheet"
              :href "https://unpkg.com/mobi.css/dist/mobi.min.css"}]]
     [:body
      [:div.flex-center
       [:div.container
        html]]

      (base/footer)]]))



(defn escape-html
    "Change special characters into HTML character entities."
    [text state]
    [(if-not (or (:code state) (:codeblock state))
       (clojure.string/escape
         text
         {\& "&amp;"
          \< "&lt;"
          \> "&gt;"
          \" "&quot;"
          \' "&#39;"})
       text) state])


(defn heading-anchors [text state]
  [(or (when (:inline-heading state)
         (when-let [[level inner] (rest (re-matches #"<h(\d)>(.*)</h\d>" text))]
           (let [anchor (-> inner str/lower-case (str/replace " " "-"))]
             (format "<h%s id=\"%s\"><a href=\"#%s\">%s</a></h%s>"
               level anchor anchor text level))))
       text)
   state])


(defn render [^String md]
  (markdown/md-to-html md
    :reference-links? true
    :footnotes? true
    :custom-transformers [heading-anchors ; native heading-anchors have no link
                          #_escape-html]))
