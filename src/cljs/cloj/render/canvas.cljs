(ns cloj.render.canvas
  (:require 
    [gaz.color              :refer [rgb-str]]
    [cloj.math.vec2         :refer [ v2 ]]
    [hipo.core              :as hipo  :include-macros true]  
    [cloj.render.protocols  :as rp]
    [cloj.resources.manager :as rman]
    [cloj.html.utils :refer [set-elem-dims!]]
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
        ctx (.getContext canvas-el "2d")
        atom-dims (atom dims)
        ]
    (do
      (set-smoothing ctx false)
      (reify
        IHTMLCanvas
        (get-element [_] canvas-el)
        (get-ctx-2d  [_] ctx)

        rp/ITransformable
        (matrix! [this [a c e b d f]]
          (.setTransform ctx a b c d e f)
          this)

        (identity! [this]
          (.resetTransform ctx)
          this)

        (translate! [this {:keys [x y]}]
          (.translate ctx x y)
          this)

        (scale! [this {:keys [x y] }]
          (.scale ctx x y)
          this)

        (rotate! [this v]
          (.rotate ctx v)
          this)

        rp/IImage

        (id     [_] canvas-id)
        (dims   [_] [0 0  (:x @atom-dims)(:y @atom-dims)])
        (width  [_] (:x @atom-dims))
        (height [_] (:y @atom-dims))
        (img    [_] canvas-el)

        rp/IRenderBackend
        (resize! [_ {:keys [x y] :as new-dims}]
          (do
          (set-elem-dims! canvas-el new-dims)
          (reset! atom-dims new-dims) 
            )
          )

        (save!    [_] (.save ctx))
        (restore! [_] (.restore ctx))

        (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
          (let [[sx sy sw sh] (rp/dims spr)
                spr-img (rp/img spr) ]
            (.drawImage ctx spr-img sx sy sw sh (px x ) (px y ) (px w ) (px h )))
          this)

        (spr! [this spr pos]
          (let [[sx sy sw sh] (rp/dims spr)]
            (rp/spr-scaled! this spr pos (v2 sw sh) )))

        (clear! [this col]
          (doto this
            (rp/save!)
            (rp/identity! )
            (rp/box! (v2 0 0) @atom-dims col)
            (rp/restore!)))

        (box! [this {x :x y :y} {w :x h :y} col]
          (let [col-str (rgb-str col)]
            (set! (.-fillStyle ctx) col-str)
            (.fillRect ctx x y w h)))))  )

  )
