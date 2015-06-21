(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman :refer [IResourceManager IResourceManagerInfo IImage]]
            [cloj.render.canvas     :as canvas-render]
            [cloj.math.vec2 :refer [v2]]
            [cloj.web.utils :refer [by-id log-js]]

            [cljs.core.async        :refer [put! >! chan <! alts! close!]]
            [dommy.core             :as dommy :include-macros true]    
            [hipo.core              :as hipo  :include-macros true]))



;; =============================================================================
;; Multi method to turn a blob into an element
(defn img->iimage [img]
  (reify
    IImage
    (width [_] (.-width img))
    (height [_] (.-height img))
    (img [_] img)))

(defmulti blob->element (fn [e id] (-> (.-type e) (.split "/") (aget 0))))

(defmethod blob->element "image" [blob id]
  (let [blobURL (.createObjectURL js/URL blob)
        img (hipo/create [:img ^:attrs { :src blobURL :id id}]) ]
    img))

(defmethod blob->element :default [e] (println (str "unknown type " (.-type e))))

;; =============================================================================
;; Extend req to make it a little easier to deal with 
(defprotocol IXHRReq
  (get-status [_])
  (u-okay-hun? [_])
  (get-blob! [_ uri cb]))

(extend-type js/XMLHttpRequest
  IXHRReq
  (get-status [this] (.-status this))

  (u-okay-hun? [this] (== 200 (get-status this)))

  (get-blob! [xhr uri cb]
    (doto xhr
      (.open "GET" uri true)
      (aset "responseType" "blob")
      (aset "onload"
            (fn [r]
              (when (u-okay-hun? xhr)
                (cb (.-response xhr)))))
      (.send))))

(defn cb->chan
  "Convert a callback function "
  [ cb-fn ]
  (let [ret-chan (chan )]
    (do
      (->>
        (fn [ret-val] (put! ret-chan ret-val) )
        (cb-fn))
      ret-chan))
  )

;; =============================================================================
;; Crappy stab at seperating out loading into a channel
(defprotocol ILoader
  (load-blob! [_ file-name]))

(def xhr-loader
  (reify ILoader
    (load-blob! [_ file-name]
      (let [ret-chan (chan)
            xhr (js/XMLHttpRequest.)]
        (go
          (put! ret-chan (-> (fn [cb] (get-blob! xhr file-name cb))
                             (cb->chan )
                             (<!)))
          ret-chan)))))

;; =============================================================================
;; todo
;; split into 3 I guese
;; loader
;; store
;; info
;; and change to standalone objects - not held in dom
;; yeah - loader
;;        builder
;;        store

(def empty-store {:imgs {} :targets {}})

(defn el->in-div [el div-el]
  (let [id (.-id el)]
    (do
      (dommy/append! div-el el)
      (by-id id))))

(defn el->img [el]
  (reify
    IImage
    (width [_] (.-width el))
    (height [_] (.-height el))
    (img [_] el)))


(defn mk-resource-manager [save-div]
  (let [store (atom empty-store)
        div-el (by-id save-div) ]
    (reify
      IResourceManagerInfo
      (find-img [_ id]           (println "not implemented"))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_]   (println "not implemented"))
      (list-imgs [_]             (println "not implemented"))

      IResourceManager
      (clear-resources! [_]
        (reset! store empty-store))

      (create-render-target! [this id w h]
        (canvas-render/canvas id {:x w :y h}))

      (load-img! [this id file-name]
        (let [xhr (js/XMLHttpRequest.)]
          (go
            (let [ret-chan (chan)]
              (put! ret-chan (-> (cb->chan #(get-blob! xhr file-name %))
                                 (<! )
                                 (blob->element id)
                                 (el->in-div div-el)
                                 (el->img)))
              
              ret-chan)))))))



