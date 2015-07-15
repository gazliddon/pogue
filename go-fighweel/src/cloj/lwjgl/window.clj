(ns cloj.lwjgl.window
  (:require
    [cloj.math.vec2 :refer [v2]]
    [cloj.jvm.resources       :as res]
    [cloj.protocols.render    :as rend-p]
    [cloj.protocols.system    :refer [ISystem]]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.window    :as window-p]
    [cloj.jvm.loader          :as loader])

  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl ContextAttribs Display DisplayMode GL11 GL15 GL20 GL30 PixelFormat)
           (org.lwjgl.util.glu GLU)))

(defn- init-window
  [{width :x height :y} title]
  (let [pixel-format (PixelFormat.)
        context-attributes (-> (ContextAttribs. 3 2)
                               (.withForwardCompatible true)
                               (.withProfileCore false))
        current-time-millis 0]
    (def globals (ref {:width width
                       :height height
                       :title title }))
    (Display/setDisplayMode (DisplayMode. width height))
    (Display/setTitle title)
    ; (Display/create pixel-format context-attributes)
    (Display/create)
    ))

(defn- init-gl
  []
  (let [{:keys [width height]} @globals]
    (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
    (GL11/glClearColor 0.0 0.0 0.0 0.0)
    (GL11/glViewport 0 0 width height)
    (GL11/glDisable GL11/GL_TEXTURE_2D)
    (GL11/glDisable GL11/GL_DEPTH_TEST)
    (GL11/glBlendFunc GL11/GL_SRC_ALPHA GL11/GL_ONE_MINUS_SRC_ALPHA)
    (GL11/glDisable GL11/GL_BLEND) 

    (GL11/glClearColor 0.0 0.0 0.0 0.0)
    (GL11/glMatrixMode GL11/GL_PROJECTION)
    (GLU/gluOrtho2D 0.0 width
                    0.0 height)
    (GL11/glMatrixMode GL11/GL_MODELVIEW)
    (GL11/glLoadIdentity)
    ))


(defn mk-lwjgl-window []
  (let [exists? (atom false)
        dims (atom {}) ]
    (reify
      window-p/IWindow

      (create! [this dims-in title]
        (do
          (when (not @exists? ) 
            (init-window dims-in title)
            (reset! dims dims-in)
            (swap! exists? not))))

      (destroy! [_]
        (if @exists?
          (do
            (Display/destroy)
            (swap! exists? not))))

      (update! [_]
        (when @exists?
          (do
            (Display/update)
            (Display/sync 60))
          )))))
