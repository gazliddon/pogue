(ns gaz.tiles
  (:require [gaz.tilemapprotocol :as tmp]
            [gaz.tilemaputils :as tmu]
            [gaz.color :as col]
            [cloj.render.protocols :as rp]
            [cloj.math.vec2 :as v2 :refer [vec2]]
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

(defn traverse-map [f level]
  (tmu/reducer
    level
    (fn [memo x y v]
      (cons (f level (v2/v2 x y) v) memo))
    ()))

(def duff-tile {:b0 col/purple})

(def tiles
  {
   :b0      {:col [0.15 0.15 0.15]}
   :b1      {:col [0 1 0]}
   :b2      {:col [0 0 0.75]}
   :b-floor {:col [0.25 0.25 0]}
   :b4      {:col [0.25 0.25 0]}
   :b5      {:col [0.25 0.25 0]}
   :b6      {:col [0.25 0.25 0]}
   :b7      {:col [0.25 0.25 0]}
   })

(defn render-tile! [rend level pos v]
  (let [tile  (get tiles v duff-tile) ]
    (rp/spr! rend v (v2/mul (vec2 16 16) pos))))

(defn render-level! [rend level]
  (let [render-tile! (partial render-tile! rend)] 
    (traverse-map render-tile! level)
    rend))

(defn rand-coord [level]
  (let [[w h] (tmu/get-size level)
        [x y] [(rand-int w) (rand-int h)] ]
    [x y]))

(defn rand-tile [] (rand-nth (keys tiles )))

(defn set-rand-tile [l]
  (let [[x y] (rand-coord l)
        tile (rand-tile) ]
    (tmp/set-tile l x y tile)))

(defn mix-it-up [level]
  (reduce
    (fn [memo i] (set-rand-tile memo))
    level
    (range 1000)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
