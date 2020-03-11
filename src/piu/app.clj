(ns piu.app
  (:require [clojure.string :as str]
            [reitit.ring :as ring]
            [reitit.ring.middleware.parameters :as params]
            [ring.middleware.cookies :as cookies]
            [jsonista.core :as json]
            [buddy.sign.jwt :as jwt]

            [piu.store :as store]
            [piu.highlight :as hl]
            [piu.views.base :as base]
            [piu.views.index :as index]
            [piu.views.show :as show]
            [piu.views.render :as render]
            [clojure.java.io :as io]))


(set! *warn-on-reflection* true)


(def SECRET (or (System/getenv "SECRET")
                (binding [*out* *err*]
                  (print "\nWARNING: set 'SECRET' env variable to be secure\n\n")
                  "epic-secret")))

(def SPAM-RE #"^comment\d+,")
(def LEXERS (delay (let [lang-data (hl/js "l => {
                                             var x = hljs.getLanguage(l);
                                             return {name: x.name, aliases: x.aliases};
                                           }")]
                     (->> (hl/js "hljs.listLanguages()")
                          (map #(assoc (lang-data %) :lexer %))
                          (sort-by (comp str/lower-case :name))))))

(defn sign [id]
  (jwt/sign {:id id} SECRET))


(defn unsign [s]
  (try
    (jwt/unsign s SECRET)
    (catch Exception e
      nil)))


(defn q? [req k]
  (or (= (:query-string req) k)
      (contains? (:query-params req) k)))


(def mapper (json/object-mapper {:pretty true}))
(defn pretty-json [s]
  (-> s (json/read-value) (json/write-value-as-string mapper)))


(defn show [req]
  (let [id     (-> req :path-params :id)
        data   (store/read store/db id)
        owner? (-> req :cookies (get id) :value unsign some?)
        lexer  (get (:query-params req) "as" (:lexer data))
        data   (cond-> data
                 (not= lexer (:lexer data))
                 (assoc :html (:html (hl/hl lexer (:raw data))))

                 (and (= lexer "json")
                      (q? req "pretty"))
                 (assoc :html (:html (hl/hl lexer (pretty-json (:raw data))))))]
    (if data
      {:status  200
       :headers {"content-type" "text/html"}
       :body    (base/wrap
                  (show/t {:data   data
                           :owner? owner?
                           :lexer  lexer
                           :lexers @LEXERS}))}
      {:status 404
       :body "Not Found"})))


(defn render [req]
  (let [id   (-> req :path-params :id)
        data (store/read store/db id)]
    (if data
      {:status  200
       :headers {"content-type" "text/html"}
       :body    (render/t (render/render (:raw data)))}
      {:status 404
       :body   "Not Found"})))


(defn raw [req]
  (let [id   (-> req :path-params :id)
        data (store/read store/db id)]
    (if data
      {:status  200
       :headers {"content-type" "text/plain; charset=utf-8"}
       :body    (if (q? req "more")
                  (pr-str data)
                  (:raw data))}
      {:status  307 ;; temporary redirect
       :headers {"location" "/"}})))


(defn index [req]
  {:status 200
   :body   (base/wrap
             (index/form {:lexers @LEXERS}))})


(defn create [req]
  (let [form (:form-params req)
        id   (-> req :path-params :id)]
    (cond
      (and id
           (nil? (-> req :cookies (get id) :value unsign)))
      {:status 403
       :body   "That does not look like your data!\n"}

      (str/blank? (get form "data"))
      {:status 400
       :body   "You have to submit non-empty 'data' field\n"}

      (re-find SPAM-RE (get form "data"))
      {:status 402
       :body   "Wanna spam? I'll let you if you pay me! :-)"}

      :else
      (let [lexer (get form "lexer" "guess")
            id    (store/write store/db {:id    id
                                         :lexer lexer
                                         :raw   (get form "data")})]
        {:status  303
         :cookies {id {:value     (sign id)
                       :path      "/"
                       :same-site :strict
                       :max-age   (* 3600 24 7)}}
         :headers {"location" (format "/%s/" id)}}))))


(defn edit [req]
  (let [id   (-> req :path-params :id)
        data (store/read store/db id)]
    (if (and data
             (-> req :cookies (get id) :value unsign some?))
      {:status 200
       :body   (base/wrap
                 (index/form {:lexers @LEXERS
                              :lexer  (:lexer data)
                              :raw    (:raw data)}))}
      {:status 404
       :body "Not Found"})))


(defn about [req]
  {:status  200
   :headers {"content-type" "text/html"}
   :body    (base/wrap (render/render (slurp (io/resource "about.md"))))})


(def resource (ring/create-resource-handler {:parameter :path}))
(defn one-file [path]
  (fn [req]
    (resource (assoc req :path-params {:path path}))))


(defn piu-py [req]
  (let [lexers (->> @LEXERS
                    (map #(format "'%s'" (:lexer %)))
                    (str/join ",")
                    (format "[%s]"))
        extmap (->> @LEXERS
                    (remove #(#{"lasso" "arduino" "sml" "c-like"} (:lexer %)))
                    (mapcat (fn [x] (map vector (:aliases x) (repeat (:lexer x)))))
                    (map #(format "'*.%s': '%s'" (first %) (second %)))
                    (str/join ",")
                    (format "{%s}"))
        src    (-> (slurp (io/resource "piu.py.tpl"))
                   (str/replace #"#lexers#" lexers)
                   (str/replace #"#extmap#" extmap))]
    {:status  200
     :headers {"content-type" "text/html"}
     :body    src}))


(defn router []
  (ring/router
    [["/static/*path" resource]
     ["/favicon.ico" (one-file "favicon.ico")]
     ["/robots.txt" (one-file "robots.txt")]
     ["/about/" about]
     ["/piu" piu-py]
     ["/piu.el" (one-file "piu.el")]
     ["/:id/" show]
     ["/:id/edit/" {:get  edit
                    :post create}]
     ["/:id/raw/" raw]
     ["/:id/render/" render]
     ["/" {:get  index
           :post create}]]
    {:conflicts (constantly nil)}))


(def app (fn [req]
           ((ring/ring-handler (router)
              (ring/routes
                (ring/redirect-trailing-slash-handler {:method :add})
                (ring/create-default-handler))
              {:middleware [cookies/wrap-cookies
                            params/parameters-middleware]})
            req)))
