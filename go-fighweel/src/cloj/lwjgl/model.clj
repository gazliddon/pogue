(ns cloj.lwjgl.model
  (:require

    [cloj.totransit :as to-transit]

    [clojure.core.async :as async]

    [experiments.chan :as expch]

    [experiments.depdelay :as exp :refer [gl-create-texture! 
                                          gl-create-vao! 
                                          gl-create-render-buffer!
                                          depends-on-file
                                          IUnrealize
                                          ]]

    [cloj.lwjgl.buffers   :as buffers :refer [to-indicies-gl
                                              to-floats-gl]]

    [cloj.lwjgl.glverts   :as glverts :refer [defverts]]
    [cloj.protocols.model :as model-p :refer [IModel draw!]]

    [clojure.pprint       :as pprint  :refer [pprint]]
    [clojure.reflect      :as reflect :refer [reflect]]
    )

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (java.io ByteArrayInputStream ByteArrayOutputStream )
    (org.lwjgl BufferUtils)
    (org.lwjgl.opengl GL11 GL15 GL20 GL30))
  )


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; loading stuff
(defn load-it [base-dir file-name]
  (slurp (str base-dir file-name )))

(def model-xform 
  (comp
    (map #(load-it "resources/public/generated/" %))
    (map #(to-transit/read-transit-str %))))

(def file-data-ch (async/chan 1 model-xform))

(defn mk-file-bind-chan []
  (async/chan 1 model-xform))


(def the-model
  (expch/make-stuff
    (mk-file-bind-chan)
    (fn [v]
      (make-other-buffers v)
      )
    )
  )

(defn do-it
  (let [j])
  )

;; make a responder
;; load the file
;; send it down the channel
;; bind the channel to any file changes
;; job done



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defverts set-standard-vert
  vec3 pos
  vec2 uv
  vec4 col)

(defn make-other-buffers [{:keys [verts indicies] :as model}]
  (let [vao-id   (GL30/glGenVertexArrays)
        ibo-id   (to-indicies-gl indicies)
        vbo-id   (to-floats-gl verts)]
    (do
      (GL30/glBindVertexArray vao-id)
      (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER vbo-id)
      (set-standard-vert)
      (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER ibo-id) 
      (GL30/glBindVertexArray 0))
    {:vao-id vao-id
     :num-of-indicies (count indicies)})
  )

(defn make-buffers [model]
  (let [verts    (-> model :verts :vals)
        indicies (-> model :indicies )
        vao-id   (GL30/glGenVertexArrays)
        ibo-id   (to-indicies-gl indicies)
        vbo-id   (to-floats-gl verts)]
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


(defn make-model [id model]
  (let [gl-model (gl-create-vao! id (make-other-buffers model)) ]
    (reify
      IUnrealize
      (unrealize! [this]
        (exp/unrealize! gl-model))
   
      IModel
      (draw! [_]
        (let [{:keys [vao-id num-of-indicies]} @gl-model] 
          (do
            (GL30/glBindVertexArray vao-id)
            (GL11/glDrawElements GL11/GL_TRIANGLE_STRIP ^Integer num-of-indicies GL11/GL_UNSIGNED_INT 0)
            ))))))

#_(defn make-model-2 [model]
  (let [gl-model (gl-create-vao! id (make-other-buffers model)) ]
    (reify
      IUnrealize
      (unrealize! [this]
        (exp/unrealize! gl-model))
   
      IModel
      (draw! [_]
        (let [{:keys [vao-id num-of-indicies]} @gl-model] 
          (do
            (GL30/glBindVertexArray vao-id)
            (GL11/glDrawElements GL11/GL_TRIANGLE_STRIP ^Integer num-of-indicies GL11/GL_UNSIGNED_INT 0)
            ))))))




