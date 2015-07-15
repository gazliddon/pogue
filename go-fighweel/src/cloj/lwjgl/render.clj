(ns cloj.lwjgl.render
  (:require 
    [clojure-gl.math  :refer [mul
                              translation
                              rotation
                              scale]]
    [cloj.math.vec2         :refer [ v2 ]]

    [cloj.protocols.render  :as rend-p]
    [cloj.protocols.resources :as res-p])

  (:import (org.lwjgl.util.vector Matrix Matrix2f Matrix3f Matrix4f)
           (org.lwjgl.util.vector Vector2f Vector3f Vector4f)
           (org.lwjgl.opengl GL20 GL11))
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn px [v]
  ; (int (+ 0.5 v))
  v)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defprotocol
  IShader
  (set-matrix-unifom! [this id mat])
  (load-shader! [this shader-init]))

(defn mk-shader []
  (reify
    IShader
    (set-matrix-unifom! [this id mat])
    (load-shader! [this shader-init]) ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn draw-quad [x y w h r g b a]
  (let [w 1000
        h 1000
        r r
        g g
        b b
        a 1]
    (GL11/glBegin GL11/GL_QUADS)
    (GL11/glColor4f r g b a)
    (GL11/glVertex2f x y)
    (GL11/glVertex2f x (+ y h))
    (GL11/glVertex2f (+ x w) (+ y h))
    (GL11/glVertex2f (+ x w) y)
    (GL11/glEnd)
    )
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mk-lwjgl-renderer [canvas-id dims]
  (let [data (atom {:dims    dims })
        shader (atom (mk-shader))
        matrix (create-matrix!)]

    (reify
      rend-p/ITransformable

      (matrix! [this mat]
        (swap! matrix set-matrix! mat)
        this)

      (mul! [this mat]
        (rend-p/matrix! this #(mul % mat)))

      (identity! [this]
        (rend-p/matrix! this ( Matrix4f. )))

      (translate! [this {:keys [x y]}]
        (rend-p/mul! this (translation x y 0)))

      (scale! [this {:keys [x y] }]
        (rend-p/mul! this (scale x y 1)))

      (rotate! [this v]
        (rend-p/mul! this (rotation v 0 0 1)))

      rend-p/IImage
      (id     [_] canvas-id)
      (dims   [this] [(v2 0 0) (:dims @data)])
      (width  [_] (-> @data :dims :x))
      (height [_] (-> @data :dims :y))
      (img    [_] nil )

      rend-p/IRenderBackend
      (resize! [this {:keys [x y] :as new-dims}]
        (do
          (reset! data :dims new-dims)
          this))

      (save!    [this] (swap! matrix save-matrix!))
      (restore! [this] (swap! matrix restore-matrix!))

      (spr! [this spr pos]
        (rend-p/spr-scaled! this spr pos (v2 (rend-p/width spr) (rend-p/height spr))))

      (clear! [this [r g b a]]
        (GL11/glClearColor r g b a)
        (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)))

      (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
        (if (dirty? @matrix)
          (swap! shader set-matrix-unifom! 0 (get-matrix @matrix))
          (swap! matrix clean!))
        (println "Should have printed a sprite")
        this)

      (box! [this {x :x y :y} {w :x h :y} [r g b a]]
        ; (if (dirty? @matrix)
        ;   (swap! shader set-matrix-unifom! 0 (get-matrix @matrix))
        ;   (swap! matrix clean!))
        (draw-quad x y w h r g b a))))

  )
