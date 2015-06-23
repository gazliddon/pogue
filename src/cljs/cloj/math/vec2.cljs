(ns cloj.math.vec2
  (:require-macros [cloj.macros :refer [mk-vec-op]]))

(mk-vec-op add + :x :y)
(mk-vec-op sub - :x :y)
(mk-vec-op mul * :x :y)
(mk-vec-op div / :x :y)

(defn v2 [x y] {:x x :y y})

(defn vec2 
  ([x y]   {:x x :y y})  
  ([[x y]] {:x x :y y}))

(defn applyv [{fx :x fy :y} {x :x y :y}]
  (vec2 (fx x) (fy y)))

