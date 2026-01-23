#!/usr/bin/env bb
(ns inspect-agent
  (:require [cheshire.core :as json]
            [babashka.http-client :as http]))

(def backend-url "http://localhost:3000")

(defn -main [& _args]
  (let [resp (http/get (str backend-url "/sim/state") {:throw false})
        body (json/parse-string (:body resp) true)
        agents (:agents body)]
    (if (empty? agents)
      (println "No agents in simulation")
      (do
        (println "Agent count:" (count agents))
        (println "\nFirst agent:")
        (println (pr-str (first agents)))
        (println "\nAgent keys:")
        (println (keys (first agents)))))))

(-main)
