(ns cloj.resources.html
  (:require [cloj.resources.manager :as rman]
            [cloj.render.canvas :refer [canvas-immediate-renderer]]
            [dommy.core :as dommy :refer-macros [sel sel1]]))

(defn id-ize [v] (str "#" v))

(defn mk-resource-manager []
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
        (let [canvas (sel1 (id-ize id))]
          (canvas-immediate-renderer canvas  {:x w :y h})))

      (load-img! [this id]
        (let [img     (sel1 (id-ize id))
              [w h]   [(aget img "width")(aget img "height")]]
          (reify
            IImage
            (width [_] 255)
            (height [_] 255)

            ICanvasImage
            (img [_] img)))))))


