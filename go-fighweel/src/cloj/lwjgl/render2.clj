(ns cloj.lwjgl.render2
  (:require 
    [cognitect.transit :as transit]

    [clojure.pprint :as pprint :refer [pprint]]

    [clojure.reflect :as reflect :refer [reflect]]

    [cloj.renderutils :refer [get-viewport]]
    [cloj.math.vec2           :as v2 :refer [ v2 v2f ]]

    [cloj.lwjgl.offscreen :refer [screen-buffer mk-offscreen-buffer!]]

    [clojure-gl.buffers :as buff]


    [cloj.lwjgl.protocols     :refer [bind-texture! IGLTexture get-uv-coords bind-fbo!]]
    [clojure-gl.texture       :refer [make-texture-low!]]
    [cloj.protocols.render    :as rend-p :refer [IImage]]
    [cloj.protocols.resources :as res-p])

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (java.io ByteArrayInputStream ByteArrayOutputStream )
    (org.lwjgl BufferUtils)
    (org.lwjgl.util.vector Matrix Matrix4f)
    (org.lwjgl.opengl GL11 GL15)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(set! *warn-on-reflection* true)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn px [v]
  ; (int (+ 0.5 v))
  v)

(def clear-mask (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def models-src "[\"^ \",\"~:models\",[\"^ \",\"~:quad\",[\"^ \",\"~:indicies\",[0,3,1,2],\"~:verts\",[\"^ \",\"~:vals\",[0,0,0,0,0,0,0,0,1,1,1,1,0,1,0,1],\"~:num\",4]]]]" )

(defn read-transit-str [^String s]
  (->
    (.getBytes s)
    (ByteArrayInputStream. )
    (transit/reader :json)
    (transit/read )))

(def models (read-transit-str models-src))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn ^FloatBuffer to-floats [vs]
  (doto (BufferUtils/createFloatBuffer (count vs))
    (.put (float-array vs))
    (.flip)))

(defn ^IntBuffer to-ints [vs]
 (doto (BufferUtils/createIntBuffer (count vs))
    (.put (int-array vs))
    (.flip)) )

(defn to-floats-gl [verts]
  (let [glb (GL15/glGenBuffers)]
    (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER glb)
    (GL15/glBufferData GL15/GL_ARRAY_BUFFER (to-floats verts) GL15/GL_STATIC_DRAW)
    glb))

(defn to-indicies-gl [indicies]
  (let [vbo-id (GL15/glGenBuffers) ]
    (do
        (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER vbo-id)
        (GL15/glBufferData GL15/GL_ELEMENT_ARRAY_BUFFER (to-ints indicies) GL15/GL_STATIC_DRAW))
    vbo-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(->>
    (reflect 'GL11/glLoadMatrix)
    (pprint)
)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
    

(defn draw-quad [x y w h r g b a]
  (GL11/glBegin GL11/GL_QUADS)
  (GL11/glColor4f r g b a)
  (GL11/glVertex2f x y)
  (GL11/glVertex2f x (+ y h))
  (GL11/glVertex2f (+ x w) (+ y h))
  (GL11/glVertex2f (+ x w) y)
  (GL11/glEnd))

(defn draw-quad-textured [x y w h u v u-w v-h ]
  (do
    (GL11/glPushMatrix)
    (GL11/glTranslatef x y 0) 
    (GL11/glScalef w h 1)

    (GL11/glMatrixMode GL11/GL_TEXTURE)
    (GL11/glLoadIdentity)
    (GL11/glTranslatef u v 0) 
    (GL11/glScalef u-w v-h 1)

    (GL11/glBegin GL11/GL_QUADS)
    (GL11/glColor4f 1 1 1 1)

    (GL11/glTexCoord2f 0 0)
    (GL11/glVertex2f 0 0)

    (GL11/glTexCoord2f 1 0)
    (GL11/glVertex2f 1 0)

    (GL11/glTexCoord2f 1 1)
    (GL11/glVertex2f 1 1 )

    (GL11/glTexCoord2f 0 1)
    (GL11/glVertex2f 0 1)

    (GL11/glEnd) 

    (GL11/glPopMatrix)
    (GL11/glMatrixMode GL11/GL_MODELVIEW)
    (GL11/glPopMatrix)
    ))



(defn draw-quad-2 [x y w h r g b a]
  (do
    (GL11/glPushMatrix)
    (GL11/glMatrixMode GL11/GL_MODELVIEW)
    (GL11/glTranslatef x y 0) 
    (GL11/glScalef w h 1)
    (draw-quad 0 0 1 1 r g b a)
    (GL11/glPopMatrix)
  ))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- get-uv-cords [[t-w t-h] [x y w h]]
  (let [x-scale (/ 1 t-w)
        y-scale (/ 1 t-h)]
  [(* x-scale x)
   (* y-scale y)
   (* x-scale w)
   (* y-scale h) ]) )

(defn- my-make-spr! [id img [x y w h]]
  (let [get-gl-texture (memoize make-texture-low!) ]
    (try
      (reify
        IGLTexture
        (get-uv-coords [ this ]
          (get-uv-cords (:dims (get-gl-texture (rend-p/img img)) ) [x y w h]))

        (bind-texture! [this]
          (->>
            (get-gl-texture (rend-p/img img))
            (:tex-id)
            (GL11/glBindTexture GL11/GL_TEXTURE_2D )))

        IImage
        (id     [_] id)
        (dims   [this] [x y w h])
        (width  [_] w)
        (height [_] h )
        (img    [ this ] img))
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
  (GL11/glEnable GL11/GL_BLEND)  )

(def clear-mask (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))

(defn mk-renderer []
  (let [dims (atom (v2 100 100)) ]
    (reify
      rend-p/ITransformable
      (matrix! [this mat]
        (GL11/glLoadMatrix ^FloatBuffer mat)
        this)

      (mul! [this mat]
        (GL11/glMultMatrix ^FloatBuffer mat )
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

      rend-p/IRenderBackend
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

      (save! [this]
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
        (let [uv (get-uv-coords spr)]
          (GL11/glEnable GL11/GL_TEXTURE_2D)
          (bind-texture! spr)
          (apply draw-quad-textured x y w h uv)
          ; (apply draw-t-quad x y w h uv)
          this))

      (box! [this {x :x y :y} {w :x h :y} [r g b a]]
        (GL11/glDisable GL11/GL_TEXTURE_2D)
        (draw-quad-2 x y w h r g b a)
        this) 

      (spr! [this img pos]
        (rend-p/spr-scaled! this img pos (v2 (rend-p/width img) (rend-p/height img)))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mk-lwjgl-render-manager []
  (reify
    rend-p/IRenderManager
    (init! [this]
      (init-gl!)
      this)

    (make-spr! [this id img dims]
      (my-make-spr! id img dims))

    (make-render-target! [this {w :x h :y}]
      (->
        (mk-renderer) 
        (mk-offscreen-buffer! w h false)))

    (make-screen-renderer! [this]
      (let [renderer (mk-renderer)]
        (reify
          rend-p/IRenderTarget
          (get-renderer [_] renderer)
          (activate! [this]
            (bind-fbo! screen-buffer)
            (rend-p/get-renderer this)))))))

