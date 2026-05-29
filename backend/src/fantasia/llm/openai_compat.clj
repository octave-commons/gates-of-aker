(ns fantasia.llm.openai-compat
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.string :as str]))

(defn- strip-trailing-slashes
  [value]
  (str/replace (or value "") #"/+$" ""))

(defn completions-url
  "Normalize a configured OpenAI-compatible base URL into a /v1/chat/completions URL."
  [base-url]
  (let [trimmed (strip-trailing-slashes base-url)]
    (cond
      (str/blank? trimmed) nil
      (str/ends-with? trimmed "/v1/chat/completions") trimmed
      (str/ends-with? trimmed "/chat/completions") trimmed
      (str/ends-with? trimmed "/v1") (str trimmed "/chat/completions")
      :else (str trimmed "/v1/chat/completions"))))

(defn- auth-headers
  [api-key]
  (cond-> {"Content-Type" "application/json"}
    (seq api-key) (assoc "Authorization" (str "Bearer " api-key))))

(defn- extract-content-part
  [part]
  (cond
    (string? part) part
    (map? part) (or (:text part)
                    (:content part)
                    (when-let [parts (:parts part)]
                      (->> parts
                           (map extract-content-part)
                           (remove str/blank?)
                           (str/join "\n"))))
    :else nil))

(defn message-text
  "Extract assistant text from a chat-completions response.
   Supports plain string content and structured content arrays."
  [response]
  (let [message (get-in response [:choices 0 :message])
        content (:content message)]
    (cond
      (string? content) (str/trim content)
      (vector? content) (->> content
                             (map extract-content-part)
                             (remove str/blank?)
                             (str/join "\n")
                             (str/trim))
      :else "")))

(defn chat-completion!
  "Send a chat completion request to an OpenAI-compatible endpoint.
   Returns {:success boolean :text string :response map :error string}."
  [{:keys [base-url api-key model temperature max-tokens timeout-ms messages]}]
  (let [url (completions-url base-url)
        body (cond-> {:model model
                      :messages messages}
               (some? temperature) (assoc :temperature temperature)
               (some? max-tokens) (assoc :max_tokens max-tokens))]
    (if (or (str/blank? url) (str/blank? model))
      {:success false :error "Missing base-url or model"}
      (try
        (let [response (http/post url
                                  {:headers (auth-headers api-key)
                                   :content-type :json
                                   :body (json/generate-string body)
                                   :as :text
                                   :throw-exceptions false
                                   :socket-timeout (or timeout-ms 90000)
                                   :connection-timeout 15000})
              parsed (when-let [body-text (:body response)]
                       (json/parse-string body-text true))
              text (when parsed (message-text parsed))]
          (if (= 200 (:status response))
            (if (seq text)
              {:success true :text text :response parsed}
              {:success false :error "Empty assistant content" :response parsed})
            {:success false
             :error (str "HTTP " (:status response))
             :response parsed}))
        (catch Exception e
          {:success false :error (.getMessage e)})))))
