(ns fantasia.server
   (:gen-class)
   (:require
     [cheshire.core :as json]
     [clojure.string :as str]
     [org.httpkit.server :as http]
     [reitit.ring :as ring]
      [fantasia.sim.ecs.tick :as sim]
      [fantasia.sim.ecs.adapter :as adapter]
      [fantasia.sim.scribes :as scribes]
     [nrepl.server :as nrepl]
     [fantasia.dev.logging :as log]))

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

(defn- normalize-structure [value]
  (cond
    (keyword? value) value
    (string? value) (case value
                      "statue_dog" :statue/dog
                      "statue/dog" :statue/dog
                      "improvement_hall" :improvement-hall
                      "improvement-hall" :improvement-hall
                      (keyword value))
    :else nil))

(defn- normalize-stockpile [stockpile]
  (when (map? stockpile)
    (let [resource (:resource stockpile)
          resource (cond
                     (keyword? resource) resource
                     (string? resource) (keyword resource)
                     :else nil)
          max-qty (or (:max-qty stockpile)
                      (:max_qty stockpile)
                      (:maxQty stockpile))]
      (when resource
        {:resource resource :max-qty max-qty}))))

(defonce *clients (atom #{}))
(defonce *runner (atom {:running? false :future nil :ms 66 :tick-ms 0}))

(require '[fantasia.sim.ecs.tick :as sim-tick])
(defonce *nrepl-server (atom nil))

(defn- start-nrepl!
  []
  (when-not @*nrepl-server
    (let [port (or (some-> (System/getenv "NREPL_PORT") Integer/parseInt) 7888)
          server (nrepl/start-server :port port)]
      (reset! *nrepl-server server)
      (log/log-info "nREPL server started on port" port)
      server)))

(defn- stop-nrepl!
  []
  (when-let [server @*nrepl-server]
    (nrepl/stop-server server)
    (reset! *nrepl-server nil)
    (log/log-info "nREPL server stopped")))

(defn ws-send! [ch msg]
  (http/send! ch (json/generate-string msg)))

(defn broadcast! [msg]
  (doseq [ch @*clients]
    (ws-send! ch msg)))

(defn compute-health-status [tick-ms target-ms]
  (cond
    (zero? target-ms) "unknown"
    (< tick-ms (* target-ms 0.7)) "healthy"
    (< tick-ms (* target-ms 0.9)) "degraded"
    :else "unhealthy"))

(defn start-runner! []
  (when-not (:running? @*runner)
    (let [fut (future
                 (swap! *runner assoc :running? true)
                 (try
                    (while (:running? @*runner)
                      (let [start-time (System/currentTimeMillis)
                             o (last (sim-tick/tick-ecs! 1))
                            end-time (System/currentTimeMillis)
                            tick-ms (- end-time start-time)
                            target-ms (:ms @*runner)
                            health (compute-health-status tick-ms target-ms)]
                         (swap! *runner assoc :tick-ms tick-ms)
                           (let [result (sim-tick/tick-ecs! 1)] 
                              (when result 
                                (broadcast! {:op "tick" :data (select-keys result [:tick :snapshot :attribution])})
                                (when-let [ds (:delta-snapshot result)] 
                                  (broadcast! {:op "tick_delta" :data ds}))
                                (broadcast! {:op "tick_health" :data {:target-ms target-ms :tick-ms tick-ms :health health}})
                                (when-let [ev (:event result)] 
                                  (broadcast! {:op "event" :data ev}))
                                (doseq [tr (:traces result)]
                                   (broadcast! {:op "trace" :data tr}))
                                (when-let [bs (:books result)] 
                                  (broadcast! {:op "books" :data {:books bs}}))
                                (doseq [si (:social-interactions result)]
                                   (broadcast! {:op "social_interaction" :data si}))
                                (doseq [ce (:combat-events result)]
                                   (broadcast! {:op "combat_event" :data ce}))))))
                    (finally                      (swap! *runner assoc :running? false :future nil)))]                   (while (:running? @*runner)
