(ns cloj.render.canvas
  (:require 

    [gaz.color              :refer [rgb-str]]
    [cloj.math.vec2         :refer [ v2 ]]
    [hipo.core              :as hipo  :include-macros true]  
    [cloj.render.protocols  :as rp]))


(defprotocol IHTMLCanvas
  (get-element [_])
  (get-ctx-2d [_]))


(defn canvas [canvas-id dims]
  (let [canvas-el (hipo/create [:canvas ^:attrs {:id id :width w :height h}])
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
            (.fillRect ctx x y w h)))))  )
  )
