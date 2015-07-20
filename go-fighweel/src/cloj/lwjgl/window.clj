(ns cloj.lwjgl.window
  (:require
    [cloj.math.vec2 :refer [v2]]
    [cloj.protocols.system    :refer [ISystem]]
    [cloj.protocols.window    :as window-p]
    [cloj.jvm.loader          :as loader])

  (:import (org.lwjgl.opengl Display DisplayMode)))


(defn- init-window
  [{width :x height :y} title]
  (Display/setDisplayMode (DisplayMode. width height))
  (Display/setTitle title)
  (Display/create))


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
