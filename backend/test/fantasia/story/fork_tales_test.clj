(ns fantasia.story.fork-tales-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [fantasia.llm.openai-compat :as openai]
            [fantasia.story.fork-tales :as fork-tales])
  (:import (java.nio.file Files Path)
           (java.nio.file.attribute FileAttribute)))

(defn- temp-dir-path
  []
  (.toFile (Files/createTempDirectory "fork-tales-test" (make-array FileAttribute 0))))

(defn- delete-tree!
  [file]
  (when (and file (.exists (io/file file)))
    (doseq [child (reverse (file-seq (io/file file)))]
      (.delete child))))

(deftest generate-next-chapter-writes-chapter-and-myth-memory
  (let [root (temp-dir-path)
        narrative-dir (str (io/file root "narrative"))
        myths-path (str (io/file root ".myth" "myths.jsonl"))
        existing-path (io/file narrative-dir "Chapter_01_Existing.md")
        cfg {:provider :openai-compatible
             :base-url "https://example.test"
             :api-key "token"
             :model "mistral-large-3:675b"
             :temperature 0.7
             :max-tokens 400
             :timeout-ms 1000
             :context-chapters 2
             :context-chars 800
             :character-roster ["Duct" "Null"]
             :narrative-dir narrative-dir
             :myths-path myths-path}
        snapshot {:tick 42
                  :calendar {:day 2 :season :spring}
                  :agents [{:name "Duct" :role :priest :pos [0 0]}]
                  :recent-events [{:type :omen :facets [:gate :witness]}]}
        generated "# 2 — Signal Shelter\n\nDuct watched the Gate breathe and refused to call it magic.\n\nNull called it a log line with weather." ]
    (try
      (io/make-parents existing-path)
      (spit existing-path "# 1 — Existing\n\nA first chapter.")
      (with-redefs [openai/chat-completion! (fn [_] {:success true :text generated})]
        (let [result (fork-tales/generate-next-chapter! cfg snapshot {})
              myth-lines (str/split-lines (slurp myths-path))
              myth-record (json/parse-string (first myth-lines) true)]
          (is (:ok result))
          (is (:written result))
          (is (= 2 (:chapter_number result)))
          (is (.exists (io/file (:path result))))
          (is (str/includes? (slurp (:path result)) "# 2 — Signal Shelter"))
          (is (= "Signal Shelter" (:title myth-record)))
          (is (= 2 (:chapter_number myth-record)))
          (is (= "gates-of-aker" (:source myth-record)))))
      (finally
        (delete-tree! root)))))

(deftest generate-next-chapter-respects-dry-run
  (let [root (temp-dir-path)
        narrative-dir (str (io/file root "narrative"))
        myths-path (str (io/file root ".myth" "myths.jsonl"))
        cfg {:provider :openai-compatible
             :base-url "https://example.test"
             :api-key "token"
             :model "mistral-large-3:675b"
             :character-roster ["Patch" "Sei"]
             :narrative-dir narrative-dir
             :myths-path myths-path}
        snapshot {:tick 1 :agents []}
        generated "# 1 — Dry Run\n\nNothing is written yet." ]
    (try
      (with-redefs [openai/chat-completion! (fn [_] {:success true :text generated})]
        (let [result (fork-tales/generate-next-chapter! cfg snapshot {:dry_run true})]
          (is (:ok result))
          (is (not (:written result)))
          (is (not (.exists (io/file (:path result)))))
          (is (not (.exists (io/file myths-path))))))
      (finally
        (delete-tree! root)))))

(deftest storyteller-status-counts-existing-chapters
  (let [root (temp-dir-path)
        narrative-dir (io/file root "narrative")
        cfg {:provider :openai-compatible
             :base-url "https://example.test"
             :api-key "token"
             :model "mistral-large-3:675b"
             :narrative-dir (str narrative-dir)
             :myths-path (str (io/file root ".myth" "myths.jsonl"))}]
    (try
      (io/make-parents (io/file narrative-dir "Chapter_01_Existing.md"))
      (spit (io/file narrative-dir "Chapter_01_Existing.md") "# 1 — Existing\n\nA first chapter.")
      (spit (io/file narrative-dir "Chapter_02_Second.md") "# 2 — Second\n\nA second chapter.")
      (let [status (fork-tales/storyteller-status cfg)]
        (is (:configured status))
        (is (= 2 (:chapter_count status)))
        (is (= 2 (get-in status [:latest_chapter :number])))
        (is (= "Second" (get-in status [:latest_chapter :title]))))
      (finally
        (delete-tree! root)))))

(deftest chapter-history-and-detail-return-browsable-data
  (let [root (temp-dir-path)
        narrative-dir (io/file root "narrative")
        cfg {:provider :openai-compatible
             :base-url "https://example.test"
             :api-key "token"
             :model "mistral-large-3:675b"
             :narrative-dir (str narrative-dir)
             :myths-path (str (io/file root ".myth" "myths.jsonl"))}]
    (try
      (io/make-parents (io/file narrative-dir "Chapter_01_First.md"))
      (spit (io/file narrative-dir "Chapter_01_First.md") "# 1 — First\n\nThe first memory stays warm.")
      (spit (io/file narrative-dir "Chapter_02_Second.md") "# 2 — Second\n\nThe second memory sharpens into a blade.")
      (let [history (fork-tales/chapter-history cfg)
            first-history (first (:chapters history))
            detail (fork-tales/chapter-detail cfg 2)]
        (is (= 2 (:chapter_count history)))
        (is (= 2 (:number first-history)))
        (is (= "Second" (:title first-history)))
        (is (string? (:preview first-history)))
        (is (not (str/includes? (:preview first-history) "# 2 —")))
        (is (= 2 (:number detail)))
        (is (str/includes? (:text detail) "The second memory sharpens into a blade.")))
      (finally
        (delete-tree! root)))))
