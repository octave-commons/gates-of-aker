(ns fantasia.sim.gates.policy-pack
  (:require [fantasia.sim.gates.kernel :as kernel]))

(def user-timezone "America/Phoenix")

(defn- has-policy-signal?
  [signals signal]
  (kernel/has-signal? signals signal))

(def g-001-web-browsing-freshness
  {:id :g-001/web-browsing-freshness
   :priority 96
   :trigger #(has-policy-signal? % :policy/freshness-required)
   :enforce (fn [_]
              {:obligations [{:id :g-001/execute-freshness-check
                              :value :required}]
               :prohibitions [{:id :g-001/finalize-without-freshness
                               :value :forbidden}]})})

(def g-002-citations
  {:id :g-002/citations
   :priority 94
   :trigger #(has-policy-signal? % :policy/citation-required)
   :enforce (fn [_]
              {:obligations [{:id :g-002/provide-citations
                              :value :required}]
               :prohibitions [{:id :g-002/uncited-factual-claims
                               :value :forbidden}]
               :formatting-constraints [{:id :g-002/citation-format
                                         :value {:style :inline-citation
                                                 :forbid-code-fences true
                                                 :forbid-emphasis true}}]})})

(def g-003-personal-context
  {:id :g-003/personal-context
   :priority 92
   :trigger #(has-policy-signal? % :policy/context-required)
   :enforce (fn [{:keys [signals]}]
              {:obligations [{:id :g-003/retrieve-context-first
                              :value :required}]
               :prohibitions [{:id :g-003/silent-fabricated-continuity
                               :value :forbidden}]
               :formatting-constraints [{:id :g-003/explicit-assumptions-when-context-missing
                                         :value (if (has-policy-signal? signals :policy/context-missing)
                                                  :required
                                                   :not-required)}]})})

(def g-004-artifact-handoff-first
  {:id :g-004/artifact-handoff-first
   :priority 91
   :trigger #(has-policy-signal? % :policy/artifact-request)
   :enforce (fn [{:keys [signals]}]
              {:obligations [{:id :g-004/prepare-artifact-handoff
                              :value :required}
                             {:id :g-004/handoff-before-generation
                              :value :required}]
               :prohibitions [{:id :g-004/direct-generation-without-handoff
                               :value :forbidden}]
               :formatting-constraints [{:id :g-004/handoff-status
                                         :value (if (has-policy-signal? signals :policy/artifact-handoff-missing)
                                                  :missing
                                                  :ready)}]})})

(def g-005-pdf-screenshots
  {:id :g-005/pdf-screenshots
   :priority 90
   :trigger #(has-policy-signal? % :policy/pdf-analysis-required)
   :enforce (fn [_]
              {:obligations [{:id :g-005/screenshot-first-routing
                              :value :required}
                             {:id :g-005/citations-required
                              :value :required}]
                :prohibitions [{:id :g-005/ocr-first-default
                                :value :forbidden}]})})

(def g-006-image-generation-routing
  {:id :g-006/image-generation-routing
   :priority 88
   :trigger #(has-policy-signal? % :policy/image-request)
   :enforce (fn [{:keys [signals]}]
              {:obligations [{:id :g-006/route-to-image-tooling
                              :value :required}]
               :prohibitions [{:id :g-006/non-image-route-for-image-request
                               :value :forbidden}]
               :formatting-constraints [{:id :g-006/routing-status
                                         :value (if (has-policy-signal? signals :policy/image-route-bypass)
                                                  :bypass-detected
                                                  :image-path)}]})})

(def g-007-writing-block-email-contract
  {:id :g-007/writing-block-email-contract
   :priority 86
   :trigger #(or (has-policy-signal? % :policy/email-writing-required)
                 (has-policy-signal? % :policy/writing-block-present))
   :enforce (fn [{:keys [signals]}]
              {:obligations [{:id :g-007/writing-block-metadata
                              :value {:required-fields [:id :variant :subject]
                                      :variant :email}}]
               :prohibitions [{:id :g-007/code-inside-writing-block
                               :value :forbidden}
                              {:id :g-007/writing-block-outside-email
                               :value :forbidden}]
               :formatting-constraints [{:id :g-007/writing-block-required
                                         :value (if (has-policy-signal? signals :policy/email-writing-required)
                                                  :required
                                                  :not-required)}
                                        {:id :g-007/code-placement
                                         :value (if (has-policy-signal? signals :policy/writing-block-code-present)
                                                  :required-outside-writing-block
                                                  :compliant)}]})})

(def g-008-no-async-promises
  {:id :g-008/no-async-promises
   :priority 80
   :trigger #(has-policy-signal? % :policy/runtime-response)
   :enforce (fn [_]
              {:obligations [{:id :g-008/respond-in-current-turn
                              :value :required}
                             {:id :g-008/partial-if-incomplete
                              :value :required}]
               :prohibitions [{:id :g-008/forbidden-deferral-markers
                               :value ["later"
                                       "soon"
                                       "after this"
                                       "i will follow up"
                                       "eta"]}]
                :formatting-constraints [{:id :g-008/no-time-estimates
                                          :value :required}]})})

(def g-009-timezone-dates
  {:id :g-009/timezone-dates
   :priority 82
   :trigger #(has-policy-signal? % :policy/time-relative-context)
   :enforce (fn [_]
              {:obligations [{:id :g-009/resolve-relative-time
                              :value :required}]
                :formatting-constraints [{:id :g-009/time-interpretation
                                          :value {:timezone user-timezone
                                                  :include-absolute-date true}}]})})

(def g-010-filesystem-workflow
  {:id :g-010/filesystem-workflow
   :priority 76
   :trigger #(has-policy-signal? % :policy/filesystem-workflow)
   :enforce (fn [_]
              {:obligations [{:id :g-010/discovery-order
                              :value [:glob :grep :read]}
                             {:id :g-010/minimal-additive-changes
                              :value :required}]
                :prohibitions [{:id :g-010/no-unbounded-tree-first
                                :value :forbidden}]})})

(def g-011-tool-minimality
  {:id :g-011/tool-minimality
   :priority 78
   :trigger #(has-policy-signal? % :policy/tool-usage)
   :enforce (fn [_]
              {:obligations [{:id :g-011/targeted-tool-calls
                              :value :required}
                             {:id :g-011/max-alternate-route
                              :value 1}
                             {:id :g-011/prefer-reasoning-when-safe
                              :value :required}]
                :prohibitions [{:id :g-011/no-retry-fanout
                                :value :forbidden}]})})

(def runtime-policy-gates
  [g-001-web-browsing-freshness
   g-002-citations
   g-003-personal-context
   g-004-artifact-handoff-first
   g-005-pdf-screenshots
   g-006-image-generation-routing
   g-007-writing-block-email-contract
   g-008-no-async-promises
   g-009-timezone-dates
   g-010-filesystem-workflow
   g-011-tool-minimality])

(def runtime-policy-registry
  (kernel/build-registry runtime-policy-gates))
