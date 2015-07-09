(ns cloj.lwjgl.renderer 
  (:require 
    [cloj.math.vec2         :refer [ v2 ]]
    [cloj.render.protocols  :as rp]
    [cloj.resources.manager :as rman]
    )

  (:import (org.lwjgl.util.vector Matrix4f Vector3f)
           (org.lwjgl.opengl GL20)))




(defn px [v]
  ; (int (+ 0.5 v))
  v)

(defprotocol IShader
  (set-matrix-unifom [this id])
  (load-shader [this shader-init]))

(defn mk-shader []
  (reify
    IShader
    (set-matrix-unifom! [this id mat]))
  )

(defn mk-lwjgl-renderer [canvas-id {:keys [x y] :as dims}]
  (let [atom-dims (atom dims)
        atom-mat (atom Matrix4f.)

        atom-shader (atom (mk-shader))

        ]
    (do
      (reify
        IHTMLCanvas
        (get-element [_] canvas-el)
        (get-ctx-2d  [_] ctx)

        rp/ITransformable
        (matrix! [this mat]
          (reset! atom-mat mat)
          this)

        (identity! [this]
          (rp/matrix! this (Matrix4f.))
          this)

        (translate! [this {:keys [x y]}]
          (swap! atom-mat #(mul % (translation x y 0)))
          this)

        (scale! [this {:keys [x y] }]
          (swap! atom-mat #(mul % (scale x y 1)))
          this)

        (rotate! [this v]
          (swap! atom-mat #(mul % (rotation v 0 0 1)))
          this)

        rp/IImage
        (id     [_] canvas-id)
        (dims   [_] [0 0  (:x @atom-dims)(:y @atom-dims)])
        (width  [_] (:x @atom-dims))
        (height [_] (:y @atom-dims))
        (img    [_] canvas-el)

        rp/IRenderBackend
        (resize! [this {:keys [x y] :as new-dims}]
          (do
            (set-elem-dims! (rp/img this) new-dims)
            (reset! atom-dims new-dims) ))

        (save!    [_] (throw (Exception. "not implemented")))
        (restore! [_] (throw (Exception. "not implemented")))

        (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
          (throw (Exception. "not implemented")) 
          this)

        (spr! [this spr pos]
          (throw (Exception. "not implemented"))
          this)

        (clear! [this col]
          (doto this
            (rp/save!)
            (rp/identity! )
            (rp/box! (v2 0 0) @atom-dims col)
            (rp/restore!)))

        (box! [this {x :x y :y} {w :x h :y} col]
          (throw (Exception. "not implemented"))
          ))))

  )
