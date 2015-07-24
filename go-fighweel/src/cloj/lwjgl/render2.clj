(ns cloj.lwjgl.render2
  (:require 
    [cognitect.transit :as transit]

    [cloj.lwjgl.buffers :as buffers :refer [to-indicies-gl
                                            to-floats-gl]]

    [clojure.pprint :as pprint :refer [pprint]]

    [clojure.reflect :as reflect :refer [reflect]]

    [cloj.renderutils         :refer [get-viewport]]
    [cloj.math.vec2           :as v2 :refer [ v2 v2f ]]

    [cloj.lwjgl.offscreen     :refer [screen-buffer mk-offscreen-buffer!]]

    [cloj.lwjgl.protocols     :refer [bind-texture! IGLTexture get-uv-coords bind-fbo!]]
    [clojure-gl.texture       :refer [make-texture-low!]]
    [cloj.protocols.render    :as rend-p :refer [IImage]]
    [cloj.protocols.resources :as res-p])

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (java.io ByteArrayInputStream ByteArrayOutputStream )
    (org.lwjgl BufferUtils)
    (org.lwjgl.util.vector Matrix Matrix4f)
    (org.lwjgl.opengl GL11 GL15 GL20 GL30)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(set! *warn-on-reflection* true)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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

(defn make-bufffers [model]
  (let [verts (-> model :verts :vals)
        indicies (-> model :indicies )
        vao-id (GL30/glGenVertexArrays)
        ibo-id (to-indicies-gl indicies)
        vbo-id (to-floats-gl verts)]
    (do
      (GL30/glBindVertexArray vao-id)

      ;; Enable all the things
      (GL11/glEnableClientState GL11/GL_INDEX_ARRAY)
      (GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
      (GL11/glEnableClientState GL11/GL_TEXTURE_COORD_ARRAY)

      ;; Bind the vert buffer
      (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER vbo-id)
      ;; point at the right vert elements
      (GL11/glVertexPointer 2 GL11/GL_FLOAT 16 0)
      (GL11/glTexCoordPointer 2 GL11/GL_FLOAT 16 8)

      ;; Bind the index buffer
      (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER ibo-id) 

      ;; Bind back to a nothing VAO
      (GL30/glBindVertexArray 0)
      )
    {:vao-id vao-id
     :num-of-indicies (count indicies)}))

(defprotocol IModel
  (draw! [_]))

(defn make-model [model]
  (let [gl-model (delay (make-bufffers model))]
    (reify
      IModel
      (draw! [_]
        (let [{:keys [vao-id num-of-indicies]} @gl-model] 
          (do
            (GL30/glBindVertexArray vao-id)
            (GL11/glDrawElements GL11/GL_TRIANGLE_STRIP ^Integer num-of-indicies GL11/GL_UNSIGNED_INT 0)
            (GL30/glBindVertexArray 0)
            ))))))


(def the-model (make-model (-> models :models :quad )))

; (pprint the-model)


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

    (draw! the-model)

    ; (GL11/glBegin GL11/GL_QUADS)
    ; (GL11/glColor4f 1 1 1 1)

    ; (GL11/glTexCoord2f 0 0)
    ; (GL11/glVertex2f 0 0)

    ; (GL11/glTexCoord2f 1 0)
    ; (GL11/glVertex2f 1 0)

    ; (GL11/glTexCoord2f 1 1)
    ; (GL11/glVertex2f 1 1 )

    ; (GL11/glTexCoord2f 0 1)
    ; (GL11/glVertex2f 0 1)

    ; (GL11/glEnd) 

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
  (let [gl-texture (delay (make-texture-low! (rend-p/img img)))]
    (try
      (reify
        IGLTexture
        (get-uv-coords [_]
          (get-uv-cords (:dims @gl-texture) [x y w h]))

        (bind-texture! [_]
          (->>
            (:tex-id @gl-texture)
            (GL11/glBindTexture GL11/GL_TEXTURE_2D )))

        IImage
        (id     [_] id)
        (dims   [this] [x y w h])
        (width  [_] w)
        (height [_] h )
        (img    [ this ] img))

      (catch Exception e
        (do
          (println "[Error making texture] " (.getMessage e)))))))

(defn- init-gl! []
  (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
  (GL11/glClearColor 0.0 0.0 0.0 0.0)
  (GL11/glClear clear-mask) 
  (GL11/glDisable GL11/GL_TEXTURE_2D)
  (GL11/glDisable GL11/GL_DEPTH_TEST)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
  (GL11/glEnable GL11/GL_SCISSOR_TEST)
  (GL11/glEnable GL11/GL_BLEND)  )

(defn mk-renderer []
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
        this))

    (box! [this {x :x y :y} {w :x h :y} [r g b a]]
      (GL11/glDisable GL11/GL_TEXTURE_2D)
      (draw-quad-2 x y w h r g b a)
      this) 

    (spr! [this img pos]
      (rend-p/spr-scaled! this img pos (v2 (rend-p/width img) (rend-p/height img))))))

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

