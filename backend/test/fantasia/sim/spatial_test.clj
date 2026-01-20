(ns fantasia.sim.spatial-test
  (:require [clojure.test :refer [deftest is]]
            [fantasia.sim.spatial :as spatial]))

(defn sample-world []
  {:map {:kind :hex :layout :pointy :bounds {:shape :rect :w 5 :h 5 :origin [0 0]}}})

(deftest in-bounds-and-distance
  (is (spatial/in-bounds? (sample-world) [0 0]))
  (is (not (spatial/in-bounds? (sample-world) [5 0])))
  (is (= 4 (spatial/manhattan [0 0] [1 3]))))

(deftest neighbor-ordering
  (is (= [[2 1] [0 1] [1 2] [1 0]]
         (spatial/neighbors [1 1]))))

(deftest shrine-and-tree-detection
  (let [world {:tiles {[1 1] {:resource :tree}} :shrine [2 2]}]
    (is (spatial/at-trees? world [1 1]))
    (is (not (spatial/at-trees? world [0 0])))
    (is (spatial/near-shrine? world [3 2]))
    (is (not (spatial/near-shrine? world [6 6])))))

(deftest move-agent-stays-deterministic
  (let [world {:map {:kind :hex :layout :pointy :bounds {:shape :rect :w 3 :h 3 :origin [0 0]}}
                :tick 5 :seed 42}
        agent {:id 2 :pos [1 1]}
        moved (spatial/move-agent world agent)]
    (is (spatial/in-bounds? world (:pos moved)))
    (is (not= (:pos agent) (:pos moved)))))
