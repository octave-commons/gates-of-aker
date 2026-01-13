(ns fantasia.dev.watch
  (:require [clojure.java.io :as io])
  (:import (java.nio.file FileSystems Paths StandardWatchEventKinds WatchEvent$Kind)))

(def watch-kinds
  (into-array WatchEvent$Kind
              [StandardWatchEventKinds/ENTRY_CREATE
               StandardWatchEventKinds/ENTRY_MODIFY
               StandardWatchEventKinds/ENTRY_DELETE]))

(defn- ensure-dir ^java.io.File [path]
  (let [f (io/file path)]
    (if (.isDirectory f) f (.getParentFile f))))

(defn- start-process []
  (println "[watch] starting clojure -M:server")
  (-> (ProcessBuilder. (into-array String ["clojure" "-M:server"]))
      (.directory (io/file "."))
      (.inheritIO)
      (.start)))

(defn- stop-process [^Process proc]
  (when proc
    (println "[watch] stopping clojure -M:server")
    (.destroy proc)
    (.waitFor proc)))

(defn watch-server
  "Watch source files and restart the Fantasia server on change."
  ([] (watch-server {:paths ["src" "deps.edn"]}))
  ([{:keys [paths] :or {paths ["src" "deps.edn"]}}]
   (let [fs (FileSystems/getDefault)
         watcher (.newWatchService fs)
         dirs (->> paths (map ensure-dir) (remove nil?) distinct)]
     (doseq [dir dirs]
       (.register (.toPath dir) watcher watch-kinds))
      (println "[watch] watching" (map #(.getPath ^java.io.File %) dirs))
      (loop [proc (start-process)]
        (let [key (.take watcher)]
          (stop-process proc)
          (Thread/sleep 200)
          (.reset key)
          (recur (start-process)))))))

