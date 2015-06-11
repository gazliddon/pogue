(ns gaz.canvascomp
  (:require
    [gaz.canvascomprenderbackend :as renderer ]
    [gaz.gamerender      :as gr ]
    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))

(def mk-renderer renderer/canvas-immediate-renderer )
(def get-dims gr/get-scaled-dims )
(def render-it! gr/render! )

(defn build-canvas-component [ canvas-id  ]
  (fn [render-data owner]
    (reify
      om/IDidMount
      (did-mount [_]
        (let [dims (get-dims render-data)
              canvas (om/get-node owner canvas-id) ]
          (om/set-state!
            owner
            [:renderer-backend ]
            (mk-renderer canvas dims))))

      om/IDidUpdate
      (did-update [_ _ {:keys [renderer-backend] }]
        (when renderer-backend
          (render-it! render-data renderer-backend)))

      om/IRender
      (render [_]
        (let [dims (get-dims render-data) ]
          (dom/div
            nil
            (dom/canvas
              #js {:width (:x dims) :height (:y dims)
                   :className "canvas"
                   :ref canvas-id})))))))

