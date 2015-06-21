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
  (let [img (js/Image.)
        ret-chan (chan)]

    (doto img
      (aset "id" id)
      (aset "onload" (fn [_] (put! ret-chan img)))
      (aset "src" (.createObjectURL js/URL blob)))

    ret-chan))

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
          (->>
            (fn [cb] (get-blob! xhr file-name cb))
            (cb->chan)
            (<!)
            (put! ret-chan)))

          ret-chan 
        ))))

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

(defn el->img [el]
  (reify
    IImage
    (width [_] (.-width el))
    (height [_] (.-height el))
    (dims [this]
      [0 0 (rman/width this) (rman/height this)])
    (img [_] el)))

(defn msg [v s]
  (println (str s "(" (type v) ")"))
  v)

(defn mk-resource-manager []
  (let [store (atom empty-store)]
    (println "TRYIN THIS!")
    (do
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
          (let [ret-chan (chan)]
            (go
              (put! ret-chan (-> 
                               (load-blob! xhr-loader file-name)
                               (<!) 
                               (blob->element id)
                               (<!)
                               (el->img))))
            ret-chan)))  
      )
    ))



