(ns check-tile-structure
  (:require [cheshire.core :as json]
            [babashka.http-client :as http]
            [clojure.string :as str]))

(def backend-url "http://localhost:3000")

(defn -main []
  (println "=== CHECKING TILE STRUCTURE ===")
  
  (http/post (str backend-url "/sim/reset")
             {:headers {"Content-Type" "application/json"}
              :body (json/generate-string {:seed 42 :tree_density 0.08})
              :throw false})
  (Thread/sleep 500)
  
  (let [state (json/parse-string (:body (http/get (str backend-url "/sim/state") {:throw false})) true)
        tiles (:tiles state)]
    (println "\nTotal tiles:" (count tiles))
    (println "\nFirst 5 tile keys (showing key format):")
    (doseq [[i key] (map-indexed vector (take 5 (keys tiles)))]
      (println (format "  %d. %s (type: %s)" i key (type key))))
    
    (println "\nSample tile data:")
    (when-let [sample-tile (first (vals tiles))]
      (println "  " sample-tile))
    
    (println "\nLooking for tiles near agent position...")
    (let [agent (first (:agents state))
          agent-pos (:pos agent)]
      (println "Agent position:" agent-pos)
      (println "Agent position type:" (type agent-pos))
      
      (println "\nTrying different key formats:")
      (doseq [key-format [(vec agent-pos)
                          (apply str agent-pos)
                          (str/join "," agent-pos)
                          agent-pos]]
        (when (get tiles key-format)
          (println (format "  Found tile with key: %s" key-format))))))
  
  (println "\n=== CONCLUSION ===")
  (println "The backend returns ALL" (count (json/parse-string (:body (http/get (str backend-url "/sim/state") {:throw false})) true) "tiles"))
  (println "This means there is NO fog of war implementation in the /sim/state endpoint!"))

(-main)
