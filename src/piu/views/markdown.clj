(ns piu.views.markdown
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]]
            [markdown.core :as markdown]
            [clojure.string :as str]))


(set! *warn-on-reflection* true)


(defn t [html]
  (hi/html
    (:html5 doctype)
    [:html {:style "height: 100%"}
     [:head
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
      [:title "paste.in.ua"]
      [:link {:rel  "stylesheet"
              :href "https://unpkg.com/mobi.css/dist/mobi.min.css"}]]
     [:body.flex-vertical {:style "min-height: 100%"}
      [:div.unit.flex-center
       [:div.container
        html]]

      [:footer.unit-0.flex-center
       {:style "border-top: solid 1px; background-color: #f5f5f5"}
       [:div
        "paste.in.ua ("
        [:a {:href "/about/"} "about"]
        ")"]]]]))


(defn heading-anchors
  "build-in heading-anchors generate no link"
  [text state]
  [(or (when (:inline-heading state)
         (when-let [[level inner] (rest (re-matches #"<h(\d)>(.*)</h\d>" text))]
           (let [anchor (-> inner str/lower-case (str/replace " " "-"))]
             (format "<h%s id=\"%s\"><a href=\"#%s\">%s</a></h%s>"
               level anchor anchor text level))))
       text)
   state])

(defn render [^String md]
  (markdown/md-to-html-string md
    :reference-links? true
    :footnotes? true
    :custom-transformers [heading-anchors]))
