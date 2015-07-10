(ns cloj.lwjgl.renderer 
  (:require 
    [clojure-gl.math  :refer [mul
                              translation
                              rotation
                              scale]]
    [cloj.math.vec2         :refer [ vec2 ]]
    [cloj.render.protocols  :as rp]
    [cloj.resources.manager :as rman])

  (:import (org.lwjgl.util.vector Matrix Matrix2f Matrix3f Matrix4f)
           (org.lwjgl.util.vector Vector2f Vector3f Vector4f)
           (org.lwjgl.opengl GL20)  
           )

  )


(defn px [v]
  ; (int (+ 0.5 v))
  v)

(defprotocol
  IShader
  (set-matrix-unifom! [this id mat])
  (load-shader! [this shader-init]))

(defn mk-shader []
  (reify
    IShader
    (set-matrix-unifom! [this id mat])
    (load-shader! [this shader-init]) ))


(defrecord MatrixSaver [current saved dirty?])

(defn create-matrix! [] (atom (->MatrixSaver (Matrix4f.) (Matrix4f.) true)))
(defn save-matrix! [this] (assoc this :saved (:current this)))
(defn restore-matrix! [this] (assoc this
                                   :current (:saved this)
                                   :dirty true))
(defn dirty! [this v] (assoc this :dirty true ))
(defn clean! [this v] (assoc this :dirty false ))
(defn set-matrix! [this m]
  (assoc this
         :matrix m
         :dirty true))

(defn get-matrix [this] (:current this))
(defn dirty? [this] (:dirty? this))


(defn mk-lwjgl-renderer [canvas-id dims]
  (let [data (atom {:dims    dims })
        shader (atom (mk-shader))
        matrix (create-matrix!)]

    (reify

      rp/ITransformable
      (matrix! [this mat]
        (swap! matrix set-matrix! mat)
        this)

      (mul! [this mat]
        (rp/matrix! this #(mul % mat)))

      (identity! [this]
        (rp/matrix! this ( Matrix4f. )))

      (translate! [this {:keys [x y]}]
        (rp/mul! this (translation x y 0)))

      (scale! [this {:keys [x y] }]
        (rp/mul! this (scale x y 1)))

      (rotate! [this v]
        (rp/mul! this (rotation v 0 0 1)))

      rp/IImage
      (id     [_] canvas-id)
      (dims   [this] [(vec2 0 0) (:dims @data)])
      (width  [_] (-> @data :dims :x))
      (height [_] (-> @data :dims :y))
      (img    [_] nil )

      rp/IRenderBackend
      (resize! [this {:keys [x y] :as new-dims}]
        (do
          (reset! data :dims new-dims)
          this))

      (save!    [this] (swap! matrix save-matrix!))
      (restore! [this] (swap! matrix restore-matrix!))

      (spr! [this spr pos]
        (rp/spr-scaled! this spr pos (vec2 (rp/width spr) (rp/height spr))))

      (clear! [this col]
        (doto this
          (rp/save!)
          (rp/identity! )
          (rp/box! (v2 0 0) (:dims @data) col)
          (rp/restore!)))

      (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
        (if (dirty? @matrix)
          (swap! shader set-matrix-unifom! 0 (get-matrix @matrix))
          (swap! matrix clean!))
        this)

      (box! [this {x :x y :y} {w :x h :y} col]
        (if (dirty? @matrix)
          (swap! shader set-matrix-unifom! 0 (get-matrix @matrix))
          (swap! matrix clean!))
        )))

  )
