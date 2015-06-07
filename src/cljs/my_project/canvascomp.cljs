(ns gaz.canvascomp
  (:require

    [gaz.renderprotocols :refer [IRenderBackend
                                 load-sprs!  
                                 spr-scaled!
                                 spr!
                                 box!
                                 clear!
                                 ]]

    [gaz.gamerender      :refer [render!
                                 get-scaled-dims]]

    [gaz.color           :refer [rgb-str]]
    [gaz.vec2            :as    v2 ]

    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))

(enable-console-print!)

(def canvas-id "main-canvas-id")

(defn canvas-immediate-renderer [canvas dims]
  (let [ctx (.getContext canvas "2d") ]
    (reify
      IRenderBackend

      (load-sprs! [_ _]
        (println "not implemented"))

      (spr-scaled! [_ _]
        (println "not implemented"))

      (spr! [_ _]
        (println "not implemented"))

      (clear! [this col]
        (box! this [(v2/v2 0 0) dims col]))

      (box! [_  [{x :x y :y} {w :x h :y} col]]
        (let [col-str (rgb-str col)]
          (set! (.-fillStyle ctx) col-str)
          (.fillRect ctx x y w h))))))

(defn canvas-component [ {:keys [render-data] :as app} owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (let [dims (get-scaled-dims render-data)
            canvas (om/get-node owner canvas-id)
            renderer (canvas-immediate-renderer canvas dims)]
        (om/set-state! owner [:renderer] renderer)))

    om/IDidUpdate
    (did-update [this _ {:keys [renderer] }]
      (when renderer
        (render! render-data renderer)
        ))

    om/IRender
    (render [_]
      (let [dims (get-scaled-dims render-data) ]
        (dom/div
          nil
          (dom/canvas
            #js {:id "main-canvas"
                 :width (:x dims) :height (:y dims)
                 :className "canvas"
                 :ref canvas-id})     
          )))))

