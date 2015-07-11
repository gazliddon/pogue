(ns cloj.jvm.resources
  (:require
    [digest :as digest]

    [cloj.protocols.render    :as rend-p]
    [cloj.protocols.resources :as res-p]
    [cloj.protocols.loader    :as loader-p]

    [clojure.core.async :as async :refer [chan >! <! put! go]]
    [clojure.java.io :refer [file output-stream input-stream]]
    [mikera.image.core :as imgz :refer [load-image]] ))

;; =============================================================================
(defn put-close! [ch v]
  (do
    (async/put! ch v)
    (async/close! ch)))

;; =============================================================================
;; Async loading

(defn mk-resource-manager [loader]
  (let [store (atom {})
        load-async! (fn [fname] (loader-p/load-async! loader fname))
        load-blocking! (fn [fname] (loader-p/load-blocking! loader fname)) ]

    (reify
      res-p/IResourceManagerInfo
      (find-img [_ id]           (println "not implemented"))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_]   (println "not implemented"))
      (list-imgs [_]             (println "not implemented"))

      res-p/IResourceManager
      (clear-resources! [_] (reset! store {}))
      (create-render-target! [this id w h] (throw (Exception. "not implemented")))

      (load-img! [this id file-name]
        (go
          (try
            (let [buffered-image (load-image file-name)
                  texture-in-gl (atom nil)]
              (reify
                rend-p/IImage
                (id [_] id)

                (dims [this] [(rend-p/width this) (rend-p/height this)])

                (width [_] (.getWidth buffered-image))

                (height [_](.getHeight buffered-image) )

                (img [ this ]
                  buffered-image
                  )))

            (catch Exception e
              e))))))) 

