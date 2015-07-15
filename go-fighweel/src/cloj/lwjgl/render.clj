(ns cloj.lwjgl.render
  (:require 
    [clojure-gl.math  :refer [mul
                              translation
                              rotation
                              scale]]
    [cloj.math.vec2         :as v2 :refer [ v2 ]]

    [cloj.protocols.render  :as rend-p]
    [cloj.protocols.resources :as res-p])

  (:import (org.lwjgl.util.vector Matrix Matrix2f Matrix3f Matrix4f)
           (org.lwjgl.util.vector Vector2f Vector3f Vector4f)
           (org.lwjgl.util.glu GLU)
           (org.lwjgl.opengl GL20 GL11))
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn px [v]
  ; (int (+ 0.5 v))
  v)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn get-vp [ {winW :x winH :y} dest-ar ]
  (let [vp-w  (* winW dest-ar)
        vp-h  (/ winH dest-ar) ]
    (v2 vp-w vp-h))
  )

(defn get-ar [{w :x h :y}]
  (/ w h))

(defn get-vp [ win-dims canv-dims ]

  (let [dest-ar (get-ar canv-dims)

        vp-dims         (v2/mul
                          canv-dims
                          (v2 dest-ar dest-ar))

        vp-left-top     (v2/mul
                          (v2/sub win-dims vp-dims)
                          v2/half
                          win-dims)

        vp-bottom-right (v2/mul
                          win-dims   
                          (v2/add
                            vp-left-top
                            vp-dims))

        {left :x top :y}  vp-left-top
        {right :x bottom :y} vp-bottom-right ]

    [left top right bottom])) 

(get-vp (v2 1 1) (v2 1 2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn draw-quad [x y w h r g b a]
  (GL11/glBegin GL11/GL_QUADS)
  (GL11/glColor4f r g b a)
  (GL11/glVertex2f x y)
  (GL11/glVertex2f x (+ y h))
  (GL11/glVertex2f (+ x w) (+ y h))
  (GL11/glVertex2f (+ x w) y)
  (GL11/glEnd))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mk-lwjgl-renderer [canvas-id]
  (let [dims (atom (v2 100 100))]
    (do
      (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
      (GL11/glClearColor 0.0 0.0 0.0 0.0)
      (GL11/glViewport 0 0 1 1)
      (GL11/glDisable GL11/GL_TEXTURE_2D)
      (GL11/glDisable GL11/GL_DEPTH_TEST)
      (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
      (GL11/glDisable GL11/GL_BLEND)  

      (reify
        rend-p/ITransformable
        (matrix! [this mat]
          (GL11/glLoadMatrix mat)
          this)

        (mul! [this mat]
          (GL11/glMultMatrix mat )
          mat)

        (identity! [this]
          (GL11/glLoadIdentity)
          this)

        (translate! [this {:keys [x y]}]
          (GL11/glTranslatef x y 0)
          this)

        (scale! [this {:keys [x y] }]
          (GL11/glScalef x y 1)
          this)

        (rotate! [this v]
          (GL11/glRotatef v 0 0 1)
          this)

        rend-p/IImage
        (id     [_] canvas-id)
        (dims   [_] [v2/zero @dims])
        (width  [_] (:x @dims))
        (height [_] (:y @dims))
        (img    [_] nil )

        rend-p/IRenderBackend

        (ortho! [this win-dims canv-dims]
          (let [[a b c d] (get-vp win-dims canv-dims)]
            (do
              (GL11/glMatrixMode GL11/GL_PROJECTION)
              (GLU/gluOrtho2D a b c d)
              (GL11/glMatrixMode GL11/GL_MODELVIEW)
              (GL11/glLoadIdentity) 
              (GL11/glScalef (:x canv-dims) (:y canv-dims) 1))))

        (save!    [this] (GL11/glPushMatrix))

        (restore! [this] (GL11/glPopMatrix))

        (clear! [this [r g b a]]
          (GL11/glClearColor r g b a)
          (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)))

        (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
          (println "Should have printed a sprite")
          this)

        (box! [this {x :x y :y} {w :x h :y} [r g b a]]
          (draw-quad x y w h r g b a)) 

        (spr! [this spr pos]
          (rend-p/spr-scaled! this spr pos (v2 (rend-p/width spr) (rend-p/height spr))))
        ))))
