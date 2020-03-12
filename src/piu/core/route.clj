(ns piu.core.route)


(def groups-re #"\(\?<([a-zA-Z][a-zA-Z0-9]*)>")


(defn make-handler [re handler]
  (-> (cond
        (fn? handler)  {:get  handler
                        :post handler
                        :put  handler}
        (map? handler) handler
        :else          (throw (ex-info "Unknown handler type" {:handler handler})))
      (assoc :names (->> (re-seq groups-re (.pattern re))
                         (mapv (comp keyword second))))))


(defn router [routes]
  (mapv (fn [[re handler]] [re (make-handler re handler)]) routes))


(defn check-match [[re handler] req]
  (let [m      (re-matches re (:uri req))
        func   (when m (get handler (:request-method req)))
        params (when m (into {}
                         (zipmap (:names handler)
                           (rest m))))]
    (when m
      [(assoc req :path-params params)
       func])))


(defn match [router req]
  (reduce
    (fn [_ entry]
      (some-> (check-match entry req)
        reduced))
    nil
    router))


(comment
  (make-handler #"/(?<id>[a-z0-9])/" identity))
