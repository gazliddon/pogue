(ns gaz.canvascomp
  (:require

    [gaz.canvascomprenderbackend :refer [canvas-immediate-renderer]]

    [gaz.renderprotocols :as rp ]

    [gaz.gamerender      :refer [render!
                                 get-scaled-dims]]

    [gaz.color           :refer [rgb-str]]
    [gaz.vec2            :as    v2 ]

    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))

(enable-console-print!)

(def canvas-id "main-canvas-id")

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
        (render! render-data renderer)))

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

