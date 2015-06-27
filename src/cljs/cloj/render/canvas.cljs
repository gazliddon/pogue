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


(defn set-smoothing [ctx v]
  (doto ctx
        (aset "mozImageSmoothingEnabled"     v)
        (aset "webkitImageSmoothingEnabled"  v)
        (aset "msImageSmoothingEnabled"      v)
        (aset "imageSmoothingEnabled"        v)))

(defn px [v]
  ; (int (+ 0.5 v))
  v
  )

(defn canvas [canvas-id {:keys [x y] :as dims}]
  (let [canvas-el (hipo/create [:canvas ^:attrs {:id canvas-id :width x :height y}])
        ctx (.getContext canvas-el "2d")]
    (do
      (set-smoothing ctx false)
      (reify
        IHTMLCanvas
        (get-element [_] canvas-el)
        (get-ctx-2d  [_] ctx)

        rp/ITransformable
        (matrix! [this [a c e b d f]]
          (do
            (.setTransform ctx a b c d e f))
          this)

        (identity! [this]
          (do
            (.resetTransform ctx))
          this)

        (translate! [this {:keys [x y]}]
          (do
            (.translate ctx x y))
          this)

        (scale! [this {:keys [x y] }]
          (do
            (.scale ctx x y))
          this)

        (rotate! [this v]
          (do 
            (.rotate ctx v))
          this)

        rp/IImage

        (id     [_] canvas-id)
        (dims   [_] [0 0 x y])
        (width  [_] x)
        (height [_] y)
        (img    [_] canvas-el)

        rp/IRenderBackend

        (save!    [_] (.save ctx))
        (restore! [_] (.restore ctx))


        (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
          (do 
            (let [[sx sy sw sh] (rp/dims spr)
                  spr-img (rp/img spr) ]
              (.drawImage ctx spr-img sx sy sw sh (px x ) (px y ) (px w ) (px h ))))
          this)

        (spr! [this spr pos]
          (let [[sx sy sw sh] (rp/dims spr)]
            (rp/spr-scaled! this spr pos (v2 sw sh) )))

        (clear! [this col]
          (doto this
            (rp/save!)
            (rp/identity! )
            (rp/box! (v2 0 0) dims col)
            (rp/restore!)))

        (box! [this {x :x y :y} {w :x h :y} col]
          (do
            (let [col-str (rgb-str col)]
              (set! (.-fillStyle ctx) col-str)
              (.fillRect ctx x y w h))))))  )

  )
