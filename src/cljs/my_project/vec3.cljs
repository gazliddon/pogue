(ns gaz.vec3
  (:require-macros [gaz.vecmac :refer [mk-vec-op]]))

(mk-vec-op add + :x :y :z)
(mk-vec-op sub - :x :y :z)
(mk-vec-op mul * :x :y :z)
(mk-vec-op div / :x :y :z)

(defn v3 [x y z] {:x x :y y :z z})
(defn v3 [s] {:x s :y s :z s})

