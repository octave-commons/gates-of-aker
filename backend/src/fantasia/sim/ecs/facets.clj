(ns fantasia.sim.ecs.facets
  "ECS facets - personality traits and utility functions.
   Facets represent behavioral tendencies and personality aspects of agents.")

(defn clamp01
  "Clamp value to range [0, 1]."
  [x]
  (max 0.0 (min 1.0 x)))

(defn lerp
  "Linear interpolation between a and b by factor t (clamped to [0,1])."
  [a b t]
  (+ a (* (- b a) (clamp01 t))))

(defn normalize-range
  "Normalize value from [min, max] to [0, 1]."
  [value min-val max-val]
  (if (= max-val min-val)
    0.0
    (clamp01 (/ (- value min-val)
               (- max-val min-val)))))

(defn inverse-lerp
  "Find t such that lerp(a, b, t) = value."
  [a b value]
  (if (= b a)
    0.0
    (clamp01 (/ (- value a)
               (- b a)))))

(def facet-archetypes
  "Facet archetypes defining personality profiles.
   Each archetype maps to a set of facet values affecting agent behavior."
  {
   ;; Social archetypes
   :gregarious {:social-affinity 0.9 :conversation-chance 0.3}
   :reserved {:social-affinity 0.3 :conversation-chance 0.1}
   
   ;; Work archetypes
   :diligent {:work-priority 0.9 :job-completion-speed 1.2}
   :lazy {:work-priority 0.4 :job-completion-speed 0.8}
   
   ;; Combat archetypes
   :aggressive {:combat-priority 0.9 :combat-damage 1.3}
   :peaceful {:combat-priority 0.2 :combat-damage 0.7}
   
   ;; Survival archetypes
   :survivalist {:foraging-priority 0.9 :food-preservation 0.8}
   :dependent {:foraging-priority 0.4 :food-preservation 0.3}
   })

(defn get-archetype-facets
  "Get facet values for a given archetype."
  [archetype-key]
  (get facet-archetypes archetype-key
          {:social-affinity 0.5
           :conversation-chance 0.15
           :work-priority 0.5
           :job-completion-speed 1.0
           :combat-priority 0.5
           :combat-damage 1.0
           :foraging-priority 0.5
           :food-preservation 0.5}))

(defn merge-facets
  "Merge multiple facet maps, with later values overriding earlier ones."
  [& facet-maps]
  (apply merge facet-maps))

(defn random-variation
  "Apply random variation to facet values.
   variation-range is a [min-factor max-factor] pair, e.g. [0.8 1.2]."
  [facets variation-range]
  (let [[min-factor max-factor] variation-range]
    (reduce-kv (fn [acc k v]
                   (assoc acc k (* v (+ min-factor (* (- max-factor min-factor) (rand)))))
                 {}
                 facets))))

(defn generate-random-facets
  "Generate random facets for an agent by combining archetypes with variation."
  [archetype-keys variation-range]
  (let [base-facets (apply merge (map get-archetype-facets archetype-keys))]
    (random-variation base-facets variation-range)))

(defn apply-facet-modifier
  "Apply facet modifier to base value.
   modifier is a keyword like :combat-damage or :job-completion-speed."
  [base-value facets modifier-key]
  (let [modifier (get facets modifier-key 1.0)]
    (* base-value modifier)))

(defn get-social-probability
  "Get probability of social interaction based on facets."
  [facets]
  (get facets :conversation-chance 0.15))

(defn get-work-priority
  "Get work assignment priority based on facets."
  [facets]
  (get facets :work-priority 0.5))

(defn get-combat-probability
  "Get probability of initiating combat based on facets."
  [facets]
  (get facets :combat-priority 0.5))

(defn get-foraging-priority
  "Get foraging priority based on facets."
  [facets]
  (get facets :foraging-priority 0.5))

(def facets-by-role
  "Default facet archetypes by agent role."
  {:priest [:gregarious :peaceful]
   :knight [:aggressive :diligent]
   :peasant [:reserved :survivalist]
   :builder [:diligent]
   :scribe [:reserved]})

(defn get-default-facets-for-role
  "Get default facets for an agent role."
  [role]
  (let [archetypes (get facets-by-role role [:reserved])]
    (apply merge (map get-archetype-facets archetypes))))

(defn generate-facets-for-agent
  "Generate facets for a new agent with role-based defaults and random variation."
  ([role]
   (generate-facets-for-agent role [0.8 1.2]))
  ([role variation-range]
   (let [base-facets (get-default-facets-for-role role)]
     (random-variation base-facets variation-range))))
