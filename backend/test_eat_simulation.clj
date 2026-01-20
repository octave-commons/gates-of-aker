(do
  (require '[fantasia.sim.core :as core])
  (require '[fantasia.sim.jobs :as jobs])

  (let [world (core/initial-world {:seed 1 :bounds {:shape :rect :w 128 :h 128} :tree-density 0.05})
        ticked (reduce (fn [w _] (core/tick-once w)) world (range 30))]

    (println "===== After 30 ticks =====")
    (println "Tick:" (:tick ticked))
    (println "Alive agents:" (count (filter #(get-in % [:status :alive?]) (:agents ticked))))
    (println "Jobs:" (count (:jobs ticked)))

    (doseq [agent (:agents ticked)]
      (let [id (:id agent)
            food (get-in agent [:needs :food])
            sleep (get-in agent [:needs :sleep])]
        (println (format "Agent %d: food=%.2f sleep=%.2f" id food sleep))))

    (println "\nStockpiles:")
    (doseq [[k sp] (:stockpiles ticked)]
      (println (format "  %s: %s qty=%d" k (:resource sp) (:current-qty sp))))))