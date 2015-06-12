(ns cloj.math.vec4
  (:require-macros [cloj.macros :refer [mk-vec-op]]))

(mk-vec-op add + :x :y :z :w)
(mk-vec-op sub - :x :y :z :w)
(mk-vec-op mul * :x :y :z :w)
(mk-vec-op div / :x :y :z :w)

(defn v4 [x y z w] {:x x :y y :z z :w w})
(defn v4 [s] {:x s :y s :z s :w s})
