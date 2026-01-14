(ns fantasia.sim.facets-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.sim.facets :as facets]))

(deftest clamp01-bounds-values
  (is (= 0.0 (facets/clamp01 -0.5)))
  (is (= 1.0 (facets/clamp01 2.0)))
  (is (= 0.42 (facets/clamp01 0.42))))

(deftest bump-facet-and-decay-frontier
  (let [frontier {}
        bumped (facets/bump-facet frontier :fire 0.4)
        decayed (facets/decay-frontier bumped {:decay 0.5 :drop-threshold 0.1})]
    (is (= {:a 0.4} (select-keys (get bumped :fire) [:a])))
    (is (= {:a 0.2} (select-keys (get decayed :fire) [:a])))))

(deftest seed-and-spread-step
  (let [frontier {}
        seeded (facets/seed frontier [:fire :trees] {:seed-strength 0.3})
        edges {[:fire :patron/fire] 0.5
               [:trees :forest] 0.8}
        {:keys [frontier deltas]} (facets/spread-step seeded edges {:spread-gain 0.6 :max-hops 1})]
    (is (contains? frontier :patron/fire))
    (is (contains? frontier :forest))
    (is (not (empty? deltas)))))

(deftest event-recall-computes-score
  (let [frontier {:fire {:a 0.8}
                  :patron/fire {:a 0.5}}
        signature {:fire 1.0 :patron/fire 0.25}
        {:keys [score recalled?]} (facets/event-recall frontier signature {:threshold 0.6})]
    (is (>= score 0.0))
    (is recalled?)))
