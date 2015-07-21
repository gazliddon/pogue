(ns game.tiles
  (:require
    [game.tilemapprotocol :as tmp :refer [ITileMap]]
    [game.tilemaputils    :as tmu]
    [cloj.math.vec2       :as v2 :refer [vec2]]
    [cloj.math.misc       :refer [in-range?
                                  clamp]]))

(defn- tiles-four [tile] (into [] (take 4 (repeat tile))))
(def duff-tile {:gfx (tiles-four :b-purple)})

(defn in-rect? [[x y w h] x' y']
  (and 
    (in-range? x (+ x w -1) x')
    (in-range? y (+ y h -1) y')))

(defn mk-2d-vec [w h v]
  (vec (take h (repeat (mapv (fn [_] v) (range w))))))

(defn mk-tile-map-cell [all-tile-data id]
  (->
    (get all-tile-data id duff-tile)
    (assoc :id id)))

(defn mk-map-tile-cell [{:keys [gfx] :as tile} x y]
  (let [pos (vec2 x y)
        pos-px (v2/mul (vec2 16 16) pos)
        offsets (for [x [0 16] y [0 16]] (v2/add pos-px (vec2 x y)))]
    (assoc tile 
           :pos pos
           :pixel-pos pos-px
           :print-info (map vector gfx offsets))))

(defrecord TileMap [width height tiles all-tile-data]

  ITileMap

  (get-width [_] width)
  (get-height [_] height)

  (get-tile [_ x y]
    (if (in-rect? [0 0 width height] x y)
      (-> tiles (nth y) (nth x))
      nil))

  (get-tile-data [_ tile-id]
    (mk-tile-map-cell all-tile-data tile-id))

  (set-tile [this x y tile-id]
    (if (in-rect? [0 0 width height] x y)
      (let [tile (->
                   (tmp/get-tile-data this tile-id)
                   (mk-map-tile-cell x y)) ]
        (->>
          (-> (nth tiles y) (assoc x tile))
          (assoc tiles y)
          (assoc this :tiles)))
      this)
    )

  (fill [this x y w h v]
    (throw "fill not implemented")
    this))

(defn mk-tile-map [w h v all-tile-data]
  (TileMap. w h (mk-2d-vec w h (mk-tile-map-cell all-tile-data v)) all-tile-data))

(defn traverse-map! [f level]
  (tmu/reducer
    level
    (fn [memo x y v]
      (cons (f level (v2/v2 x y) v) memo))
    ()))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
