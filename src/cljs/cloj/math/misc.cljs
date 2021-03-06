(ns cloj.math.misc)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Proxies for js - needs to be in own file
(defn log [v] (Math/log v))
(defn cos [^float v] (Math/cos v))
(defn sin [^float v] (Math/sin v))

(defn ceil [v] (Math/ceil v))
(defn floor [v] (Math/floor v))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



(defn fract "doc-string" [v] (- v (floor v)))

(defn clamp [l h v] (min h (max v l)))

(defn sin-01 [^float v] (/ (+ 1 (sin v)) 2))
(defn cos-01 [^float v] (/ (+ 1 (cos v)) 2))


(defn in-range? [lo hi v] (= v (clamp lo hi v)))
(defn clamp01 [^float v] (min 1 (max v 0)))
(defn to-255 [^float v] (int (* 255 (clamp01 v)))  )

(defn log-base-n [v n] (/ (log v) (log n)))
(defn log-2 [v] (log-base-n v 2))

(defn num-digits [v base ] (floor (inc  (log-base-n v base))))
