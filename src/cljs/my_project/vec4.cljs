(ns gaz.vec4
  (:require-macros [gaz.vecmac :refer [mk-vec-op]]))

(mk-vec-op add + :x :y :z :w)
(mk-vec-op sub - :x :y :z :w)
(mk-vec-op mul * :x :y :z :w)
(mk-vec-op div / :x :y :z :w)

