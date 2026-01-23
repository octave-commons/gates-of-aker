#!/usr/bin/env bb
(ns test-hex-view
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [babashka.http-client :as http]))

(def backend-url "http://localhost:3000")

(defn get-two-letter-code [name]
  (let [name (str/upper-case (or name ""))
        unique-chars (loop [chars (seq name)
                           seen #{}
                           result []]
                      (if (or (empty? chars) (>= (count result) 2))
                        result
                        (let [c (first chars)]
                          (if (contains? seen c)
                            (recur (rest chars) seen result)
                            (recur (rest chars) (conj seen c) (conj result c))))))]
    (apply str (concat unique-chars (repeat (- 2 (count unique-chars)) "?")))))

(defn biome-to-char [biome]
  (case biome
    :forest "F"
    :plains "P"
    :swamp "S"
    :water "W"
    :mountain "M"
    :desert "D"
    :field "L"
    :rocky "R"
    "."))

(defn structure-to-char [structure]
  (case structure
    :wall "█"
    :house "H"
    :campfire "C"
    :temple "T"
    :library "L"
    :warehouse "W"
    :workshop "S"
    :school "K"
    :statue/dog "D"
    :road "="
    nil ""
    ""))

(defn resource-to-char [resource]
  (let [r (keyword (or resource ""))]
    (case r
      :tree "♣"
      :stone "▪"
      :grain "•"
      nil ""
      "")))

(defn tile-to-string [tile agents-at]
  (let [biome (biome-to-char (:biome tile))
        structure (structure-to-char (:structure tile))
        resource (resource-to-char (:resource tile))
        agent-code (if (seq agents-at)
                     (get-two-letter-code (:name (first agents-at)))
                     "")
        main-char (if (seq agent-code)
                      agent-code
                      (if (seq structure)
                          structure
                          (if (seq resource)
                              resource
                              biome)))]
    (str/trim (format "%-3s" main-char))))

(defn draw-hex-row [row-q world-state offset-q offset-r width]
  (let [row-str (StringBuilder.)
        padding (if (odd? (+ row-q offset-q)) "  " "")]
    (.append row-str padding)
    (dotimes [col width]
      (let [q (+ col row-q offset-q)
            r (+ col offset-r)
            tile-key (keyword (str "[" q " " r "]"))
            tile (get-in world-state [:tiles tile-key])
            agents-at (filter #(= [q r] (:pos %)) (:agents world-state))]
        (if tile
          (.append row-str (format "[%s] " (tile-to-string tile agents-at)))
          (.append row-str "[   ] "))))
    (.toString row-str)))

(defn test-2-letter-codes []
  (println "=== Testing 2-letter codes ===")
  (doseq [name ["encumber" "entomb" "Alice" "Bob" "AA" "A" ""]]
    (println (format "  '%s' -> '%s'" name (get-two-letter-code name)))))

(defn test-hex-rendering []
  (println "\n=== Testing hex rendering ===")
  (try
    (let [resp (http/get (str backend-url "/sim/state") {:throw false})
          body (json/parse-string (:body resp) true)
          world-state body]
      (println "  Connected to backend")
      (println (format "  Total tiles: %d" (count (:tiles world-state))))
      (println (format "  Total agents: %d" (count (:agents world-state))))
      (let [sample-tiles (take 5 (keys (:tiles world-state)))]
        (println "  Sample tile keys:")
        (doseq [k sample-tiles]
          (println "    " k)))
      (println "\n  Sample agent positions:")
      (doseq [a (take 5 (:agents world-state))]
        (println (format "    %s @ %s" (:name a) (:pos a))))
      (println "\n  Sample hex rows (offset [95, 5]):")
      (dotimes [row 10]
        (println "  " (draw-hex-row row world-state 95 5 15))))
    (catch Exception e
      (println "  Error:" (.getMessage e)))))

(defn -main [& _args]
  (println "=== Hex View Test Suite ===")
  (println)
  
  (test-2-letter-codes)
  (test-hex-rendering)
  
  (println "\n=== Tests complete ==="))

(-main)
