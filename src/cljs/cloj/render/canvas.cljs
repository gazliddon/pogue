(ns cloj.render.canvas
  (:require 
    [cloj.math.vec2        :as v2]
    [cloj.render.protocols :as rp]))

(defn canvas [ctx dims]
  (reify
    ITransformable

    (matrix! [this [a c e b d f]]
      (.setTransform ctx a b c d e f)
      this)

    (identity! [this]
      (.resetTransform ctx)
      this)

    (translate! [this [ {:keys [x y]}]]
      (.translate ctx x y)
      this)

    (scale! [this [ {:keys [x y] } ]]
      (.scale ctx x y)
      this)

    (rotate! [this v]
      (.rotate ctx v)
      this)

    IRenderBackend

    (spr-scaled! [_ _]
      (println "not implemented"))

    (spr! [_ _]
      (println "not implemented"))

    (clear! [this col]
      (box! this [(v2 0 0) dims col]))

    (box! [_  [{x :x y :y} {w :x h :y} col]]
      (let [col-str (rgb-str col)]
        (set! (.-fillStyle ctx) col-str)
        (.fillRect ctx x y w h)))))
