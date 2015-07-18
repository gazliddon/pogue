(ns cloj.lwjgl.render
  (:require 
    [cloj.renderutils :refer [get-viewport]]
    [cloj.math.vec2           :as v2 :refer [ v2 v2f ]]

    [cloj.lwjgl.protocols     :refer [bind-texture! IOGLTexture]]
    [clojure-gl.texture       :refer [make-texture-low!]]
    [cloj.protocols.render    :as rend-p :refer [IImage]]
    [cloj.protocols.resources :as res-p])

  (:import (org.lwjgl.util.vector Matrix Matrix4f)
           (org.lwjgl.opengl GL11)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn px [v]
  ; (int (+ 0.5 v))
  v)

(def clear-mask (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)  )


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

  (GL11/glTexCoord2f 1 0)
  (GL11/glVertex2f (+ x w) y)

  (GL11/glTexCoord2f 1 1)
  (GL11/glVertex2f (+ x w) (+ y h))

  (GL11/glTexCoord2f 0 1)
  (GL11/glVertex2f x (+ y h))

  (GL11/glEnd))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- my-make-spr! [id img [x y w h]]
  (let [ get-gl-texture (memoize make-texture-low!)  ]
    (try
      (reify
        IOGLTexture
        (bind-texture! [this]
              (->>
                (get-gl-texture (rend-p/img img))
                (:tex-id)
                (GL11/glBindTexture GL11/GL_TEXTURE_2D)))
        IImage
        (id [_] id)
        (dims [this] [x y w h])
        (width [_] w)
        (height [_] h )
        (img [ this ] img))
      (catch Exception e
        (do
          (println "[Error making textuer ] " (.getMessage e)))))))


(defn- init-gl! []
  (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
  (GL11/glClearColor 0.0 0.0 0.0 0.0)
  (GL11/glClear clear-mask) 
  (GL11/glDisable GL11/GL_TEXTURE_2D)
  (GL11/glDisable GL11/GL_DEPTH_TEST)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
  (GL11/glEnable GL11/GL_SCISSOR_TEST)
  (GL11/glDisable GL11/GL_BLEND)  )

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

      (make-spr! [this id img dims]
        (my-make-spr! id img dims)
        this)

      (init! [this]
        (init-gl!)
        this)

      (ortho! [this win-dims canv-dims]
        (let [[a b c d] (get-viewport win-dims canv-dims) ]
          (do
            (GL11/glViewport a b c d)
            (GL11/glScissor a b c d)
            (GL11/glMatrixMode GL11/GL_PROJECTION)
            (GL11/glLoadIdentity) 
            (GL11/glOrtho 0 (:x canv-dims) (:y canv-dims) 0 -1 1)
            (GL11/glMatrixMode GL11/GL_MODELVIEW)
            (GL11/glLoadIdentity) ))
        this)

      (save!    [this]
        (GL11/glPushMatrix)
        this)

      (restore! [this]
        (GL11/glPopMatrix)
        this)

      (clear-all! [this rgba]
        (do
          (GL11/glDisable GL11/GL_SCISSOR_TEST)
          (rend-p/clear! this rgba)
          (GL11/glEnable GL11/GL_SCISSOR_TEST)
          this))

      (clear! [this [r g b a]]
        (GL11/glClearColor r g b a)
        (GL11/glClear clear-mask)
        this)

      (spr-scaled! [this spr {x :x y :y} {w :x h :y}]
        (GL11/glEnable GL11/GL_TEXTURE_2D)
        (bind-texture! spr)
        (draw-t-quad x y w h 1 1 1 1)  
        this)

      (box! [this {x :x y :y} {w :x h :y} [r g b a]]
        (GL11/glDisable GL11/GL_TEXTURE_2D)
        (draw-quad x y w h r g b a)
        this) 

      (spr! [this img pos]
        (rend-p/spr-scaled! this img pos (v2 (rend-p/width img) (rend-p/height img)))))))
