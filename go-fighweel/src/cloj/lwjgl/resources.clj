(ns cloj.lwjgl.resources
  (:require

    [cloj.protocols.render    :as rend-p :refer [IImage]]
    [cloj.protocols.resources :as res-p  :refer [IResourceManager
                                                 IResourceManagerInfo]]
    [cloj.protocols.loader    :as loader-p]

    [clojure.core.async       :refer [chan >! <! put! go]]
    [mikera.image.core        :refer [load-image]]))

(defn- bufffer-img->iimage [buffered-img id]
  (let [ [w h] [(.getWidth img) (.getHeight img)] ]
    (reify
      IImage
      (id [_] id)
      (dims [_] [0 0 w h])
      (width [_] w)
      (height [_] h)
      (img [_] buffered-image))))

;; =============================================================================
(defn mk-resource-manager [loader]
  (let [store (atom {})
        get-image (memoize load-image) ]
    (reify
      IResourceManagerInfo
      (find-img [_ id]           (println "not implemented"))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_]   (println "not implemented"))
      (list-imgs [_]             (println "not implemented"))

      IResourceManager
      (clear-resources! [_]
        (reset! store {}))

      (create-render-target! [this id w h]
        (throw (Exception. "not implemented")))

      (get-loader [_] loader)

      (load-img!
        "Async loads img file"
        [this file-name]
        (go
          (try
            (->
              (get-image file-name)
              (bufffer-img->iimage  file-name))

            (catch Exception e
              (do
                (println "[Error loading ] " file-name (.getMessage e)) 
                (println  (.getMessage e)) 
                e))))))))

