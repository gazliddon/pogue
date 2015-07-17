(ns cloj.lwjgl.resources
  (:require

    [cloj.protocols.render    :as rend-p :refer [IImage]]
    [cloj.protocols.resources :as res-p  :refer [IResourceManager
                                                 IResourceManagerInfo]]
    [cloj.protocols.loader    :as loader-p]

    [cloj.lwjgl.protocols     :refer [IOGLTexture]]
    [clojure-gl.texture       :refer [make-texture-low]]
    [clojure.core.async       :refer [chan >! <! put! go]]
    [clojure.java.io          :refer [file output-stream input-stream]]
    [mikera.image.core        :refer [load-image]])

  (:import (org.lwjgl.opengl GL11)))

;; =============================================================================
;; Async loading
(defn mk-resource-manager [loader]
  (let [store (atom {}) ]
    (reify

      IResourceManagerInfo
      (find-img [_ id]           (println "not implemented"))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_]   (println "not implemented"))
      (list-imgs [_]             (println "not implemented"))

      IResourceManager
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
                  (->>
                    (get-gl-texture buffered-image)
                    (:tex-id)
                    (GL11/glBindTexture GL11/GL_TEXTURE_2D)))

                IImage
                (id [_] id)
                (dims [this] [(rend-p/width this) (rend-p/height this)])
                (width [_] width)
                (height [_] height )
                (img [ this ] buffered-image)))

            (catch Exception e
              (do
                (println "[Error loading ] " file-name (.getMessage e)) 
                (println  (.getMessage e)) 
                e        )
              )))))))

