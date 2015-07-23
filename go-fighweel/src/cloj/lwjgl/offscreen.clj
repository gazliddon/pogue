(ns cloj.lwjgl.offscreen

  (:require
    [cloj.lwjgl.protocols :refer [IGLTexture
                                  IGLFBO
                                  bind-fbo!  ]]

    [clojure.reflect :as reflect :refer [reflect] ]

    [clojure-gl.buffers :refer [create-int-buffer
                                native-byte-buffer
                                ]]

    [cloj.protocols.render :as rend-p :refer [IImage]]
    [cloj.math.vec2 :as v2 :refer [v2 v2i]])

  (:import (java.nio IntBuffer ByteBuffer)
           (org.lwjgl.opengl GL11 GL15 GL20 GL30)))

(set! *warn-on-eflection* true)

(defn ^Integer get-id [func]
  (let [^IntBuffer ib (create-int-buffer 1)]
    (func ib)
    (.get ib 0)))

(defn create-texture-id []       (get-id #(GL11/glGenTextures %)))
(defn create-frame-buffer-id []  (get-id #(GL30/glGenFramebuffers %)))
(defn create-render-buffer-id [] (get-id #(GL30/glGenRenderbuffers %)))

(defn mk-texture! [^Integer w ^Integer h]
  (let [texid (create-texture-id)]

    (doto  GL11/GL_TEXTURE_2D
      (GL11/glBindTexture texid)
      (GL11/glTexParameteri GL11/GL_TEXTURE_MIN_FILTER GL11/GL_NEAREST)
      (GL11/glTexParameteri GL11/GL_TEXTURE_MAG_FILTER GL11/GL_NEAREST)
      (GL11/glTexParameteri GL11/GL_TEXTURE_WRAP_S GL11/GL_CLAMP)
      (GL11/glTexParameteri GL11/GL_TEXTURE_WRAP_T GL11/GL_CLAMP))


    (GL11/glTexImage2D GL11/GL_TEXTURE_2D
                       0
                       GL11/GL_RGBA
                       w h
                       0
                       GL11/GL_RGBA
                       GL11/GL_BYTE
                       ^ByteBuffer ( native-byte-buffer (* w h 4)))
    {:tex-id texid
     :dims (v2i w h)
     :width  w
     :height h}))

;; Main screen buffer, bind to 0 draws to screen
(def screen-buffer
  (reify
    IGLFBO
    (bind-fbo! [_]
      (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER 0))

    (has-z? [_]
      true)))

(defrecord FrameBufferObject [renderer tex-id fbo-id dims has-z?]
  IGLFBO
  (bind-fbo! [_]
    (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER fbo-id))

  (has-z? [_] has-z?)

  IGLTexture
  (get-uv-coords [_] [0 0 1 1] )
  (bind-texture! [_]
    (GL11/glBindTexture GL11/GL_TEXTURE_2D tex-id))
  
  rend-p/IRenderTarget
  (get-renderer [_] renderer)
  (activate! [ this ]
    (bind-fbo! this)
    renderer)

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
  [renderer ^Integer w ^Integer h ^Boolean has-z-buffer?]

  (let [tex (mk-texture! w h)
        tex-id (:tex-id tex)
        fb-id (create-frame-buffer-id)]

    (do
      (GL30/glBindFramebuffer GL30/GL_FRAMEBUFFER fb-id) 
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
        renderer tex-id fb-id (v2i w h) has-z-buffer?))))


