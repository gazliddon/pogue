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
    [cloj.protocols.model :as model-p ]

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
      (GL30/glBindVertexArray 0))
    {:vao-id vao-id
     :num-of-indicies (count indicies)}))

(defn draw! [{:keys [vao-id num-of-indicies]} ]
  (GL30/glBindVertexArray vao-id)
  (GL11/glDrawElements GL11/GL_TRIANGLE_STRIP ^Integer num-of-indicies GL11/GL_UNSIGNED_INT 0))
