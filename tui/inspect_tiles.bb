#!/usr/bin/env bb
(ns inspect-tiles
  (:require [cheshire.core :as json]
            [babashka.http-client :as http]))

(def backend-url "http://localhost:3000")

(defn -main [& _args]
  (let [resp (http/get (str backend-url "/sim/state") {:throw false})
        body (json/parse-string (:body resp) true)
        tiles (:tiles body)]
    (if (empty? tiles)
      (println "No tiles in simulation")
      (do
        (println "Tile count:" (count tiles))
        (println "\nFirst tile:")
        (println (pr-str (first (vals tiles))))
        (println "\nTile keys:")
        (println (keys (first (vals tiles))))))))

(-main)
