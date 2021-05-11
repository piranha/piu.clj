(ns piu.views.markdown
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]])
  (:import [org.commonmark.parser Parser]
           [org.commonmark.renderer.html HtmlRenderer]
           [org.commonmark.ext.heading.anchor HeadingAnchorExtension]
           [org.commonmark.ext.gfm.strikethrough StrikethroughExtension]
           [org.commonmark.ext.gfm.tables TablesExtension]
           [org.commonmark.ext.ins InsExtension]
           [org.commonmark.ext.autolink AutolinkExtension]
           [org.commonmark.ext.task.list.items TaskListItemsExtension]))


(set! *warn-on-reflection* true)


(def extensions [(HeadingAnchorExtension/create)
                 (StrikethroughExtension/create)
                 (TablesExtension/create)
                 (InsExtension/create)
                 (AutolinkExtension/create)
                 (TaskListItemsExtension/create)])
(def ^Parser parser
  (-> (Parser/builder)
      (.extensions extensions)
      (.build)))
(def ^HtmlRenderer renderer
  (-> (HtmlRenderer/builder)
      (.extensions extensions)
      (.build)))


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

      [:footer.unit-0.flex-center {:style "border-top: solid 1px; background-color: #f5f5f5"}
       [:div
        "paste.in.ua ("
        [:a {:href "/about/"} "about"]
        ")"]]]]))


(defn render [^String md]
  (->> md
       (.parse parser)
       (.render renderer)))
