(ns cloj.lwjgl.render2
  (:require 
    [game.appstate :refer [get-time]]


    [clojure-watch.core :refer [start-watch]]   

    [cloj.lwjgl.buffers :as buffers :refer [to-indicies-gl
                                            to-floats-gl]]

    [experiments.depdelay :as exp :refer [gl-create-texture!
                                          depends-on-file]]


    [cloj.totransit :refer [read-transit-str]]

    [cloj.utils :as utils :refer [pref]]

    [clojure.pprint :as pprint :refer [pprint]]

    [clojure.reflect :as reflect :refer [reflect]]

    [cloj.renderutils         :refer [get-viewport]]
    [cloj.math.vec2           :as v2 :refer [ v2 v2f ]]

    [cloj.lwjgl.offscreen     :refer [screen-buffer mk-offscreen-buffer!]]
    [cloj.lwjgl.model :as model]

    [cloj.lwjgl.protocols     :refer [bind-texture! IGLTexture get-uv-coords bind-fbo!]]
    [clojure-gl.texture       :refer [make-texture-low!]]
    [cloj.protocols.render    :as rend-p :refer [IImage]]
    [cloj.protocols.model     :as model-p :refer [IModel ]]
    [cloj.protocols.resources :as res-p])

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (java.io ByteArrayInputStream ByteArrayOutputStream )
    (org.lwjgl BufferUtils)
    (org.lwjgl.util.vector Matrix Matrix4f)
    (org.lwjgl.opengl
      GLContext
      NVPrimitiveRestart   
      GL11 GL15 GL20 GL30 GL31
      )))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TODO put in a utils file somewhere

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn set-primitive-restart-ext
  "Enable and set primive restart using the NV extension
   because my laptop doesn't support gl31"
  [^Integer index]
  (GL11/glEnableClientState NVPrimitiveRestart/GL_PRIMITIVE_RESTART_NV)
  (NVPrimitiveRestart/glPrimitiveRestartIndexNV index)
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def clear-mask (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT)  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-model! [file-name]
  (->>
    (slurp file-name)
    (read-transit-str)
    (first)
    (model/make-model file-name)))

(def test-model-file-name "resources/public/generated/quad.json")

(def the-model
  (let [mod-loader (fn []
                     (load-model! test-model-file-name))
        model (mod-loader)]
    (do
      (start-watch [{:path file-name
                     :event-types [:modify]
                     :bootstrap (fn [path] (println "Starting to watch " path))
                     :callback (fn [_ _] (do
                                           (println "The file changed!")
                                           (unrealize! thing-to-update)))
                     :options {:recursive false}}])    
      )
    model))

(defn depends-on-file
  "make this resource depend on a file
   How do I watch files in clj then?"
  [korks file-name]
  (let [thing-to-update (get-in @gl-resources korks)]
    (when (not= nil thing-to-update)
      (start-watch [{:path file-name
                     :event-types [:modify]
                     :bootstrap (fn [path] (println "Starting to watch " path))
                     :callback (fn [_ _] (do
                                           (println "The file changed!")
                                           (unrealize! thing-to-update)))
                     :options {:recursive false}}]))
    )
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

(def rot (atom 0))

(defn draw-quad-textured [x y w h u v u-w v-h ]
  (do
    (GL11/glPushMatrix)
    (GL11/glTranslatef x y 0) 
    (GL11/glScalef w h 1)
    (GL11/glRotatef (* 30 ( get-time )) 0 1 1 )

    (GL11/glMatrixMode GL11/GL_TEXTURE)
    (GL11/glLoadIdentity)
    (GL11/glTranslatef u v 0) 
    (GL11/glScalef u-w v-h 1)

    (model-p/draw! the-model)

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
  (let [gl-texture (gl-create-texture! id (make-texture-low! (rend-p/img img)))]
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

(defn check-capabilities [a-set]
  (let [caps (GLContext/getCapabilities)]
    (map (fn [func]
           {:func func
            :supported? true}
           ))))

(defn setup-frame
  "set up ogl to be a known state every frame"
  []
  (GL11/glClear clear-mask) 
  (GL11/glEnable GL11/GL_DEPTH_TEST)
  (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
  (GL11/glEnable GL11/GL_SCISSOR_TEST)
  ; (GL11/glEnable GL11/GL_BLEND)
  (GL11/glDisable GL11/GL_BLEND)
  ; (GL11/glEnable GL31/GL_PRIMITIVE_RESTART)
  ; (GL31/glPrimitiveRestartIndex 0x7fffffff)
  (set-primitive-restart-ext 0x7fffffff)

  (GL11/glCullFace GL11/GL_BACK)
  (GL11/glEnable GL11/GL_CULL_FACE)
  ; (GL11/glFrontFace GL11/GL_CW)
  )

(defn- init-gl! []
  (pprint
    (GLContext/getCapabilities))
  (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
  (setup-frame)
  )

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
          (setup-frame)
          (GL11/glViewport a b c d)
          (GL11/glScissor a b c d)
          (GL11/glMatrixMode GL11/GL_PROJECTION)
          (GL11/glLoadIdentity) 
          (GL11/glOrtho 0 (:x canv-dims) (:y canv-dims) 0 -100 100)
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

