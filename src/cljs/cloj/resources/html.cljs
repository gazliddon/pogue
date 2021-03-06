(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman :refer [IResourceManager IResourceManagerInfo ]]
            [cloj.render.protocols :as rp]
            [cloj.render.canvas     :as canvas-render]
            [cloj.math.vec2 :refer [v2]]
            [cloj.web.utils :refer [by-id log-js]]

            [cljs.core.async        :as async :refer [>! chan <! alts!]]
            [dommy.core             :as dommy :include-macros true]    
            [hipo.core              :as hipo  :include-macros true]))

;; =============================================================================
;; {{{ Misc TODO move into utils file
(defn put-close! [ch v]
  (do
    (async/put! ch v)
    (async/close! ch)))

(defn cb->chan
  "Convert a callback function "
  [ cb-fn ]
  (let [ret-chan (chan )]
    (do
      (->>
        (fn [ret-val] (put-close! ret-chan ret-val))
        (cb-fn))
      ret-chan)))

;; }}}

;; =============================================================================
;; {{{ Multi method to turn a blob into an element
(defn img->iimage [img]
  (reify
    rp/IImage
    (width [_] (.-width img))
    (height [_] (.-height img))
    (img [_] img)))

(defmulti blob->element (fn [e id] (-> (.-type e) (.split "/") (aget 0))))

(defmethod blob->element "image" [blob id]
  (let [img (js/Image.)
        ret-chan (chan)]

    (doto img
      (aset "id" id)
      (aset "onload" (fn [_] (put-close! ret-chan img)))
      (aset "src" (.createObjectURL js/URL blob)))

    ret-chan))

(defmethod blob->element :default [e] (println (str "unknown type " (.-type e))))
;; }}}

;; =============================================================================
;; {{{ Extend req to make it a little easier to deal with 
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

;; }}}

;; =============================================================================
;; {{{ Crappy stab at seperating out loading into a channel
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
            (put-close! ret-chan))
          )
          ret-chan 
        ))))

;; }}}

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

(defn el->img [el id]
  (reify
    rp/IImage
    (id [_] id)
    (width [_] (.-width el))
    (height [_] (.-height el))
    (dims [this]
      [0 0 (rp/width this) (rp/height this)])
    (img [_] el)))

(defn mk-resource-manager []
  (let [store (atom empty-store)]
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
              (put-close! ret-chan
                          (-> 
                            (load-blob! xhr-loader file-name)
                            (<!) 
                            (blob->element id)
                            (<!)
                            (el->img id))))
            ret-chan)))  
      )
    ))

