(ns fantasia.sim.traces
  (:require [clojure.set :as set]
            [fantasia.sim.spatial_facets :as spatial-facets]
            [fantasia.dev.logging :as log]))

(def high-impact-facets
  "Facets that make an event culturally significant."
  #{:death :judgment :miracle :revelation :war :plague :harvest :birth})

(defn create-trace
  "Create a new trace record.
   trace-id: unique identifier
   culture-id: which culture created this
   event-id: the event being traced
   title: culturally resonant title
   text: full narrative text
   facets: semantic facets
   created-by: agent-id of scribe who wrote it
   created-at: tick when trace was created
   strength: cultural salience (0.0-1.0)"
  [trace-id culture-id event-id title text facets created-by created-at strength]
  {:trace/id trace-id
   :culture-id culture-id
   :event-id event-id
   :title title
   :text text
   :facets facets
   :strength strength
   :created-at created-at
   :created-by created-by
   :referenced-traces []
   :decay-rate 0.001
   :read-count 0
   :canonical? false})

(defn create-culture
  "Create a new culture.
   culture-id: unique identifier
   name: human-readable name
   shared-facets: concepts this culture values
   trace-vocabulary: words scribes use"
  [culture-id name shared-facets trace-vocabulary]
  {:culture/id culture-id
   :name name
   :shared-facets shared-facets
   :trace-vocabulary trace-vocabulary
   :trace-history []
   :canonical-traces []
   :scribe-count 0
   :myth-density 0.5})

(defn trace-has-facet?
  "Check if trace contains a specific facet."
  [trace facet]
  (contains? (set (:facets trace)) facet))

(defn get-traces-by-culture
  "Get all traces belonging to a culture."
  [world culture-id]
  (->> (:traces world)
       (filter #(= (:culture-id %) culture-id))))

(defn get-canonical-traces
  "Get canonical (core) traces for a culture."
  [world culture-id]
  (if-let [culture (get (:cultures world) culture-id)]
    (map #(get (:traces world) %) (:canonical-traces culture))
    []))

(defn find-similar-by-embedding
  "Find traces similar to given facets using embedding similarity.
   Returns top-n most similar traces, sorted by similarity score."
  [world facets culture-id n]
  (let [culture-traces (get-traces-by-culture world culture-id)
        scored (map (fn [trace]
                     (let [intersection (set/intersection (set facets) (set (:facets trace)))
                           similarity (/ (count intersection)
                                      (Math/sqrt (* (count facets)
                                                    (count (:facets trace)))))]
                       [trace similarity]))
                   culture-traces)]
    (->> scored
         (sort-by second >)
         (take n)
         (map first))))

(defn decay-traces!
  "Decay all non-canonical traces by their decay-rate.
   Returns updated traces map."
  [world]
  (-> world
      (update :traces
              (fn [traces]
                (mapv (fn [trace]
                         (if (:canonical? trace)
                           trace
                           (let [new-strength (max 0.0 (- (:strength trace) (:decay-rate trace)))]
                             (assoc trace :strength new-strength))))
                       traces)))))

(defn canonicalize-trace!
  "Mark a trace as canonical (core mythology)."
  [world culture-id trace-id]
  (let [trace (get (:traces world) trace-id)]
    (if trace
      (-> world
          (assoc-in [:traces trace-id :canonical?] true)
          (assoc-in [:traces trace-id :decay-rate] 0.0)
          (update-in [:cultures culture-id :canonical-traces] conj trace-id))
      world)))

(defn read-trace!
  "Record that an agent has read a trace."
  [world trace-id reader-id]
  (-> world
      (update-in [:traces trace-id :read-count] inc)
      (update-in [:agents reader-id :events] conj {:type :read-trace :trace-id trace-id})))

(defn trace-influence-on-facet
  "Calculate how much traces influence a specific facet for a culture."
  [world culture-id facet]
  (let [traces (get-traces-by-culture world culture-id)]
    (reduce (fn [acc trace]
               (if (trace-has-facet? trace facet)
                 (+ acc (* (:strength trace) 0.15))
                 acc))
             0.0
             traces)))

(defn get-culture
  "Get a culture by ID, or nil if not found."
  [world culture-id]
  (get (:cultures world) culture-id))

(defn add-trace-to-culture!
  "Add a trace to a culture's history and world traces."
  [world culture-id trace]
  (-> world
      (update-in [:cultures culture-id :trace-history] conj (:trace/id trace))
      (assoc-in [:traces (:trace/id trace)] trace)))
