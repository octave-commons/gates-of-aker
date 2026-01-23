#!/usr/bin/env bb
(ns test-tui
  (:require [cheshire.core :as json]
            [babashka.http-client :as http]))

(def backend-url "http://localhost:3000")

(defn test-connection []
  (try
    (let [resp (http/get (str backend-url "/healthz") {:throw false})
          status (:status resp)]
      (if (= status 200)
        (do (println "✓ Backend connection successful") true)
        (do (println "✗ Backend returned status:" status) false)))
    (catch Exception e
      (println "✗ Connection error:" (.getMessage e))
      false)))

(defn test-state-api []
  (try
    (let [resp (http/get (str backend-url "/sim/state") {:throw false})
          body (json/parse-string (:body resp) true)]
      (println "✓ State API response:")
      (println "  Tick:" (:tick body))
      (println "  Agents:" (count (:agents body)))
      true)
    (catch Exception e
      (println "✗ State API error:" (.getMessage e))
      false)))

(defn -main [& _args]
  (println "=== Fantasia TUI Test Suite ===")
  (println)
  
  (println "Testing connection to backend...")
  (test-connection)
  (println)
  
  (println "Testing state API...")
  (test-state-api)
  (println)
  
  (println "=== All tests complete ==="))

(-main)
