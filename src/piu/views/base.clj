(ns piu.views.base
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]]))

(set! *warn-on-reflection* true)

(defn head []
  (hi/html
    [:head
     [:meta {:charset "utf-8"}]
     [:title "paste.in.ua"]
     [:link {:rel "stylesheet" :href "/static/main.css"}]
     [:link {:rel "stylesheet" :href "/static/xcode.css"}]
     [:script {:src "/static/all.js"}]]))


(defn footer []
  (hi/html
    [:footer
     [:div
      "paste.in.ua ("
      [:a {:href "/about/"} "about"]
      ") © since 2009 "
      [:a {:href "https://solovyov.net/"} "Alexander Solovyov"]]]))


(defn wrap [content]
  (hi/html
    (:html5 doctype)
    [:html
     (head)
     [:body.flexv
      [:header
       [:h1 [:a {:href "/"} "paste.in.ua"]]]

      [:div#content.flexv.unit content]

      (footer)]]))


(defn lexer-select [lexer lexers]
  (hi/html
    [:input#lexers {:list    "lexers-list"
                    :name    "lexer"
                    :value   lexer
                    :onfocus "this.select()"
                    :tabindex 1}]
    [:datalist#lexers-list
     [:option {:value "guess"} "Guess type"]
     (for [item lexers]
       [:option {:value (:lexer item)}
        (:name item)])]))
