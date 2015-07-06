(ns cloj.math.vec3
  (:require-macros [cloj.macros :refer [mk-vec-op]]))

(mk-vec-op add + :x :y :z)
(mk-vec-op sub - :x :y :z)
(mk-vec-op mul * :x :y :z)
(mk-vec-op div / :x :y :z)

(defn vec3
  ([s] (vec3 s s s))
  ([x y z] { :x x :y y :z z }))

(defn to-vec [v]
  (mapv v [:x :y :z]))

