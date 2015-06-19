(ns cloj.render.canvas
  (:require 
    [gaz.color :refer [rgb-str]]
    [cloj.math.vec2        :refer [ v2 ]]
    [cloj.render.protocols :as rp]))

(defn canvas [ctx dims]
  (reify
    rp/ITransformable

    (matrix! [this [a c e b d f]]
      (do
        (.setTransform ctx a b c d e f))
      this)

    (identity! [this]
      (do
        (.resetTransform ctx))
      this)

    (translate! [this [ {:keys [x y]}]]
      (do
        (.translate ctx x y))
      this)

    (scale! [this [ {:keys [x y] } ]]
      (do
        (.scale ctx x y))
      this)

    (rotate! [this v]
      (do 
        (.rotate ctx v))
      this)

    rp/IRenderBackend

    (spr-scaled! [this _]
      (do 
        (println "not implemented"))
      this)

    (spr! [this _]
      (do 
        (println "not implemented"))
      this)

    (clear! [this col]
      (rp/box! this [(v2 0 0) dims col]))

    (box! [this [{x :x y :y} {w :x h :y} col]]
      (do
        (let [col-str (rgb-str col)]
          (set! (.-fillStyle ctx) col-str)
          (.fillRect ctx x y w h))))))
