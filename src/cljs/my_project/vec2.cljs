(ns gaz.vec2
  (:require-macros [gaz.vecmac :refer [mk-vec-op]]))

(mk-vec-op add + :x :y)
(mk-vec-op sub - :x :y)
(mk-vec-op mul * :x :y)
(mk-vec-op div / :x :y)

(defn v2 [x y] {:x x :y y})
