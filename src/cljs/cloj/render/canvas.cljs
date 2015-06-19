(ns cloj.render.canvas
  (:require 
    [gaz.color :refer [rgb-str]]
    [cloj.math.vec2        :refer [ v2 ]]
    [cloj.render.protocols :as rp]))

(defn canvas [ctx dims]
  (reify
    rp/ITransformable

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

    rp/IRenderBackend

    (spr-scaled! [this _]
      (println "not implemented")
      this)

    (spr! [this _]
      (println "not implemented")
      this)

    (clear! [this col]
      (do
        (.log js/console ctx)
        (rp/box! this [(v2 0 0) col])))

    (box! [this [{x :x y :y} {w :x h :y} col]]
      (let [col-str (rgb-str col)]
        (set! (.-fillStyle ctx) col-str)
        (.fillRect ctx x y w h))
      this)))
