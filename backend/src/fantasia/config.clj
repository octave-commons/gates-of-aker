(ns fantasia.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [fantasia.dev.logging :as log]))

(def default-ollama-config
  "Default configuration if config file is missing."
  {:ollama
   {:url "http://localhost:11434/api/generate"
    :primary-model "qwen3:4b"
    :fallback-models ["llama3.2:1b" "mistral:7b"]
    :timeout-ms 60000
    :retries 1
    :retry-delay-ms 2000
    :keep-alive-enabled true
    :keep-alive-interval-ms 300000}
   :ollama-embed
   {:url "http://localhost:11434/api/embed"
    :model "nomic-embed-text"
    :timeout-ms 10000}
   :storyteller
   {:provider :openai-compatible
    :base-url nil
    :model "mistral-large-3:675b"
    :temperature 0.9
    :max-tokens 1400
    :timeout-ms 90000
    :context-chapters 4
    :context-chars 2400
    :narrative-dir "../../../../vaults/fork_tales/narrative"
    :myths-path "../.myth/myths.jsonl"
    :character-roster ["Duct" "Null" "Patch" "Sei" "莉津律宗利都" "Rin" "Truth" "Axiom" "Cephalon"]}})

(defn- load-edn-file
  "Load EDN file from path, return nil if file doesn't exist or can't be read."
  [path]
  (try
    (when-let [file (io/file path)]
      (when (.exists file)
        (edn/read-string (slurp file))))
    (catch Exception e
      (log/log-warn "[CONFIG:LOAD-FAILED]" {:path path :error (.getMessage e)})
      nil)))

(defn- deep-merge
  [left right]
  (merge-with (fn [x y]
                (if (and (map? x) (map? y))
                  (deep-merge x y)
                  y))
              (or left {})
              (or right {})))

(defn get-ollama-config-path
  "Get the path to the Ollama configuration file.
   Checks environment variable OLLAMA_CONFIG_PATH first, then default location."
  []
  (or (System/getenv "OLLAMA_CONFIG_PATH")
      "config/ollama.edn"))

(defn load-ollama-config!
  "Load Ollama configuration from EDN file, merge with defaults."
  []
  (let [config-path (get-ollama-config-path)
        loaded-config (load-edn-file config-path)
        merged-config (deep-merge default-ollama-config loaded-config)]
    (if loaded-config
      (log/log-info "[CONFIG:OLLAMA-LOADED]"
                    {:path config-path
                     :primary-model (get-in merged-config [:ollama :primary-model])
                     :fallbacks-count (count (get-in merged-config [:ollama :fallback-models]))})
      (log/log-info "[CONFIG:OLLAMA-USING-DEFAULTS]"
                    {:primary-model (get-in merged-config [:ollama :primary-model])}))
    merged-config))

(defn get-ollama-models
  "Get list of Ollama models in priority order (primary first, then fallbacks)."
  [config]
  (let [primary (get-in config [:ollama :primary-model])
        fallbacks (get-in config [:ollama :fallback-models] [])]
    (if primary
      (vec (concat [primary] fallbacks))
      fallbacks)))

(defn get-ollama-primary-model
  "Get the primary Ollama model."
  [config]
  (get-in config [:ollama :primary-model] "qwen3:4b"))

(defn get-storyteller-config
  "Get storyteller configuration with env-backed proxy defaults."
  [config]
  (let [storyteller (get config :storyteller {})]
    (-> storyteller
        (update :provider #(or % :openai-compatible))
        (update :base-url #(or %
                               (System/getenv "OPEN_HAX_OPENAI_PROXY_URL")
                               (System/getenv "OPENAI_BASE_URL")
                               (System/getenv "PROMETHEAN_OPENAI_API")))
        (assoc :api-key (or (:api-key storyteller)
                            (System/getenv "OPEN_HAX_OPENAI_PROXY_AUTH_TOKEN")
                            (System/getenv "OPENAI_API_KEY")
                            (System/getenv "OPENAI_KEY")))
        (update :model #(or (System/getenv "STORYTELLER_MODEL")
                            (System/getenv "FORK_TALES_MODEL")
                            %
                            "mistral-large-3:675b"))
        (update :narrative-dir #(or % (System/getenv "FORK_TALES_NARRATIVE_DIR") "../../../../vaults/fork_tales/narrative"))
        (update :myths-path #(or % (System/getenv "FORK_TALES_MYTHS_PATH") "../.myth/myths.jsonl"))
        (update :context-chapters #(or % 4))
        (update :context-chars #(or % 2400))
        (update :timeout-ms #(or % 90000))
        (update :temperature #(or % 0.9))
        (update :max-tokens #(or % 1400)))))
