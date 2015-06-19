(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman]
            [cloj.render.canvas     :as canvas-render]
            [cloj.web.utils         :refer [by-id]]
            [cljs.core.async        :refer [put! >! chan <! alts! close!]]
            [cljs-http.client       :as http]
            [dommy.core             :as dommy :include-macros true]    
            [hipo.core              :as hipo  :include-macros true]))

(defn mk-img-el [id]
  (hipo/create [:img ^:attrs {:id id :src (str "data/" id ".png")}]))

(defn mk-canvas-el [id w h]
  (hipo/create [:canvas ^:attrs {:id id :width w :heigh h}]))

(defn data->element [id img-data]
  (hipo/create [:img ^:attrs {:id id :src img-data}] ))

(defn element->iimage
  "Turn a HTML image element into an reified IImage"
  [img]
  (reify
    rman/IImage
    (id [_]     (.-id img))
    (width [_]  (.-width img))
    (height [_] (.-height img))
    (img [_]    img)))

(defn xhr->iimage! [img-req parent id]
  (->>
    img-req
    (:body)
    (data->element id)
    (dommy/append! parent)
    (by-id id)
    (element->iimage)))

(defn mk-resource-manager [dom-div-id]
  (let [store (atom {:imgs [] :targets []})
        dom-div (by-id dom-div-id) ]

    (reify
      rman/IResourceManagerInfo
      (find-img [_ id] (element->iimage (by-id id)))

      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_] (println "not implemented"))
      (list-imgs [_] (println "not implemented"))

      rman/IResourceManager
      (clear-resources! [_]
        (dommy/clear! dom-div))

      (create-render-target! [this id w h]
        (let [canvas-el (->>
                          (mk-canvas-el id w h)
                          (dommy/append! dom-div)
                          (by-id id))]
          (canvas-render/canvas (.getContext canvas-el "2d") {:x w :y h})))

      (load-img! [this id]
        (let [ret-chan (chan)
              get-chan (http/get (str "rez/png/" id))]
          (go
            (let [img-req  (<! get-chan)]
              (put! ret-chan (xhr->iimage! img-req dom-div id))))

          ret-chan)))))

