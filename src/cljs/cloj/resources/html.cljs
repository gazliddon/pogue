(ns cloj.resources.html
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ] )

  (:require [cloj.resources.manager :as rman :refer [IResourceManager IResourceManagerInfo]]
            [cloj.render.canvas     :as canvas-render]
            [cloj.math.vec2 :refer [v2]]
            [cljs.core.async        :refer [put! >! chan <! alts! close!]]
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

(defmulti blob->element (fn [e] (-> (.-type e) (.split "/") (aget 0))))

(defmethod blob->element "image" [blob]
  (let [blobURL (.createObjectURL js/URL blob)
        img (hipo/create [:img ^:attrs {:src blobURL}]) ]
    img))

(defmethod blob->element :default [e] (println (str "unknown type " (.-type e))))

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
(defn mk-resource-manager [loader]
  (let [store (atom {:imgs [] :targets []}) ]
    (reify
      IResourceManagerInfo
      (find-img [_ id] (println "not implemented"))
      (find-render-target [_ id] (println "not implemented"))
      (list-render-targets [_] (println "not implemented"))
      (list-imgs [_] (println "not implemented"))

      IResourceManager
      (clear-resources! [_]
        (reset! store {:imgs [] :targets []}))

      (create-render-target! [this id w h]
        (canvas-render/canvas id {:x w :y h}))

      (load-img! [this file-name]
        (cb->chan #(get-elem! (js/XMLHttpRequest.) file-name %))))))


