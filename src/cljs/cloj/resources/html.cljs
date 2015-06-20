(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman :refer [IResourceManager IResourceManagerInfo IImage]]
            [cloj.render.canvas     :as canvas-render]
            [cloj.math.vec2 :refer [v2]]
            [cloj.web.utils :refer [by-id]]
            [cljs.core.async        :refer [put! >! chan <! alts! close!]]
            [hipo.core              :as hipo  :include-macros true]))



;; =============================================================================
;; Multi method to turn a blob into an element
(defn img->iimage [img]
  (reify
    IImage
    (width [_] (.-width img))
    (height [_] (.-height img))
    (img [_] img)))

(defmulti blob->element (fn [id e] (-> (.-type e) (.split "/") (aget 0))))

(defmethod blob->element "image" [id blob]
  (let [blobURL (.createObjectURL js/URL blob)
        img (hipo/create [:img ^:attrs {:id (str id) :src blobURL}]) ]
    (.log js/console img)
    (img->iimage img)))

(defmethod blob->element :default [e] (println (str "unknown type " (.-type e))))

;; =============================================================================
;; Extend req to make it a little easier to deal with 
(defprotocol IXHRReq
  (get-status [_])
  (u-okay-hun? [_])
  (get-elem! [_ id uri cb])
  (get-blob! [_ uri cb]))

(extend-type js/XMLHttpRequest
  IXHRReq
  (get-status [this] (.-status this))

  (u-okay-hun? [this] (== 200 (get-status this)))

  (get-elem! [xhr id uri cb]
    (get-blob! xhr uri #(cb (blob->element id %))))

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
  (let [ret-chan (chan)]
    (do
      (->>
        (fn [ret-val] (put! ret-chan ret-val) )
        (cb-fn))
      ret-chan)))

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


(defprotocol ILoader
  (load-async! [_ file-name]))

(defn mk-resource-manager [save-div]
  (let [store (atom empty-store)
        div-el (by-id save-div)
        ]
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
        (cb->chan #(get-elem! (js/XMLHttpRequest.) id file-name %))))))


