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

      (get-dims [_]
        @dims)

      (update! [_]
        (when @exists?
          (do
            (Display/update)
            (Display/sync 60))
          )))))
