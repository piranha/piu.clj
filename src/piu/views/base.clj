(ns piu.views.base
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]]))


(defn head []
  (hi/html
    [:head
     [:meta {:charset "utf-8"}]
     [:title "paste.in.ua"]
     [:link {:rel "stylesheet" :href "/static/main.css"}]
     [:link {:rel "stylesheet" :href "/static/xcode.css"}]
     [:script {:src "/static/main.js"}]]))


(defn footer []
  (hi/html
    [:div#footer
     [:p
      "© since 2009 "
      [:a {:href "https://solovyov.net/"} "Alexander Solovyov"] ", "
      [:a {:href "/about/"} "about paste.in.ua"]]]))


(defn wrap [content]
  (hi/html
    (:html5 doctype)
    [:html
     (head)
     [:body
      [:header
       [:h1 [:a {:href "/"} "paste.in.ua"]]]

      [:div#content content]

      (footer)]]))


(defn lexer-select [lexer lexers]
  (hi/html
    [:select#lexers {:name "lexer"}
     [:option {:value "guess"} "Guess type"]
     (for [item lexers]
       [:option {:value    (:lexer item)
                 :selected (= (:lexer item) lexer)}
        (:name item)])]))
