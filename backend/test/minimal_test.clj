(ns fantasia.sim.minimal-test
  (:require [clojure.test :refer [deftest is]]
            [fantasia.sim.spatial :as spatial]
            [fantasia.sim.hex :as hex]))

(defn sample-rect-world []
  {:map {:kind :hex :layout :pointy :bounds {:shape :rect :w 10 :h 10 :origin [0 0]}})

(deftest passable-without-walls
  (is (spatial/passable? (sample-rect-world) [5 5])))
