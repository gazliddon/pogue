(ns cloj.lwjgl.glverts
  (:require
    [cloj.lwjgl.buffers :as buffers :refer [to-indicies-gl
                                            to-floats-gl]])

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (java.io ByteArrayInputStream ByteArrayOutputStream )
    (org.lwjgl BufferUtils)
    (org.lwjgl.opengl GL11 GL15 GL20 GL30)))

(defn mk-float-desc [elems]
  {:gl-type GL11/GL_FLOAT
   :size-bytes (* 4 elems)
   :elems elems})

(def elem->info
  {:f32  (mk-float-desc 1)
   :vec2 (mk-float-desc 2)
   :vec3 (mk-float-desc 3)
   :vec4 (mk-float-desc 4)})

(defmulti set-elem-pointer (fn [elem-type elem-descriptor stride offset] elem-type) )

(defmethod set-elem-pointer :pos [_ {:keys [elems gl-type]} stride offset]
  (do
    (GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
    (GL11/glVertexPointer elems gl-type stride offset)))

(defmethod set-elem-pointer :col [_ {:keys [elems gl-type]} stride offset]
  (do 
    (GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
    (GL11/glColorPointer elems gl-type stride offset)))

(defmethod set-elem-pointer :uv [_ {:keys [elems gl-type]} stride offset]
  (do 
    (GL11/glEnableClientState GL11/GL_TEXTURE_COORD_ARRAY)
    (GL11/glTexCoordPointer elems gl-type stride offset)))

(defn get-vert-size-bytes [vdef]
  (reduce (fn [acc v]
            (let [info ((:elem-type v) elem->info )]
              (+ acc (:size-bytes info))))0 vdef))


(defn make-elem [type elem-type]
  {:type type :elem-info (elem-type elem->info)})

(defn set-pointers [vdef]
  (let [stride (get-vert-size-bytes vdef)]

    (loop [vdefs-left vdef
           offset 0]


      
      )
    )
  )

