(ns piu.app
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [ring.util.response :as response]
            [ring.middleware.params :as params]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.content-type :as ct]
            [mount.core :as mount]
            [charred.api :as j]

            [piu.config :as config]
            [piu.store :as store]
            [piu.store.fstore :as fstore]
            [piu.highlight :as hl]
            [piu.core.sign :as sign]
            [piu.core.route :as route]
            [piu.views.base :as base]
            [piu.views.index :as index]
            [piu.views.show :as show]
            [piu.views.markdown :as markdown]))


(set! *warn-on-reflection* true)


(mount/defstate db
  :start (fstore/create (config/DBPATH)))


(def SPAM-RE #"^comment\d+,")

(def LEXERS (delay (-> (sh/sh "sh" "-c" "./highlight.exe --langs")
                       :out
                       (j/read-json :key-fn keyword))))
(def LEXER-SET (delay (into #{} (map :lexer @LEXERS))))


(defn q? [req k]
  (or (= (:query-string req) k)
      (contains? (:query-params req) k)))


(defn pretty-json [s]
  (-> s
      j/read-json
      (j/write-json-str {:indent-str "  "})))


(defn show [req]
  (let [id     (-> req :path-params :id)
        data   (store/read db id)
        owner? (-> req :cookies (get id) :value sign/decrypt some?)
        lexer  (get (:query-params req) "as" (:lexer data))
        data   (cond-> data
                 (not= lexer (:lexer data))
                 (merge (hl/hl lexer (:raw data)))

                 (and (= lexer "json")
                      (q? req "pretty"))
                 (merge (hl/hl lexer (pretty-json (:raw data)))))
        data (cond
               (:lines data) data
               (:html data)  (assoc data :lines (hl/make-lines (:html data)))
               :else         nil)]
    (if data
      {:status  200
       :headers {"content-type" "text/html; charset=utf-8"}
       :body    (base/wrap
                  (show/t {:data   data
                           :owner? owner?
                           :lexer  lexer
                           :lexers @LEXERS}))}
      {:status  404
       :headers {"content-type" "text/plain"}
       :body    "Not Found"})))


(defn render [req]
  (let [id   (-> req :path-params :id)
        data (store/read db id)]
    (if data
      {:status  200
       :headers {"content-type" "text/html; charset=utf-8"}
       :body    (markdown/t (markdown/render (:raw data)))}
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
  (let [form  (:form-params req)
        id    (-> req :path-params :id)
        lexer (get form "lexer" "guess")
        lexer (cond
                (= lexer "guess")            lexer
                (contains? @LEXER-SET lexer) lexer
                :else                        "plaintext")]
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

      (not (and lexer raw))
      {:status 400
       :body "lexer and raw are required parameters"}

      :else
      (let [raw (get form "data")
            res (hl/hl lexer raw)
            id  (store/write db {:id    id
                                 :lexer (:lexer res)
                                 :raw   raw
                                 :html  (:html res)
                                 :lines (:lines res)})]
        {:status  303
         :cookies {id      {:value     (sign/encrypt id)
                            :path      "/"
                            :same-site :strict
                            :max-age   (* 3600 24 365)}
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
      {:status  200
       :headers {"content-type" "text/html; charset=utf-8"}
       :body    (base/wrap
                  (index/form {:lexers @LEXERS
                               :lexer  (:lexer data)
                               :raw    (:raw data)}))}
      {:status  404
       :headers {"content-type" "text/plain; charset=utf-8"}
       :body    "Not Found"})))


(defn about [req]
  {:status  200
   :headers {"content-type" "text/html; charset=utf-8"}
   :body    (base/wrap [:div.about (markdown/render (slurp (io/resource "about.md")))])})


(defmethod response/resource-data :resource
  [^java.net.URL url]
  (let [conn (.openConnection url)]
    {:content        (.getInputStream conn)
     :content-length (let [len (.getContentLength conn)] (when-not (pos? len) len))}))


(defn resource [req]
  (cond-> (response/resource-response (-> req :path-params :path) {:root "public"})
    (not (config/DEV)) (assoc-in [:headers "cache-control"] "max-age=31536000")))


(defn one-file [path]
  (fn [req]
    (resource (assoc req :path-params {:path path}))))


(defn piu-py [req]
  (let [lexers (j/write-json-str
                 (map :lexer @LEXERS))
        extmap (->> (for [item  @LEXERS
                          :when (not (#{"lasso" "arduino" "sml" "c-like"} (:lexer item)))
                          ext   (sort (:aliases item))]
                      [(str "*." (str/lower-case ext)) (:lexer item)])
                    (into (sorted-map))
                    j/write-json-str)
        script (-> (slurp (io/resource "piu.py.tpl"))
                   (str/replace #"#lexers#" lexers)
                   (str/replace #"#extmap#" extmap))]
    {:status  200
     :headers {"content-type" "text/plain"}
     :body    script}))


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
         :headers {"Location" (str (:uri req) "/")}}

        :else
        {:status 404
         :body   "Not Found"}))))
