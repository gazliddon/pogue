(ns cloj.math.vec2.faster )

(defprotocol IVecMath
  (add [_ b]))

(defmulti v-lookup (fn [a k] k))

(defmethod v-lookup :x [a k] (nth a 0))
(defmethod v-lookup :y [a k] (nth a 1))
(defmethod v-lookup :z [a k] (nth a 2))
(defmethod v-lookup :w [a k] (nth a 3))
(defmethod v-lookup :r [a k] (nth a 0))
(defmethod v-lookup :b [a k] (nth a 1))
(defmethod v-lookup :g [a k] (nth a 2))
(defmethod v-lookup :a [a k] (nth a 3))

(defmethod v-lookup :default [a k] nil)

(defrecord Vec21 [ data ]
  IVecMath
  (add [{x :x y :y} {x1 :x y1 :y}]
    )

  clojure.lang.ILookup
  (valAt [_ k]
    (v-lookup data k))
   
  )

(def v1 (vec2 10 10))
(def v2 (vec2 12 11))
(get-vec  (add v1 v2))

(println (type v))

(:x v)

(v-lookup [0 1 2] :y)

