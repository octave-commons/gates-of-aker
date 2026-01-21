(ns fantasia.sim.social-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [fantasia.sim.core :as core]
            [fantasia.sim.social :as social]
            [fantasia.sim.spatial_facets :as spatial-facets]))

(deftest social-interaction-updates-relationships-and-memory
  (spatial-facets/init-entity-facets!)
  (let [world (core/initial-world 1)
        agent1 (assoc (first (:agents world)) :needs {:mood 0.5 :social 0.4})
        agent2 (assoc (second (:agents world)) :needs {:mood 0.5 :social 0.4})
        res (social/trigger-social-interaction! world agent1 agent2)
        world' (:world res)
        agent1' (:agent-1 res)
        rel (get-in agent1' [:relationships (:id agent2)])
        memories (vals (:memories world'))
        bond-facets (set (spatial-facets/get-entity-facets :memory/social-bond))
        conflict-facets (set (spatial-facets/get-entity-facets :memory/social-conflict))
        memory-facets (set (mapcat :facets memories))]
    (testing "relationship updates"
      (is (map? rel))
      (is (number? (:affinity rel)))
      (is (= (:tick world) (:last-interaction rel)))
      (is (> (get-in agent1' [:needs :social]) 0.4)))
    (testing "memory creation"
      (is (seq memories))
      (is (or (seq (set/intersection bond-facets memory-facets))
              (seq (set/intersection conflict-facets memory-facets)))))))
