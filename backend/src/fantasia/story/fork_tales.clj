(ns fantasia.story.fork-tales
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [fantasia.config :as config]
            [fantasia.dev.logging :as log]
            [fantasia.llm.openai-compat :as openai]))

(def ^:private chapter-filename-re #"^Chapter_(\d+)_.*\.md$")
(def ^:private default-character-roster
  ["Duct"
   "Null"
   "Patch"
   "Sei"
   "莉津律宗利都"
   "Rin"
   "Truth"
   "Axiom"
   "Cephalon"])

(defn storyteller-config
  []
  (config/get-storyteller-config (config/load-ollama-config!)))

(defn- canonical-path
  [path]
  (some-> path io/file .getCanonicalPath))

(defn- file-exists?
  [path]
  (boolean (and path (.exists (io/file path)))))

(defn- chapter-number-from-file
  [file]
  (when-let [[_ num] (re-find chapter-filename-re (.getName file))]
    (Long/parseLong num)))

(defn- normalize-heading-title
  [heading]
  (-> heading
      (str/replace #"^#\s*" "")
      (str/replace #"^(?:Chapter\s+)?\d+\s*[—-]\s*" "")
      (str/replace #"^Chapter\s+\d+\s*" "")
      (str/trim)))

(defn- chapter-title-from-text
  [text]
  (some->> (re-find #"(?m)^#\s+.+$" (or text ""))
           normalize-heading-title
           not-empty))

(defn- chapter-record
  [file]
  (let [text (slurp file)
        number (chapter-number-from-file file)
        title (or (chapter-title-from-text text)
                  (some-> file .getName (str/replace #"\.md$" "")))]
    {:number number
     :title title
     :path (.getCanonicalPath file)
     :text text}))

(defn chapter-records
  [narrative-dir]
  (if (str/blank? (str narrative-dir))
    []
    (let [dir (io/file narrative-dir)]
      (if (.exists dir)
        (->> (.listFiles dir)
             (filter #(.isFile %))
             (filter #(chapter-number-from-file %))
             (map chapter-record)
             (sort-by :number)
             (vec))
        []))))

(defn- trim-context
  [value max-chars]
  (let [text (str/trim (or value ""))]
    (if (and (pos? max-chars) (> (count text) max-chars))
      (str (subs text 0 max-chars) "\n[…truncated…]")
      text)))

(defn- agent-summary-line
  [agent]
  (let [agent-name (or (:name agent) (str "agent-" (:id agent)))
        role (or (:role agent) :unknown)
        role-name (if (keyword? role) (name role) (str role))
        pos (:pos agent)]
    (format "- %s (%s)%s"
            agent-name
            role-name
            (if (and (vector? pos) (= 2 (count pos)))
              (format " at [%s %s]" (first pos) (second pos))
              ""))))

(defn- summarize-agents
  [snapshot]
  (let [agents (or (:agents snapshot) [])]
    (if (seq agents)
      (->> agents
           (take 8)
           (map agent-summary-line)
           (str/join "\n"))
      "- No agents present.")))

(defn- summarize-events
  [snapshot]
  (let [events (or (:recent-events snapshot) [])]
    (if (seq events)
      (->> events
           (take-last 6)
           (map pr-str)
           (map #(trim-context % 240))
           (map #(str "- " %))
           (str/join "\n"))
      "- No recent events captured.")))

(defn- summarize-ledger
  [snapshot]
  (let [ledger (or (:ledger snapshot) {})]
    (if (seq ledger)
      (->> ledger
           (take 6)
           (map (fn [[claim metrics]]
                  (format "- %s => %s"
                          claim
                          (trim-context (pr-str metrics) 180))))
           (str/join "\n"))
      "- No active myth ledger entries.")))

(defn- chapter-context
  [chapters max-chars]
  (if (seq chapters)
    (->> chapters
         (map (fn [{:keys [number title text]}]
                (format "Chapter %d — %s\n%s"
                        number
                        title
                        (trim-context text max-chars))))
         (str/join "\n\n---\n\n"))
    "No prior chapters available."))

(defn- build-messages
  [snapshot {:keys [character-roster context-chapters context-chars model]} chapters chapter-number user-prompt]
  (let [active-roster (or (seq character-roster) default-character-roster)
        latest-chapters (take-last (max 1 (or context-chapters 4)) chapters)
        chapter-title-hint (format "Return markdown beginning with '# %d — <Title>' and then the chapter body." chapter-number)
        system-prompt (str
                       "You are continuing the Fork Tales narrative as an in-world myth engine.\n"
                       "Maintain the established voice: precise, mythic, technological, consent-aware, and character-consistent.\n"
                       "Keep systems causal. Do not summarize the whole setting; continue it.\n"
                       "Use the named cast when possible. Do not invent a totally new cast unless world state demands it.\n"
                       chapter-title-hint)
        world-prompt (str
                      "Story engine target: continue Fork Tales from Gates of Aker state.\n\n"
                      "Current game-state summary\n"
                      "- Tick: " (:tick snapshot 0) "\n"
                      "- Calendar: " (trim-context (pr-str (:calendar snapshot)) 240) "\n"
                      "- Agents:\n" (summarize-agents snapshot) "\n\n"
                      "Recent events\n" (summarize-events snapshot) "\n\n"
                      "Myth ledger\n" (summarize-ledger snapshot) "\n\n"
                      "Character roster\n"
                      (->> active-roster (map #(str "- " %)) (str/join "\n")) "\n\n"
                      "Recent Fork Tales chapters\n"
                      (chapter-context latest-chapters (or context-chars 2400))
                      (when (seq user-prompt)
                        (str "\n\nAdditional operator prompt\n" user-prompt))
                      (str "\n\nWrite Chapter " chapter-number
                           " as a direct continuation. Prefer 6-14 short paragraphs."
                           " Return only markdown for the chapter; no explanation."
                           " Model hint: " model))]
    [{:role "system" :content system-prompt}
     {:role "user" :content world-prompt}]))

(defn- strip-code-fences
  [text]
  (let [trimmed (str/trim (or text ""))]
    (if (and (str/starts-with? trimmed "```")
             (str/ends-with? trimmed "```"))
      (-> trimmed
          (str/replace-first #"^```[a-zA-Z0-9_-]*\n?" "")
          (str/replace #"\n?```$" "")
          (str/trim))
      trimmed)))

(defn- normalize-title
  [title chapter-number]
  (or (some-> title str/trim not-empty)
      (format "Chapter %d" chapter-number)))

(defn- chapter-body
  [text]
  (let [lines (str/split-lines text)]
    (->> lines
         (drop-while str/blank?)
         (drop-while #(re-find #"^#\s+" %))
         (str/join "\n")
         (str/trim))))

(defn- chapter-markdown
  [chapter-number title text]
  (let [cleaned (strip-code-fences text)
        parsed-title (chapter-title-from-text cleaned)
        final-title (normalize-title (or parsed-title title) chapter-number)
        body (or (not-empty (chapter-body cleaned)) cleaned)]
    {:title final-title
     :text (format "# %d — %s\n\n%s\n" chapter-number final-title (str/trim body))}))

(defn- slugify-title
  [title]
  (let [slug (-> title
                 (str/replace #"[^\p{L}\p{N}]+" "_")
                 (str/replace #"_+" "_")
                 (str/replace #"^_+|_+$" ""))]
    (if (seq slug) slug "Untitled")))

(defn- chapter-path
  [narrative-dir chapter-number title]
  (str (io/file narrative-dir
                 (format "Chapter_%02d_%s.md" chapter-number (slugify-title title)))))

(defn- chapter-preview
  [text max-chars]
  (-> text
      chapter-body
      (str/replace #"\s+" " ")
      (trim-context max-chars)))

(defn- chapter-summary
  [{:keys [number title path text]}]
  {:number number
   :title title
   :path path
   :preview (chapter-preview text 220)})

(defn- append-jsonl!
  [path record]
  (io/make-parents path)
  (spit path (str (json/generate-string record) "\n") :append true))

(defn storyteller-status
  ([] (storyteller-status (storyteller-config)))
  ([cfg]
   (let [narrative-dir (canonical-path (:narrative-dir cfg))
         chapters (chapter-records narrative-dir)
         latest (last chapters)]
     {:configured (boolean (and (seq (:base-url cfg)) (seq (:api-key cfg)) (seq narrative-dir)))
      :provider (:provider cfg)
      :model (:model cfg)
      :narrative_dir narrative-dir
      :narrative_exists (file-exists? narrative-dir)
      :chapter_count (count chapters)
      :latest_chapter (when latest
                        (select-keys latest [:number :title :path]))})))

(defn chapter-history
  ([] (chapter-history (storyteller-config)))
  ([cfg]
   (let [narrative-dir (canonical-path (:narrative-dir cfg))
         chapters (chapter-records narrative-dir)]
     {:configured (boolean (and (seq (:base-url cfg)) (seq (:api-key cfg)) (seq narrative-dir)))
      :chapter_count (count chapters)
      :chapters (->> chapters
                     reverse
                     (take 12)
                     (mapv chapter-summary))})))

(defn chapter-detail
  ([chapter-number]
   (chapter-detail (storyteller-config) chapter-number))
  ([cfg chapter-number]
   (let [narrative-dir (canonical-path (:narrative-dir cfg))
         chapters (chapter-records narrative-dir)
         target-number (cond
                         (integer? chapter-number) chapter-number
                         (string? chapter-number) (try
                                                    (Long/parseLong chapter-number)
                                                    (catch Exception _ nil))
                         :else nil)
         chapter (some #(when (= (:number %) target-number) %) chapters)]
     (when chapter
       (assoc (chapter-summary chapter)
              :text (:text chapter))))))

(defn generate-next-chapter!
  ([snapshot opts]
   (generate-next-chapter! (storyteller-config) snapshot opts))
  ([cfg snapshot {:keys [dry_run user_prompt]}]
   (let [narrative-dir (canonical-path (:narrative-dir cfg))
         myths-path (canonical-path (:myths-path cfg))
         configured? (boolean (and (seq (:base-url cfg))
                                   (seq (:api-key cfg))
                                   (seq narrative-dir)))
         chapters (chapter-records narrative-dir)
         chapter-number (inc (or (:number (last chapters)) 0))]
     (if-not configured?
       {:ok false
        :configured false
        :chapter_number chapter-number
        :written false
        :error "Storyteller is not fully configured"}
       (let [messages (build-messages snapshot cfg chapters chapter-number user_prompt)
             response (openai/chat-completion! {:base-url (:base-url cfg)
                                                :api-key (:api-key cfg)
                                                :model (:model cfg)
                                                :temperature (:temperature cfg)
                                                :max-tokens (:max-tokens cfg)
                                                :timeout-ms (:timeout-ms cfg)
                                                :messages messages})]
         (if-not (:success response)
           {:ok false
            :configured true
            :chapter_number chapter-number
            :written false
            :error (:error response)}
           (let [{:keys [title text]} (chapter-markdown chapter-number (format "Chapter %d" chapter-number) (:text response))
                 path (chapter-path narrative-dir chapter-number title)
                 write? (not (true? dry_run))
                 myth-record {:title title
                              :text text
                              :facets ["fork-tales" "chapter" "continuation"]
                              :created_at (System/currentTimeMillis)
                              :chapter_number chapter-number
                              :source "gates-of-aker"}]
             (when write?
               (io/make-parents path)
               (spit path text)
               (when myths-path
                 (append-jsonl! myths-path myth-record))
               (log/log-info "[FORK-TALES:CHAPTER-WRITTEN]"
                             {:chapter-number chapter-number
                              :title title
                              :path path}))
             {:ok true
              :configured true
              :chapter_number chapter-number
              :title title
              :path path
              :written write?
              :text text})))))))
