(ns fantasia.sim.spatial-test
  (:require [clojure.test :refer [deftest is]]
            [fantasia.sim.spatial :as spatial]))

(deftest in-bounds-and-distance
  (is (spatial/in-bounds? [5 5] [0 0]))
  (is (not (spatial/in-bounds? [5 5] [5 0])))
  (is (= 4 (spatial/manhattan [0 0] [1 3]))))

(deftest neighbor-ordering
  (is (= [[2 1] [0 1] [1 2] [1 0]]
         (spatial/neighbors [1 1]))))

(deftest shrine-and-tree-detection
  (let [world {:trees #{[1 1]} :shrine [2 2]}]
    (is (spatial/at-trees? world [1 1]))
    (is (not (spatial/at-trees? world [0 0])))
    (is (spatial/near-shrine? world [3 2]))
    (is (not (spatial/near-shrine? world [6 6])))))

(deftest move-agent-stays-deterministic
  (let [world {:size [3 3] :tick 5 :seed 42}
        agent {:id 2 :pos [1 1]}
        moved (spatial/move-agent world agent)]
    (is (spatial/in-bounds? (:size world) (:pos moved)))
    (is (not= (:pos agent) (:pos moved)))))
