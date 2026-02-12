(ns fantasia.server
   (:gen-class)
   (:require
      [cheshire.core :as json]
      [fantasia.config :as config]
      [fantasia.dev.logging :as log]
      [fantasia.sim.ecs.adapter :as adapter]
      [fantasia.sim.ecs.tick :as sim]
      [fantasia.sim.embeddings :as embeddings]
      [fantasia.sim.gates.runtime :as gates-runtime]
      [fantasia.sim.scribes :as scribes]
      [nrepl.server :as nrepl]
      [org.httpkit.server :as http]
      [reitit.ring :as ring]))

(defn json-resp
  "Create a JSON HTTP response."
  ([body]
   {:status 200
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string body)})
  ([status body]
   {:status status
    :headers {"Content-Type" "application/json"}
    :body (json/generate-string body)}))

(defn read-json-body
  "Read and parse JSON from request body."
  [req]
  (when-let [body (:body req)]
    (let [body-str (if (string? body) 
                     body 
                     (slurp body))]
      (json/parse-string body-str true))))

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

(defn- levers->gate-runtime-mode
  [levers]
  (or (:gate-runtime-mode levers)
      (:gate_runtime_mode levers)
      (:gate-mode levers)
      (:gate_mode levers)))

(defonce *clients (atom #{}))
(defonce *runner (atom {:running? false :future nil :ms 66 :tick-ms 0}))
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
                             o (last (sim/tick-ecs! 1))
                            end-time (System/currentTimeMillis)
                            tick-ms (- end-time start-time)
                            target-ms (:ms @*runner)
                            health (compute-health-status tick-ms target-ms)]
                        (gates-runtime/evaluate! {:boundary :runtime
                                                  :op :tick
                                                  :tick (:tick o)})
                         (swap! *runner assoc :tick-ms tick-ms)
                          (let [tick-data (select-keys o [:tick :snapshot :attribution])]
                            (println "[Server] Broadcasting tick with keys:" (keys tick-data))
                            (println "[Server] Snapshot agents count:" (get-in tick-data [:snapshot :agents] "NO SNAPSHOT"))
                            (broadcast! {:op "tick" :data tick-data}))
                         (when-let [ds (:delta-snapshot o)]
                           (broadcast! {:op "tick_delta" :data ds}))
                       (broadcast! {:op "tick_health" :data {:target-ms target-ms :tick-ms tick-ms :health health}})
                       (when-let [ev (:event o)]
                         (broadcast! {:op "event" :data ev}))
                        (doseq [tr (:traces o)]
                           (broadcast! {:op "trace" :data tr}))
                        (when-let [bs (:books o)]
                           (broadcast! {:op "books" :data {:books bs}}))
                       (doseq [si (:social-interactions o)]
                          (broadcast! {:op "social_interaction" :data si}))
                       (doseq [ce (:combat-events o)]
                          (broadcast! {:op "combat_event" :data ce}))
                       (Thread/sleep (long (:ms @*runner)))))
                  (finally
                    (swap! *runner assoc :running? false :future nil))))]
      (swap! *runner assoc :future fut))))

(defn stop-runner! []
  (swap! *runner assoc :running? false)
  true)

(defn handle-ollama-test []
  (let [start-time (System/currentTimeMillis)
        test-prompt "test"
         ollama-config (config/load-ollama-config!)
         ollama-model (config/get-ollama-primary-model ollama-config)
        result (scribes/call-ollama-sync! test-prompt ollama-model)
        end-time (System/currentTimeMillis)
        latency (- end-time start-time)]
    (if (:success result)
      (json-resp 200 {:connected true :latency_ms latency :model ollama-model})
      (json-resp 200 {:connected false :latency_ms latency :model ollama-model :error (:error result)}))))

(defn get-visible-tiles
   "Return only visible or revealed tiles from state.
   Tiles without a visibility marker are treated as visible."
   [state]
   (let [tile-visibility (:tile-visibility state {})]
     (if (empty? tile-visibility)
       (:tiles state)
       (into {}
             (filter (fn [[tile-key]]
                       (let [vis (get tile-visibility tile-key)]
                         (or (nil? vis) (= vis :visible) (= vis :revealed))))
                   (:tiles state))))))

(defn handle-ws [req]
    (try
      (http/with-channel req ch
        (swap! *clients conj ch)
        ;; Auto-create world if state is empty (e.g., on server startup)
        (when (empty? (:tiles (sim/get-state)))
          (println "[WS] State is empty, creating initial world")
          (sim/reset-world! {:seed 1 :tree_density 0.05}))
        (let [initial-state (sim/get-state)
               ecs-world (sim/get-ecs-world)
               snapshot (adapter/ecs->snapshot ecs-world initial-state)
               visible-tiles (:tiles snapshot)]
          (println "[WS] New client connected, sending hello with" (count visible-tiles) "tiles")
          (ws-send! ch {:op "hello"
                        :state (merge (select-keys snapshot [:tick :shrine :levers :map :agents :calendar :temperature :daylight :cold-snap :tile-visibility :revealed-tiles-snapshot])
                                              {:tiles visible-tiles})}))
       (http/on-close ch (fn [_] (swap! *clients disj ch)))
      (http/on-receive ch
        (fn [raw]
(let [msg (try (json/parse-string raw true)
                          (catch Exception _ nil))
                op (:op msg)]
            (gates-runtime/evaluate! {:boundary :ws
                                      :op op})
            (case op
               "tick"
               (let [n (int (or (:n msg) 1))
                        outs (sim/tick-ecs! n)]
                  (doseq [o outs]
                     (gates-runtime/evaluate! {:boundary :runtime
                                               :op :tick
                                               :tick (:tick o)})
                      (broadcast! {:op "tick" :data (select-keys o [:tick :snapshot :attribution])})
                      (when-let [ds (:delta-snapshot o)]
                        (broadcast! {:op "tick_delta" :data ds}))
                     (when-let [ev (:event o)]
                       (broadcast! {:op "event" :data ev}))
                     (doseq [tr (:traces o)]
                       (broadcast! {:op "trace" :data tr}))
                      (doseq [si (:social-interactions o)]
                        (broadcast! {:op "social_interaction" :data si}))
                      (doseq [ce (:combat-events o)]
                        (broadcast! {:op "combat_event" :data ce}))))

"reset"
  (let [seed (long (or (:seed msg) 1))
        tree-density (or (:tree_density msg) 0.05)
        bounds (when (:bounds msg)
                 (:bounds msg))]
    (sim/reset-world! {:seed seed :tree_density tree-density :bounds bounds})
    (let [state (sim/get-state)
          ecs-world (sim/get-ecs-world)
          snapshot (adapter/ecs->snapshot ecs-world state)]
      (broadcast! {:op "reset" :state snapshot})))

             "set_levers"
              (do
                (when-let [mode (levers->gate-runtime-mode (:levers msg))]
                  (gates-runtime/set-mode! mode))
                (sim/set-levers! (:levers msg))
                (broadcast! {:op "levers" :levers (:levers (sim/get-state))}))

"place_shrine"
               (do
                 (let [[q r] (:pos msg)]
                   (sim/place-shrine! q r))
                (broadcast! {:op "shrine" :shrine (:shrine (sim/get-state))})
                (broadcast! {:op "tiles" :tiles (get-visible-tiles (sim/get-state))}))

"place_wall_ghost"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-wall-ghost! q r))
                (broadcast! {:op "tiles" :tiles (get-visible-tiles (sim/get-state))}))

              "appoint_mouthpiece"
              (do
                (sim/appoint-mouthpiece! (:agent_id msg))
                (broadcast! {:op "mouthpiece"
                               :mouthpiece (get-in (sim/get-state) [:levers :mouthpiece-agent-id])}))

             "get_agent_path"
             (if-let [path (sim/get-agent-path! (:agent_id msg))]
               (broadcast! {:op "agent_path" :agent-id (:agent_id msg) :path path})
               (broadcast! {:op "error" :message "Agent not found or has no path"}))

              "place_stockpile"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-stockpile! q r))
                (broadcast! {:op "stockpiles" :stockpiles (:stockpiles (sim/get-state))}))

              "place_warehouse"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-warehouse! q r))
                (broadcast! {:op "stockpiles" :stockpiles (:stockpiles (sim/get-state))}))

"place_campfire"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-campfire! q r))
                (broadcast! {:op "tiles" :tiles (get-visible-tiles (sim/get-state))}))

              "place_statue_dog"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-statue-dog! q r))
                (broadcast! {:op "tiles" :tiles (get-visible-tiles (sim/get-state))}))

              "place_tree"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-tree! q r))
                (broadcast! {:op "tiles" :tiles (get-visible-tiles (sim/get-state))}))

              "place_deer"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-deer! q r))
                (broadcast! {:op "agents" :agents (:agents (sim/get-state))}))

              "place_wolf"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-wolf! q r))
                (broadcast! {:op "agents" :agents (:agents (sim/get-state))}))

              "place_bear"
              (do
                (let [[q r] (:pos msg)]
                  (sim/place-bear! q r))
                (broadcast! {:op "agents" :agents (:agents (sim/get-state))}))

"queue_build"
              (let [structure (normalize-structure (:structure msg))]
                 (when (and structure (:pos msg))
                  (sim/queue-build-job! {:structure structure
                                         :pos (:pos msg)
                                         :stockpile (:stockpile msg)})
                   (broadcast! {:op "jobs" :jobs (:jobs (sim/get-state))})
                (when (= structure :wall)
                     (broadcast! {:op "tiles" :tiles (get-visible-tiles (sim/get-state))}))
                 (when (= structure :shrine)
                   (broadcast! {:op "shrine" :shrine (:shrine (sim/get-state))}))))

             "assign_job"
              (let [agent-id (:agent_id msg)
                    job-type (:job_type msg)
                    target-pos (:target_pos msg)
                    state (sim/get-state)
                    agent-exists? (contains? (:agents state) agent-id)]
                (when (and agent-exists? job-type target-pos)
                  (sim/queue-build-job! {:agent-id agent-id
                                         :job-type job-type
                                         :target-pos target-pos
                                         :priority 80})
                  (broadcast! {:op "jobs" :jobs (:jobs (sim/get-state))})
                  (broadcast! {:op "agents" :agents (:agents (sim/get-state))})))

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

             "set_facet_limit"
             (do
               (sim/set-facet-limit! (:limit msg))
               (broadcast! {:op "facet_limit" :limit (:limit msg)}))

             "set_vision_radius"
             (do
               (sim/set-vision-radius! (:radius msg))
               (broadcast! {:op "vision_radius" :radius (:radius msg)}))

             "config_facets"
             (let [facet-limit (or (:facet_limit msg) (:facet-limit msg))
                   vision-radius (or (:vision_radius msg) (:vision-radius msg))]
               (when (number? facet-limit)
                 (sim/set-facet-limit! facet-limit)
                 (broadcast! {:op "facet_limit" :limit facet-limit}))
               (when (number? vision-radius)
                 (sim/set-vision-radius! vision-radius)
                 (broadcast! {:op "vision_radius" :radius vision-radius})))

             (ws-send! ch {:op "error" :message "unknown op"}))))))
      (catch Exception e
        (log/log-error "[WS:HANDLE-FAILED]" {:error (.getMessage e)})))

)

(def app
  (ring/ring-handler
    (ring/router
      [["/healthz"
        {:get (fn [_] (json-resp {:ok true}))
         :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/ws" {:get handle-ws}]

          ["/sim/state"
           {:get (fn [_] (let [state (sim/get-state)
                                 ecs-world (sim/get-ecs-world)
                                 snapshot (adapter/ecs->snapshot ecs-world state)]
                             (gates-runtime/evaluate! {:boundary :http
                                                       :op :sim/state})
                             (json-resp 200 snapshot)))
            :options (fn [_] (json-resp 200 {:ok true}))}]

        ["/sim/reset"
           {:post (fn [req]
(let [b (read-json-body req)
                           seed (long (or (:seed b) 1))
                           tree-density (or (:tree_density b) (:tree-density b) 0.08)
                           bounds (:bounds b)]
                        (gates-runtime/evaluate! {:boundary :http
                                                  :op :sim/reset})
                        (sim/reset-world! {:seed seed
                                           :tree_density tree-density
                                           :bounds bounds})
                        (json-resp 200 {:ok true :seed seed :tree_density tree-density})))
            :options (fn [_] (json-resp 200 {:ok true}))}]

       ["/sim/tick"
         {:post (fn [req]
                  (let [b (read-json-body req)
                        n (int (or (:n b) 1))
                        outs (sim/tick-ecs! n)]
                    (doseq [o outs]
                      (gates-runtime/evaluate! {:boundary :runtime
                                                :op :tick
                                                :tick (:tick o)}))
                    (gates-runtime/evaluate! {:boundary :http
                                              :op :sim/tick
                                              :n n})
                    (json-resp 200 {:ok true :last (last outs)})))
          :options (fn [_] (json-resp 200 {:ok true}))}]

        ["/sim/run"
         {:post (fn [_] (start-runner!) (json-resp 200 {:ok true :running true}))
          :options (fn [_] (json-resp 200 {:ok true}))}]

        ["/sim/pause"
         {:post (fn [_] (stop-runner!) (json-resp 200 {:ok true :running false}))
          :options (fn [_] (json-resp 200 {:ok true}))}]

        ["/api/ollama/test"
         {:get (fn [_] (handle-ollama-test))
          :post (fn [_] (handle-ollama-test))
          :options (fn [_] (json-resp 200 {:ok true}))}]])))

(defn -main [& _]
  (let [port 3000]
    (println (str "Fantasia backend listening on http://localhost:" port))
    (start-nrepl!)
    (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop-nrepl!))
    (embeddings/init-embeddings!) ; Initialize embedding system (this will load config)
    (sim/create-ecs-initial-world {}) ; Initialize ECS world on startup
    (scribes/start-ollama-keep-alive!) ; This will load Ollama config on first call
    (http/run-server app {:port port})
    @(promise)))
