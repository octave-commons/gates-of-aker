(ns fantasia.sim.names)

(def ^:private names-file "data/names.edn")
(def ^:private default-names
  {:priest ["Caius" "Octavia" "Livia" "Marcus" "Aurelia" "Valerius" "Flavia" "Cornelius" "Drusilla" "Sextus"
            "Titus" "Claudia" "Gaius" "Julia" "Lucius" "Paulina" "Hadrian" "Sabina" "Quintus"]
   :knight ["Gareth" "Isolde" "Roland" "Elaine" "Percival" "Bors" "Lancelot" "Guinevere" "Tristan" "Iseult"
            "Gawain" "Morgana" "Bedivere" "Lamorak" "Erec" "Enid" "Geraint" "Mabuz"]
   :peasant ["Alden" "Brea" "Colm" "Dara" "Eamon" "Fiona" "Gael" "Hanna" "Ian" "Jora"
            "Kael" "Lira" "Merrick" "Nola" "Orin" "Pia" "Rowan" "Sera" "Taran"]})

(defn- names-dir []
  (let [dir (java.io.File. "data")]
    (when-not (.exists dir)
      (.mkdirs dir))
    dir))

(defn- save-names! [names-map]
  (let [dir (names-dir)
        file (java.io.File. dir "names.edn")]
    (spit file (pr-str names-map))
    names-map))

(defn- load-names []
  (let [file (java.io.File. names-file)]
    (if (.exists file)
      (try
        (clojure.edn/read-string (slurp file))
        (catch Exception e
          (println "[NAMES] Error loading names:" (.getMessage e))
          default-names))
        default-names)))

(defn- ensure-names-file! []
  (let [dir (names-dir)]
    (when-not (.exists (java.io.File. dir "names.edn"))
      (save-names! default-names)
      (println "[NAMES] Created default names file"))))

(defn- pick-name [rng role]
  (let [names-map (load-names)
        role-names (role names-map default-names)]
    (when (seq role-names)
      (nth role-names (.nextInt rng (count role-names))))))

(defn generate-names-for-world! [agent-count rng]
  (println "[NAMES] Ensuring names file exists...")
  (ensure-names-file!)
  (println "[NAMES] Generating" agent-count "names for agents...")
  (let [roles (cycle [:priest :knight :peasant])]
    (->> (map #(pick-name rng %) roles)
         (take agent-count)
         vec)))
