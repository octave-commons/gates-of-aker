(ns fantasia.sim.time)

(def seasons [:spring :summer :autumn :winter])
(def ticks-per-day 200)
(def days-per-season 2)
(def ticks-per-season (* ticks-per-day days-per-season))
(def ticks-per-year (* ticks-per-season (count seasons)))

(defn clamp01 [v]
  (cond
    (< v 0.0) 0.0
    (> v 1.0) 1.0
    :else v))

(defn temperature-at [seed tick]
  (let [base 0.6
        seasonal (* 0.25 (Math/sin (+ (/ seed 13.0)
                                      (* 2.0 Math/PI (/ tick ticks-per-year)))))
        drift (* 0.08 (Math/sin (+ (/ seed 7.0) (* 2.0 Math/PI (/ tick 37.0)))))
        temp (+ base seasonal drift)]
    (clamp01 temp)))

(defn daylight-at [seed tick]
  (let [cycle (* 2.0 Math/PI (/ tick ticks-per-day))
        wobble (* 0.08 (Math/sin (+ (/ seed 9.0) (* 2.0 Math/PI (/ tick 53.0)))))
        value (+ 0.5 (* 0.5 (Math/sin cycle)) wobble)]
    (clamp01 value)))

(defn- time-of-day [day-progress]
  (cond
    (< day-progress 0.2) :night
    (< day-progress 0.3) :dawn
    (< day-progress 0.7) :day
    (< day-progress 0.8) :dusk
    :else :night))

(defn calendar-info [world]
  (let [tick (long (or (:tick world) 0))
        day-progress (if (pos? ticks-per-day)
                       (/ (mod tick ticks-per-day) (double ticks-per-day))
                       0.0)
        day (inc (long (Math/floor (/ tick ticks-per-day))))
        year (inc (long (Math/floor (/ tick ticks-per-year))))

        season-index (int (Math/floor (/ (mod tick ticks-per-year)
                                         (double ticks-per-season))))
        season (nth seasons (min season-index (dec (count seasons))))]
    {:tick tick
     :day day
     :year year
     :season season
     :day-progress day-progress
     :hour (* 24.0 day-progress)
     :time-of-day (time-of-day day-progress)
     :daylight (:daylight world)
     :temperature (:temperature world)
     :cold-snap (get-in world [:levers :cold-snap])}))
