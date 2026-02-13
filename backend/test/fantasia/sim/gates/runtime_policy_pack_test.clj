(ns fantasia.sim.gates.runtime-policy-pack-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [fantasia.sim.gates.runtime :as gates-runtime]))

(defn- by-id
  [collection id]
  (some #(when (= id (:id %)) %) collection))

(defn- with-clean-gates
  [f]
  (let [initial-mode (gates-runtime/get-mode)]
    (try
      (gates-runtime/reset-registry!)
      (gates-runtime/clear-decision-records!)
      (gates-runtime/set-mode! :shadow)
      (f)
      (finally
        (gates-runtime/reset-registry!)
        (gates-runtime/clear-decision-records!)
        (gates-runtime/set-mode! initial-mode)))))

(use-fixtures :each with-clean-gates)

(deftest g-004-artifact-handoff-is-required-before-generation
  (let [handoff-ready (gates-runtime/evaluate! {:boundary :http
                                                :op :sim/state
                                                :artifact-request? true
                                                :artifact-handoff-prepared? true})
        handoff-missing (gates-runtime/evaluate! {:boundary :http
                                                  :op :sim/state
                                                  :artifact-request? true
                                                  :artifact-handoff-prepared? false})
        unrelated (gates-runtime/evaluate! {:boundary :http
                                            :op :sim/state})
        ready-triggered? (set (map :id (:triggered-gates handoff-ready)))
        missing-triggered? (set (map :id (:triggered-gates handoff-missing)))
        unrelated-triggered? (set (map :id (:triggered-gates unrelated)))]
    (is (contains? ready-triggered? :g-004/artifact-handoff-first))
    (is (contains? missing-triggered? :g-004/artifact-handoff-first))
    (is (= :required
           (:value (by-id (:obligations handoff-ready) :g-004/prepare-artifact-handoff))))
    (is (= :required
           (:value (by-id (:obligations handoff-ready) :g-004/handoff-before-generation))))
    (is (= :ready
           (:value (by-id (:formatting-constraints handoff-ready) :g-004/handoff-status))))
    (is (= :missing
           (:value (by-id (:formatting-constraints handoff-missing) :g-004/handoff-status))))
    (is (= :forbidden
           (:value (by-id (:prohibitions handoff-missing) :g-004/direct-generation-without-handoff))))
    (is (not (contains? unrelated-triggered? :g-004/artifact-handoff-first)))))

(deftest g-006-image-requests-route-to-image-tooling-and-flag-bypass
  (let [image-request (gates-runtime/evaluate! {:boundary :http
                                                :op :sim/state
                                                :image-request? true
                                                :image-route-selected? true})
        image-bypass (gates-runtime/evaluate! {:boundary :http
                                               :op :sim/state
                                               :image-request? true
                                               :image-route-selected? false})
        non-image (gates-runtime/evaluate! {:boundary :http
                                            :op :sim/state
                                            :image-request? false})
        request-triggered? (set (map :id (:triggered-gates image-request)))
        bypass-triggered? (set (map :id (:triggered-gates image-bypass)))
        non-image-triggered? (set (map :id (:triggered-gates non-image)))]
    (is (contains? request-triggered? :g-006/image-generation-routing))
    (is (contains? bypass-triggered? :g-006/image-generation-routing))
    (is (= :required
           (:value (by-id (:obligations image-request) :g-006/route-to-image-tooling))))
    (is (= :image-path
           (:value (by-id (:formatting-constraints image-request) :g-006/routing-status))))
    (is (= :bypass-detected
           (:value (by-id (:formatting-constraints image-bypass) :g-006/routing-status))))
    (is (= :forbidden
           (:value (by-id (:prohibitions image-bypass) :g-006/non-image-route-for-image-request))))
    (is (not (contains? non-image-triggered? :g-006/image-generation-routing)))))

(deftest g-007-writing-block-contract-requires-email-metadata-and-keeps-code-outside
  (let [email-writing (gates-runtime/evaluate! {:boundary :http
                                                :op :sim/state
                                                :email-draft? true
                                                :writing-block? true
                                                :writing-block-contains-code? false})
        non-email-writing-block (gates-runtime/evaluate! {:boundary :http
                                                          :op :sim/state
                                                          :email-draft? false
                                                          :writing-block? true})
        email-with-code-in-writing-block (gates-runtime/evaluate! {:boundary :http
                                                                   :op :sim/state
                                                                   :email-draft? true
                                                                   :writing-block? true
                                                                   :writing-block-contains-code? true})
        plain-response (gates-runtime/evaluate! {:boundary :http
                                                 :op :sim/state})
        email-triggered? (set (map :id (:triggered-gates email-writing)))
        non-email-triggered? (set (map :id (:triggered-gates non-email-writing-block)))
        plain-triggered? (set (map :id (:triggered-gates plain-response)))
        metadata-rule (by-id (:obligations email-writing) :g-007/writing-block-metadata)]
    (is (contains? email-triggered? :g-007/writing-block-email-contract))
    (is (contains? non-email-triggered? :g-007/writing-block-email-contract))
    (is (= {:required-fields [:id :variant :subject]
            :variant :email}
           (:value metadata-rule)))
    (is (= :required
           (:value (by-id (:formatting-constraints email-writing) :g-007/writing-block-required))))
    (is (= :not-required
           (:value (by-id (:formatting-constraints non-email-writing-block) :g-007/writing-block-required))))
    (is (= :required-outside-writing-block
           (:value (by-id (:formatting-constraints email-with-code-in-writing-block)
                          :g-007/code-placement))))
    (is (= :forbidden
           (:value (by-id (:prohibitions non-email-writing-block) :g-007/writing-block-outside-email))))
    (is (= :forbidden
           (:value (by-id (:prohibitions email-with-code-in-writing-block) :g-007/code-inside-writing-block))))
    (is (not (contains? plain-triggered? :g-007/writing-block-email-contract)))))

(deftest g-008-no-async-promises-records-forbidden-deferral-markers
  (let [decision (gates-runtime/evaluate! {:boundary :http
                                           :op :sim/state})
        triggered-ids (set (map :id (:triggered-gates decision)))
        marker-rule (by-id (:prohibitions decision) :g-008/forbidden-deferral-markers)]
    (is (contains? triggered-ids :g-008/no-async-promises))
    (is (= :forbidden (:value (by-id (:prohibitions decision) :g-011/no-retry-fanout))))
    (is (vector? (:value marker-rule)))
    (is (some #{"later"} (:value marker-rule)))
    (is (some #{"eta"} (:value marker-rule)))))

(deftest g-009-timezone-constraints-appear-in-time-relative-context
  (let [decision (gates-runtime/evaluate! {:boundary :runtime
                                           :op :tick
                                           :tick 42})
        triggered-ids (set (map :id (:triggered-gates decision)))
        time-format-rule (by-id (:formatting-constraints decision) :g-009/time-interpretation)]
    (is (contains? triggered-ids :g-009/timezone-dates))
    (is (= {:timezone "America/Phoenix"
            :include-absolute-date true}
           (:value time-format-rule)))))

(deftest g-010-filesystem-workflow-and-g-011-tool-minimality-constraints-recorded
  (let [decision (gates-runtime/evaluate! {:boundary :ws
                                           :op "set_levers"})
        triggered-ids (set (map :id (:triggered-gates decision)))
        discovery-order (by-id (:obligations decision) :g-010/discovery-order)
        tool-budget (by-id (:obligations decision) :g-011/max-alternate-route)]
    (is (contains? triggered-ids :g-010/filesystem-workflow))
    (is (contains? triggered-ids :g-011/tool-minimality))
    (is (= [:glob :grep :read] (:value discovery-order)))
    (is (= 1 (:value tool-budget)))
    (is (= :forbidden (:value (by-id (:prohibitions decision) :g-010/no-unbounded-tree-first))))))

(deftest g-001-freshness-path-is-required-for-time-sensitive-factual-requests
  (let [freshness-decision (gates-runtime/evaluate! {:boundary :http
                                                     :op :sim/state
                                                     :time-sensitive? true
                                                     :factual-claim? true})
        neutral-decision (gates-runtime/evaluate! {:boundary :http
                                                   :op :sim/state
                                                   :factual-claim? true})
        freshness-triggered? (set (map :id (:triggered-gates freshness-decision)))
        neutral-triggered? (set (map :id (:triggered-gates neutral-decision)))]
    (is (contains? freshness-triggered? :g-001/web-browsing-freshness))
    (is (= :required
           (:value (by-id (:obligations freshness-decision) :g-001/execute-freshness-check))))
    (is (= :forbidden
           (:value (by-id (:prohibitions freshness-decision) :g-001/finalize-without-freshness))))
    (is (not (contains? neutral-triggered? :g-001/web-browsing-freshness)))))

(deftest g-002-citations-are-required-for-load-bearing-claims
  (let [citation-decision (gates-runtime/evaluate! {:boundary :http
                                                    :op :sim/state
                                                    :load-bearing-factual-claim? true})
        non-claim-decision (gates-runtime/evaluate! {:boundary :http
                                                     :op :sim/state})
        citation-triggered? (set (map :id (:triggered-gates citation-decision)))
        non-claim-triggered? (set (map :id (:triggered-gates non-claim-decision)))
        citation-format (by-id (:formatting-constraints citation-decision) :g-002/citation-format)]
    (is (contains? citation-triggered? :g-002/citations))
    (is (= :required
           (:value (by-id (:obligations citation-decision) :g-002/provide-citations))))
    (is (= :forbidden
           (:value (by-id (:prohibitions citation-decision) :g-002/uncited-factual-claims))))
    (is (= {:forbid-code-fences true
            :forbid-emphasis true
            :style :inline-citation}
           (:value citation-format)))
    (is (not (contains? non-claim-triggered? :g-002/citations)))))

(deftest g-003-context-workflow-captures-retrieval-and-fallback-assumptions
  (let [context-present (gates-runtime/evaluate! {:boundary :http
                                                  :op :sim/state
                                                  :continuity-request? true
                                                  :context-available? true})
        context-missing (gates-runtime/evaluate! {:boundary :http
                                                  :op :sim/state
                                                  :continuity-request? true
                                                  :context-available? false})
        present-triggered? (set (map :id (:triggered-gates context-present)))
        missing-triggered? (set (map :id (:triggered-gates context-missing)))]
    (is (contains? present-triggered? :g-003/personal-context))
    (is (contains? missing-triggered? :g-003/personal-context))
    (is (= :required
           (:value (by-id (:obligations context-present) :g-003/retrieve-context-first))))
    (is (= :not-required
           (:value (by-id (:formatting-constraints context-present)
                          :g-003/explicit-assumptions-when-context-missing))))
    (is (= :required
           (:value (by-id (:formatting-constraints context-missing)
                          :g-003/explicit-assumptions-when-context-missing))))
    (is (= :forbidden
           (:value (by-id (:prohibitions context-missing) :g-003/silent-fabricated-continuity))))))

(deftest g-005-pdf-analysis-uses-screenshot-first-routing
  (let [pdf-decision (gates-runtime/evaluate! {:boundary :http
                                               :op :sim/state
                                               :pdf-analysis? true})
        non-pdf-decision (gates-runtime/evaluate! {:boundary :http
                                                   :op :sim/state
                                                   :pdf-analysis? false})
        pdf-triggered? (set (map :id (:triggered-gates pdf-decision)))
        non-pdf-triggered? (set (map :id (:triggered-gates non-pdf-decision)))]
    (is (contains? pdf-triggered? :g-005/pdf-screenshots))
    (is (= :required
           (:value (by-id (:obligations pdf-decision) :g-005/screenshot-first-routing))))
    (is (= :required
           (:value (by-id (:obligations pdf-decision) :g-005/citations-required))))
    (is (= :forbidden
           (:value (by-id (:prohibitions pdf-decision) :g-005/ocr-first-default))))
    (is (not (contains? non-pdf-triggered? :g-005/pdf-screenshots)))))
