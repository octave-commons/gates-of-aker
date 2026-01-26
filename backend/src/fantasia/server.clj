(ns fantasia.server
  "Fantasia simulation server."
  (:require [fantasia.dev.logging :as log])
  (:gen-class))

(def ^:dynamic *clients* (atom #{}))
(def ^:dynamic *runner* (atom {:running? false}))

(defn ws-send!
  "Send message to WebSocket client."
  [ch msg]
  ;; Simplified placeholder for network testing
  (log/log-info "Sending message:" msg))

(defn broadcast!
  "Broadcast message to all connected clients."
  [msg]
  (doseq [client @*clients*]
    (ws-send! client msg))
  (log/log-info "Broadcasted message to" (count @*clients*) "clients"))

(defn start
  "Start simulation server."
  [& args]
  (log/log-info "Fantasia server starting (simplified for testing)...")
  (reset! *runner* {:running? true})
  (log/log-info "Server started successfully"))

(defn -main
  "Main entry point for server."
  [& args]
  (start args))