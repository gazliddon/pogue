(ns cloj.math.vec2.faster )

(defprotocol IVecMath
  (+ [_ b]))

(defmulti vec-lookup (fn [a k] a))

(defmethod vec-lookup :x (fn [a _] (nth a 0)))
(defmethod vec-lookup :y (fn [a _] (nth a 1)))
(defmethod vec-lookup :z (fn [a _] (nth a 2)))
(defmethod vec-lookup :w (fn [a _] (nth a 3)))
(defmethod vec-lookup :r (fn [a _] (nth a 0)))
(defmethod vec-lookup :b (fn [a _] (nth a 1)))
(defmethod vec-lookup :g (fn [a _] (nth a 2)))
(defmethod vec-lookup :a (fn [a _] (nth a 3)))

(defmethod vec-lookup :default (fn [a k]
                                (throw (str "unknown key " k ))))

(defn vec2 [x y]
  (let [data [x y]]
    (reify
      IVecMath
      (+ [_ [x1 y1]]
        (vec2 (+x x1) (+y y1)))
      ILookup
      (-lookup [_ k]
        (vec-lookup k data))
      )
    )
  )
