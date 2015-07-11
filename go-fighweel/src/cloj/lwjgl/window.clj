(ns cloj.lwjgl.window
  (:require
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
  [width height title]
  (let [pixel-format (PixelFormat.)
        context-attributes (-> (ContextAttribs. 3 2)
                               (.withForwardCompatible true)
                               (.withProfileCore true))
        current-time-millis 0]
    (def globals (ref {:width width
                       :height height
                       :title title
                       :angle 0.0
                       :last-time current-time-millis
                       ;; geom ids
                       :vao-id 0
                       :vbo-id 0
                       :vboc-id 0
                       :vboi-id 0
                       :indices-count 0
                       ;; shader program ids
                       :vs-id 0
                       :fs-id 0
                       :p-id 0
                       ::angle-loc 0}))
    (Display/setDisplayMode (DisplayMode. width height))
    (Display/setTitle title)
    (Display/create pixel-format context-attributes)))

(defn- init-gl
  []
  (let [{:keys [width height]} @globals]
    (println "OpenGL version:" (GL11/glGetString GL11/GL_VERSION))
    (GL11/glClearColor 0.0 0.0 0.0 0.0)
    (GL11/glViewport 0 0 width height)
    ))

(defn mk-lwjgl-window []
  (let [exists? (atom false)
        dims (atom {}) ]
    (reify
      window-p/IWindow

      (create [this w h title]
        (do
          (when (not @exists? ) 
            (init-window w h title)
            (reset! dims {:x w :h h})
            (swap! exists? not))))

      (destroy [_]
        (if @exists?
          (do
            (Display/destroy)
            (swap! exists? not))))

      (updater [_]
        (when @exists?
          (Display/update))))))
