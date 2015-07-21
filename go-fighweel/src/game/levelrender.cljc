;; =============================================================================
; {{{ Requires
(ns game.levelrender
  (:require
    [game.tilemapprotocol  :as tmp ]
    [game.tiles            :as tiles]
    [game.sprs             :as sprs]
    [cloj.math.vec2        :as v2     :refer [v2 v2i v2f]]
    [cloj.protocols.render :as rend-p :refer [IRenderBackend
                                              IRenderManager
                                              make-render-target! 
                                              ortho!
                                              clear!
                                              identity!
                                              render-to
                                              spr!]])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(set! *warn-on-reflection* true)

;; }}}
;; =============================================================================
;; {{{ tiles
(defn shit-line [tmap block {:keys [x y] :as pos} add len]
  (let [v2-len (v2i len len) ]
    (loop [tmap (tmp/set-tile tmap x y block)
           i    (dec len)]
      (if (pos? i)
        (let [{x :x y :y} (->
                            (v2/mul add (v2i i i))
                            (v2/add pos)) ]
          (recur (tmp/set-tile tmap x y block)
                 (dec i)))
        tmap))))

(defn shit-h-line [tmap bl p len] (shit-line tmap bl p v2/right len))
(defn shit-v-line [tmap bl p len] (shit-line tmap bl p v2/down len))

(defn shit-box [tmap bl {:keys [x y] :as pos}  {w :x h :y}]
  (->
    (fn [res y]
      (shit-h-line res bl (v2/add pos (v2i 0 y)) w))
    (reduce tmap (range h))))

(defn shit-room [tmap {:keys [x y] :as pos} {w :x h :y}]
  (let [fl :floor
        wl :wall
        {x1 :x y1 :y} (v2/add pos (v2i (dec w) (dec h)))
        ]
    (-> tmap
        (shit-box fl pos (v2i w h))
        (shit-h-line wl pos w)
        (shit-h-line wl (v2i x y1) w)
        (shit-v-line wl (v2i x  y) h)
        (shit-v-line wl (v2i y1 x) h))))

(def tile-offsets
  (let [mul 16
        mul-vec (v2i mul mul) ]
    (->>
      (for [x [0 1] y [0 1]] (v2i x y))
      (mapv #(v2/mul mul-vec %))
      (into []))))

(defn mk-tile-printer [rend]
  (reify
    IRenderBackend
    (spr! [this {gfx :gfx} pos]
      (doseq [ [tile offset ] (map vector gfx (map #(v2/add pos %) tile-offsets))]
        (spr! rend tile offset)))))

(defn render-level! [render-target level sprs]
  (let [spr-printer (sprs/mk-spr-printer render-target sprs)
        tile-printer (mk-tile-printer spr-printer)
        [w h] [(tmp/get-width level) (tmp/get-height level)]
        to-print (for [x (range w) y (range h)]
                   {:pos  (v2i x y)
                    :pixel-pos (v2/mul (v2i 32 32) (v2i x y))
                    :tile (tmp/get-tile level x y)}) ]

    (doseq [{pos :pixel-pos tile :tile} to-print ]
      (spr! tile-printer tile pos)   
      )))

(defn mk-level-spr! [rman sprs w-b h-b all-tile-data]
  (let [dims (v2i (* 16 w-b) (* 16 h-b))
        render-target (rend-p/make-render-target! rman dims)
        level (->
                (tiles/mk-tile-map w-b h-b :blank all-tile-data)
                (shit-room (v2i 3 3) (v2i 10 10 ))) ]
    (do
      (render-to render-target
          (ortho! dims dims)
          (identity!)
          (clear! [0 1 0 1])
          (render-level! level sprs)
          )

      render-target)))

;; }}}

