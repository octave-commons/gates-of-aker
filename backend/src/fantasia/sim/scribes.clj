(ns fantasia.sim.scribes
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [fantasia.dev.logging :as log]
            [fantasia.sim.traces :as traces]
            [clj-http.client :as http]
            [clojure.java.io :as io]))

(def ollama-url "http://localhost:11434/api/generate")

(def ollama-model "qwen3:4b")

(def ^:private myths-file-path "myths.jsonl")

(def ^:private max-myths-for-prompt 5)

(defn- load-myths!
  "Load all myths from the myths file.
  Returns a lazy sequence of myth maps."
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
  "Append a new myth to the myths file."
  [myth]
  (try
    (io/make-parents myths-file-path)
    (spit myths-file-path (str (json/generate-string myth) "\n") :append true)
    (log/log-info "[MYTHS:SAVED]" {:title (:title myth) :themes (:facets myth)})
    (catch Exception e
      (log/log-error "[MYTHS:SAVE-FAILED]" {:error (.getMessage e)}))))

(defn- get-random-myths
  "Get N random myths from loaded myths for context."
  [n]
  (let [myths (load-myths!)]
    (if (empty? myths)
      []
      (let [rng (java.util.Random. (System/currentTimeMillis))
            count-myths (count myths)
            indices (distinct (repeatedly (min n count-myths) #(.nextInt rng count-myths)))]
        (mapv #(nth myths %) indices)))))

(defn call-ollama!
  "Make an async call to Ollama API and return a future with the response."
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

(defn generate-book-title
  "Generate a book title for a book based on facets and events."
  [facets event-facets]
  (let [facet-str (str/join ", " (map name facets))
        event-facet-str (str/join ", " (map name event-facets))
        primary-facet (or (first (seq (concat event-facets facets))) :community)]
    (cond
      (contains? (set event-facets) :fire) "Tales of the Ember"
      (contains? (set event-facets) :death) "Echoes of the Departed"
      (contains? (set event-facets) :harvest) "Songs of Plenty"
      :else "Chronicles of Our People")))

(defn- build-myths-context
  "Build context string from ancient myths for Ollama prompt."
  [myths]
  (when (seq myths)
    (str "\n\nAncient echoes from worlds before:\n"
          (str/join "\n"
                    (map (fn [m]
                           (let [text-preview (if (:text m)
                                              (subs (:text m) 0 (min 80 (count (:text m)))
                                              "")]
                             (str "- " (:title m) ": " text-preview "...")))
                        myths)))

(defn generate-book-text
  "Generate a mythological short story from traces and facets.
  Uses Ollama asynchronously with fallback to local generation.
  Draws from persistent myths for context."
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
          (let [generated (deref ollama-future 5000 fallback-text)]
            (if (and generated (pos? (count generated)))
              (str (subs generated 0 (min 200 (count generated))) ".")
              fallback-text))
          (catch Exception e
            (log/log-warn "[SCRIBE:OLLAMA-TIMEOUT]" {:fallback true})
            fallback-text)))
      fallback-text)))

(defn create-book-placeholder
  "Create a placeholder book that will be filled with Ollama-generated content."
  [book-id trace-ids title facets created-at created-by]
  {:book/id book-id
   :trace-ids trace-ids
   :title title
   :text "The scribes are still writing this book..."
   :facets facets
   :created-at created-at
   :created-by created-by
   :read-count 0
   :generating? true})

(defn generate-book-content-async!
  "Generate book content asynchronously, update world, and persist as myth.
  Returns book-id for tracking purposes."
  [book-id selected-traces facets title]
  (future
    (let [book-text (generate-book-text selected-traces facets title)
          timestamp (System/currentTimeMillis)]
      (try
        (when-let [state-var (find-var 'fantasia.sim.tick.core/*state)]
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
  (let [all-traces (traces/get-traces-by-culture world culture-id)
        facet-set (set facets)]
    (->> all-traces
         (filter (fn [trace]
                   (some #(facet-set %) (:facets trace))))
         (sort-by :created-at >)
         (take n))))

(defn create-book
  "Create a new book record with generated title and text."
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
  "Add a book to world's library collection."
  [world book]
  (let [book-id (:book/id book)]
    (-> world
        (assoc-in [:books book-id] book)
        (update-in [:books-list] conj book-id))))

(defn complete-scribe-job!
  "Complete a scribe job by creating a book placeholder and starting async content generation."
  [world agent-id]
  (let [agent (get-in world [:agents agent-id])
        library-pos (:target (get-in world [:agents agent-id :current-job]))
        culture-id (get-in world [:levers :default-culture-id] "culture-1")
        recent-events (take 3 (:recent-events world []))
        event-facets (mapcat #(get % :facets []) recent-events)
        selected-traces (select-recent-traces world culture-id event-facets 3)
        culture (traces/get-culture world culture-id)
        facets (concat event-facets (when culture (:shared-facets culture [])))
        tick (:tick world)
        book-id (random-uuid)
        title (if (and (seq selected-traces) (seq facets))
                 (generate-book-title facets event-facets)
                 "A Fragment of Memory")
        trace-ids (map :trace/id selected-traces)
        book (create-book-placeholder book-id trace-ids title facets tick agent-id)
        world' (add-book-to-world! world book)]
    (log/log-info "[SCRIBE:BOOK-STARTED]"
                  {:book-id book-id
                   :agent-id agent-id
                   :library-pos library-pos
                   :title title
                   :trace-ids trace-ids
                   :generating? true})
    (generate-book-content-async! book-id selected-traces facets title)
    world'))
