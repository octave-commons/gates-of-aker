(ns fantasia.dev.format
  (:require [clojure.java.shell :refer [sh]]
            [clojure.string :as str]))

(defn run
  "Run cljfmt to format src and test directories."
  [_]
  (println "[format] Running cljfmt...")
  (let [result (sh "cljfmt" "fix" "src" "test")]
    (println (:out result))
    (when-not (str/blank? (:err result))
      (println "[format] stderr:" (:err result))))
  (println "[format] Done. Run 'clojure -X:lint' to verify formatting."))
