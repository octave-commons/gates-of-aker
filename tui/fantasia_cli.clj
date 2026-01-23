(ns fantasia-cli.core
  (:require [clojure.string :as str]
            [cheshire.core :as json]
            [babashka.http-client :as http]))

(defonce *state (atom {:tick 0
                       :running? false
                       :agents []
                       :tiles {}
                       :traces []
                       :last-event nil
                       :connection-status "disconnected"
                       :view :grid
                       :cursor-pos [0 0]
                       :zoom 1}))

(def backend-url "http://localhost:3000")

(def tile-chars
  {:ground "."
   :forest "T"
   :water "~"
   :mountain "^"
   :wall "#"
   :house "H"
   :campfire "C"
   :shrine "S"
   :stockpile "="
   :warehouse "W"
   :statue/dog "D"
   :agent "?"})

(defn clear-screen []
  (println "\033[H\033[J")
  (flush))

(defn set-color [color]
  (case color
    :red (print "\033[31m")
    :green (print "\033[32m")
    :yellow (print "\033[33m")
    :blue (print "\033[34m")
    :magenta (print "\033[35m")
    :cyan (print "\033[36m")
    :white (print "\033[37m")
    :bright-red (print "\033[91m")
    :bright-green (print "\033[92m")
    :bright-yellow (print "\033[93m")
    :bright-blue (print "\033[94m")
    :reset (print "\033[0m")))

(defn get-state []
  (try
    (let [resp (http/get (str backend-url "/sim/state") {:throw false})
          status (:status resp)]
      (if (= status 200)
        (let [body (json/parse-string (:body resp) true)]
          (swap! *state assoc
                 :tick (:tick body)
                 :agents (:agents body)
                 :tiles (:tiles body)
                 :connection-status "connected"
                 :traces []
                 :last-event nil)
          body)
        (swap! *state assoc :connection-status "error")))
    (catch Exception e
      (println "Error getting state:" (.getMessage e))
      (swap! *state assoc :connection-status "error"))))

(defn connect []
  (try
    (let [resp (http/get (str backend-url "/healthz") {:throw false})
          status (:status resp)]
      (if (= status 200)
        (swap! *state assoc :connection-status "connected")
        (swap! *state assoc :connection-status "error")))
    (catch Exception e
      (swap! *state assoc :connection-status "error"))))

(defn tick [n]
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
               :agents (get-in last-result [:snapshot :agents] []))
        (get-state)))
    (catch Exception e
      (println "Error ticking:" (.getMessage e)))))

(defn reset []
  (try
    (let [resp (http/post (str backend-url "/sim/reset")
                          {:headers {"Content-Type" "application/json"}
                           :body (json/generate-string {:seed 1 :tree_density 0.08})
                           :throw false})
          body (json/parse-string (:body resp) true)]
      (when (:ok body)
        (swap! *state assoc :tick 0 :traces [])
        (get-state)))
    (catch Exception e
      (println "Error resetting:" (.getMessage e)))))

(defn start []
  (try
    (let [resp (http/post (str backend-url "/sim/run") {:throw false})
          body (json/parse-string (:body resp) true)]
      (when (:ok body)
        (swap! *state assoc :running? true)))
    (catch Exception e
      (println "Error starting:" (.getMessage e)))))

(defn pause []
  (try
    (let [resp (http/post (str backend-url "/sim/pause") {:throw false})
          body (json/parse-string (:body resp) true)]
      (when (:ok body)
        (swap! *state assoc :running? false)))
    (catch Exception e
      (println "Error pausing:" (.getMessage e)))))

(defn get-tile-char [tile agent-at-pos]
  (cond
    agent-at-pos "?"
    (:structure tile) (get tile-chars (:structure tile) "#")
    (:terrain tile) (get tile-chars (:terrain tile) ".")
    :else "."))

(defn draw-header []
  (let [{:keys [tick running? connection-status view cursor-pos]} @*state
        status (if running? "RUNNING" "PAUSED")]
    (set-color :bright-blue)
    (print (format "Fantasia CLI - Tick: %d [%s] | View: %s | Connection: %s" tick status view connection-status))
    (set-color :reset)
    (println)
    (set-color :cyan)
    (println "Commands: help | view <grid|agents|events|state|tile> | tick <n> | reset | start | pause | quit")
    (set-color :reset)))

(defn draw-grid []
  (let [{:keys [tiles agents cursor-pos zoom]} @*state
        agent-positions (into {} (map (fn [a] [(:pos a) a]) agents))
        view-width 40
        view-height 20
        center-x (first cursor-pos)
        center-y (second cursor-pos)]
    (println)
    (println "=== HEX GRID ===")
    (println (format "Cursor: [%d %d] | Zoom: %dx" center-x center-y zoom))
    (println)
    (doseq [r (range center-y (+ center-y view-height))]
      (set-color :white)
      (print (format "%2d " r))
      (doseq [q (range center-x (+ center-x view-width))]
        (let [pos [q r]
              tile (get tiles (vec pos))
              agent (get agent-positions pos)
              char (get-tile-char tile agent)]
          (if agent
            (set-color :bright-yellow)
            (set-color :reset))
          (print char)
          (set-color :reset)))
      (println))))

(defn draw-agents [limit]
  (let [agents (:agents @*state)]
    (println)
    (println "=== AGENTS ===")
    (doseq [[idx agent] (map-indexed vector (take limit agents))]
      (let [name (or (:name agent) (str "Agent-" (:id agent)))
            pos (:pos agent)
            role (:role agent)
            current-job (:current-job agent)
            needs (:needs agent)
            food (:food needs "N/A")
            health (:health needs "N/A")]
        (if current-job
          (println (format "  %d. %s (%s) @ %s | job: %s | food: %.2f | health: %.2f"
                        idx name role pos current-job food health))
          (println (format "  %d. %s (%s) @ %s | idle | food: %.2f | health: %.2f"
                        idx name role pos food health)))))))

(defn draw-events [limit]
  (let [traces (:traces @*state)
        last-event (:last-event @*state)]
    (println)
    (println "=== RECENT EVENTS ===")
    (when last-event
      (set-color :bright-yellow)
      (println (format "  Event: %s" (:message last-event)))
      (set-color :reset))
    (doseq [[idx trace] (map-indexed vector (take limit (reverse traces)))]
      (let [message (:message trace)]
        (set-color :green)
        (print (format "  %d. " idx))
        (set-color :reset)
        (println message)))))

(defn draw-state []
  (let [{:keys [tick running? connection-status]} @*state
        agents (:agents @*state)
        tiles (:tiles @*state)]
    (println)
    (println "=== SIMULATION STATE ===")
    (println (format "  Tick: %d" tick))
    (println (format "  Running: %s" running?))
    (println (format "  Connection: %s" connection-status))
    (println (format "  Agents: %d" (count agents)))
    (println (format "  Tiles: %d" (count tiles)))))

(defn draw-tile-info [pos-str]
  (let [[q r] (mapv #(Long/parseLong %) (str/split pos-str #"[,\s]+"))
        {:keys [tiles agents]} @*state
        tile (get tiles (vec [q r]))
        agent-at-pos (first (filter #(= (:pos %) [q r]) agents))]
    (println)
    (println "=== TILE INFO ===")
    (println (format "  Position: [%d %d]" q r))
    (if tile
      (do
        (println (format "  Terrain: %s" (:terrain tile)))
        (println (format "  Biome: %s" (:biome tile)))
        (when-let [structure (:structure tile)]
          (println (format "  Structure: %s" structure)))
        (when-let [stockpile (:stockpile tile)]
          (println (format "  Stockpile: %s" stockpile))))
      (println "  No tile data"))
    (when agent-at-pos
      (println)
      (set-color :bright-yellow)
      (println (format "  Agent: %s (%s)" (:name agent-at-pos) (:role agent-at-pos)))
      (set-color :reset)
      (println (format "    ID: %d" (:id agent-at-pos)))
      (let [needs (:needs agent-at-pos)]
        (println (format "    Food: %.2f | Health: %.2f | Warmth: %.2f"
                      (:food needs) (:health needs) (:warmth needs)))))))

(defn draw-help []
  (println)
  (println "=== COMMANDS ===")
  (println)
  (println "Navigation:")
  (println "  view <grid|agents|events|state|tile> - Switch view mode")
  (println "  tile <q,r> - Show tile info at position")
  (println)
  (println "Simulation Control:")
  (println "  tick [n] - Advance simulation (default 1)")
  (println "  start - Start continuous simulation")
  (println "  pause - Pause simulation")
  (println "  reset - Reset simulation")
  (println)
  (println "Information:")
  (println "  agents - Show agents")
  (println "  events - Show events")
  (println "  state - Show simulation state")
  (println)
  (println "Grid View Navigation:")
  (println "  hjkl - Move cursor")
  (println "  +/- - Zoom in/out")
  (println)
  (println "Utility:")
  (println "  help - Show this help")
  (println "  quit - Exit CLI"))

(defn handle-command [input]
  (let [parts (str/split input #"\s+")
        cmd (first parts)
        args (rest parts)]
    (cond
      (= cmd "q") :quit
      (= cmd "quit") :quit
      (= cmd "?") :help
      (= cmd "help") :help
      
      (= cmd "view")
      (if-let [view-mode (first args)]
        (do (swap! *state assoc :view (keyword view-mode)) :redraw)
        (do (println "Please specify a view: grid, agents, events, state, tile") :redraw))
      
      (= cmd "tick")
      (let [n (if (empty? args) 1 (Long/parseLong (first args)))]
        (tick n)
        :redraw)
      
      (= cmd "reset")
      (do (reset) :redraw)
      
      (= cmd "start")
      (do (start) :redraw)
      
      (= cmd "pause")
      (do (pause) :redraw)
      
      (= cmd "agents")
      (do (swap! *state assoc :view :agents) :redraw)
      
      (= cmd "events")
      (do (swap! *state assoc :view :events) :redraw)
      
      (= cmd "state")
      (do (swap! *state assoc :view :state) :redraw)
      
      (= cmd "tile")
      (if-let [pos-str (first args)]
        (do (swap! *state assoc :view :tile) (swap! *state assoc :tile-query pos-str) :redraw)
        (do (println "Usage: tile <q,r>") :redraw))
      
      (= cmd "h")
      (let [[x y] (:cursor-pos @*state)]
        (swap! *state assoc :cursor-pos [(dec x) y])
        :redraw)
      
      (= cmd "j")
      (let [[x y] (:cursor-pos @*state)]
        (swap! *state assoc :cursor-pos [x (inc y)])
        :redraw)
      
      (= cmd "k")
      (let [[x y] (:cursor-pos @*state)]
        (swap! *state assoc :cursor-pos [x (dec y)])
        :redraw)
      
      (= cmd "l")
      (let [[x y] (:cursor-pos @*state)]
        (swap! *state assoc :cursor-pos [(inc x) y])
        :redraw)
      
      (= cmd "+")
      (let [zoom (:zoom @*state)]
        (swap! *state assoc :zoom (min 4 (inc zoom)))
        :redraw)
      
      (= cmd "-")
      (let [zoom (:zoom @*state)]
        (swap! *state assoc :zoom (max 1 (dec zoom)))
        :redraw)
      
      :else :redraw)))

(defn draw-ui []
  (clear-screen)
  (draw-header)
  (case (:view @*state)
    :grid (draw-grid)
    :agents (draw-agents 20)
    :events (draw-events 20)
    :state (draw-state)
    :tile (when-let [pos-str (:tile-query @*state)] (draw-tile-info pos-str)))
  (println)
  (print ">>> " (flush)))

(defn -main [& _args]
  (println "Fantasia CLI - Ultimate Debugging Tool")
  (connect)
  (get-state)
  
  (loop []
    (draw-ui)
    (when-let [input (read-line)]
      (case (handle-command input)
        :quit (do (println "Goodbye!") (System/exit 0))
        :redraw (recur)
        nil (recur)))))

(-main)
