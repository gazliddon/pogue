(ns cloj.renderutils
  (:require
    [cloj.math.vec2         :as v2 :refer [ v2f ]] ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-aspect-ratio
  "Get the aspect ratio for this vector"
  [{w :x h :y}] (/ w h))

(defn get-viewport
  "Take dimensions of the window we're displaying in
   and the canvas we want to draw in and then return
   the viewport for the window for that canvas at it's
   maximum size but maintaining aspect ratio"
  [win-dims canv-dims]

  (let [canv-ar (get-aspect-ratio canv-dims)
        win-ar (get-aspect-ratio win-dims)
        dom-axis (if (> canv-ar win-ar) :x :y)
        scale (/ (dom-axis win-dims) (dom-axis canv-dims))
        vp-dims (v2/mul
                  (v2f scale scale)
                  canv-dims)
        tl  (v2/mul v2/half (v2/sub win-dims vp-dims)) ]

    (mapv int [(:x tl)
               (:y tl)
               (:x vp-dims)
               (:y vp-dims) ] )
    ))
