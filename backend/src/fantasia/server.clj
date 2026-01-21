(ns fantasia.server
  (:gen-class)
  (:require
    [cheshire.core :as json]
    [clojure.string :as str]
    [org.httpkit.server :as http]
    [reitit.ring :as ring]
    [fantasia.sim.tick :as sim]
    [fantasia.sim.jobs :as jobs]))

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
                          o (last (sim/tick! 1))
                          end-time (System/currentTimeMillis)
                          tick-ms (- end-time start-time)
                          target-ms (:ms @*runner)
                          health (compute-health-status tick-ms target-ms)]
                      (swap! *runner assoc :tick-ms tick-ms)
                      (broadcast! {:op "tick" :data (select-keys o [:tick :snapshot :attribution])})
                      (broadcast! {:op "tick_health" :data {:target-ms target-ms :tick-ms tick-ms :health health}})
                      (when-let [ev (:event o)]
                        (broadcast! {:op "event" :data ev}))
                       (doseq [tr (:traces o)]
                          (broadcast! {:op "trace" :data tr}))
                       (when-let [bs (:books o)]
                          (broadcast! {:op "books" :data {:books bs}}))
                      (doseq [si (:social-interactions o)]
                         (broadcast! {:op "social_interaction" :data si}))
                      (Thread/sleep (long (:ms @*runner)))))
                  (finally
                    (swap! *runner assoc :running? false :future nil))))]
      (swap! *runner assoc :future fut))))

(defn stop-runner! []
  (swap! *runner assoc :running? false)
  true)

(defn handle-ws [req]
  (http/with-channel req ch
    (swap! *clients conj ch)
    (ws-send! ch {:op "hello"
                  :state (select-keys (sim/get-state) [:tick :shrine :levers :map :tiles])})
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
                   (broadcast! {:op "trace" :data tr}))
                 (doseq [si (:social-interactions o)]
                   (broadcast! {:op "social_interaction" :data si}))))

            "reset"
            (let [opts {:seed (long (or (:seed msg) 1))
                        :tree-density (or (:tree_density msg) 0.08)}
                  opts (if (:bounds msg)
                         (assoc opts :bounds (:bounds msg))
                         opts)]
              (sim/reset-world! opts)
              (broadcast! {:op "reset" :state (sim/get-state)}))

            "set_levers"
            (do (sim/set-levers! (:levers msg))
                (broadcast! {:op "levers" :levers (:levers (sim/get-state))}))

            "place_shrine"
            (do (sim/place-shrine! (:pos msg))
                (broadcast! {:op "shrine" :shrine (:shrine (sim/get-state))}))

            "place_wall_ghost"
            (do (sim/place-wall-ghost! (:pos msg))
                (broadcast! {:op "tiles" :tiles (:tiles (sim/get-state))}))

            "appoint_mouthpiece"
            (do (sim/appoint-mouthpiece! (:agent_id msg))
                (broadcast! {:op "mouthpiece"
                             :mouthpiece (get-in (sim/get-state) [:levers :mouthpiece-agent-id])}))

            "get_agent_path"
            (if-let [path (sim/get-agent-path! (:agent_id msg))]
              (broadcast! {:op "agent_path" :agent-id (:agent_id msg) :path path})
              (broadcast! {:op "error" :message "Agent not found or has no path"}))
            
            "place_stockpile"
            (do (sim/place-stockpile! (:pos msg) (:resource msg) (:max_qty msg))
                (broadcast! {:op "stockpiles" :stockpiles (:stockpiles (sim/get-state))}))
            
            "place_warehouse"
            (do (sim/place-warehouse! (:pos msg) (:resource msg) (:max_qty msg))
                (broadcast! {:op "stockpiles" :stockpiles (:stockpiles (sim/get-state))}))

            "place_campfire"
            (do (sim/place-campfire! (:pos msg))
                (broadcast! {:op "tiles" :tiles (:tiles (sim/get-state))}))

            "place_statue_dog"
            (do (sim/place-statue-dog! (:pos msg))
                (broadcast! {:op "tiles" :tiles (:tiles (sim/get-state))}))

            "place_tree"
            (do (sim/place-tree! (:pos msg))
                (broadcast! {:op "tiles" :tiles (:tiles (sim/get-state))}))

            "place_deer"
            (do (sim/place-deer! (:pos msg))
                (broadcast! {:op "agents" :agents (:agents (sim/get-state))}))

            "place_wolf"
            (do (sim/place-wolf! (:pos msg))
                (broadcast! {:op "agents" :agents (:agents (sim/get-state))}))

            "place_bear"
            (do (sim/place-bear! (:pos msg))
                (broadcast! {:op "agents" :agents (:agents (sim/get-state))}))

            "queue_build"
            (let [structure (normalize-structure (:structure msg))
                  stockpile (normalize-stockpile (:stockpile msg))]
              (when (and structure (:pos msg))
                (sim/queue-build-job! structure (:pos msg) stockpile)
                (broadcast! {:op "jobs" :jobs (:jobs (sim/get-state))})
                (when (= structure :wall)
                  (broadcast! {:op "tiles" :tiles (:tiles (sim/get-state))}))
                (when (= structure :shrine)
                  (broadcast! {:op "shrine" :shrine (:shrine (sim/get-state))}))))

             "assign_job"
             (let [agent-id (:agent_id msg)
                   job-type (:job_type msg)
                   target-pos (:target_pos msg)]
               (when (and (get-in (sim/get-state) [:agents agent-id])
                          target-pos)
                 (when-let [job (jobs/create-job job-type target-pos)]
                   (swap! sim/*state jobs/assign-job! job agent-id)
                   (broadcast! {:op "jobs" :jobs (:jobs (sim/get-state))}))))

            "start_run"
            (do
              (start-runner!)
              (broadcast! {:op "runner_state" :running true :fps (int (/ 1000 (:ms @*runner)))}))

            "stop_run"
            (do
              (stop-runner!)
              (broadcast! {:op "runner_state" :running false :fps (int (/ 1000 (:ms @*runner)))}))

            "set_fps"
            (let [fps (int (or (:fps msg) 15))
                  ms (if (pos? fps) (/ 1000.0 fps) 66)]
              (swap! *runner assoc :ms ms)
              (broadcast! {:op "runner_state" :running (:running? @*runner) :fps fps}))

             (ws-send! ch {:op "error" :message "unknown op"})))))))

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
                   (let [b (read-json-body req)
                         seed (long (or (:seed b) 1))
                         tree-density (or (:tree_density b) 0.08)
                         opts {:seed seed :tree-density tree-density}
                         opts (if (:bounds b)
                                (assoc opts :bounds (:bounds b))
                                opts)]
                     (sim/reset-world! opts)
                     (json-resp 200 {:ok true :seed seed :tree_density tree-density})))
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
