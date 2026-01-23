(ns fog-of-war-test
  (:require [cheshire.core :as json]
            [babashka.http-client :as http]))

(def backend-url "http://localhost:3000")

(defn get-state []
  (let [resp (http/get (str backend-url "/sim/state") {:throw false})
        body (json/parse-string (:body resp) true)]
    body))

(defn reset-sim [seed]
  (println "\n=== RESETTING SIMULATION (seed:" seed ") ===")
  (let [resp (http/post (str backend-url "/sim/reset")
                        {:headers {"Content-Type" "application/json"}
                         :body (json/generate-string {:seed seed :tree_density 0.08})
                         :throw false})
        body (json/parse-string (:body resp) true)]
    (println "Reset OK:" (:ok body))
    (Thread/sleep 500)
    (get-state)))

(defn analyze-visibility [state]
  (println "\n=== VISIBILITY ANALYSIS ===")
  (let [agents (:agents state)
        tiles (:tiles state)
        tile-count (count tiles)
        agent-count (count agents)]
    (println "Total agents:" agent-count)
    (println "Total tiles returned:" tile-count)
    
    (when (pos? agent-count)
      (let [first-agent (first agents)
            agent-pos (:pos first-agent)
            agent-id (:id first-agent)
            agent-role (:role first-agent)]
        (println "\nFirst agent:")
        (println "  ID:" agent-id)
        (println "  Role:" agent-role)
        (println "  Position:" agent-pos)
        
        (println "\nSample tiles around agent (should only be seen/visible):")
        (doseq [offset [[0 0] [1 0] [0 1] [-1 0] [0 -1] [2 2]]]
          (let [q (+ (first agent-pos) (first offset))
                r (+ (second agent-pos) (second offset))
                pos-key (str q "," r)
                tile (get tiles (vec [q r]))]
            (if tile
              (let [has-seen (:seen? tile)
                    has-visible (:visible? tile)
                    terrain (:terrain tile)
                    biome (:biome tile)]
                (println (format "  Tile [%d %d]: terrain=%s biome=%s seen?=%s visible?=%s"
                              q r terrain biome has-seen has-visible)))
              (println (format "  Tile [%d %d]: NOT RETURNED (good!)" q r)))))))))

(defn check-agent-memory [state]
  (println "\n=== AGENT MEMORY ANALYSIS ===")
  (doseq [agent (take 2 (:agents state))]
    (println "\nAgent" (:id agent) ":" (:name agent))
    (let [pos (:pos agent)
          memory (:memory agent)
          seen-count (when memory (count (:seen memory)))
          vision-radius (:vision-radius agent 10)]
      (println "  Position:" pos)
      (println "  Vision radius:" vision-radius)
      (if memory
        (do
          (println "  Seen tiles count:" seen-count)
          (when (and memory (:seen memory))
            (let [sample-seen (take 5 (:seen memory))]
              (println "  Sample seen positions:" sample-seen))))
        (println "  No memory data")))))

(defn tick-and-analyze [n]
  (println "\n=== TICKING" n "TIMES ===")
  (dotimes [_ n]
    (http/post (str backend-url "/sim/tick")
               {:headers {"Content-Type" "application/json"}
                :body (json/generate-string {:n 1})
                :throw false})
    (Thread/sleep 50))
  (let [state (get-state)]
    (println "Tick after advance:" (:tick state))
    state))

(defn -main []
  (println "=== FOG OF WAR VISIBILITY TEST ===")
  
  (let [initial-state (reset-sim 42)]
    (analyze-visibility initial-state)
    (check-agent-memory initial-state))
  
  (let [state (tick-and-analyze 5)]
    (analyze-visibility state)
    (check-agent-memory state))
  
  (let [state (tick-and-analyze 20)]
    (analyze-visibility state)
    (check-agent-memory state))
  
  (println "\n=== TEST COMPLETE ==="))

(-main)
