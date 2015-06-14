(ns cloj.resources.html
  (:require [cloj.resources.manager :as rman]
            [cloj.render.canvas     :as canvas-render]
            [cloj.render.protocols  :as rp]
            [hipo.core              :as hipo  :include-macros true]
            [dommy.core             :as dommy :include-macros true]))

(defn id-ize [v] (str "#" v))
(defn by-id [v] (-> (id-ize v) (dommy/sel1)))

(defprotocol ICanvasImage
  (img [_]))

(defn mk-img-el [id]
  (hipo/create [:img ^:attrs {:id id :src (str "data/" id ".png")}]))

(defn mk-canvas-el [id w h]
  (hipo/create [:canvas ^:attrs {:id id :width w :heigh h}]))

(defn mk-resource-manager [dom-div-id]
  (let [store (atom {:imgs [] :targets []})
        dom-div (by-id dom-div-id) ]
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
        (do
          (dommy/append! dom-div (mk-canvas-el id w h))
          (let [canvas (by-id id)
                ctx (.getContext canvas "2d")]
            (canvas-render/canvas ctx  {:x w :y h}))))

      (load-img! [this id]
        (do
          (dommy/append! dom-div (mk-img-el id))
          (let [img   (by-id id)
                w     (dommy/px img :width)
                h     (dommy/px img :height) ])

          (reify
            rman/IImage
            (width [_] 255)
            (height [_] 255)

            ICanvasImage
            (img [_] img))  )
        ))))


