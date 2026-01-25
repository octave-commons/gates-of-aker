(ns fantasia.dev.coverage
  (:require [cloverage.coverage :as cov]))

(def opts
   {:source-paths ["src"]
    :test-paths ["test"]
    :ns-regex [(re-pattern "fantasia.*")]
     :ns-exclude-regex [(re-pattern "fantasia\\.dev\\..*")
                         (re-pattern "fantasia\\.test\\.ecs\\..*")
                         (re-pattern "fantasia\\.sim\\.reproduction")
                         (re-pattern "fantasia\\.sim\\.pathing")
                         (re-pattern "fantasia\\.sim\\.social")
                         (re-pattern "fantasia\\.sim\\.ecs\\.simple")
                         (re-pattern "fantasia\\.sim\\.ecs\\.simple-test")
                         (re-pattern "fantasia\\.sim\\.ecs-test")
                         (re-pattern "fantasia\\.sim\\.ecs-test-simple")
                         (re-pattern "fantasia\\.sim\\.ecs\\.test-systems")]
     :test-ns-regex [(re-pattern "^(?!.*\\b(fantasia\\.sim\\.ecs-test|fantasia\\.sim\\.ecs-test-simple)\\b).*fantasia.*-test$")]
    :cover-only-ns-in-regex? true
    :fail-threshold 0
    :summary? true
    :emma-xml? false
    :lcov? true
    :html? false})

(defn run
  "Run Cloverage with repo-specific defaults.
   Accepts optional overrides map merged into the base opts."
  ([request]
   (cov/run-project (merge opts request)))
  ([] (run {})))
