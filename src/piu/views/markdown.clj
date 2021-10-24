(ns piu.views.markdown
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]])
  (:import [com.github.rjeschke.txtmark Processor Configuration]))


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


(defn render [^String md]
  (let [cfg (-> (Configuration/builder)
                (.forceExtentedProfile)
                (.build))]
    (Processor/process md cfg)))
