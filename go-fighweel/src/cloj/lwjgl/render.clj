(ns cloj.lwjgl.render
  (:require 
    [clojure-gl.math  :refer [mul
                              translation
                              rotation
                              scale]]
    [cloj.math.vec2         :as v2 :refer [ v2 v2f ]]

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
(defn get-ar [{w :x h :y}] (/ w h))

(defn get-vp [win-dims canv-dims]
  (let [canv-ar (get-ar canv-dims)
        win-ar (get-ar win-dims)
        dom-axis (if (> canv-ar win-ar) :x :y)
        scale (/ (dom-axis win-dims) (dom-axis canv-dims))
        vp-dims (v2/mul
                  (v2 scale scale)
                  canv-dims)
        tl  (v2/mul v2/half (v2/sub win-dims vp-dims)) ]

    {:canv-ar (float canv-ar )
     :win-ar (float win-ar )
     :dom-axis dom-axis
     :vp-dims (v2/apply float vp-dims)
     :scale scale
     :viewport (mapv int [(:x tl)
                (:y tl)
                (:x vp-dims)
                (:y vp-dims) ] )
     }))

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
  (let [dims (atom (v2 100 100))
        clear-mask (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT) ]
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

      (init! [_]
        (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
        (GL11/glClearColor 0.0 0.0 0.0 0.0)
        (GL11/glClear clear-mask) 
        (GL11/glDisable GL11/GL_TEXTURE_2D)
        (GL11/glDisable GL11/GL_DEPTH_TEST)
        (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
        (GL11/glEnable GL11/GL_SCISSOR_TEST)
        (GL11/glDisable GL11/GL_BLEND)  )

      (ortho! [this win-dims canv-dims]
        (let [{:keys [scale viewport]} (get-vp win-dims canv-dims)
              [a b c d] viewport ]
          (do
            (println viewport)
            (GL11/glViewport a b c d)
            (GL11/glScissor a b c d)
            (GL11/glMatrixMode GL11/GL_PROJECTION)
            (GL11/glLoadIdentity) 
            (GLU/gluOrtho2D 0 (:x canv-dims) 0 (:y canv-dims))
            (GL11/glMatrixMode GL11/GL_MODELVIEW)
            (GL11/glLoadIdentity) 
            (GL11/glScalef  (:x scale) (:y scale) 1)
            )))

      (save!    [this] (GL11/glPushMatrix))
      (restore! [this] (GL11/glPopMatrix))

      (clear! [this [r g b a]]
        (GL11/glClearColor r g b a)
        (GL11/glClear clear-mask))

      (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
        (println "Should have printed a sprite")
        this)

      (box! [this {x :x y :y} {w :x h :y} [r g b a]]
        (draw-quad x y w h r g b a)) 

      (spr! [this spr pos]
        (rend-p/spr-scaled! this spr pos (v2 (rend-p/width spr) (rend-p/height spr))))
      )))
