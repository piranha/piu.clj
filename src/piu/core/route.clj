(ns piu.core.route)


(def groups-re #"\(\?<([a-zA-Z][a-zA-Z0-9]*)>")


(defn make-handler [re handler]
  (let [names (->> (re-seq groups-re (.pattern re))
                   (mapv (comp keyword second)))]
    (-> (cond
          (fn? handler)  {:get  handler
                          :post handler
                          :put  handler}
          (map? handler) handler
          :else          (throw (ex-info "Unknown handler type" {:handler handler})))
        (assoc :names names))))


(defn check-match [[re handler] req]
  (when-let [m (re-matches re (:uri req))]
    (let [func   (get handler (:request-method req))
          params (zipmap
                   (:names handler)
                   (rest m))]
      [(assoc req :path-params params)
       func])))


;;; API

(defn router [routes]
  (mapv (fn [[re handler]] [re (make-handler re handler)]) routes))


(defn match [router req]
  (or (reduce
        (fn [_ entry]
          (some-> (check-match entry req)
            reduced))
        nil
        router)
      [req nil]))


(comment
  (make-handler #"/(?<id>[a-z0-9])/" identity))
