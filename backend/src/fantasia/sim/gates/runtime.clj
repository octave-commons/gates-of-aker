(ns fantasia.sim.gates.runtime
  (:require [clojure.string :as str]
            [fantasia.dev.logging :as log]
            [fantasia.sim.gates.kernel :as kernel]
            [fantasia.sim.gates.policy-pack :as policy-pack]))

(def supported-modes
  #{:off :shadow :enforce})

(def ^:private empty-registry
  policy-pack/runtime-policy-registry)

(defonce ^:private *mode
  (atom :enforce))

(defonce ^:private *registry
  (atom empty-registry))

(defonce ^:private *decision-records
  (atom []))

(defn- normalize-mode
  [mode]
  (cond
    (keyword? mode) mode
    (string? mode) (keyword mode)
    :else nil))

(defn get-mode
  []
  @*mode)

(defn set-mode!
  [mode]
  (let [normalized (normalize-mode mode)]
    (when-not (contains? supported-modes normalized)
      (throw (ex-info "Unsupported gate runtime mode"
                      {:mode mode
                       :supported-modes (sort supported-modes)})))
    (reset! *mode normalized)))

(defn set-registry!
  [registry]
  (reset! *registry registry))

(defn reset-registry!
  []
  (reset! *registry empty-registry))

(defn clear-decision-records!
  []
  (reset! *decision-records []))

(defn decision-records
  []
  @*decision-records)

(defn- op->signal
  [op]
  (cond
    (keyword? op) op
    (string? op) (keyword op)
    :else :unknown/op))

(defn- input-kind->keyword
  [value]
  (cond
    (keyword? value) value
    (string? value) (keyword value)
    :else nil))

(defn- op-token
  [op-signal]
  (name op-signal))

(defn- op-contains?
  [op-signal token]
  (str/includes? (op-token op-signal) token))

(defn extract-signals
  [{:keys [boundary
           op
           tick
           time-relative?
           relative-date?
           time-sensitive?
           freshness-required?
           requires-freshness?
           citation-required?
           factual-claim?
           load-bearing-factual-claim?
           web-derived-claim?
           continuity-request?
            context-required?
            context-missing?
            context-available?
            pdf-analysis?
            artifact-request?
            artifact-generation?
            spreadsheet-request?
            slide-request?
            artifact-handoff-prepared?
            artifact-handoff-missing?
            generation-before-handoff?
            image-generation?
            image-edit?
            image-request?
            image-route-bypass?
            image-route-selected?
            output-format
            artifact-type
            email-draft?
            email-output?
            writing-block?
            writing-block-requested?
            writing-block-contains-code?
            code-in-writing-block?
            input-format
            file-kind]}]
  (let [boundary-signal (if (keyword? boundary) boundary :unknown/boundary)
         op-signal (op->signal op)
         op-name (op-token op-signal)
         input-format-signal (input-kind->keyword input-format)
         file-kind-signal (input-kind->keyword file-kind)
         output-format-signal (input-kind->keyword output-format)
         artifact-type-signal (input-kind->keyword artifact-type)
         relative-time-context? (or time-relative?
                                     relative-date?
                                     (number? tick)
                                     (= :tick op-signal)
                                     (= :sim/tick op-signal))
         freshness-context? (or time-sensitive?
                               freshness-required?
                               requires-freshness?)
         citation-context? (or citation-required?
                             factual-claim?
                             load-bearing-factual-claim?
                             web-derived-claim?)
          context-required-signal? (or continuity-request?
                                       context-required?)
          context-missing-signal? (or context-missing?
                                      (and context-required-signal?
                                           (false? context-available?)))
          pdf-analysis-context? (or pdf-analysis?
                                    (= :pdf input-format-signal)
                                    (= :pdf file-kind-signal))
          artifact-request-context? (or artifact-request?
                                       artifact-generation?
                                       spreadsheet-request?
                                       slide-request?
                                       (= :spreadsheet output-format-signal)
                                       (= :slides output-format-signal)
                                       (= :spreadsheet artifact-type-signal)
                                       (= :slides artifact-type-signal)
                                       (op-contains? op-signal "spreadsheet")
                                       (op-contains? op-signal "slide")
                                       (op-contains? op-signal "artifact"))
          artifact-handoff-missing-context? (or artifact-handoff-missing?
                                                generation-before-handoff?
                                                (and artifact-request-context?
                                                     (false? artifact-handoff-prepared?)))
          image-request-context? (or image-request?
                                    image-generation?
                                    image-edit?
                                    (= :image output-format-signal)
                                    (= :image input-format-signal)
                                    (= :image file-kind-signal)
                                    (op-contains? op-signal "image"))
          image-route-bypass-context? (or image-route-bypass?
                                          (and image-request-context?
                                               (false? image-route-selected?)))
          email-writing-context? (or email-draft?
                                     email-output?
                                     (= :email output-format-signal)
                                     (op-contains? op-signal "email"))
          writing-block-present-context? (or writing-block?
                                             writing-block-requested?)
          writing-block-code-context? (or writing-block-contains-code?
                                          code-in-writing-block?)]
    (cond-> #{boundary-signal
              op-signal
              (keyword (str (name boundary-signal) "/" op-name))
              :policy/runtime-response
              :policy/filesystem-workflow
              :policy/tool-usage}
      relative-time-context? (conj :policy/time-relative-context)
      freshness-context? (conj :policy/freshness-required)
      citation-context? (conj :policy/citation-required)
      context-required-signal? (conj :policy/context-required)
      context-missing-signal? (conj :policy/context-missing)
      pdf-analysis-context? (conj :policy/pdf-analysis-required)
      artifact-request-context? (conj :policy/artifact-request)
      artifact-handoff-missing-context? (conj :policy/artifact-handoff-missing)
      image-request-context? (conj :policy/image-request)
      image-route-bypass-context? (conj :policy/image-route-bypass)
      email-writing-context? (conj :policy/email-writing-required)
      writing-block-present-context? (conj :policy/writing-block-present)
      (and writing-block-present-context?
           (not email-writing-context?)) (conj :policy/writing-block-outside-email)
      writing-block-code-context? (conj :policy/writing-block-code-present))))

(defn evaluate!
  [context]
  (let [mode (get-mode)]
    (when-not (= :off mode)
      (let [signals (extract-signals context)
            decision (kernel/evaluate @*registry signals)
            record (assoc decision
                          :mode mode
                          :boundary (:boundary context)
                          :op (:op context)
                          :at-ms (System/currentTimeMillis))]
        (swap! *decision-records conj record)
        (log/log-info "[GATES:DECISION]"
                      {:mode mode
                       :boundary (:boundary context)
                       :op (:op context)
                       :signals signals
                       :triggered (count (:triggered-gates decision))
                       :obligations (count (:obligations decision))
                       :prohibitions (count (:prohibitions decision))
                       :formatting-constraints (count (:formatting-constraints decision))})
        record))))
