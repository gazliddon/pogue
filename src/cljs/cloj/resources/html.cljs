(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman :refer [IResourceManager IResourceManagerInfo]]
            [cloj.render.canvas     :as canvas-render]
            [cloj.web.utils         :refer [by-id]]
            [cloj.math.vec2 :refer [v2]]
            [cljs.core.async        :refer [put! >! chan <! alts! close!]]
            [cljs-http.client       :as http]
            [dommy.core             :as dommy :include-macros true]    
            [hipo.core              :as hipo  :include-macros true]))




;; =============================================================================
;; {{{ XHR2 Stuff


;; =============================================================================
;; Multi method to turn a blob into an element
(defprotocol IXHRReq
  (get-status [_])
  (okay? [_])
  (get-elem! [_ uri cb])
  (get-blob! [_ uri cb]))

(defmulti blob->element (fn [e] (.-type e)))

(defmethod blob->element "image/png" [blob]
  (let [blobURL (.createObjectURL js/URL blob)
        img (hipo/create [:img ^:attrs {:src blobURL}]) ]
    img))

(defmethod blob->element :default [e]
  (println (str "unknown type " e)))

;; =============================================================================
;; Extend req to make it a little easier to deal with 
(extend-type js/XMLHttpRequest
  IXHRReq
  (get-status [this] (.-status this))

  (okay? [this] (== 200 (get-status this)))

  (get-elem! [xhr uri cb]
    (get-blob! xhr uri #(cb (blob->element %))))

  (get-blob! [xhr uri cb]
    (do
      (.open xhr "GET" uri true)
      (aset xhr "responseType" "blob")
      (aset xhr "onload" (fn [r]
                           (when (okay? xhr)
                             (cb (.-response xhr)))))
      (.send xhr))))

(defn cb->chan
  "Convert a callback routine into a channel one"
  [ cb-fn ]
  (let [ret-chan (chan)
        put-fn (fn [blob]
                (put! ret-chan blob) ) ]
    (cb-fn put-fn)
    ret-chan))

(defn load-image! [uri]
  (cb->chan #(get-elem! (js/XMLHttpRequest.) uri %)))

;; =============================================================================
(defn mk-img-el [id]
  (hipo/create [:img ^:attrs {:id id :src (str "data/" id ".png")}]))

(defn mk-canvas-el [id w h]
  (hipo/create [:canvas ^:attrs {:id id :width w :height h}]))

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

;; split into 3 I guese
;; loader
;; store
;; info
;; and change to standalone objects - not held in dom
;; yeah - loader
;;        builder
;;        store

(defn mk-resource-manager [dom-div-id]
  (let [store (atom {:imgs [] :targets []})
        dom-div (by-id dom-div-id) ]
    (reify
      IResourceManagerInfo
      (find-img [_ id] (element->iimage (by-id id)))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_] (println "not implemented"))
      (list-imgs [_] (println "not implemented"))

      IResourceManager
      (clear-resources! [_]
        (dommy/clear! dom-div))

      (attach-renderer [this id]
        (print id)
        (let [el (by-id id)
              ctx (.getContext el "2d")
              w (aget ctx "width")
              h (aget ctx "height") ]
        (canvas-render/canvas ctx (v2 w h))))

      (create-render-target! [this id w h]
        (let [canvas-el (->>
                          (mk-canvas-el id w h))
              ctx (.getContext canvas-el "2d") ]
          (canvas-render/canvas (.getContext canvas-el "2d") {:x w :y h})))

      (load-img! [this file-name]
        (cb->chan #(get-elem! (js/XMLHttpRequest.) file-name %)) 
        ))))


