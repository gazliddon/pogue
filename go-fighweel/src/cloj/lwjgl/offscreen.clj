(ns cloj.lwjgl.offscreen

  (:require
    [cloj.lwjgl.protocols :refer [IGLTexture
                                  IGLFBO]]
    [cloj.protocols.render :refer [IImage]]
    [cloj.math.vec2 :as v2 :refer [v2]])

  (:import

    (java.nio ByteBuffer ByteOrder IntBuffer FloatBuffer)
    (org.lwjgl.opengl GL11 GL30 GL31)
    )
  )

(defn native-byte-buffer [sz]
  (let [bb (ByteBuffer/allocateDirect (* 4 sz))]
    (.order bb (ByteOrder/nativeOrder))
    bb))

(defn create-int-buffer [sz]
  (.asIntBuffer (native-byte-buffer sz)))


(defn create-texture-id []
  (let [^IntBuffer ib (create-int-buffer 1)]
    (GL11/glGenTextures ib)
    (.get ib 0)))

(defn create-frame-buffer-id []
  (let [^IntBuffer ib (create-int-buffer 1)]
    (GL30/glGenFramebuffers ib)
    (.get ib 0)))

(defn create-render-buffer-id []
  (let [^IntBuffer ib (create-int-buffer 1)]
    (GL30/glGenRenderbuffers ib)
    (.get ib 0)))

(defn mk-texture! [w h]
  (let [texid (create-texture-id) ]
    (GL11/glBindTexture GL11/GL_TEXTURE_2D texid)

    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MIN_FILTER GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_MAG_FILTER GL11/GL_LINEAR)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_S GL11/GL_CLAMP)
    (GL11/glTexParameteri GL11/GL_TEXTURE_2D GL11/GL_TEXTURE_WRAP_T GL11/GL_CLAMP)

    (GL11/glTexImage2D GL11/GL_TEXTURE_2D
                       0
                       GL11/GL_RGBA
                       w h
                       0
                       GL11/GL_RGBA 
                       GL11/GL_UNSIGNED_BYTE
                       nil)
    {:tex-id texid
     :dims (v2 w h)
     :width  w
     :height  h }))


(defrecord FrameBufferObject [tex-id fbo-id dims has-z?]

  IGLFBO
  (bind-fbo! [_]
    (assert false)
    )
  (has-z? [_] has-z?)

  IGLTexture
  (get-uv-coords [_] [0 0 1 1] )
  (bind-texture! [_]
    (assert false)) 

  IImage
  (id [_] nil)
  (dims [_] dims)
  (width [_] (:x dims))
  (height [_] (:y dims) )
  (img [_] nil) )
  
(defn check-fbo-status! []
  (let [status (GL30/glCheckFramebufferStatus GL30/GL_FRAMEBUFFER)]
    (cond
      (= status GL30/GL_FRAMEBUFFER_COMPLETE) true 
      (= status GL30/GL_FRAMEBUFFER_UNSUPPORTED) (throw (Exception. "fbo broken")) 
      :default (throw (Exception. "fbo broken unkown")) 
      )
    )
  )

(defn mk-offscreen-buffer!
  [w h has-z-buffer?]
  (let [tex (mk-texture! w h)
        tex-id (:tex-id tex)
        fb-id (create-frame-buffer-id)]

    (do
      ;; Create the fbo bound to the texture
      (GL30/glFramebufferTexture2D
        GL30/GL_FRAMEBUFFER
        GL30/GL_COLOR_ATTACHMENT0
        GL11/GL_TEXTURE_2D
        tex-id
        0)

      (when has-z-buffer?
        (let [depth-id (create-render-buffer-id)]

          (GL30/glBindRenderbuffer
            GL30/GL_RENDERBUFFER
            depth-id)

          (GL30/glRenderbufferStorage 
            GL30/GL_RENDERBUFFER
            GL30/GL_DEPTH_COMPONENT32F
            w h  )

          (GL30/glFramebufferRenderbuffer
            GL30/GL_RENDERBUFFER
            GL30/GL_DEPTH_ATTACHMENT
            GL30/GL_RENDERBUFFER
            depth-id)))

      (check-fbo-status!)

      (->FrameBufferObject
        tex-id fb-id (v2 w h) has-z-buffer?))))


