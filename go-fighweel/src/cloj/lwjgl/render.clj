(ns cloj.lwjgl.render
  (:require 
    [cloj.renderutils :refer [get-viewport]]

    [cloj.math.vec2           :as v2 :refer [ v2 v2f ]]

    [cloj.protocols.render    :as rend-p]
    [cloj.protocols.resources :as res-p])

  (:import (org.lwjgl.util.vector Matrix Matrix4f)
           (org.lwjgl.opengl GL11)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn px [v]
  ; (int (+ 0.5 v))
  v)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn draw-quad [x y w h r g b a]
  (GL11/glBegin GL11/GL_QUADS)
  (GL11/glColor4f r g b a)
  (GL11/glVertex2f x y)
  (GL11/glVertex2f x (+ y h))
  (GL11/glVertex2f (+ x w) (+ y h))
  (GL11/glVertex2f (+ x w) y)
  (GL11/glEnd))

(defn draw-t-quad [x y w h r g b a]
  (GL11/glBegin GL11/GL_QUADS)
  (GL11/glColor4f r g b a)

  (GL11/glTexCoord2f 0 0)
  (GL11/glVertex2f x y)

  (GL11/glVertex2f x (+ y h))
  (GL11/glTexCoord2f 0 1)

  (GL11/glVertex2f (+ x w) (+ y h))
  (GL11/glTexCoord2f 1 1)

  (GL11/glVertex2f (+ x w) y)
  (GL11/glTexCoord2f 1 0)
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
        (let [[a b c d] (get-viewport win-dims canv-dims) ]
          (do
            (GL11/glViewport a b c d)
            (GL11/glScissor a b c d)
            (GL11/glMatrixMode GL11/GL_PROJECTION)
            (GL11/glLoadIdentity) 
            (GL11/glOrtho 0 (:x canv-dims) 0 (:y canv-dims) -1 1)
            (GL11/glMatrixMode GL11/GL_MODELVIEW)
            (GL11/glLoadIdentity) )))

      (save!    [this] (GL11/glPushMatrix))
      (restore! [this] (GL11/glPopMatrix))

      (clear! [this [r g b a]]
        (GL11/glClearColor r g b a)
        (GL11/glClear clear-mask))

      (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
        (GL11/glEnable GL11/GL_TEXTURE_2D)
        (GL11/glBindTexture GL11/GL_TEXTURE_2D (:tex-id spr))
        (draw-t-quad x y w h 1 1 1 1)  
        this)

      (box! [this {x :x y :y} {w :x h :y} [r g b a]]
        (GL11/glDisable GL11/GL_TEXTURE_2D)
        (draw-quad x y w h r g b a)
        this) 

      (spr! [this spr pos]
        (rend-p/spr-scaled! this spr pos (v2 (rend-p/width spr) (rend-p/height spr)))))))
