(ns fantasia.dev.coverage-simple
   (:require [cloverage.coverage]))

(defn run []
  "Simple coverage runner that excludes problematic ECS simple tests."
  (let [opts {:source-paths ["src"]
               :test-paths ["test"]
               :ns-regex [(re-pattern "fantasia.*")]
               :ns-exclude-regex [(re-pattern "fantasia\\.dev\\..*")
                                   (re-pattern "fantasia\\.sim\\.ecs\\.simple-test")
                                   (re-pattern "fantasia\\.sim\\.ecs\\.test-systems")]
               :test-ns-regex [(re-pattern "fantasia.*-test$")]
               :cover-only-ns-in-regex? true
               :fail-threshold 0
               :summary? true
               :emma-xml? false
               :lcov? true
               :html? false}]
    (cov/run-project opts)))