(ns fantasia-tui
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [babashka.http-client :as http]))

(defonce *state (atom {:tick 0
                       :running? false
                       :agents []
                       :last-event nil
                       :traces []
                       :connection-status "disconnected"
                       :world-state nil
                       :view-mode :default
                       :view-offset [95 5]
                       :view-width 20
                       :view-height 15}))

(def backend-url "http://localhost:3000")

(defn clear-screen []
  (println "\033[H\033[J")
  (flush))

(defn connect-to-backend []
  (try
    (let [resp (http/get (str backend-url "/healthz") {:throw false})
          status (:status resp)]
      (if (= status 200)
        (swap! *state assoc :connection-status "connected")
        (swap! *state assoc :connection-status "error")))
    (catch Exception e
      (swap! *state assoc :connection-status "error"))))

(defn reset-simulation []
  (try
    (let [resp (http/post (str backend-url "/sim/reset")
                          {:headers {"Content-Type" "application/json"}
                           :body (json/generate-string {:seed 1 :tree_density 0.08})
                           :throw false})
          body (json/parse-string (:body resp) true)]
      (when (:ok body)
        (swap! *state assoc :tick 0 :traces [])))
    (catch Exception e
      (println "Error resetting simulation:" (.getMessage e)))))

(defn tick-simulation [n]
  (try
    (let [resp (http/post (str backend-url "/sim/tick")
                          {:headers {"Content-Type" "application/json"}
                           :body (json/generate-string {:n n})
                           :throw false})
          body (json/parse-string (:body resp) true)]
      (when-let [last-result (:last body)]
        (swap! *state assoc
               :tick (get-in last-result [:tick] 0)
               :traces (concat (:traces @*state) (get-in last-result [:traces] []))
               :last-event (get-in last-result [:event])
               :agents (get-in last-result [:snapshot :agents] []))))
    (catch Exception e
      (println "Error ticking simulation:" (.getMessage e)))))

(defn start-simulation []
  (try
    (let [resp (http/post (str backend-url "/sim/run") {:throw false})
          body (json/parse-string (:body resp) true)]
      (when (:ok body)
        (swap! *state assoc :running? true)))
    (catch Exception e
      (println "Error starting simulation:" (.getMessage e)))))

(defn pause-simulation []
  (try
    (let [resp (http/post (str backend-url "/sim/pause") {:throw false})
          body (json/parse-string (:body resp) true)]
      (when (:ok body)
        (swap! *state assoc :running? false)))
    (catch Exception e
      (println "Error pausing simulation:" (.getMessage e)))))

(defn get-world-state []
  (try
    (let [resp (http/get (str backend-url "/sim/state") {:throw false})
          body (json/parse-string (:body resp) true)]
      (swap! *state assoc
             :tick (:tick body)
             :agents (:agents body)
             :world-state body))
    (catch Exception e
      (println "Error getting world state:" (.getMessage e)))))

(defn format-agent [agent]
  (let [name (or (:name agent) (str "Agent-" (:id agent)))
        pos (:pos agent)
        role (:role agent)
        current-job (:current-job agent)
        needs (:needs agent)
        food (:food needs "N/A")
        health (:health needs "N/A")]
    (if current-job
      (format "  %s (%s) @ %s | job: %s | food: %.2f | health: %.2f" 
              name role pos current-job food health)
      (format "  %s (%s) @ %s | idle | food: %.2f | health: %.2f" 
              name role pos food health))))

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
  (let [b (keyword (or biome ""))]
    (case b
      :forest "F"
      :plains "P"
      :swamp "S"
      :water "W"
      :mountain "M"
      :desert "D"
      :field "L"
      :rocky "R"
      ".")))

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

(defn axial-to-screen [[q r]]
  [(+ (* 3 q) 0)
   (+ (* 2 r) (if (odd? q) 1 0))])

(defn draw-hex-row [row-q world-state offset-q offset-r]
  (let [cols (:view-width @*state)
        row-str (StringBuilder.)
        padding (if (odd? (+ row-q offset-q)) "  " "")]
    (.append row-str padding)
    (dotimes [col cols]
      (let [q (+ col row-q offset-q)
            r (+ col offset-r)
            tile-key (keyword (str "[" q " " r "]"))
            tile (get-in world-state [:tiles tile-key])
            agents-at (filter #(= [q r] (:pos %)) (:agents world-state))]
        (if tile
          (.append row-str (format "[%s] " (tile-to-string tile agents-at)))
          (.append row-str "[   ] "))))
    (println (.toString row-str))))

(defn draw-hex-grid []
  (let [world-state (:world-state @*state)
        [offset-q offset-r] (:view-offset @*state)
        rows (:view-height @*state)]
    (println "\n=== HEX MAP VIEW ===")
    (println (format "Offset: [%d, %d] | j/l: pan left/right | k/i: pan up/down | h: toggle view" offset-q offset-r))
    (dotimes [row rows]
      (draw-hex-row row world-state offset-q offset-r))))

(defn draw-header []
  (let [{:keys [tick running? connection-status view-mode]} @*state
        status (if running? "[RUNNING]" "[PAUSED]")]
    (println "===============================================")
    (println (format "Fantasia TUI - Tick: %d %s | Connection: %s | View: %s" tick status connection-status (name view-mode)))
    (if (= view-mode :hex)
      (println "q: quit | r: reset | space: start/pause | t: tick | h: toggle view | j/l/k/i: pan")
      (println "q: quit | r: reset | space: start/pause | t: tick | s: status | h: toggle hex view"))
    (println "===============================================")))

(defn draw-agents []
  (let [agents (:agents @*state)]
    (println "\n=== AGENTS ===")
    (if (empty? agents)
      (println "  No agents")
      (doseq [agent (take 10 agents)]
        (println (format-agent agent))))))

(defn draw-events []
  (let [traces (:traces @*state)
        last-event (:last-event @*state)]
    (println "\n=== RECENT EVENTS ===")
    (when last-event
      (println (format "  Event: %s" (:message last-event))))
    (doseq [trace (take 5 (reverse traces))]
      (println (format "  - %s" (:message trace))))))

(defn draw-ui []
  (clear-screen)
  (draw-header)
  (if (= (:view-mode @*state) :hex)
    (draw-hex-grid)
    (do
      (draw-agents)
      (draw-events)))
  (println "\nPress any key to refresh..."))

(defn pan-view [dq dr]
  (let [[q r] (:view-offset @*state)]
    (swap! *state assoc :view-offset [(+ q dq) (+ r dr)])))

(defn handle-input [input]
  (case input
    "q" (System/exit 0)
    "r" (do (reset-simulation) (get-world-state))
    " " (if (:running? @*state)
           (pause-simulation)
           (start-simulation))
    "t" (do (tick-simulation 1) (get-world-state))
    "s" (get-world-state)
    "h" (swap! *state update :view-mode #(if (= % :hex) :default :hex))
    "j" (pan-view -1 0)
    "l" (pan-view 1 0)
    "k" (pan-view 0 -1)
    "i" (pan-view 0 1)
    nil))

(defn -main [& _args]
  (println "Starting Fantasia TUI client...")
  (connect-to-backend)
  (get-world-state)
  
  (loop []
    (draw-ui)
    (when-let [input (read-line)]
      (handle-input input)
      (recur))))

(-main)
