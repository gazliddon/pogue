(ns cloj.lwjgl.system
  (:require
    [cloj.jvm.resources       :refer [mk-resource-manager]]
    [cloj.protocols.system    :refer [ISystem]]
    [cloj.protocols.resources :as res-p]

    [cloj.lwjgl.window        :refer [mk-lwjgl-window]]
    [cloj.jvm.loader          :refer [mk-loader]])

  (:import (java.nio ByteBuffer FloatBuffer)
           (org.lwjgl BufferUtils)
           (org.lwjgl.opengl ContextAttribs Display DisplayMode GL11 GL15 GL20 GL30 PixelFormat)
           (org.lwjgl.util.glu GLU)))


(defn mk-system []
  (let [loader  (mk-loader)
        rm      (mk-resource-manager loader)
        window  (mk-lwjgl-window)
        rend    nil]
    (do
      (res-p/clear-resources! rm)

      (reify
        ISystem
        (log [_ txt]
          (println txt))

        (get-window [_] window)

        (get-loader [_] loader)

        (get-resource-manager [_] rm)

        (get-render-engine [_]
          (do
            {:all-done "yeah!"}))))))





