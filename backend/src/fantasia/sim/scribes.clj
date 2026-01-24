(ns fantasia.sim.scribes
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [fantasia.dev.logging :as log]
            [clj-http.client :as http]
            [clojure.java.io :as io]))

(def ollama-url "http://localhost:11434/api/generate")
(def ollama-model "qwen3:4b")
(def ^:private myths-file-path "myths.jsonl")
(def ^:private max-myths-for-prompt 5)

(defonce ^:private *ollama-keep-alive (atom {:future nil :running? false}))

(defn- get-ollama-config
  "Get Ollama configuration from world levers with sensible defaults."
  [state]
  {:timeout-ms (get-in state [:levers :ollama-timeout-ms] 60000)
   :retries (get-in state [:levers :ollama-retries] 1)
   :retry-delay-ms (get-in state [:levers :ollama-retry-delay-ms] 2000)
   :keep-alive-enabled (get-in state [:levers :ollama-keep-alive-enabled] true)
   :keep-alive-interval-ms (get-in state [:levers :ollama-keep-alive-interval-ms] 300000)
   :url (get-in state [:levers :ollama-url] ollama-url)
   :model (get-in state [:levers :ollama-model] ollama-model)})

(defn- load-myths!
  "Load all myths from the persistent myths file."
  []
  (when (.exists (io/file myths-file-path))
    (try
      (with-open [rdr (io/reader myths-file-path)]
        (->> (line-seq rdr)
             (filter (complement str/blank?))
             (map #(json/parse-string % true))))
      (catch Exception e
        (log/log-warn "[MYTHS:LOAD-FAILED]" {:error (.getMessage e)})
        []))))

(defn- save-myth!
  "Append a new myth to the persistent myths file."
  [myth]
  (try
    (io/make-parents myths-file-path)
    (spit myths-file-path (str (json/generate-string myth) "\n") :append true)
    (log/log-info "[MYTHS:SAVED]" {:title (:title myth) :themes (:facets myth)})
    (catch Exception e
      (log/log-error "[MYTHS:SAVE-FAILED]" {:error (.getMessage e)}))))

(defn- get-random-myths
  "Get N random myths from loaded myths for Ollama context."
  [n]
  (let [myths (load-myths!)]
    (if (empty? myths)
      []
      (let [rng (java.util.Random. (System/currentTimeMillis))
            count-myths (count myths)
            indices (distinct (repeatedly (min n count-myths) #(.nextInt rng count-myths)))]
        (mapv #(nth myths %) indices)))))

(defn call-ollama!
  "Make an async call to the Ollama API."
  [prompt model]
  (future
    (try
      (let [response (http/post ollama-url
                                {:content-type :json
                                 :body (json/generate-string {:model model
                                                                :prompt prompt
                                                                :stream false})
                                 :throw-exceptions false
                                 :socket-timeout 10000
                                 :connection-timeout 5000})]
        (if (= (:status response) 200)
          (let [body (json/parse-string (:body response) true)
                text (get-in body [:response] "")]
            (log/log-info "[OLLAMA:SUCCESS]" {:model model :length (count text)})
            text)
          (do
            (log/log-warn "[OLLAMA:FAILED]" {:status (:status response)})
            nil)))
      (catch Exception e
        (log/log-error "[OLLAMA:ERROR]" {:error (.getMessage e)})
         nil))))

(defn- call-ollama-raw!
  "Make a single raw HTTP call to Ollama API with given options."
  [prompt model timeout-ms opts]
  (try
    (let [response (http/post ollama-url
                              {:content-type :json
                               :body (json/generate-string (merge {:model model
                                                                     :prompt prompt
                                                                     :stream false}
                                                                    opts))
                               :throw-exceptions false
                               :socket-timeout timeout-ms
                               :connection-timeout 5000})]
      (if (= (:status response) 200)
        (let [body (json/parse-string (:body response) true)
              text (or (get-in body [:response] "")
                       (get-in body [:thinking] "")
                       "")]
          (log/log-info "[OLLAMA:SUCCESS]" {:model model :length (count text)})
          {:success true :text text})
        {:success false :error (str "HTTP " (:status response))}))
    (catch Exception e
      (log/log-error "[OLLAMA:ERROR]" {:error (.getMessage e)})
      {:success false :error (.getMessage e)})))

(defn- call-ollama-with-retry!
  "Make a call to Ollama API with retry logic for cold starts."
  [prompt model config]
  (let [{:keys [timeout-ms retries retry-delay-ms]} config]
    (letfn [(retry-call [attempt]
              (let [result (call-ollama-raw! prompt model timeout-ms {:options {:num_predict 50 :temperature 0.7}})]
                (if (:success result)
                  result
                  (if (< attempt retries)
                    (do
                      (log/log-warn "[OLLAMA:RETRY]" {:attempt (inc attempt) :max retries :error (:error result)})
                      (Thread/sleep retry-delay-ms)
                      (retry-call (inc attempt)))
                    result))))]
      (retry-call 0))))

(defn call-ollama!
  "Make an async call to the Ollama API with retry logic."
  [prompt model]
  (future
    (let [config (when-let [state-var (find-var 'fantasia.sim.ecs.tick/*global-state)]
                   (when state-var (get-ollama-config @state-var)))
          {:keys [success text]} (call-ollama-with-retry! prompt model (or config {}))]
      (when success text))))

(defn call-ollama-sync!
  "Make a synchronous call to Ollama API for testing with configurable timeout."
  [prompt model]
  (let [config (when-let [state-var (find-var 'fantasia.sim.ecs.tick/*global-state)]
                 (when state-var (get-ollama-config @state-var)))
        {:keys [timeout-ms]} (or config {})
        actual-timeout (or timeout-ms 60000)]
    (try
      (let [response (http/post ollama-url
                                {:content-type :json
                                 :body (json/generate-string {:model model
                                                                :prompt prompt
                                                                :stream false
                                                                :options {:num_predict 10 :temperature 0.1}})
                                 :throw-exceptions false
                                 :socket-timeout actual-timeout
                                 :connection-timeout 5000})]
        (if (= (:status response) 200)
          (let [body (json/parse-string (:body response) true)
                text (or (get-in body [:response] "")
                         (get-in body [:thinking] "")
                         "")]
            (log/log-info "[OLLAMA:TEST-SUCCESS]" {:model model :length (count text)})
            {:success true :text text})
          {:success false :error (str "HTTP " (:status response))}))
      (catch Exception e
        (log/log-error "[OLLAMA:TEST-ERROR]" {:error (.getMessage e)})
        {:success false :error (.getMessage e)}))))

(defn- keep-alive-ping!
  "Send a minimal keep-alive request to Ollama to keep model in memory."
  [config]
  (let [{:keys [url model]} config
        result (call-ollama-raw! "." model 5000 {:options {:num_predict 1 :temperature 0.0}})]
    (when (:success result)
      (log/log-debug "[KEEP-ALIVE:PING]" {:model model}))
    result))

(defn start-ollama-keep-alive!
  "Start Ollama keep-alive heartbeat to prevent model unloading."
  []
  (let [config (when-let [state-var (find-var 'fantasia.sim.ecs.tick/*global-state)]
                 (when state-var (get-ollama-config @state-var)))]
    (when (and config (:keep-alive-enabled config))
      (reset! *ollama-keep-alive {:future nil :running? true})
      (let [{:keys [keep-alive-interval-ms]} config
            fut (future
                  (try
                    (log/log-info "[KEEP-ALIVE:STARTED]" {:interval-ms keep-alive-interval-ms})
                    (keep-alive-ping! config)
                    (while (:running? @*ollama-keep-alive)
                      (Thread/sleep keep-alive-interval-ms)
                      (when (:running? @*ollama-keep-alive)
                        (keep-alive-ping! config)))
                    (catch Exception e
                      (log/log-error "[KEEP-ALIVE:ERROR]" {:error (.getMessage e)}))
                    (finally
                      (log/log-info "[KEEP-ALIVE:STOPPED]")
                      (swap! *ollama-keep-alive assoc :future nil :running? false))))]
        (swap! *ollama-keep-alive assoc :future fut)))))

(defn stop-ollama-keep-alive!
  "Stop Ollama keep-alive heartbeat."
  []
  (when (:running? @*ollama-keep-alive)
    (swap! *ollama-keep-alive assoc :running? false)
    (when-let [fut (:future @*ollama-keep-alive)]
      (future-cancel fut))))
 
(defn generate-book-title
  "Generate a book title based on facets and events."
  [facets event-facets]
  (let [facet-str (str/join ", " (map name facets))
        event-facet-str (str/join ", " (map name event-facets))
        primary-facet (or (first (seq (concat event-facets facets))) :community)]
    (cond
      (contains? (set event-facets) :fire) "Tales of Ember"
      (contains? (set event-facets) :death) "Echoes of Departed"
      (contains? (set event-facets) :harvest) "Songs of Plenty"
      :else "Chronicles of Our People")))

(defn- build-myths-context
  "Build context string from ancient myths for Ollama prompt."
  [myths]
  (when (seq myths)
    (let [lines (map (fn [m] (str "- " (:title m) ": " (:text m))) myths)]
      (str "\n\nAncient echoes from worlds before:\n" (str/join "\n" lines)))))

(defn generate-book-text
  "Generate a mythological short story from traces and facets."
  [selected-traces facets title]
  (let [facet-str (str/join ", " (map name facets))
        trace-count (count selected-traces)
        first-trace (first selected-traces)
        ancient-myths (get-random-myths max-myths-for-prompt)
        myths-context (build-myths-context ancient-myths)
        fallback-text (cond
                        (and (= trace-count 0) (seq facets))
                          (str "The elders gather to speak of " facet-str 
                               ". Stories yet to be written, but the spirit of our people endures.")
                        
                        (pos? trace-count)
                          (let [trace-title (:title first-trace)
                                primary-facet (or (first (concat facets (:facets first-trace))) :memory)]
                            (case primary-facet
                              :fire (str "The flames remember: \"" trace-title "\". "
                                         "Our ancestors watched the sacred fire dance, "
                                         "and in its glow we found strength and community. "
                                         "This flame that once warmed cold nights now illuminates our shared history.")
                              :death (str "The departed speak through \"" trace-title "\": "
                                         "They are not gone, merely transformed. "
                                         "Each spirit carries forward the lessons of their life, "
                                         "a torch passed from generation to generation.")
                              :harvest (str "The bounty is celebrated: \"" trace-title "\". "
                                          "From the earth's generosity we draw life, "
                                          "and with gratitude we remember that every fruit, every grain "
                                          "carries the blessing of those who cultivated it before us.")
                              :community (str "The village echoes: \"" trace-title "\". "
                                           "We are many voices, one heart. "
                                           "In our shared stories, in our gathered memories, "
                                           "we find the wisdom that no single person could hold alone.")
                              :otherwise (str "The scribes record: \"" trace-title "\". "
                                           "In this moment we capture what might otherwise fade, "
                                           "preserving for future generations the essence of who we are.")))
                        
                        :else "The scribes sit with empty quills. The library grows quiet, waiting for events worth remembering.")]
    (if (and (seq selected-traces) (seq facets))
      (let [trace-titles (str/join "; " (map :title selected-traces))
            prompt (str "Write a short mythological story (2-3 sentences max) with title: \"" title "\"\n"
                       "Inspired by these events: " trace-titles 
                       "\nThe story should reflect these themes: " facet-str 
                       ". Keep it poetic and atmospheric."
                       myths-context) 
            ollama-future (call-ollama! prompt ollama-model)]
         (try
           (let [generated (deref ollama-future 30000 fallback-text)]
            (if (and generated (pos? (count generated)))
              (str (subs generated 0 (min 200 (count generated))) ".")
              fallback-text))
          (catch Exception e
            (log/log-warn "[SCRIBE:OLLAMA-TIMEOUT]" {:fallback true})
            fallback-text)))
      fallback-text)))

(defn create-book-placeholder
   "Create a placeholder book."
   [book-id trace-ids title facets created-at created-by]
  {:book/id book-id
   :trace-ids trace-ids
   :title title
   :text "The scribes are still writing this book..."
   :facets facets
   :created-at created-at
   :created-by created-by
   :read-count 0
   :favor-per-tick 0.001
   :generating? true})

(defn generate-book-content-async!
  "Generate book content asynchronously and persist as myth."
  [book-id selected-traces facets title]
  (future
    (let [book-text (generate-book-text selected-traces facets title)
          timestamp (System/currentTimeMillis)]
      (try
        (when-let [state-var (find-var 'fantasia.sim.ecs.tick/*global-state)]
          (when state-var
            (let [current-world @state-var]
              (when (get-in current-world [:books book-id])
                (swap! state-var
                       (fn [world]
                         (-> world
                             (assoc-in [:books book-id :text] book-text)
                             (assoc-in [:books book-id :generating?] false)
                             (assoc-in [:books book-id :completed-at] timestamp))))
                (let [myth-record {:title (get-in current-world [:books book-id :title])
                                 :text book-text
                                 :facets facets
                                 :created-at timestamp}]
                  (save-myth! myth-record))
                (log/log-info "[SCRIBE:BOOK-COMPLETE]"
                              {:book-id book-id
                               :text-length (count book-text)})))))
        (catch Exception e
          (log/log-error "[SCRIBE:ASYNC-UPDATE-FAILED]"
                         {:book-id book-id
                          :error (.getMessage e)}))))
    book-id))

(defn select-recent-traces
  "Select N recent traces relevant to given facets."
  [world culture-id facets n]
  ;; TODO: Migrate traces to ECS
  [])

(defn create-book
  "Create a new book record."
  [book-id trace-ids title text facets created-at created-by]
  {:book/id book-id
   :trace-ids trace-ids
   :title title
   :text text
   :facets facets
   :created-at created-at
   :created-by created-by
   :read-count 0})

(defn add-book-to-world!
  "Add a book to world's library."
  [world book]
  (let [book-id (:book/id book)]
    (-> world
        (assoc-in [:books book-id] book)
        (update-in [:books-list] conj book-id))))

(defn complete-scribe-job!
  "Complete a scribe job by creating a book, awarding favor, and starting async content generation."
  [world agent-id]
  (let [agent nil ; TODO: Get agent from ECS system
        library-pos nil ; TODO: Get from ECS job system
        culture-id (get-in world [:levers :default-culture-id] "culture-1")
        recent-events (take 3 (:recent-events world []))
        event-facets (mapcat #(get % :facets []) recent-events)
         selected-traces (select-recent-traces world culture-id event-facets 3)
         culture nil ; TODO: Migrate traces/get-culture to ECS
        facets (concat event-facets (when culture (:shared-facets culture [])))
        tick (:tick world)
        book-id (random-uuid)
        title (if (and (seq selected-traces) (seq facets))
                 (generate-book-title facets event-facets)
                 "A Fragment of Memory")
        trace-ids (map :trace/id selected-traces)
        favor-gain 0.01
        book (create-book-placeholder book-id trace-ids title facets tick agent-id)
        world-initial (add-book-to-world! world book)]
    (log/log-info "[SCRIBE:BOOK-STARTED]"
                  {:book-id book-id
                   :agent-id agent-id
                   :library-pos library-pos
                   :title title
                   :trace-ids trace-ids
                   :favor-gained favor-gain})
    (generate-book-content-async! book-id selected-traces facets title)
    (update-in world-initial [:favor] + favor-gain)))

(defn apply-book-favor!
  "Generate favor from books each tick.
   Each book generates favor_per_tick * (1 + read_count * 0.1)."
  [world]
  (let [books (vals (:books world))
         favor-gain (reduce
                      (fn [acc book]
                        (let [favor-per-tick (get book :favor-per-tick 0.001)
                              read-count (get book :read-count 0)
                              multiplier (+ 1.0 (* read-count 0.1))]
                          (+ acc (* favor-per-tick multiplier))))
                      0.0
                      books)]
    (update-in world [:favor] + favor-gain)))
