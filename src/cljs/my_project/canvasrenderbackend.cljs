(ns gaz.canvascomprenderbackend
  (:require [gaz.renderprotocols :as rp]
            [gaz.color           :refer [rgb-str]]
            [gaz.vec2            :as    v2 ])
  )

(defn canvas-immediate-renderer [canvas dims]
  (let [ctx (.getContext canvas "2d") ]
    (reify
      rp/ITransform

      (matrix! [this [a c e b d f]]
        (.setTransform ctx a b c d e f)
        this)

      (identity! [this]
        (.resetTransform ctx)
        this)

      (translate! [this {x :x y :y}]
        (.translate ctx x y)
        this)

      (scale! [this {w :x h :y}]
        (.scale ctx w h)
        this)

      (rotate! [this v]
        (.rotate ctx v)
        this)

      rp/IRenderBackend

      (load-sprs! [_ _]
        (println "not implemented"))

      (spr-scaled! [_ _]
        (println "not implemented"))

      (spr! [_ _]
        (println "not implemented"))

      (clear! [this col]
        (rp/box! this [(v2/v2 0 0) dims col]))

      (box! [_  [{x :x y :y} {w :x h :y} col]]
        (let [col-str (rgb-str col)]
          (set! (.-fillStyle ctx) col-str)
          (.fillRect ctx x y w h))))))
