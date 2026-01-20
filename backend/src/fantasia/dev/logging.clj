(ns fantasia.dev.logging)

(def log-levels
  {:error 0
   :warn 1
   :info 2
   :debug 3})

(def current-level
  (or (some-> (System/getenv "LOG_LEVEL")
              keyword
              log-levels)
      0))

(defn should-log?
  [level]
  (<= (log-levels level) current-level))

(defn log-error
  [& args]
  (when (should-log? :error)
    (apply println "[ERROR]" args)))

(defn log-warn
  [& args]
  (when (should-log? :warn)
    (apply println "[WARN]" args)))

(defn log-info
  [& args]
  (when (should-log? :info)
    (apply println "[INFO]" args)))

(defn log-debug
  [& args]
  (when (should-log? :debug)
    (apply println "[DEBUG]" args)))
