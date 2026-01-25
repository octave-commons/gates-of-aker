(ns fantasia.dev.autofix
  (:require [clojure.java.io :as io]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str])
  (:import (java.io File)))

(defn run-clj-kondo []
  (let [result (sh "clj-kondo" "--lint" "src" "--lint" "test"
                   "--no-parallel")]
    (when (seq (:err result))
      (println "[autofix] clj-kondo stderr:" (:err result)))
    (let [lines (str/split-lines (:out result))
          issues (keep (fn [line]
                        (when-let [[_ file-path line-col type-level message]
                                   (re-find #"^([^:]+):(\d+):(\d+): (warning|error|info): (.+)$" line)]
                          {:filename file-path
                           :row (Integer/parseInt line-col)
                           :col (Integer/parseInt (second (re-seq #"\d+" line-col)))
                           :level type-level
                           :type (case type-level
                                   "warning" (when (str/includes? message "unused binding") "unused-binding"
                                             (when (str/includes? message "Unused import") "unused-import"
                                               (when (str/includes? message "Misplaced docstring") "misplaced-docstring")))
                           :file-path file-path
                           :message message}))
                      lines)]
      (vec issues))))

(defn unused-binding? [issue]
  (= "unused-binding" (:type issue)))

(defn unused-import? [issue]
  (= "unused-import" (:type issue)))

(defn misplaced-docstring? [issue]
  (= "misplaced-docstring" (:type issue)))

(defn read-file [path]
  (slurp (io/file path)))

(defn write-file [path content]
  (spit (io/file path) content))

(defn prefix-with-underscore [binding-name]
  (str "_" binding-name))

(defn remove-line-by-line-col [content line col]
  (let [lines (str/split-lines content)
        line-idx (dec line)
        target-line (get lines line-idx)
        _ (when-not target-line
            (throw (ex-info "Line not found" {:line line})))
        col-idx (dec col)
        prefix (subs target-line 0 col-idx)
        suffix (subs target-line col-idx)]
    (assoc lines line-idx (str prefix "_" suffix))))

(defn fix-unused-binding [content line col _binding-name]
  (remove-line-by-line-col content line col))

(defn remove-import-line [content line]
  (let [lines (str/split-lines content)
        line-idx (dec line)
        prev-line (get lines (dec line-idx))]
    (if (str/blank? prev-line)
      (vec (concat (subvec lines 0 (dec line-idx)) (subvec lines line)))
      (vec (concat (subvec lines 0 line-idx) (subvec lines line))))))

(defn remove-require [content ns-name]
  (let [lines (str/split-lines content)
        ns-line-idx (some #(when (and (str/includes? % ":require")
                                      (str/includes? % ns-name)) %) lines)]
    (if ns-line-idx
      (vec (remove #(= % ns-line-idx) lines))
      lines)))

(defn fix-file [path issues]
  (let [content (read-file path)
        sorted-issues (sort-by (juxt :row :col) issues)
        fixed-content (reduce
                       (fn [curr-content issue]
                         (cond
                           (unused-binding? issue)
                           (fix-unused-binding curr-content (:row issue) (:col issue) (:message issue))
                           
                           (unused-import? issue)
                           (remove-import-line curr-content (:row issue))
                           
                           (misplaced-docstring? issue)
                           (let [lines (str/split-lines curr-content)
                                 line-idx (dec (:row issue))]
                             (vec (concat (subvec lines 0 line-idx) (subvec lines (inc line-idx)))))
                           
                           :else curr-content))
                       content
                       sorted-issues)]
    (write-file path (str/join "\n" fixed-content))))

(defn group-issues-by-file [issues]
  (group-by :filename issues))

(defn filter-fixable-issues [issues]
  (filter
    (some-fn unused-binding?
             unused-import?
             misplaced-docstring?)
    issues))

(defn autofix [_]
  (println "[autofix] Running clj-kondo to detect issues...")
  (let [issues (run-clj-kondo)
        fixable (filter-fixable-issues issues)]
    (println "[autofix] Found" (count issues) "total issues")
    (println "[autofix] Can auto-fix" (count fixable) "issues")
    (if (zero? (count fixable))
      (println "[autofix] No fixable issues found")
      (let [by-file (group-issues-by-file fixable)]
        (doseq [[file-path file-issues] by-file]
          (println "[autofix] Fixing" (count file-issues) "issues in" file-path)
          (try
            (fix-file file-path file-issues)
            (println "[autofix] ✓ Fixed" file-path)
            (catch Exception e
              (println "[autofix] ✗ Error fixing" file-path ":" (.getMessage e)))))))
    (println "[autofix] Done. Run 'clojure -X:lint' to verify fixes.")))
