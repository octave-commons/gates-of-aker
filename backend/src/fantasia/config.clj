(ns fantasia.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [fantasia.dev.logging :as log]))

(def default-ollama-config
  "Default Ollama configuration if config file is missing."
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
    :timeout-ms 10000}})

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
        merged-config (merge default-ollama-config loaded-config)]
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
