(ns fantasia.dev.coverage
  (:require [cloverage.coverage :as cov]))

(def opts
  {:source-paths ["src"]
   :test-paths ["test"]
   :ns-regex [(re-pattern "fantasia.*")]
   :ns-exclude-regex [(re-pattern "fantasia\\.sim\\..*-test$")
                      (re-pattern "fantasia\\.dev\\..*")]
   :test-ns-regex [(re-pattern "fantasia.*-test$")]})

(defn run
  "Run Cloverage with repo-specific defaults.
   Accepts optional overrides map merged into the base opts."
  ([request]
   (cov/run-project (merge opts request)))
  ([] (run {})))
