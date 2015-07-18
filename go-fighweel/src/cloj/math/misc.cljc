(ns cloj.math.misc
  (:require
    [cloj.utils :as utils :refer [with-clj with-cljs]]
    #?(:clj [clojure.math.numeric-tower :as nm])
    ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Proxies for js - needs to be in own file
(def epsilon 0.00001)

(with-cljs
  (defn log [v] (Math/log v))
  (defn cos [^float v] (Math/cos v))
  (defn sin [^float v] (Math/sin v))

  (defn ceil [v] (nm/ceil v))
  (defn floor [v] (nm/floor v))
  (defn abs [v] (nm/abs v))
  (defn sqrt [v] (nm/sqrt v)))

(with-clj
  (defn log   [^double v] (Math/log v))
  (defn cos   [^double v] (Math/cos v))
  (defn sin   [^double v] (Math/sin v))
  (defn sqrt  [^double v] (Math/sqrt v))
  (defn exp   [^double v] (Math/exp v))

  (defn abs   [v]         (Math/abs v))

  (defn ceil  [v]         (Math/ceil v))
  (defn floor [v]         (Math/floor v))
  (defn round [v]         (Math/round v))
  )

(defn float=
  ([x y] (float= x y 0.00001))
  ([x y epsilon]
     (let [scale (if (or (zero? x) (zero? y)) 1 (abs x))]
       (<= (abs (- x y)) (* scale epsilon)))) )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn fract "doc-string" [v] (- v (floor v)))

(defn clamp [l h v] (if (< l h)
                      (min h (max v l)) 
                      (min l (max v h)) ))

(defn sin-01 [^double v] (/ (+ 1 (sin v)) 2))
(defn cos-01 [^double v] (/ (+ 1 (cos v)) 2))

(defn in-range? [lo hi v] (= v (clamp lo hi v)))

(defn clamp01 [^double v] (min 1 (max v 0)))
(defn to-255 [^double v] (round (* 255 (clamp01 v)))  )

(defn log-base-n [v n] (/ (log v) (log n)))
(defn log-2 [v] (log-base-n v 2))
(defn num-digits [v base ] (inc (round (log-base-n v base))))

(defn next-power-of-two [n]
  (loop [x 2]
    (if (< x n)
      (recur (* x 2))
      x)))


