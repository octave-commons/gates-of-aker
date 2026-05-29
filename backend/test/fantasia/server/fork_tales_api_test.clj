(ns fantasia.server.fork-tales-api-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [fantasia.server :as server]
            [fantasia.sim.ecs.adapter :as adapter]
            [fantasia.sim.ecs.tick :as sim]
            [fantasia.story.fork-tales :as fork-tales]))

(defn- parse-body
  [resp]
  (json/parse-string (:body resp) true))

(defn- request
  ([method uri]
   (request method uri nil))
  ([method uri body]
   (server/app {:request-method method
                :uri uri
                :headers {"content-type" "application/json"}
                :body body})))

(deftest fork-tales-status-endpoint-returns-structured-status
  (with-redefs [fork-tales/storyteller-status (fn [] {:configured true :chapter_count 49})]
    (let [resp (request :get "/api/fork-tales/status")
          body (parse-body resp)]
      (is (= 200 (:status resp)))
      (is (= true (:configured body)))
      (is (= 49 (:chapter_count body))))))

(deftest fork-tales-history-endpoints-return-list-and-detail
  (with-redefs [fork-tales/chapter-history (fn [] {:chapter_count 2
                                                   :chapters [{:number 2 :title "Second" :path "/tmp/second.md" :preview "second preview"}]})
                fork-tales/chapter-detail (fn [chapter-number]
                                            (when (= chapter-number "2")
                                              {:number 2 :title "Second" :path "/tmp/second.md" :text "# 2 — Second\n\ntext"}))]
    (let [history-resp (request :get "/api/fork-tales/history")
          history-body (parse-body history-resp)
          detail-resp (request :get "/api/fork-tales/history/2")
          detail-body (parse-body detail-resp)
          missing-resp (request :get "/api/fork-tales/history/99")
          missing-body (parse-body missing-resp)]
      (is (= 200 (:status history-resp)))
      (is (= 2 (:chapter_count history-body)))
      (is (= "Second" (get-in history-body [:chapters 0 :title])))
      (is (= 200 (:status detail-resp)))
      (is (= 2 (:number detail-body)))
      (is (= 404 (:status missing-resp)))
      (is (= false (:ok missing-body))))))

(deftest fork-tales-continue-endpoint-forwards-body-to-story-engine
  (let [captured (atom nil)]
    (with-redefs [sim/get-state (constantly {:tick 7})
                  sim/get-ecs-world (constantly :fake-ecs-world)
                  adapter/ecs->snapshot (fn [_ _] {:tick 7 :agents [{:name "Duct" :role :priest :pos [0 0]}]})
                  fork-tales/generate-next-chapter! (fn [snapshot opts]
                                                     (reset! captured {:snapshot snapshot :opts opts})
                                                     {:ok true
                                                      :chapter_number 50
                                                      :written false
                                                      :title "Preview"
                                                      :text "# 50 — Preview"})]
      (let [resp (request :post "/api/fork-tales/continue" "{\"dry_run\":true,\"user_prompt\":\"focus on Duct\"}")
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (= 50 (:chapter_number body)))
        (is (= true (get-in @captured [:opts :dry_run])))
        (is (= "focus on Duct" (get-in @captured [:opts :user_prompt])))
        (is (= 7 (get-in @captured [:snapshot :tick])))))))
