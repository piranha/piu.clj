(ns piu.app
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ring.util.response :as response]
            [ring.middleware.params :as params]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.content-type :as ct]
            [clojure.data.json :as j]
            [mount.core :as mount]

            [piu.store :as store]
            [piu.store.fstore :as fstore]
            [piu.highlight :as hl]
            [piu.core.sign :as sign]
            [piu.core.route :as route]
            [piu.views.base :as base]
            [piu.views.index :as index]
            [piu.views.show :as show]
            [piu.views.render :as render]))


(set! *warn-on-reflection* true)


(defn dbpath []
  (or (System/getenv "DBPATH")
      "piu.sqlite"))


(mount/defstate db
  :start (fstore/create (dbpath)))


(def SPAM-RE #"^comment\d+,")
(def LEXERS (delay (let [lang-data (hl/js "l => {
                                             var x = hljs.getLanguage(l);
                                             return {name: x.name, aliases: x.aliases};
                                           }")]
                     (->> (hl/js "hljs.listLanguages()")
                          (map #(assoc (lang-data %) :lexer %))
                          (sort-by (comp str/lower-case :name))))))

(defn q? [req k]
  (or (= (:query-string req) k)
      (contains? (:query-params req) k)))


(defn pretty-json [s]
  (with-out-str (-> s j/read-str j/pprint)))


(defn show [req]
  (let [id     (-> req :path-params :id)
        data   (store/read db id)
        owner? (-> req :cookies (get id) :value sign/decrypt some?)
        lexer  (get (:query-params req) "as" (:lexer data))
        data   (cond-> data
                 (not= lexer (:lexer data))
                 (assoc :html (:html (hl/hl lexer (:raw data))))

                 (and (= lexer "json")
                      (q? req "pretty"))
                 (assoc :html (:html (hl/hl lexer (pretty-json (:raw data))))))]
    (if data
      {:status  200
       :headers {"content-type" "text/html; charset=utf-8"}
       :body    (base/wrap
                  (show/t {:data   data
                           :owner? owner?
                           :lexer  lexer
                           :lexers @LEXERS}))}
      {:status 404
       :body   "Not Found"})))


(defn render [req]
  (let [id   (-> req :path-params :id)
        data (store/read db id)]
    (if data
      {:status  200
       :headers {"content-type" "text/html; charset=utf-8"}
       :body    (render/t (render/render (:raw data)))}
      {:status 404
       :body   "Not Found"})))


(defn raw [req]
  (let [id   (-> req :path-params :id)
        data (store/read db id)]
    (if data
      {:status  200
       :headers {"content-type" "text/plain; charset=utf-8"}
       :body    (if (q? req "more")
                  (pr-str data)
                  (:raw data))}
      {:status  404
       :headers {"content-type" "text/plain; charset=utf-8"}
       :body "Not Found"})))


(defn index [req]
  {:status  200
   :headers {"content-type" "text/html; charset=utf-8"}
   :body    (base/wrap
              (index/form {:lexer (or (get (:query-params req) "lexer")
                                      (:value (get (:cookies req) "lexer"))
                                      "guess")
                           :lexers @LEXERS}))})


(defn create [req]
  (let [form (:form-params req)
        id   (-> req :path-params :id)]
    (cond
      (and id
           (nil? (-> req :cookies (get id) :value sign/decrypt)))
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
            raw   (get form "data")
            res   (hl/hl lexer raw)
            id    (store/write db {:id    id
                                   :lexer (:lexer res)
                                   :raw   raw
                                   :html  (:html res)})]
        {:status  303
         :cookies {id      {:value     (sign/encrypt id)
                            :path      "/"
                            :same-site :strict
                            :max-age   (* 3600 24 7)}
                   "lexer" {:value     lexer
                            :path      "/"
                            :same-site :strict
                            :max-age   (* 3600 24 365)}}
         :headers {"location" (format "/%s/" id)}}))))


(defn edit [req]
  (let [id   (-> req :path-params :id)
        data (store/read db id)]
    (if (and data
             (-> req :cookies (get id) :value sign/decrypt some?))
      {:status 200
       :body   (base/wrap
                 (index/form {:lexers @LEXERS
                              :lexer  (:lexer data)
                              :raw    (:raw data)}))}
      {:status 404
       :headers {"content-type" "text/plain; charset=utf-8"}
       :body "Not Found"})))


(defn about [req]
  {:status  200
   :headers {"content-type" "text/html; charset=utf-8"}
   :body    (base/wrap (render/render (slurp (io/resource "about.md"))))})


(defn resource [req]
  (response/resource-response (-> req :path-params :path) {:root "public"}))


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


(def router
  (route/router
    [[#"/static/(?<path>.+)" resource]
     [#"/favicon.ico" (one-file "favicon.ico")]
     [#"/robots.txt" (one-file "robots.txt")]
     [#"/about/" about]
     [#"/piu" piu-py]
     [#"/piu.el" (one-file "piu.el")]
     [#"/(?<id>[^/]+)/" show]
     [#"/(?<id>[^/]+)/edit/" {:get edit
                              :post create}]
     [#"/(?<id>[^/]+)/raw/" raw]
     [#"/(?<id>[^/]+)/render/" render]
     [#"/" {:get index
            :post create}]]))


(def middleware [cookies/wrap-cookies
                 params/wrap-params
                 ct/wrap-content-type])


(let [mw (apply comp middleware)]
  (defn app [req]
    (let [[req func] (route/match router req)
          res        (when func ((mw func) req))]

      (cond
        res res

        (some? (route/match router (update req :uri str "/")))
        {:status  301
         :headers {"location" (str (:uri req) "/")}}

        :else
        {:status 404
         :body   "Not Found"}))))
