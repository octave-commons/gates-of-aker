(ns fantasia.sim.tick.actions
  (:require [fantasia.sim.hex :as hex]
            [fantasia.sim.jobs :as jobs]
            [fantasia.sim.tick.core :as core]))

(defn assign-build-wall-job!
   "Manually assign a build-wall job to an agent at a specific position."
   [agent-id pos]
   (let [world (core/get-state)]
     (when (get-in world [:agents agent-id])
       (let [job (jobs/create-job :job/build-wall pos)]
         (swap! fantasia.sim.tick.core/*state jobs/assign-job! job agent-id)))))

(defn place-wall-ghost! [pos]
   (let [world (core/get-state)
         [q r] pos
         tile-key (vector q r)
         tile (get-in world [:tiles tile-key])]
     (when (and (hex/in-bounds? (:map world) pos)
                (nil? (:structure tile)))
       (swap! fantasia.sim.tick.core/*state assoc-in [:tiles tile-key]
              {:terrain :ground :structure :wall-ghost :resource nil}))))

(defn place-stockpile! [pos resource max-qty]
  (let [world (core/get-state)
        [q r] pos
        tile-key (vector q r)
        tile (get-in world [:tiles tile-key])]
    (when (and (hex/in-bounds? (:map world) pos)
               (nil? (:structure tile))
               (nil? (get-in world [:stockpiles tile-key])))
      (swap! fantasia.sim.tick.core/*state jobs/create-stockpile! pos resource (or max-qty 100)))))

(defn place-warehouse! [pos resource max-qty]
  (let [world (core/get-state)
        [q r] pos
        tile-key (vector q r)
        tile (get-in world [:tiles tile-key])]
    (when (and (hex/in-bounds? (:map world) pos)
               (nil? (:structure tile))
               (nil? (get-in world [:stockpiles tile-key])))
      (swap! fantasia.sim.tick.core/*state 
             (fn [w] 
               (-> w
                   (assoc-in [:tiles tile-key] {:terrain :ground :structure :warehouse :resource nil})
                   (jobs/create-stockpile! pos resource (or max-qty 100))))))))

(defn place-campfire! [pos]
  (let [world (core/get-state)
        [q r] pos
        tile-key (vector q r)
        tile (get-in world [:tiles tile-key])]
    (when (and (hex/in-bounds? (:map world) pos)
               (nil? (:structure tile)))
      (swap! fantasia.sim.tick.core/*state
             (fn [w]
               (-> w
                   (assoc :campfire pos)
                   (assoc-in [:tiles tile-key]
                             {:terrain :ground :structure :campfire :resource nil})))))))

(defn place-statue-dog! [pos]
  (let [world (core/get-state)
        [q r] pos
        tile-key (vector q r)
        tile (get-in world [:tiles tile-key])]
    (when (and (hex/in-bounds? (:map world) pos)
               (nil? (:structure tile)))
      (swap! fantasia.sim.tick.core/*state assoc-in [:tiles tile-key]
             {:terrain :ground :structure :statue/dog :resource nil}))))

(defn place-tree! [pos]
  (let [world (core/get-state)
        [q r] pos
        tile-key (vector q r)
        tile (get-in world [:tiles tile-key])]
    (when (and (hex/in-bounds? (:map world) pos)
               (nil? (:resource tile)))
      (swap! fantasia.sim.tick.core/*state assoc-in [:tiles tile-key]
             {:terrain :ground :structure nil :resource :tree}))))

(defn place-wolf! [pos]
  (let [world (core/get-state)]
    (when (hex/in-bounds? (:map world) pos)
      (swap! fantasia.sim.tick.core/*state core/spawn-wolf! pos))))

(defn place-bear! [pos]
  (let [world (core/get-state)]
    (when (hex/in-bounds? (:map world) pos)
      (swap! fantasia.sim.tick.core/*state core/spawn-bear! pos))))
