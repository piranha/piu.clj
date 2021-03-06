(ns piu.views.index
  (:require [hiccup.core :as hi]
            [piu.views.base :as base]))


(set! *warn-on-reflection* true)


(defn form [{:keys [raw lexers lexer]}]
  (hi/html
    [:form.flexv.unit {:action "." :method "post"}
     [:div.meta
      (base/lexer-select lexer lexers)

      " "   [:a.hot {:href "#" :rel "guess"} "Guess"]
      " | " [:a.hot {:href "#" :rel "plaintext"} "Plain text"]
      " | " [:a.hot {:href "#" :rel "python"} "Python"]
      " | " [:a.hot {:href "#" :rel "javascript"} "JavaScript"]
      " | " [:a.hot {:href "#" :rel "ruby"} "Ruby"]
      " | " [:a.hot {:href "#" :rel "css"} "CSS"]
      " | " [:a.hot {:href "#" :rel "xml"} "HTML"]
      " | " [:a.hot {:href "#" :rel "clojure"} "Clojure"]

      [:span.note "(press Ctrl-J to put focus on selectbox)"]]

     [:textarea#text.unit {:name "data" :autofocus true :tabindex 2} raw]

     [:div
      [:input {:type "submit" :value "Paste!"}]
      [:span.note "(or press Ctrl-Enter)"]]]))
