(ns gaz.tiles
  (:require [gaz.tilemapprotocol :as tmp]
            [cloj.math.misc :refer [in-range?
                                    clamp]]))

(defn in-rect? [[x y w h] x' y']
  (and 
    (in-range? x (+ x w -1) x')
    (in-range? y (+ y h -1) y')))

(defn mk-2d-vec [w h v]
  (vec (take h (repeat (mapv (fn [_] v) (range w))))))

(defrecord TileMap [width height tiles]
  tmp/ITileMap
  (get-width [_] width)
  (get-height [_] height)

  (get-tile [_ x y]
    (if (in-rect? [0 0 width height] x y)
      (-> tiles (nth y) (nth x))
      nil))

  (set-tile [this x y v]
    (if (in-rect? [0 0 width height] x y)
      (let [line (nth tiles y)
            new-line (assoc line x v)
            new-tiles (assoc tiles y new-line) ]
        (assoc this :tiles new-tiles))
      this))

  (fill [this x y w h v]
    this))

(defn mk-tile-map [w h v]
  (TileMap. w h (mk-2d-vec w h v)))

(defn clip-rect [ [x y w h] [x' y' w' h']]
  (let [clamp-x #(clamp x (+ x w -1) %) 
        clamp-y #(clamp y (+ y h -1) %) 
        nx (clamp-x x')
        ny (clamp-y y')
        nx1 (clamp-x (+ x' w' -1))
        ny1 (clamp-y (+ y' h' -1))
        nw (- nx1 nx )
        nh (- ny1 ny ) ]
    (if (or (= 0 nw) (= 0 nh))
      nil
      [ nx ny nw nh ]  )))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
