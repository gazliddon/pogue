(ns game.tilemaputils
  (:require
    [game.tilemapprotocol :as tmp :refer [ITileMap]]
    ))

(defn get-size [tiles]
  [(tmp/get-width tiles) (tmp/get-height tiles)] )

(defn get-max-coords [tiles]
  (let [[w h] (get-size tiles)]
    [ (dec w) (dec h)]))

(defn clear [tiles v]
  (let [[w h] (get-size tiles)]
    (tmp/fill tiles 0 0 w h v)))

(defn reducer [this f memo-init]
 (let [[width height] (get-size this)]
  (loop [memo memo-init i 0]
    (if (< i (* width height))
      (let [x (int (mod i width))
            y (int (/ i width))
            new-memo (f memo x y (tmp/get-tile this x y)) ]
        (recur new-memo (inc i)))
      memo))))
