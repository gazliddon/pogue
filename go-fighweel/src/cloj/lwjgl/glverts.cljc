(ns cloj.lwjgl.glverts
  (:require
    [clojure.reflect    :as reflect :refer [reflect]]
    [clojure.walk       :as walk    :refer [prewalk-replace]]
    [clojure.pprint     :as pprint  :refer [pprint with-pprint-dispatch code-dispatch ]]
    [cloj.lwjgl.buffers :as buffers :refer [to-indicies-gl
                                            to-floats-gl]]
    [clojure.pprint     :as pprint  :refer [pprint]]
    )

  (:import 
    (java.nio FloatBuffer IntBuffer ByteOrder ByteBuffer)
    (java.io ByteArrayInputStream ByteArrayOutputStream )
    (org.lwjgl BufferUtils)
    (org.lwjgl.opengl GL11 GL15 GL20 GL30)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; File away somewhere
(defn get-first [xs alt]
  (or (first xs) alt))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti set-elem-pointer (fn [elem-def stride offset] (:type elem-def)) )

(defmethod set-elem-pointer :pos [{:keys [elems gl-type offset]} stride ]
  (do
    (GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
    (GL11/glVertexPointer elems gl-type stride offset)))

(defmethod set-elem-pointer :col [_ {:keys [elems gl-type offset]} stride ]
  (do 
    (GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
    (GL11/glColorPointer elems gl-type stride offset)))

(defmethod set-elem-pointer :uv [_ {:keys [elems gl-type offset]} stride ]
  (do 
    (GL11/glEnableClientState GL11/GL_TEXTURE_COORD_ARRAY)
    (GL11/glTexCoordPointer elems gl-type stride offset)))

(defn get-vert-size-bytes [vdef]
  (reduce (fn [acc v]
            (let [info ((:elem-type v) elem->info )]
              (+ acc (:size-bytes info))))0 vdef))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn mk-float-desc [elems]
  {:gl-type `GL11/GL_FLOAT
   :size-bytes (* 4 elems)
   :elems elems})

(def elem->info
  {:f32  (mk-float-desc 1)
   :vec2 (mk-float-desc 2)
   :vec3 (mk-float-desc 3)
   :vec4 (mk-float-desc 4)})

(defn merge-verts [[typ stream]]
  (assoc 
    ((keyword typ ) elem->info)
    :type (keyword stream))) 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def v-test
  '[vec3 pos
    vec2 uv
    ])

(do
  (def type->fn
    {:pos (fn [{:keys [elems gl-type offset]}]
            `((GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
              (GL11/glVertexPointer ~elems ~gl-type :stride ~offset)))

     :col (fn [{:keys [elems gl-type offset]}]
            `((GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
              (GL11/glVertexPointer ~elems ~gl-type :stride ~offset)))

     :uv (fn [{:keys [elems gl-type offset]}]
           `((GL11/glEnableClientState GL11/GL_VERTEX_ARRAY)
              (GL11/glVertexPointer ~elems ~gl-type :stride ~offset))) })
  
  (defn make-func-calls-forms [v]
    (((:type v) type->fn) v))

  (defn add-offsets-transducer []
    (fn [fx]
      (let [offset (volatile! 0)]
        (fn
          ([result] (fx result))
          ([result {:keys [size-bytes] :as input}]
           (let [this-offset @offset]
             (vreset! offset (+ this-offset size-bytes))
             (fx result (assoc input :offset this-offset ))))))))

  (def my-transducer-pipeline
    (comp
      (partition-all 2)
      (map merge-verts)
      (add-offsets-transducer)
      (map make-func-calls-forms)
      (mapcat identity)
      ))

  (let [forms v-test
        func-name 'set-verts 
        new-code  (sequence my-transducer-pipeline forms)
        stride 10
        final-code `(defn ~func-name []
                      (do
                        ~@(prewalk-replace {:stride stride} new-code)))
        ]

    (binding [pprint/*print-right-margin* 120]
      (with-pprint-dispatch code-dispatch (pprint final-code)))

    )

  )



(defn to-func
  "take the merged vert definition data and
   turn it into a function call"
  [ stride {:keys [offset gl-type size-bytes elems type ] :as rec} ]
  `(set-elem-pointer ~rec ~stride ~offset) 
  )


(def transducer 
  (comp
    (partition-all 2)
    (map merge-verts)
    (add-offsets-transducer)
    ) 
  )




(defmacro defverts
  "macro to take a high level vertex definition and turn
   it into the right set of gl calls that will:
   
   * Enable the right client state for the vert types
   * Set the correct vert buffer pointers
   
   Targetted for rubbishy gl11 but will be easily
   adaptable to gl core 3.2 / webgl / gles
   when I do that switch"
  
  [sym & vdefs]
  (let [merged-verts (merge-verts (partition 2 vdefs))
        vert-size (reduce #(+ %1 (:size-bytes %2)) 0 merged-verts) ]
    `(def ~sym
       (let [mverts# [ ~@merged-verts ]]
         ~@(map (fn []))
         )
       )
    )
  )

; (defverts standard-vert
;   vec3 pos
;   vec2 uv
;   vec4 col)


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

