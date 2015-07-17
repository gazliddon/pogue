(ns cloj.lwjgl.resources
  (:require
    [digest :as digest]

    [cloj.protocols.render    :as rend-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.loader    :as loader-p]
    [cloj.lwjgl.protocols     :refer [IOGLTexture bind-texture!]]

    [clojure-gl.texture  :refer [make-texture-low]]

    [clojure.core.async :as async :refer [chan >! <! put! go]]
    [clojure.java.io :refer [file output-stream input-stream]]
    [mikera.image.core :as imgz :refer [load-image]] )
  
  (:import 
           (org.lwjgl.opengl GL11))
  )

;; =============================================================================
; (defn put-close! [ch v]
;   (do
;     (async/put! ch v)
;     (async/close! ch)))

;; =============================================================================
;; Async loading
(defn mk-resource-manager [loader]
  (let [store (atom {}) ]
    (reify
      res-p/IResourceManagerInfo
      (find-img [_ id]           (println "not implemented"))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_]   (println "not implemented"))
      (list-imgs [_]             (println "not implemented"))

      res-p/IResourceManager
      (clear-resources! [_] (reset! store {}))
      (create-render-target! [this id w h] (throw (Exception. "not implemented")))
      (get-loader [_] loader)

      (load-img! [this id file-name]
        (go
          (comment
            "Async loads img file but converts to OGL texture
             non async (uses memoize) to prevent errors with
             creating a gl texture on a thread not bound to an
             opengl context.
             
             Should be fine but if stuttery I can fiddle around
             with lwjgl to get a context for the loader thread")

          (try
            (let [buffered-image (load-image file-name)
                  get-gl-texture (memoize make-texture-low)
                  width (.getWidth buffered-image)
                  height (.getHeight buffered-image)]
              (reify

                IOGLTexture
                (bind-texture! [this]
                  (GL11/glBindTexture GL11/GL_TEXTURE_2D (rend-p/img this)))

                rend-p/IImage
                (id [_] id)
                (dims [this] [(rend-p/width this) (rend-p/height this)])
                (width [_] width)
                (height [_] height )
                (img [ this ]
                  (get-gl-texture buffered-image))))

            (catch Exception e
              e))))))) 

