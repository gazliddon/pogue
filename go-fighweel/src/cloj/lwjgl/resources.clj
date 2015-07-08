(ns cloj.lwjgl.resources
  (:require [cloj.resources.manager :as rman ]
            [cloj.render.protocols :as rp]
            [cloj.math.vec2 :refer [v2]]
            [clojure-gl.texture :as cljgl-text]
            [digest :as digest]
            [clojure.core.async :as async :refer [chan >! <! put! go]]
            [clojure.java.io :refer [file output-stream input-stream]]
            [mikera.image.core :as imgz]))

;; =============================================================================
(defn put-close! [ch v]
  (do
    (async/put! ch v)
    (async/close! ch)))

(defn texture->img [gl-tex id]
  (reify rp/IImage
    (id [_] id)
    (dims [_] [(:width gl-tex) (:height gl-tex)])
    (width [_] (:width gl-tex))
    (height [_] (:width gl-tex))
    (img [_] gl-tex)))

;; =============================================================================
;; Async loading
(defrecord LoadedFile [data size file-name digest])

(defn load-blocking [file-name]
  (let [f (file file-name)
        size (.length f) ]
    (with-open [in (input-stream f)]
      (let [buf (byte-array size)
            arr (.read in buf)]
        (->LoadedFile buf size file-name (digest/sha-256 f))))))

(defn do-something-async
  ([ret-chan func]
   (future (put! ret-chan (func)))
   ret-chan)
  ([func] (do-something-async (chan) func)))

(defn load-async
  ([ret-chan file-name] (do-something-async ret-chan #(load-blocking file-name)))
  ([file-name] (load-async (chan) file-name)))

(defn mk-resource-manager []
  (let [store (atom {})]
    (do
      (reify
        rman/IResourceManagerInfo
        (find-img [_ id]           (println "not implemented"))
        (find-render-target [_ id] (println "not implemented"))
        (list-render-targets [_]   (println "not implemented"))
        (list-imgs [_]             (println "not implemented"))

        rman/IResourceManager
        (clear-resources! [_] (reset! store {}))

        (create-render-target! [this id w h] (throw (Exception. "not implemented")))

        (load-img! [this id file-name]
          (let [ret-chan (chan)]
            (future
              (put! ret-chan
                    (-> file-name
                        (imgz/load-image)
                        (cljgl-text/make-texture-low)
                        (texture->img id))))
            ret-chan)
          )))))

