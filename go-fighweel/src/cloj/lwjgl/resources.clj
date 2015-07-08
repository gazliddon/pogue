(ns cloj.lwjgl.resources
  (:require [cloj.resources.manager :as rman ]
            [cloj.render.protocols :as rp]
            [cloj.math.vec2 :refer [v2]]
            [digest :as digest]
            [clojure.core.async :as async :refer [chan >! <! put! go]]
            [clojure.java.io :refer [file output-stream input-stream]]))

;; =============================================================================
(defn put-close! [ch v]
  (do
    (async/put! ch v)
    (async/close! ch)))

;; =============================================================================
;; Async loading
(defrecord LoadedFile [data size file-name digest])

(defn load-blocking [file-name]
  (let [f (file file-name)
        size (.length f) ]
    (with-open [in (input-stream f)]
      (let [buf (byte-array size)
            arr (.read in buf)]
        (->LoadedFile buf size file-name (digest/sha-256 f))
        ))))

(defn load-async
  ([ret-chan file-name]
   (future
     (let [file (load-blocking file-name)]
       (put! ret-chan file)))
   ret-chan )
  ([file-name]
   (load-async (chan) file-name)))

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
        (clear-resources! [_]
          (reset! store {}))

        (create-render-target! [this id w h]
          (throw (Exception. "not implemented")))

        (load-img! [this id file-name]
          (let [ret-chan (chan)]
            (go
              (let [img (<! (load-async file-name))]
                (put-close! ret-chan img)))
            ret-chan))))))

