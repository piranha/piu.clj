(ns piu.views.show
  (:import [java.time.format DateTimeFormatter])
  (:require [hiccup.core :as hi]
            [piu.views.base :as base]))

(set! *warn-on-reflection* true)

(def iso8601 DateTimeFormatter/ISO_OFFSET_DATE_TIME)
(def rfc1123 DateTimeFormatter/RFC_1123_DATE_TIME)

(defn t [{:keys [data owner? lexer lexers]}]
  (hi/html
    [:div.meta.flex
     [:span.unit "Pasted at "
      [:time {:datetime (.format ^DateTimeFormatter iso8601 (:created data))}
       (.format ^DateTimeFormatter rfc1123 (:created data))]
      " | Highlighted as "
      lexer]

     [:span {:style "padding-left: 2em;"}
      (base/lexer-select lexer lexers) " "

      (when owner?
        '([:a {:href "edit/"} "edit"]
          " | "))

      (when (= lexer "json")
        '([:a {:href "?pretty"} "pretty-print JSON"]
          " | "))

      (when (#{"md" "markdown" "html"} lexer)
        '([:a {:href "render/"} "render content as HTML"]
          " | "))

      [:a#wrap {:href "#"} "toggle wrap"]
      " | "
      [:a {:href "raw/"} "raw"]]]

    [:div
     [:table.highlight
      [:tbody
       (map-indexed
         (fn [i line]
           (let [i (inc i)]
             [:tr
              [:td.line {:data-line i} i]
              [:td.code {:id i} line]]))
         (:lines data))]]]

    [:span.note
     "&uarr; Click line number to highlight; hold shift to highlight range; hold cmd/ctrl to add ranges"]))
