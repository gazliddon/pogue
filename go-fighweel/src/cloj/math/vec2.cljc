(ns cloj.math.vec2
  (:refer-clojure :exclude [min max apply])



  (:require
    [clojure.reflect :as reflect :refer [reflect]]
    #?(:clj [cloj.macros :refer [mk-vec-op]])
    [cloj.math.protocols :as math-p :refer [ IMathOps ]])

  #?(:cljs ( :require-macros [cloj.macros :refer [mk-vec-op]])))

(defrecord Vec2 [x y])

(defn as-vector [v] (mapv v [:x :y]))

(mk-vec-op add + :x :y)
(mk-vec-op sub - :x :y)
(mk-vec-op mul * :x :y)
(mk-vec-op div / :x :y)
(mk-vec-op min clojure.core/min :x :y)
(mk-vec-op max clojure.core/max :x :y)

(def zero (->Vec2 0 0))
(def half (->Vec2 0.5 0.5))
(def one  (->Vec2 1 1))

(defn clamp [lo hi v] (min hi (max lo v)))

(defn neg [v] (sub zero v))

(defn apply [f {x :x y :y}] (->Vec2 (f x) (f y)))

(defn applyv [{fx :x fy :y} {x :x y :y}]
  (->Vec2 (fx x) (fy y)))

(def left       (->Vec2 -1  0))
(def right      (->Vec2  1  0))
(def up         (->Vec2  0 -1))
(def down       (->Vec2  0  1))
(def left-up    (->Vec2 -1 -1))
(def left-down  (->Vec2 -1  1))
(def right-up   (->Vec2  1 -1))
(def right-down (->Vec2  1  1))

(def up-left    left-up)
(def down-left  left-down)
(def up-right   right-up)
(def down-right right-down)

(defn v2  [x y] (->Vec2 x y))
(defn v2i [x y] (->Vec2 (int x) (int y)))
(defn v2f [x y] (->Vec2 (float x) (float y)))

(extend-type Vec2
  IMathOps
  (min [a b] (min a b))
  (max [a b] (max a b))
  (add [a b] (add a b))
  (sub [a b] (sub a b))
  (mul [a b] (mul a b))
  (div [a b] (div a b)))

