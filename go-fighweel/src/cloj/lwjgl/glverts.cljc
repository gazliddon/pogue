(ns cloj.lwjgl.glverts
  (:require
    [cloj.lwjgl.buffers :as buffers :refer [to-indicies-gl
                                            to-floats-gl]]

    [clojure.pprint :as pprint :refer [pprint]]
    
    )

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

(defmulti set-elem-pointer (fn [elem-def stride offset] (:type elem-def)) )

(defmethod set-elem-pointer :pos [{:keys [elems gl-type]} stride offset]
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


(do

  (defn make-elem [type elem-type]

    (assoc (type elem->info)
           :type elem-type)
    )

  (def test-vdef
    [( make-elem :vec3 :pos )
     ( make-elem :vec2 :uv )
     ( make-elem :vec4 :col ) ]
    )

  (pprint/pprint test-vdef))

(defn merge-verts [vs]
  (->
    (fn [[ typ stream ]]
      (assoc 
        ((keyword typ ) elem->info)
        :type (keyword stream)))
    (map vs)))

(defn to-funcs [vs]
  (->
    (fn [{:keys [gl-type ]}])
    
    )
  )

(defmacro defverts [sym & vdefs]
  (let [merged-verts (merge-verts (partition 2 vdefs))
        vert-size (reduce #(+ %1 (:size-bytes %2)) 0 merged-verts) ]
    `(def ~sym
       [ ~@merged-verts ])
    )
  )

(defverts standard-vert
  vec3 pos
  vec2 uv
  vec4 col)


(defn set-pointers [vdef]
  (let [stride (get-vert-size-bytes vdef)]

    (loop [[this-def & vdefs-left] vdef
           ret ()
           offset 0]
      
      (if vdefs-left
        (do
          (set-elem-pointer )
          (recur (rest vdefs-left)
                 ret
                 (+ stride offset)))
        ret))))

