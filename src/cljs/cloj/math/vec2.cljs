(ns cloj.math.vec2
  (:refer-clojure :exclude [min max])
  (:require [cloj.math.protocols :as math]
            )
  (:require-macros [cloj.macros :refer [mk-vec-op]]))

(mk-vec-op add + :x :y)
(mk-vec-op sub - :x :y)
(mk-vec-op mul * :x :y)
(mk-vec-op div / :x :y)
(mk-vec-op min clojure.core/min :x :y)
(mk-vec-op max clojure.core/max :x :y)

(defn clamp [lo hi v]
  (min hi (max lo v)))

(defrecord Vec2 [x y])

(defn v2 [x y] (->Vec2 x y))

(defn vec2 
  ([x y]   {:x x :y y})  
  ([[x y]] {:x x :y y}))

(defn vec2-s [s] (vec2 s s) )

(defn applyv [{fx :x fy :y} {x :x y :y}]
  (vec2 (fx x) (fy y)))

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
