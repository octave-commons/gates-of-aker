(ns fantasia.sim.embeddings
  (:require [clj-http.client :as http]
             [cheshire.core :as json]
             [fantasia.dev.logging :as log]
             [fantasia.config :as config]))

(defonce ^:private nomic-cache (atom {}))
(defonce ^:private max-cache-size 1000)

(defn get-ollama-config
  "Get Ollama embedding configuration from loaded config."
  []
  (let [cfg (config/load-ollama-config!)]
    {:url (get-in cfg [:ollama-embed :url] "http://localhost:11434/api/embed")
     :model (get-in cfg [:ollama-embed :model] "nomic-embed-text")
     :timeout-ms (get-in cfg [:ollama-embed :timeout-ms] 10000)}))

(defn call-nomic-embed!
  "Call Ollama nomic-embed-text API to get embedding vector."
  [text config]
  (try
    (let [url (:url config)
          model (:model config)
          response (http/post url
                              {:content-type :json
                               :body (json/generate-string
                                       {:model model
                                        :input text})
                               :throw-exceptions false
                               :socket-timeout (:timeout-ms config)
                               :connection-timeout 5000})]
       (if (= (:status response) 200)
         (let [body (json/parse-string (:body response) true)
               first-embedding (first (:embeddings body))
               embedding (cond
                           (vector? first-embedding) first-embedding
                           (and (map? first-embedding) (vector? (:embedding first-embedding))) (:embedding first-embedding)
                           :else nil)]
           (if (and embedding (vector? embedding))
             {:success true :vector embedding :model model}
             {:success false :error "No embedding in response"}))
         {:success false :error (str "HTTP " (:status response))}))
    (catch Exception e
      (log/log-error "[NOMIC:EMBED]" {:error (.getMessage e)
                                       :text-length (count (str text))})
      {:success false :error (.getMessage e)}))

(defn get-or-create-embedding!
  "Get embedding from cache or create using nomic API."
  [text config]
  (let [cache-key (str text)]
    (if-let [cached (get @nomic-cache cache-key)]
      cached
      (let [{:keys [success vector]} (call-nomic-embed! text config)]
        (when (and success vector)
          (when (>= (count @nomic-cache) max-cache-size)
            (let [cache-entries (seq @nomic-cache)]
              (swap! nomic-cache
                       (into {} (drop 1 cache-entries)))))
          (swap! nomic-cache assoc cache-key vector))
        vector)))))

(defn cosine-similarity
  "Compute cosine similarity between two vectors."
  [v1 v2]
  (let [left (when (sequential? v1) (vec v1))
        right (when (sequential? v2) (vec v2))]
    (if (and left
             right
             (pos? (count left))
             (= (count left) (count right))
             (every? number? left)
             (every? number? right))
      (let [dot (reduce + 0 (map * left right))
            mag1 (Math/sqrt (reduce + 0 (map #(* % %) left)))
            mag2 (Math/sqrt (reduce + 0 (map #(* % %) right)))]
        (if (and (pos? mag1) (pos? mag2))
          (/ dot (* mag1 mag2))
          0.0))
      0.0)))

(defn init-embeddings!
  "Initialize embedding system (cache is empty at start)."
  []
  (let [cfg (get-ollama-config)]
    (log/log-info "[NOMIC] Embedding system initialized with model:" (:model cfg))
    {:loaded? true}))
