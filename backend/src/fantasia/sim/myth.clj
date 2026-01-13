(ns fantasia.sim.myth)

(defn decay-ledger
  "Decay buzz/tradition each tick. Keeps belief from becoming permanently stuck."
  [ledger]
  (let [buzz-decay 0.90
        trad-decay 0.995]
    (into {}
          (map (fn [[k v]]
                 [k (-> v
                        (update :buzz (fnil #(* (double %) buzz-decay) 0.0))
                        (update :tradition (fnil #(* (double %) trad-decay) 0.0)))])
               ledger))))

(defn add-mention
  "Update ledger for (event-type, claim) with optional event-instance grounding."
  [ledger {:keys [event-type claim weight event-instance]}]
  (let [k [event-type claim]
        w (double weight)]
    (as-> ledger ledger$
      (-> ledger$
          (update-in [k :buzz] (fnil + 0.0) w)
          ;; tradition grows slower than buzz; log-ish makes sustained talk matter more
          (update-in [k :tradition] (fnil + 0.0) (* 0.12 (Math/log (+ 1.0 w))))
          (update-in [k :mentions] (fnil inc 0)))
      (if event-instance
        (update-in ledger$ [k :event-instances] (fnil conj #{}) event-instance)
        ledger$))))

(defn attribution
  "Compute attribution probabilities per event-type from ledger."
  [ledger event-type]
  (let [rows (for [[[et claim] {:keys [tradition]}] ledger
                   :when (= et event-type)]
               [claim (double (or tradition 0.0))])
        total (reduce + 0.0 (map second rows))
        eps 1.0e-9
        total (max total eps)]
    (into {}
          (map (fn [[c t]] [c (/ t total)]) rows))))
