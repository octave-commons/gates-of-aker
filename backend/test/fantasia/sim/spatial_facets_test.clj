(ns fantasia.sim.spatial-facets-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.spatial_facets :as sf]
            [fantasia.sim.memories :as mem]))

(deftest test-init-entity-facets
  (testing "Initialize entity facets registry"
    (let [state {:entities []
                  :embeddings {}
                  :entity-facet-registry {}}
          state' (sf/init-entity-facets! state)]
      (is (contains? state' :entity-facet-registry))
      (is (> (count (:entity-facet-registry state')) 0)))))

(deftest test-cosine-similarity-identical
  (testing "Cosine similarity of identical vectors is 1"
    (let [v1 [1.0 0.0 0.0]
          v2 [1.0 0.0 0.0]
          sim (sf/cosine-similarity v1 v2)]
      (is (= 1.0 sim)))))

(deftest test-create-memory
  (testing "Create a new memory"
    (let [world {:tick 100 :memories {}}
          world' (mem/create-memory! world
                                    :memory/danger
                                    [5 10]
                                    0.8
                                    "agent-1"
                                    ["wolf" "threat"])]
      (is (= 101 (:tick world')))
      (is (contains? (:memories world') :id)))))

(deftest test-collect-memory-facets
  (testing "Collect facets from memories"
    (let [memories {"mem-1" {:location [0 0] :facets ["danger" "wolf"]}
                     "mem-2" {:location [5 0] :facets ["safe" "campfire"]}}
          agent-pos [2 0]
          facets (sf/collect-memory-facets! memories agent-pos 10)]
      (is (vector? facets)))))
