(ns cloj.lwjgl.system
  (:require
    [cloj.lwjgl.resources :as res]
    [cloj.system            :refer [ISystem]]
    [cloj.resources.manager :as rman])

  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl ContextAttribs Display DisplayMode GL11 GL15 GL20 GL30 PixelFormat)
           (org.lwjgl.util.glu GLU))
  )

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
    ; (init-buffers)
    ; (init-shaders)
    (print "@globals")
    (print @globals)
    ;;(println "")
    ))

(defn mk-system []
  (let [rm   (res/mk-resource-manager)
        rend nil]
    (do
      (rman/clear-resources! rm)

      (reify
        ISystem
        (log [_ txt])

        (get-resource-manager [_]
          rm)

        (get-render-engine [_]
          (do
            {:all-done "yeah!"}))))))


(defprotocol IWindow
  (create [_ w h title])
  (destroy [_])
  (updater [_]))

(defn mk-lwjgl-window []
  (let [exists? (atom false)
        dims (atom {}) ]

    (reify
      IWindow

      (create [this w h title]
        (do
          (when @exists?
            (destroy this))
          (init-window w h title)
          (reset! dims {:x w :h h})
          (swap! exists? not)))

      (destroy [_]
        (if @exists?
          (do
            (Display/destroy)
            (swap! exists? not))))

      (updater [_]
        (when @exists?
          (Display/update)))))
  )

(def the-window (mk-lwjgl-window))

(comment
  (mk-system )
  (create the-window 100 100 "poo")
  (updater the-window)
  (destroy the-window))

