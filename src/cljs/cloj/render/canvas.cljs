(ns cloj.render.canvas
  (:require 

    [gaz.color              :refer [rgb-str]]
    [cloj.math.vec2         :refer [ v2 ]]
    [hipo.core              :as hipo  :include-macros true]  
    [cloj.render.protocols  :as rp]
    [cloj.resources.manager :as rman]
    ))


(defprotocol IHTMLCanvas
  (get-element [_])
  (get-ctx-2d [_]))

(defn canvas [canvas-id {:keys [x y] :as dims}]
  (let [canvas-el (hipo/create [:canvas ^:attrs {:id canvas-id :width x :height y}])
        ctx (.getContext canvas-el "2d")]
    (reify
      IHTMLCanvas
      (get-element [_] canvas-el)
      (get-ctx-2d [_]  ctx)

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

      (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
        (do 
          (let [spr-w   (rman/width spr)
                spr-h   (rman/height spr)
                spr-img (rman/img spr) ]
            (.drawImage ctx spr-img x y w h)))
        this)

      (spr! [this spr {x :x y :y}]
        (do 
          (let [spr-w   (rman/width spr)
                spr-h   (rman/height spr)
                spr-img (rman/img spr) ]
            (.drawImage ctx spr-img x y 100 100 0 0 100 100)))
        this)

      (clear! [this col]
        (rp/box! this (v2 0 0) dims col))

      (box! [this {x :x y :y} {w :x h :y} col]
        (do
          (let [col-str (rgb-str col)]
            (set! (.-fillStyle ctx) col-str)
            (.fillRect ctx x y w h)))))  )

  )
