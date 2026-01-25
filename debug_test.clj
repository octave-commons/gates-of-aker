(require '[brute.entity :as be]
          '[fantasia.sim.ecs.components :as c])

(let [system (be/create-system)
      eid 1
      step1 (be/add-entity system eid)
      step2 (be/add-component step1 eid (c/->Position 5 10))
      step3 (be/add-component step2 eid (c/->Role :peasant))]
  (println "Final system:" step3))