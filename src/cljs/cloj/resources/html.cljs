(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman]
            [cloj.render.canvas     :as canvas-render]
            [cljs.core.async        :refer [put! >! chan <! alts! close!]]
            [cloj.render.protocols  :as rp]
            [cljs-http.client       :as http]
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

(defn create-data-img [id img-data]
  (hipo/create [:img ^:attrs {:id id :src img-data}] ))



(defn mk-resource-manager [dom-div-id]
  (let [store (atom {:imgs [] :targets []})
        dom-div (by-id dom-div-id) ]
    (reify
      rman/IResourceManagerInfo
      (find-img [_ id]
        (let [img (by-id id)
              w         (dommy/px img :width)
              h         (dommy/px img :height) ]
          (reify
            rman/IImage (width [_]  w) (height [_] h)
            ICanvasImage (img [_] img))))

      (find-render-target [_ id]
        (println "not implemented"))

      (list-render-targets [_]
        (println "not implemented"))

      (list-imgs [_]
        (println "not implemented"))

      rman/IResourceManager
      (clear-resources! [_]
        (dommy/clear! dom-div))

      (create-render-target! [this id w h]
        (do
          (dommy/append! dom-div (mk-canvas-el id w h))
          (let [canvas (by-id id)
                ctx (.getContext canvas "2d")]
            (canvas-render/canvas ctx  {:x w :y h}))))

      (load-img! [this id]
        (let [ret-chan (chan)
              get-chan (http/get (str "rez/png/" id))]
          (go
            (let [img-req  (<! get-chan)

                  new-img (->>
                            img-req
                            (:body)
                            (create-data-img id)
                            (dommy/append! dom-div))
                  ret-obj   (rman/find-img this id)]
              (put! ret-chan ret-obj)))
          ret-chan)
        ))))



