(ns fantasia.sim.core
  (:require [fantasia.sim.facets :as f]
            [fantasia.sim.myth :as myth]
            [fantasia.sim.events :as events]))

(defn rng [seed] (java.util.Random. (long seed)))
(defn rand-int* [^java.util.Random r n] (.nextInt r (int n)))
(defn rand-double* [^java.util.Random r] (.nextDouble r))

(defn ->agent [id x y role]
  {:id id
   :pos [x y]
   :role role
   :needs {:warmth 0.6 :food 0.7 :sleep 0.7}
   :frontier {}
   :recall {}})

(defn initial-world [seed]
  (let [r (rng seed)]
    {:seed seed
     :tick 0
     :size [20 20]
     :trees (set (for [x (range 10 18)
                       y (range 2 10)]
                   [x y]))
     :shrine nil
     :cold-snap 0.85
     :levers {:iconography {:fire->patron 0.80
                            :lightning->storm 0.75
                            :storm->deity 0.85}
              :mouthpiece-agent-id nil}
     :institutions
     {:temple {:id :temple
               :name "Temple of Embers"
               :entropy 0.2
               :broadcast-every 6
               :canonical {:facets [:fire :judgment :winter]
                           :claim-hint :claim/winter-judgment-flame}}}
     :agents
     (vec (for [i (range 12)]
            (->agent i (rand-int* r 20) (rand-int* r 20)
                    (cond
                      (= i 0) :priest
                      (= i 1) :knight
                      :else :peasant))))
     :edges
     {[:cold :fire] 0.60
      [:trees :fire] 0.45
      [:lightning :storm] 0.70
      [:storm :deity/storm] 0.80
      [:fire :patron/fire] 0.80
      [:patron/fire :judgment] 0.35
      [:deity/storm :awe] 0.25
      [:judgment :awe] 0.25}
     :ledger {}
     :recent-events []
     :recent-max 30
     :traces []
     :trace-max 250}))

(defn in-bounds? [[w h] [x y]]
  (and (<= 0 x) (< x w) (<= 0 y) (< y h)))

(defn manhattan [[ax ay] [bx by]]
  (+ (Math/abs (long (- ax bx))) (Math/abs (long (- ay by)))))

(defn neighbors [[x y]]
  [[(inc x) y] [(dec x) y] [x (inc y)] [x (dec y)]])

(defn at-trees? [world pos]
  (contains? (:trees world) pos))

(defn near-shrine? [world pos]
  (when-let [s (:shrine world)]
    (<= (manhattan pos s) 3)))

(defn move-agent [world agent]
  (let [size (:size world)
        options (->> (neighbors (:pos agent))
                     (filter #(in-bounds? size %))
                     vec)]
    (if (seq options)
      (assoc agent :pos (nth options (mod (+ (:tick world) (:id agent) (:seed world))
                                         (count options))))
      agent)))

(defn update-needs [world agent]
  (let [cold (:cold-snap world)
        warmth (get-in agent [:needs :warmth] 0.6)
        warmth' (f/clamp01 (- warmth (* 0.03 cold)))
        food' (f/clamp01 (- (get-in agent [:needs :food] 0.7) 0.01))
        sleep' (f/clamp01 (- (get-in agent [:needs :sleep] 0.7) 0.008))]
    (assoc agent :needs {:warmth warmth' :food food' :sleep sleep'})))

(defn choose-packet [world agent]
  (let [warmth (get-in agent [:needs :warmth] 0.5)
        base (cond-> [:cold]
               (at-trees? world (:pos agent)) (conj :trees)
               (< warmth 0.25) (conj :fear)
               (near-shrine? world (:pos agent)) (conj :fire))
        base (if (= (:role agent) :priest)
               (conj base :judgment)
               base)]
    {:intent (cond
               (< warmth 0.25) :warn
               (= (:role agent) :priest) :convert
               :else :chatter)
     :facets base
     :tone {:awe (if (= (:role agent) :priest) 0.3 0.1)
            :urgency (if (< warmth 0.25) 0.6 0.2)}}))

(defn interactions [agents]
  (for [a agents
        b agents
        :when (and (not= (:id a) (:id b))
                   (<= (manhattan (:pos a) (:pos b)) 1))]
    [a b]))

(defn scaled-edges [world]
  (let [base (:edges world)
        icon (get-in world [:levers :iconography] {})
        fire->patron (double (get icon :fire->patron 0.80))
        lightning->storm (double (get icon :lightning->storm 0.75))
        storm->deity (double (get icon :storm->deity 0.85))]
    (-> base
        (assoc [:fire :patron/fire] fire->patron)
        (assoc [:lightning :storm] lightning->storm)
        (assoc [:storm :deity/storm] storm->deity))))

(defn score-claim [frontier claim {:keys [claim-hint speaker]}]
  (let [deity (:deity claim)
        deity-a (if deity (double (get-in frontier [deity :a] 0.0)) 0.0)
        hint-bonus (if (= claim-hint (:id claim)) 0.15 0.0)
        speaker-bonus (cond
                        (= (:role speaker) :priest) 0.03
                        (= (:role speaker) :knight) 0.01
                        :else 0.0)]
    (+ deity-a hint-bonus speaker-bonus (double (:bonus claim 0.0)))))

(defn pick-claim [event-type frontier {:keys [claim-hint speaker]}]
  (let [claims (:claims event-type)
        scored (map (fn [c]
                      [(:id c)
                       (score-claim frontier c {:claim-hint claim-hint :speaker speaker})])
                    claims)]
    (->> scored (sort-by second >) ffirst)))

(defn recall-and-mentions [old-recall frontier packet speaker]
  (let [claim-hint (:claim-hint packet)]
    (reduce
      (fn [{:keys [new-recall mentions traces]} et]
        (let [{:keys [score]} (f/event-recall frontier (:signature et)
                                               {:threshold (:threshold et)})
              old (double (get old-recall (:id et) 0.0))
              new (double score)
              drec (- new old)
              mention? (> drec (double (:mention-delta et)))
              claim (when mention?
                      (pick-claim et frontier {:claim-hint claim-hint :speaker speaker}))
              weight (when mention?
                       (* drec
                          (cond
                            (= (:role speaker) :priest) 1.25
                            (= (:role speaker) :knight) 1.10
                            :else 1.00)))]
          {:new-recall (assoc new-recall (:id et) new)
           :mentions (cond-> mentions mention? (conj {:event-type (:id et)
                                                      :claim claim
                                                      :weight weight}))
           :traces (cond-> traces
                     mention?
                     (conj {:trace/id (str "t-" (:id et) "-" (:listener-id packet)
                                            "-" (:speaker-id packet) "-"
                                            (System/currentTimeMillis))
                            :tick (:tick packet)
                            :listener (:listener-id packet)
                            :speaker (:speaker-id packet)
                            :packet (dissoc packet :listener-id :speaker-id :tick)
                            :seeded (mapv (fn [f] {:facet f :delta 0.30})
                                          (:facets packet))
                            :spread (take 18 (:spread packet))
                            :event-recall {:event-type (:id et)
                                           :delta drec
                                           :new new}
                            :claim-activation [{:claim claim :delta (* 0.6 drec)}]
                            :mention {:event-type (:id et)
                                      :claim claim
                                      :weight weight}}))}))
      {:new-recall old-recall :mentions [] :traces []}
      (events/all-event-types))))

(defn apply-packet-to-listener [world listener speaker packet]
  (let [edges (scaled-edges world)
        fr0 (f/decay-frontier (:frontier listener) {:decay 0.93})
        fr0 (if (near-shrine? world (:pos listener))
              (f/bump-facet fr0 :fire 0.08)
              fr0)
        fr1 (f/seed fr0 (:facets packet) {:seed-strength 0.30})
        spread (f/spread-step fr1 edges {:spread-gain 0.50 :max-hops 2})
        fr2 (:frontier spread)
        res (recall-and-mentions (:recall listener)
                                 fr2
                                 (assoc packet
                                        :spread (:deltas spread)
                                        :listener-id (:id listener)
                                        :speaker-id (:id speaker)
                                        :tick (:tick world))
                                 speaker)]
    {:listener (-> listener
                  (assoc :frontier fr2)
                  (assoc :recall (:new-recall res)))
     :mentions (:mentions res)
     :traces (:traces res)}))

(defn institution-broadcasts [world]
  (let [t (:tick world)]
    (for [inst (vals (:institutions world))
          :let [every (:broadcast-every inst)]
          :when (and every (pos? every) (zero? (mod t every)))]
      {:institution (:id inst)
       :packet {:intent :convert
                :facets (get-in inst [:canonical :facets] [])
                :tone {:awe 0.55 :urgency 0.25}
                :claim-hint (get-in inst [:canonical :claim-hint])}})))

(defn apply-institution-broadcast [world agents {:keys [institution packet]}]
  (let [mouth-id (get-in world [:levers :mouthpiece-agent-id])
        mouth (when (some? mouth-id)
                (first (filter #(= (:id %) mouth-id) agents)))
        speaker (or mouth {:id (keyword (name institution))
                           :role :institution})]
    (reduce
      (fn [{:keys [agents mentions traces]} a]
        (let [res (apply-packet-to-listener world a speaker packet)]
          {:agents (conj agents (:listener res))
           :mentions (into mentions (:mentions res))
           :traces (into traces (:traces res))}))
      {:agents [] :mentions [] :traces []}
      agents)))

(defn gen-event [world agents]
  (let [t (:tick world)
        r (rng (+ (:seed world) (* 7919 t)))
        p (rand-double* r)
        [w h] (:size world)
        pos [(rand-int* r w) (rand-int* r h)]
        witnesses (->> agents
                       (filter (fn [a] (<= (manhattan (:pos a) pos) 4)))
                       (mapv :id))
        witness-score (min 1.0 (/ (count witnesses) 6.0))
        cold (:cold-snap world)
        fear (->> agents
                  (map #(get-in % [:needs :warmth] 0.6))
                  (map (fn [w] (- 1.0 (double w))))
                  (reduce + 0.0)
                  (/ (max 1 (count agents)))
                  (min 1.0))
        ev (cond
             (< p (+ 0.015 (* 0.015 cold) (* 0.01 fear)))
             {:type :winter-pyre
              :pos pos
              :impact (+ 0.6 (* 0.4 (rand-double* r)))
              :witness-score witness-score
              :witnesses witnesses}
             (< p (+ 0.004 (* 0.01 cold)))
             {:type :lightning-commander
              :pos pos
              :impact (+ 0.7 (* 0.3 (rand-double* r)))
              :witness-score (min 1.0 (+ witness-score 0.2))
              :witnesses witnesses}
             :else nil)]
    (when ev
      (assoc ev :id (str "e-" (name (:type ev)) "-" t "-" (rand-int* r 100000))
                :tick t))))

(defn apply-event-to-witness [world agent event-instance]
  (let [et (events/get-event-type (:type event-instance))
        impact (double (:impact event-instance))
        fr0 (f/decay-frontier (:frontier agent) {:decay 0.96})
        fr1 (reduce (fn [fr [facet w]]
                      (f/bump-facet fr facet (* impact 0.22 (double w))))
                    fr0
                    (:signature et))
        spread (f/spread-step fr1 (scaled-edges world) {:spread-gain 0.55 :max-hops 2})
        fr2 (:frontier spread)
        speaker {:id :world :role :world}
        packet {:intent :witness
                :facets (->> (:signature et) keys (take 4) vec)
                :tone {:awe (double (:witness-score event-instance))
                       :urgency 0.4}
                :claim-hint nil}
        res (recall-and-mentions (:recall agent)
                                 fr2
                                 (assoc packet
                                        :spread (:deltas spread)
                                        :listener-id (:id agent)
                                        :speaker-id :world
                                        :tick (:tick world))
                                 speaker)]
    {:agent (-> agent (assoc :frontier fr2) (assoc :recall (:new-recall res)))
     :mentions (:mentions res)
     :traces (:traces res)}))

(defn snapshot [world attribution]
  {:tick (:tick world)
   :shrine (:shrine world)
   :levers (:levers world)
   :recent-events (:recent-events world)
   :attribution attribution
   :agents (mapv (fn [a]
                   {:id (:id a)
                    :pos (:pos a)
                    :role (:role a)
                    :needs (:needs a)
                    :recall (:recall a)
                    :top-facets (->> (:frontier a)
                                     (sort-by (fn [[_ {:keys [a]}]] (- (double a))))
                                     (take 8)
                                     (mapv (fn [[k v]] {:facet k :a (:a v)})))})
                 (:agents world))
   :ledger (into {}
                 (map (fn [[[et claim] v]]
                        [(str (name et) "/" (name claim))
                         {:buzz (:buzz v)
                          :tradition (:tradition v)
                          :mentions (:mentions v)}])
                      (:ledger world)))} )

(defn tick-once [world]
  (let [t (inc (:tick world))
        world (assoc world :tick t)
        agents1 (->> (:agents world)
                     (map (fn [a] (-> a (move-agent world) (update-needs world))))
                     vec)
        ev (gen-event world agents1)
        ev-step (if ev
                  (reduce
                    (fn [{:keys [agents mentions traces]} a]
                      (if (contains? (set (:witnesses ev)) (:id a))
                        (let [res (apply-event-to-witness world a ev)]
                          {:agents (conj agents (:agent res))
                           :mentions (into mentions (:mentions res))
                           :traces (into traces (:traces res))})
                        {:agents (conj agents a)
                         :mentions mentions
                         :traces traces}))
                    {:agents [] :mentions [] :traces []}
                    agents1)
                  {:agents agents1 :mentions [] :traces []})
        agents2 (:agents ev-step)
        pairs (interactions agents2)
        talk-step (reduce
                   (fn [{:keys [agents mentions traces]} [speaker listener]]
                     (let [packet (choose-packet world speaker)
                           res (apply-packet-to-listener world listener speaker packet)
                           agents' (assoc agents (:id listener) (:listener res))]
                       {:agents agents'
                        :mentions (into mentions (:mentions res))
                        :traces (into traces (:traces res))}))
                   {:agents (vec agents2)
                    :mentions (:mentions ev-step)
                    :traces (:traces ev-step)}
                   pairs)
        agents3 (:agents talk-step)
        bcasts (institution-broadcasts world)
        inst-step (reduce
                   (fn [{:keys [agents mentions traces]} b]
                     (let [res (apply-institution-broadcast world agents b)]
                       {:agents (:agents res)
                        :mentions (into mentions (:mentions res))
                        :traces (into traces (:traces res))}))
                   {:agents agents3
                    :mentions (:mentions talk-step)
                    :traces (:traces talk-step)}
                   bcasts)
        agents4 (:agents inst-step)
        ledger1 (myth/decay-ledger (:ledger world))
        ledger2 (reduce myth/add-mention ledger1 (:mentions inst-step))
        attr (into {}
                   (map (fn [et]
                          [(:id et) (myth/attribution ledger2 (:id et))])
                        (events/all-event-types)))
        recent' (if ev
                  (->> (concat (:recent-events world)
                               [(select-keys ev [:id :type :tick :pos :impact :witness-score :witnesses])])
                       (take-last (:recent-max world))
                       vec)
                  (:recent-events world))
        traces' (->> (concat (:traces world) (:traces inst-step))
                     (take-last (:trace-max world))
                     vec)
        world' (-> world
                   (assoc :agents agents4)
                   (assoc :ledger ledger2)
                   (assoc :recent-events recent')
                   (assoc :traces traces'))]
    {:world world'
     :out {:tick t
           :event ev
           :mentions (:mentions inst-step)
           :traces (:traces inst-step)
           :attribution attr
           :snapshot (snapshot world' attr)}}))

(defonce *state (atom (initial-world 1)))

(defn get-state [] @*state)

(defn reset-world! [{:keys [seed] :or {seed 1}}]
  (clojure.core/reset! *state (initial-world seed)))

(defn set-levers! [levers]
  (swap! *state update :levers merge levers))

(defn place-shrine! [pos]
  (swap! *state assoc :shrine pos))

(defn appoint-mouthpiece! [agent-id]
  (swap! *state assoc-in [:levers :mouthpiece-agent-id] agent-id))

(defn tick! [n]
  (loop [i 0
         outs []]
    (if (>= i n)
      outs
        (let [{:keys [world out]} (tick-once (get-state))]
        (clojure.core/reset! *state world)
        (recur (inc i) (conj outs out))))))

