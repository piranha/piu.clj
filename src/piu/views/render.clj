(ns piu.views.render
  (:import [com.vladsch.flexmark.parser Parser]
           [com.vladsch.flexmark.html HtmlRenderer]
           [com.vladsch.flexmark.util.data MutableDataSet]
           [com.vladsch.flexmark.ext.tables TablesExtension]
           [com.vladsch.flexmark.ext.anchorlink AnchorLinkExtension]
           [com.vladsch.flexmark.ext.autolink AutolinkExtension]
           [com.vladsch.flexmark.ext.footnotes FootnoteExtension]
           [com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughSubscriptExtension]
           [com.vladsch.flexmark.ext.gfm.tasklist TaskListExtension]
           [com.vladsch.flexmark.ext.ins InsExtension]
           [com.vladsch.flexmark.ext.superscript SuperscriptExtension]
           [com.vladsch.flexmark.ext.toc TocExtension]
           [com.vladsch.flexmark.ext.typographic TypographicExtension])
  (:require [hiccup.core :as hi]
            [hiccup.page :refer [doctype]]
            [clojure.string :as str]

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


(let [opts     (doto (MutableDataSet.)
                 (.set Parser/EXTENSIONS
                   [(TablesExtension/create)
                    (AnchorLinkExtension/create)
                    (AutolinkExtension/create)
                    (FootnoteExtension/create)
                    (StrikethroughSubscriptExtension/create)
                    (TaskListExtension/create)
                    (InsExtension/create)
                    (SuperscriptExtension/create)
                    (TocExtension/create)
                    (TypographicExtension/create)])
                 (.set HtmlRenderer/PERCENT_ENCODE_URLS true)
                 (.set TablesExtension/COLUMN_SPANS false)
                 (.set TablesExtension/APPEND_MISSING_COLUMNS true)
                 (.set TablesExtension/DISCARD_EXTRA_COLUMNS true)
                 (.set TablesExtension/HEADER_SEPARATOR_COLUMN_MATCH true))
      parser   (.build (Parser/builder opts))
      renderer (.build (HtmlRenderer/builder opts))]
  (defn render [^String md]
    (let [doc  (.parse parser md)
          html (.render renderer doc)]
      (str/replace html #"<script.*?</script>" ""))))
