(ns fantasia.sim.traces-test
  (:require [clojure.test :refer :all]
            [fantasia.sim.traces :as traces]))

(deftest create-trace-test
  (testing "Creating a basic trace"
    (let [trace (traces/create-trace "t-001"
                                     "culture-1"
                                     "evt-001"
                                     "The Night Fire"
                                     "Fire consumed the temple as lightning struck."
                                     [:fire :judgment :storm]
                                     :agent-1
                                     100
                                     0.75)]
      (is (= "t-001" (:trace/id trace)))
      (is (= "culture-1" (:culture-id trace)))
      (is (= [:fire :judgment :storm] (:facets trace)))
      (is (= 0.75 (:strength trace)))
      (is (= 100 (:created-at trace)))
      (is (= :agent-1 (:created-by trace)))
      (is (= 0.001 (:decay-rate trace)))
      (is (false? (:canonical? trace)))
      (is (= 0 (:read-count trace))))))

(deftest create-culture-test
  (testing "Creating a culture"
    (let [culture (traces/create-culture "culture-1"
                                       "The Northern Covenant"
                                       [:fire :winter :judgment]
                                       ["revelation" "covenant"])]
      (is (= "culture-1" (:culture/id culture)))
      (is (= "The Northern Covenant" (:name culture)))
      (is (= [:fire :winter :judgment] (:shared-facets culture)))
      (is (= ["revelation" "covenant"] (:trace-vocabulary culture)))
      (is (= [] (:trace-history culture)))
      (is (= 0 (:scribe-count culture)))
      (is (= 0.5 (:myth-density culture))))))

(deftest trace-has-facet-test
  (testing "Checking if trace has facet"
    (let [trace {:facets [:fire :judgment :storm]}]
      (is (traces/trace-has-facet? trace :fire))
      (is (traces/trace-has-facet? trace :judgment))
      (is (not (traces/trace-has-facet? trace :cold))))))

(deftest get-traces-by-culture-test
  (testing "Getting traces by culture"
    (let [world {:traces [{:trace/id "t-001" :culture-id "culture-1"}
                        {:trace/id "t-002" :culture-id "culture-2"}
                        {:trace/id "t-003" :culture-id "culture-1"}]}]
      (is (= 2 (count (traces/get-traces-by-culture world "culture-1"))))
      (is (= 1 (count (traces/get-traces-by-culture world "culture-2"))))))

(deftest decay-traces-test
  (testing "Decaying traces"
    (let [world {:traces [{:trace/id "t-001" :strength 0.5 :decay-rate 0.001 :canonical? false}
                        {:trace/id "t-002" :strength 0.8 :decay-rate 0.001 :canonical? true}
                        {:trace/id "t-003" :strength 0.3 :decay-rate 0.001 :canonical? false}]}
          decayed (traces/decay-traces! world)]
      (is (approx= 0.499 (:strength (first (:traces decayed))) 0.001))
      (is (= 0.8 (:strength (second (:traces decayed))))
      (is (approx= 0.299 (:strength (nth (:traces decayed) 2)) 0.001)))))

(defn approx= [a b epsilon]
  (<= (Math/abs (- a b)) epsilon))

(deftest canonicalize-trace-test
  (testing "Canonicalizing a trace"
    (let [world {:traces {"t-001" {:trace/id "t-001" :strength 0.75 :canonical? false :decay-rate 0.001}}
                  :cultures {"culture-1" {:canonical-traces []}}}
          canonicalized (traces/canonicalize-trace! world "culture-1" "t-001")]
      (is (true? (get-in canonicalized [:traces "t-001" :canonical?])))
      (is (= 0.0 (get-in canonicalized [:traces "t-001" :decay-rate])))
      (is (= ["t-001"] (get-in canonicalized [:cultures "culture-1" :canonical-traces])))))

(deftest read-trace-test
  (testing "Recording a trace read"
    (let [world {:traces {"t-001" {:trace/id "t-001" :read-count 0}}
                  :agents {1 {:events []}}
          with-read (traces/read-trace! world "t-001" 1)}]
      (is (= 1 (get-in with-read [:traces "t-001" :read-count])))
      (is (= 1 (count (get-in with-read [:agents 1 :events]))))))

(deftest trace-influence-on-facet-test
  (testing "Calculating trace influence on facet"
    (let [world {:traces [{:trace/id "t-001" :culture-id "culture-1" :facets [:fire :judgment] :strength 0.8}
                        {:trace/id "t-002" :culture-id "culture-1" :facets [:fire] :strength 0.5}
                        {:trace/id "t-003" :culture-id "culture-1" :facets [:winter] :strength 0.7}]}
          influence (traces/trace-influence-on-facet world "culture-1" :fire)]
      (is (< 0.1 influence))
      (is (> 0.3 influence)))))

(deftest add-trace-to-culture-test
  (testing "Adding a trace to culture"
    (let [world {:cultures {"culture-1" {:trace-history []}}
                  :traces {}}
          trace {:trace/id "t-001" :culture-id "culture-1"}
          updated (traces/add-trace-to-culture! world "culture-1" trace)]
      (is (= ["t-001"] (get-in updated [:cultures "culture-1" :trace-history])))
      (is (= trace (get-in updated [:traces "t-001"]))))
