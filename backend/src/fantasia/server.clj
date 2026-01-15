(ns fantasia.server
  (:gen-class)
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [org.httpkit.server :as http :refer [with-channel send! on-close on-receive]]
    [reitit.ring :as ring]
    [fantasia.sim.tick :as sim]))

(defn json-resp
  ([m] (json-resp 200 m))
  ([status m]
   {:status status
    :headers {"content-type" "application/json"
              "access-control-allow-origin" "*"
              "access-control-allow-headers" "content-type"
              "access-control-allow-methods" "GET,POST,OPTIONS"}
    :body (json/generate-string m)}))

(defn- keywordize-deep [x]
  (cond
    (map? x)
    (into {}
          (map (fn [[k v]]
                 [(cond
                    (keyword? k) k
                    (string? k) (keyword k)
                    :else k)
                  (keywordize-deep v)]))
          x)

    (vector? x) (mapv keywordize-deep x)
    (seq? x) (map keywordize-deep x)
    :else x))

(defn read-json-body [req]
  (try
    (when-let [b (:body req)]
      (let [s (slurp b)]
        (when-not (str/blank? s)
          (-> (json/parse-string s true)
              (keywordize-deep)))))
    (catch Exception _
      nil)))

(defonce *clients (atom #{}))
(defonce *runner (atom {:running? false :future nil :ms 250}))

(defn ws-send! [ch msg]
  (http/send! ch (json/generate-string msg)))

(defn broadcast! [msg]
  (doseq [ch @*clients]
    (ws-send! ch msg)))

(defn handle-ws [req]
  (http/with-channel req ch
    (swap! *clients conj ch)
    (ws-send! ch {:op "hello"
                  :state (select-keys (sim/get-state) [:tick :shrine :levers :size])})
    (http/on-close ch (fn [_] (swap! *clients disj ch)))
    (http/on-receive ch
      (fn [raw]
        (let [msg (try (-> (json/parse-string raw true) keywordize-deep)
                       (catch Exception _ nil))
              op (:op msg)]
          (case op
            "tick"
            (let [n (int (or (:n msg) 1))
                  outs (sim/tick! n)]
              (doseq [o outs]
                (broadcast! {:op "tick" :data (select-keys o [:tick :snapshot :attribution])})
                (when-let [ev (:event o)]
                  (broadcast! {:op "event" :data ev}))
                (doseq [tr (:traces o)]
                  (broadcast! {:op "trace" :data tr}))))

            "reset"
            (let [seed (:seed msg)
                  size (:size msg)]
              (sim/reset-world! {:seed seed :size size})
              (broadcast! {:op "reset" :state (sim/get-state)}))

            "set_levers"
            (do (sim/set-levers! (:levers msg))
                (broadcast! {:op "levers" :levers (:levers (sim/get-state))}))

            "place_shrine"
            (do (sim/place-shrine! (:pos msg))
                (broadcast! {:op "shrine" :shrine (:shrine (sim/get-state))}))

            "appoint_mouthpiece"
            (do (sim/appoint-mouthpiece! (:agent_id msg))
                (broadcast! {:op "mouthpiece"
                             :mouthpiece (get-in (sim/get-state) [:levers :mouthpiece-agent-id])}))

            (ws-send! ch {:op "error" :message "unknown op"})))))))

(defn start-runner! []
  (when-not (:running? @*runner)
    (let [fut (future
                (swap! *runner assoc :running? true)
                (try
                  (while (:running? @*runner)
                    (let [o (last (sim/tick! 1))]
                      (broadcast! {:op "tick" :data (select-keys o [:tick :snapshot :attribution])})
                      (when-let [ev (:event o)]
                        (broadcast! {:op "event" :data ev}))
                      (doseq [tr (:traces o)]
                        (broadcast! {:op "trace" :data tr})))
                    (Thread/sleep (long (:ms @*runner))))
                  (finally
                    (swap! *runner assoc :running? false :future nil))))]
      (swap! *runner assoc :future fut))))

(defn stop-runner! []
  (swap! *runner assoc :running? false)
  true)

(def app
  (ring/ring-handler
    (ring/router
      [["/healthz"
        {:get (fn [_] (json-resp {:ok true}))
         :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/ws" {:get handle-ws}]

       ["/sim/state"
        {:get (fn [_] (json-resp 200 (sim/get-state)))
         :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/sim/reset"
        {:post (fn [req]
                 (let [b (or (read-json-body req) {})
                       opts {:seed (:seed b)
                             :size (:size b)}]
                   (sim/reset-world! opts)
                   (let [state (sim/get-state)]
                     (json-resp 200 {:ok true
                                     :seed (:seed state)
                                     :size (:size state)}))))
         :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/sim/tick"
        {:post (fn [req]
                 (let [b (read-json-body req)
                       n (int (or (:n b) 1))
                       outs (sim/tick! n)]
                   (json-resp 200 {:ok true :last (last outs)})))
         :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/sim/run"
        {:post (fn [_] (start-runner!) (json-resp 200 {:ok true :running true}))
         :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/sim/pause"
        {:post (fn [_] (stop-runner!) (json-resp 200 {:ok true :running false}))
         :options (fn [_] (json-resp 200 {:ok true}))}]])))

(defn -main [& _]
  (let [port 3000]
    (println (str "Fantasia backend listening on http://localhost:" port))
    (http/run-server app {:port port})
    @(promise)))
