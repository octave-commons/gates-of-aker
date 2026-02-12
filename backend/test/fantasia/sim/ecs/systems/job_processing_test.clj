(ns fantasia.sim.ecs.systems.job-processing-test
  (:require [brute.entity :as be]
             [clojure.test :refer [deftest testing is]]
             [fantasia.sim.ecs.components :as c]
             [fantasia.sim.ecs.core :as ecs]
             [fantasia.sim.ecs.systems.job-processing :as jp]
             [fantasia.sim.hex :as hex]
             [fantasia.sim.test-helpers :as helpers]))

(defn- tile-at
  [world pos]
  (let [tile-index-type (ecs/component-class (c/->TileIndex 0 0))
        tile-type (ecs/component-class (c/->Tile :ground :plains nil nil))
        tile-id (some (fn [entity-id]
                        (let [index (be/get-component world entity-id tile-index-type)
                              tile (be/get-component world entity-id tile-type)]
                          (when (and index tile (= [(:q index) (:r index)] pos))
                            entity-id)))
                      (be/get-all-entities world))]
    (when tile-id
      (be/get-component world tile-id tile-type))))

(defn- queue-jobs
  [world building-id]
  (let [queue (be/get-component world building-id (ecs/component-class (c/->JobQueue {} [] {})))]
    (:jobs queue {})))

(deftest test-job-assignment-component
  (testing "JobAssignment component has correct structure"
    (let [job (c/->JobAssignment "job-123" 0.5)]
      (is (= "job-123" (:job-id job)))
      (is (= 0.5 (:progress job))))))

(deftest test-process-job-for-agent
  (testing "Increments progress when adjacent to target"
    (let [world (helpers/create-test-world)
          [agent-id world] (helpers/create-test-agent world :priest 0 0)
          job-assignment (c/->JobAssignment "job-123" 0.5)
          world' (be/add-component world agent-id job-assignment)]
      (is (some? (be/get-component world' agent-id (ecs/component-class job-assignment))))))
  (testing "Progress increments by 0.1 each tick"
    (let [job (c/->JobAssignment "job-123" 0.5)]
      (is (= 0.5 (:progress job)))))
  (testing "Completes job when progress reaches 1.0"
    (let [job (c/->JobAssignment "job-123" 0.9)]
      (is (< (:progress job) 1.0))))
  (testing "Job at progress 1.0 is complete"
    (let [job (c/->JobAssignment "job-123" 1.0)]
      (is (= 1.0 (:progress job))))))

(deftest test-job-system-process
  (testing "Processes all agents with JobAssignment component"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 1 0)
          job1 (c/->JobAssignment "job-1" 0.5)
          job2 (c/->JobAssignment "job-2" 0.3)
          job-type (ecs/component-class job1)
          world' (-> world
                       (be/add-component agent1 job1)
                       (be/add-component agent2 job2))]
      (is (some? (be/get-component world' agent1 job-type)))
      (is (some? (be/get-component world' agent2 job-type)))))
  (testing "Skips agents without jobs"
    (let [world (helpers/create-test-world)
          [agent1 world] (helpers/create-test-agent world :priest 0 0)
          [agent2 world] (helpers/create-test-agent world :knight 1 0)
          job (c/->JobAssignment "job-1" 0.5)
          job-type (ecs/component-class job)
          world' (be/add-component world agent1 job)]
      (is (some? (be/get-component world' agent1 job-type)))
      (is (nil? (be/get-component world' agent2 job-type)))))
  (testing "Handles empty world"
    (let [world (helpers/create-test-world)
          global-state {:tick 10}
          result (jp/process world global-state)]
      (is (some? result)))))

(deftest test-chop-tree-completion-drops-logs
  (testing "Completing chop-tree removes tree at target and drops nearby logs"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_tile-k _tile-id world2] (ecs/create-tile world1 2 0 :ground :plains nil :tree)
          [_n1-k _n1-id world3] (ecs/create-tile world2 3 0 :ground :plains nil nil)
          [_n2-k _n2-id world4] (ecs/create-tile world3 2 -1 :ground :plains nil nil)
          [_agent-id world5] (helpers/create-test-agent world4 :peasant 1 0)
          agent-id _agent-id
          job-id "chop-1"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/chop-tree
                                       :target-pos [2 0]
                                       :target [2 0]
                                       :priority 60
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world6 (-> world5
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world6 {:tick 30})
          target-tile (tile-at result [2 0])
          dropped-log? (some (fn [pos]
                               (= :log (:resource (tile-at result pos))))
                             (conj (vec (hex/neighbors [2 0])) [2 0]))
          completed-job (get (queue-jobs result building-id) job-id)]
      (is (not= :tree (:resource target-tile)))
      (is dropped-log?)
      (is (nil? completed-job)))))

(deftest test-eat-completion-prefers-fruit-tile
  (testing "Completing eat job consumes fruit tile and restores food"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_tile-k _tile-id world2] (ecs/create-tile world1 3 0 :ground :plains nil :fruit)
          [_agent-id world3] (helpers/create-test-agent world2 :peasant 2 0
                                                         {:needs (c/->Needs 0.8 0.1 0.6 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)})
          agent-id _agent-id
          job-id "eat-1"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/eat
                                       :target-pos [3 0]
                                       :target [3 0]
                                       :priority 90
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world4 (-> world3
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world4 {:tick 31})
          needs (be/get-component result agent-id (ecs/component-class (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))
          target-tile (tile-at result [3 0])]
      (is (nil? (:resource target-tile)))
      (is (> (:food needs) 0.1)))))

(deftest test-eat-completion-falls-back-to-fruit-stockpile
  (testing "Completing eat job consumes fruit from stockpile when no fruit tile exists"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_stockpile-k stockpile-id world2] (ecs/create-tile world1 4 0 :ground :plains :stockpile nil)
          [_agent-id world3] (helpers/create-test-agent world2 :peasant 3 0)
          agent-id _agent-id
          job-id "eat-2"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/eat
                                       :target-pos [4 0]
                                       :target [4 0]
                                       :priority 90
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world4 (-> world3
                     (be/add-component building-id queue)
                     (be/add-component stockpile-id (c/->Stockpile {:fruit 2}))
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world4 {:tick 32})
          stockpile (be/get-component result stockpile-id (ecs/component-class (c/->Stockpile {})))]
      (is (= 1 (get (:contents stockpile) :fruit))))))

(deftest test-build-structure-completion-creates-harvest-stockpile
  (testing "Completing build-structure for orchard marks structure and creates fruit stockpile"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_tile-k _tile-id world2] (ecs/create-tile world1 6 0 :ground :plains nil nil)
          [_agent-id world3] (helpers/create-test-agent world2 :peasant 5 0)
          agent-id _agent-id
          job-id "build-1"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/build-structure
                                       :structure :orchard
                                       :target-pos [6 0]
                                       :target [6 0]
                                       :priority 40
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world4 (-> world3
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world4 {:tick 33})
          target-tile (tile-at result [6 0])
          target-id (some (fn [entity-id]
                            (let [index (be/get-component result entity-id (ecs/component-class (c/->TileIndex 0 0)))]
                              (when (and index (= [(:q index) (:r index)] [6 0]))
                                entity-id)))
                          (be/get-all-entities result))
          stockpile (when target-id
                      (be/get-component result target-id (ecs/component-class (c/->Stockpile {}))))]
      (is (= :orchard (:structure target-tile)))
      (is (= 0 (get (:contents stockpile) :fruit))))))

(deftest test-build-structure-completion-creates-mapped-stockpiles
  (testing "Completing harvest structure builds creates mapped stockpile resources"
    (doseq [[structure resource tick target] [[:lumberyard :log 34 [7 0]]
                                              [:granary :grain 35 [8 0]]
                                              [:quarry :stone 36 [9 0]]]]
      (let [world0 (helpers/create-test-world)
            [building-id world1] (helpers/create-test-building world0 [0 0] :house)
            [_tile-k _tile-id world2] (ecs/create-tile world1 (first target) (second target) :ground :plains nil nil)
            [_agent-id world3] (helpers/create-test-agent world2 :peasant (dec (first target)) (second target))
            agent-id _agent-id
            job-id (str "build-" (name structure))
            queue (c/->JobQueue {job-id {:id job-id
                                         :type :job/build-structure
                                         :structure structure
                                         :target-pos target
                                         :target target
                                         :priority 40
                                         :state :claimed
                                         :worker-id agent-id}}
                                []
                                {job-id agent-id})
            world4 (-> world3
                       (be/add-component building-id queue)
                       (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
            result (jp/process world4 {:tick tick})
            target-tile (tile-at result target)
            target-id (some (fn [entity-id]
                              (let [index (be/get-component result entity-id (ecs/component-class (c/->TileIndex 0 0)))]
                                (when (and index (= [(:q index) (:r index)] target))
                                  entity-id)))
                            (be/get-all-entities result))
            stockpile (when target-id
                        (be/get-component result target-id (ecs/component-class (c/->Stockpile {}))))]
        (is (= structure (:structure target-tile)))
        (is (= 0 (get (:contents stockpile) resource)))))))

(deftest test-completed-job-is-pruned-from-queue
  (testing "Completed jobs are removed so queue does not grow indefinitely"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_agent-id world2] (helpers/create-test-agent world1 :peasant 1 0)
          agent-id _agent-id
          job-id "chop-prune"
          [_tree-k _tree-id world3] (ecs/create-tile world2 2 0 :ground :plains nil :tree)
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/chop-tree
                                       :target-pos [2 0]
                                       :target [2 0]
                                       :priority 60
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world4 (-> world3
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world4 {:tick 70})
          jobs (queue-jobs result building-id)]
      (is (nil? (get jobs job-id))))))

(deftest test-rest-job-restores-sleep
  (testing "Rest jobs increase sleep and then complete"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_agent-id world2] (helpers/create-test-agent world1 :peasant 0 0
                                                         {:needs (c/->Needs 0.8 0.8 0.1 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)})
          agent-id _agent-id
          job-id "rest-1"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/rest
                                       :target-pos [0 0]
                                       :target [0 0]
                                       :priority 95
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world3 (-> world2
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world3 {:tick 71})
          needs (be/get-component result agent-id (ecs/component-class (c/->Needs 0.6 0.7 0.7 1.0 0.8 0.6 0.5 0.5 0.5 0.6 0.5 0.5 0.5)))]
      (is (> (:sleep needs) 0.1)))))

(deftest test-workshop-build-creates-functional-job-provider
  (testing "Building a workshop adds JobQueue so it can participate in simulation"
    (let [world0 (helpers/create-test-world)
          [building-id world1] (helpers/create-test-building world0 [0 0] :house)
          [_tile-k _tile-id world2] (ecs/create-tile world1 4 0 :ground :plains nil nil)
          [_agent-id world3] (helpers/create-test-agent world2 :peasant 3 0)
          agent-id _agent-id
          job-id "build-workshop"
          queue (c/->JobQueue {job-id {:id job-id
                                       :type :job/build-structure
                                       :structure :workshop
                                       :target-pos [4 0]
                                       :target [4 0]
                                       :priority 40
                                       :state :claimed
                                       :worker-id agent-id}}
                              []
                              {job-id agent-id})
          world4 (-> world3
                     (be/add-component building-id queue)
                     (be/add-component agent-id (c/->JobAssignment job-id 0.8)))
          result (jp/process world4 {:tick 72})
          target-id (some (fn [entity-id]
                            (let [index (be/get-component result entity-id (ecs/component-class (c/->TileIndex 0 0)))]
                              (when (and index (= [(:q index) (:r index)] [4 0]))
                                entity-id)))
                          (be/get-all-entities result))
          queue-component (when target-id
                            (be/get-component result target-id (ecs/component-class (c/->JobQueue {} [] {}))))]
      (is (some? queue-component)))))
