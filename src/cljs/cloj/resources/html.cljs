(ns cloj.resources.html
  (:require [cloj.resources.manager :as rman]
            [cloj.render.canvas :as canvas-render]
            [cloj.render.protocols :as rp]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn id-ize [v] (str "#" v))

(defprotocol ICanvasImage
  (img [_]))

(defn mk-resource-manager []
  (println "called!")
  (let [store (atom {:imgs [] :targets []})]
    (reify
      rman/IResourceManagerInfo
      (find-img [_ id]
        (println "not implemented"))

      (find-render-target [_ id]
        (println "not implemented"))

      (list-render-targets [_]
        (println "not implemented"))

      (list-imgs [_]
        (println "not implemented"))

      rman/IResourceManager

      (create-render-target! [this id w h]
        (let [canvas (sel1 (id-ize id))
              ctx (.getContext canvas "2d")]
          (canvas-render/canvas ctx  {:x w :y h})))

      (load-img! [this id]
        (let [img   (sel1 (id-ize id))
              [w h] [(aget img "width")(aget img "height")]]
          (reify
            rman/IImage
            (width [_] 255)
            (height [_] 255)

            ICanvasImage
            (img [_] img)))))))


