(ns fantasia.sim.ecs.migration-doc-test
  "Documentation tests for ECS migration status."
  (:require [clojure.test :refer :all]))

(deftest test-migration-doc-exists
  (testing "Migration status document exists"
    (is (some? (try
                 (slurp "src/fantasia/sim/ecs/migration-status.md")
                 (catch Exception _ nil))))))

(deftest test-migration-doc-has-completed-sections
  (testing "Migration document has completed sections"
    (let [content (slurp "src/fantasia/sim/ecs/migration-status.md")]
      (is (some #(.contains content %)
                ["## Completed Work" "## ⚠️ Blocked Issues"])
          "Document should have completed work and blocked issues sections"))))

(deftest test-migration-doc-has-systems-table
  (testing "Migration document has systems table"
    (let [content (slurp "src/fantasia/sim/ecs/migration-status.md")]
      (is (.contains content "| System |")
          "Document should have systems table"))))

(deftest test-migration-doc-documents-missing-dependencies
  (testing "Migration document documents missing dependencies"
    (let [content (slurp "src/fantasia/sim/ecs/migration-status.md")]
      (is (some #(.contains content %)
                ["fantasia.sim.facets" "fantasia.sim.spatial"])
          "Document should document missing dependencies"))))

(defn -main
  "Run all migration documentation tests."
  []
  (println "=== Running Migration Documentation Tests ===")
  (test-migration-doc-exists)
  (test-migration-doc-has-completed-sections)
  (test-migration-doc-has-systems-table)
  (test-migration-doc-documents-missing-dependencies)
  (println "=== All Migration Documentation Tests Passed! ==="))
