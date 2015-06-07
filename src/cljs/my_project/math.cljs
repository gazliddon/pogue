(ns gaz.math)

(defn clamp [l h v] (min h (max v l)))
(defn cos [^float v] (Math/cos v))
(defn cos-01 [^float v] (/ (+ 1 (cos v)) 2))
(defn in-range? [lo hi v] (= v (clamp lo hi v)))
(defn clamp01 [^float v] (min 1 (max v 0)))
(defn to-255 [^float v] (int (* 255 (clamp01 v)))  )
