(ns gaz.color
  (:require
    [cloj.math.misc :as gm]))

(defn rgb-255 [ ^floats [r g b] ]
  [ (gm/to-255 r)
    (gm/to-255 g)
    (gm/to-255 b) ])

(defn rgb-str [rgb]
  (let [[r8 g8 b8] (rgb-255 rgb)]
    (str "rgb(" r8 "," g8 "," b8  ")" )))

(def purple [1 0 1])
