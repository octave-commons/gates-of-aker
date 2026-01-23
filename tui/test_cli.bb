#!/usr/bin/env bb

(ns test-cli
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [babashka.http-client :as http]))

(def backend-url "http://localhost:3000")

(defn test-connect []
  (println "\n[TEST] Testing connection...")
  (try
    (let [resp (http/get (str backend-url "/healthz") {:throw false})
          status (:status resp)]
      (if (= status 200)
        (println "✓ Connection successful")
        (println "✗ Connection failed - status:" status)))
    (catch Exception e
      (println "✗ Connection error:" (.getMessage e)))))

(defn test-state []
  (println "\n[TEST] Testing /sim/state...")
  (try
    (let [resp (http/get (str backend-url "/sim/state") {:throw false})
          status (:status resp)]
      (if (= status 200)
        (let [body (json/parse-string (:body resp) true)]
          (println "✓ State retrieved")
          (println "  - Tick:" (:tick body))
          (println "  - Agents:" (count (:agents body)))
          (println "  - Tiles:" (count (:tiles body))))
        (println "✗ State failed - status:" status)))
    (catch Exception e
      (println "✗ State error:" (.getMessage e)))))

(defn test-tick []
  (println "\n[TEST] Testing /sim/tick...")
  (try
    (let [resp (http/post (str backend-url "/sim/tick")
                          {:headers {"Content-Type" "application/json"}
                           :body (json/generate-string {:n 1})
                           :throw false})
          body (json/parse-string (:body resp) true)]
      (if (:last body)
        (println "✓ Tick successful")
        (println "✗ Tick failed - no last result")))
    (catch Exception e
      (println "✗ Tick error:" (.getMessage e)))))

(defn test-reset []
  (println "\n[TEST] Testing /sim/reset...")
  (try
    (let [resp (http/post (str backend-url "/sim/reset")
                          {:headers {"Content-Type" "application/json"}
                           :body (json/generate-string {:seed 1 :tree_density 0.08})
                           :throw false})
          body (json/parse-string (:body resp) true)]
      (if (:ok body)
        (println "✓ Reset successful")
        (println "✗ Reset failed")))
    (catch Exception e
      (println "✗ Reset error:" (.getMessage e)))))

(defn test-start-pause []
  (println "\n[TEST] Testing /sim/run and /sim/pause...")
  (try
    (let [run-resp (http/post (str backend-url "/sim/run") {:throw false})
          run-body (json/parse-string (:body run-resp) true)]
      (if (:ok run-body)
        (do
          (println "✓ Start successful")
          (Thread/sleep 100)
          (let [pause-resp (http/post (str backend-url "/sim/pause") {:throw false})
                pause-body (json/parse-string (:body pause-resp) true)]
            (if (:ok pause-body)
              (println "✓ Pause successful")
              (println "✗ Pause failed"))))
        (println "✗ Start failed")))
    (catch Exception e
      (println "✗ Start/Pause error:" (.getMessage e)))))

(defn -main []
  (println "=== FANTASIA CLI API TESTS ===")
  (test-connect)
  (test-state)
  (test-tick)
  (test-reset)
  (test-start-pause)
  (println "\n=== ALL TESTS COMPLETE ==="))

(-main)
