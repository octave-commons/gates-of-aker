(ns fantasia.config-test
  (:require [clojure.test :refer [deftest is testing]]
            [fantasia.config :as config]))

(deftest test-load-ollama-config
  (testing "Config file loading"
    (let [cfg (config/load-ollama-config!)]
      (is (map? cfg))
      (is (contains? cfg :ollama))
      (is (contains? cfg :ollama-embed))
      (is (string? (get-in cfg [:ollama :url])))
      (is (string? (get-in cfg [:ollama :primary-model])))
      (is (vector? (get-in cfg [:ollama :fallback-models]))))))

(deftest test-get-ollama-models
  (testing "Get Ollama models in priority order"
    (let [cfg (config/load-ollama-config!)
          models (config/get-ollama-models cfg)]
      (is (vector? models))
      (is (> (count models) 0))
      (is (= (first models) (get-in cfg [:ollama :primary-model]))))))

(deftest test-get-ollama-primary-model
  (testing "Get primary Ollama model"
    (let [cfg (config/load-ollama-config!)
          primary (config/get-ollama-primary-model cfg)]
      (is (string? primary))
      (is (= primary (get-in cfg [:ollama :primary-model]))))))
