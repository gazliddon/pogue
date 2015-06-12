(ns gaz.canvascomprenderbackend
  (:require
    [gaz.renderprotocols       :as rp]
    [gaz.color                 :refer [rgb-str]]
    [cloj.math.vec2            :as    v2 ])
  )

(defn canvas-immediate-renderer [canvas dims]
  (let [ctx (.getContext canvas "2d") ]
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

      (spr-scaled! [_ _]
        (println "not implemented"))

      (spr! [this [{:keys [x y] } spr]]
        (.drawImage ctx spr x y ))

      (clear! [this col]
        (rp/box! this [(v2/v2 0 0) dims col]))

      (box! [_  [{x :x y :y} {w :x h :y} col]]
        (let [col-str (rgb-str col)]
          (set! (.-fillStyle ctx) col-str)
          (.fillRect ctx x y w h))))))
