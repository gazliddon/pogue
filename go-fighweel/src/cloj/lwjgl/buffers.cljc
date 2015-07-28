;; Some code to make it easier build lwjgl native buffers
(ns cloj.lwjgl.buffers
  (:require 

    [clojure.pprint :as pprint :refer [pprint]]
    [clojure.reflect :as reflect :refer [reflect]])

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (org.lwjgl BufferUtils)
    (org.lwjgl.opengl GL11 GL15)))

(defprotocol IToBuffer
  (to-buffer [_]))


(defprotocol IToGLBuffer
  (to-gl-buffer [_]))

;; Some nasty macroing so I can extend primitive buffers
;; with routines to create the kind of typed buffer I need
;; to send stuff to openGL

(defmacro extend-protocol-arrays []
  (let [ba (symbol "[B")
        ia (symbol "[I")
        fa (symbol "[F") ]

  `(extend-protocol IToBuffer
     ~ba
     (to-buffer [this#]
       (doto (BufferUtils/createByteBuffer (count this#))
         (.put this#)
         (.flip)))

     ~ia
     (to-buffer [this#]
       (doto (BufferUtils/createIntBuffer (count this#))
         (.put this#)
         (.flip)))

     ~fa
     (to-buffer [this#]
       (doto (BufferUtils/createFloatBuffer (count this#))
         (.put this#)
         (.flip)))

     )))

(extend-protocol-arrays)

(defn make-gl-buffer [buffer-type buffer]
  (let [vbo-id (GL15/glGenBuffers)]
    (do
        (GL15/glBindBuffer buffer-type vbo-id)
        (GL15/glBufferData buffer-type (to-buffer buffer) GL15/GL_STATIC_DRAW))
    vbo-id))


(defn to-indicies-gl [indices]
  (make-gl-buffer GL15/GL_ELEMENT_ARRAY_BUFFER (int-array indices)))

(defn to-floats-gl [verts]
  (make-gl-buffer GL15/GL_ARRAY_BUFFER (float-array verts)))
