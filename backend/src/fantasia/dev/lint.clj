(ns fantasia.dev.lint
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string]))

(defn run
  "Run clj-kondo linting on src and test directories."
  [_]
  (println "[lint] Running clj-kondo...")
  (let [result (sh "clj-kondo" "--lint" "src" "--lint" "test")]
    (println (:out result))
    (when-not (clojure.string/blank? (:err result))
      (println (:err result)))
    (System/exit (:exit result))))
