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

(defn clamp [ {xlo :x ylo :y} {xhi :x yhi :y}{x :x y :y} ]
  (vec2 (min xhi (max xlo x))
        (min yhi (max ylo y))))

(def left  (vec2 -1  0))
(def right (vec2  1  0))
(def up    (vec2  0 -1))
(def down  (vec2  0  1))

(def left-up    (vec2 -1 -1))
(def left-down  (vec2 -1  1))
(def right-up   (vec2  1 -1))
(def right-down (vec2  1  1))

(def up-left    left-up)
(def down-left  left-down)
(def up-right   right-up)
(def down-right right-down)


