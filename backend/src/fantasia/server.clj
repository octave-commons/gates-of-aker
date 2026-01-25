(ns fantasia.server
  "Fantasia simulation server with WebSocket support."
  (:require [ring.core :as ring]
            [ring.adapter.jetty9 :as http]
            [ring.util.response :as ring-resp]
            [ring.middleware.json :refer [json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [clojure.data.json :as json]
            [fantasia.dev.logging :as log]))

(def ^:dynamic *clients* (atom #{}))
(def ^:dynamic *runner* (atom {:running? false}))

(defn ws-send!
  "Send message to WebSocket client."
  [ch msg]
  ;; Simplified placeholder
  (log/log-info "Sending message:" msg))

(defn broadcast!
  "Broadcast message to all connected clients."
  [msg]
  (doseq [client @*clients*]
    (ws-send! client msg))
  (log/log-info "Broadcasted message to" (count @*clients*) "clients"))