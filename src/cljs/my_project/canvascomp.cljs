(ns gaz.canvascomp
  (:require

    [gaz.renderprotocols :refer [IRenderBackend
                                 box!
                                 clear!
                                 load-sprs!]]

    [gaz.gamerender      :refer [render!
                                 get-scaled-dims]]

    [gaz.color           :refer [rgb-str]]

    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))

(enable-console-print!)

(def canvas-id "main-canvas-id")

(def game-gfx
  {:source-gfx {:tiles "tiles.png"}
   :sprs {:wall {:src :tiles :x 0 :y 0 :w 8 :h 8} } })


(defn the-canvas-renderer [canvas w h]
  (let [ ctx (.getContext canvas "2d") ]
    (reify
      IRenderBackend

      (load-sprs! [this data]
        (assoc this :sprs data)
        this)

      (spr-scaled! [{:keys [sprs]} spr-id {:keys [x y]} {:keys [ w h ]}]
        (let [spr (spr-id sprs)]
          (println spr)
          ) )

      (spr! [this spr-id {:keys [x y]}]
        )

      (clear! [this col]
        (box! this [0 0 w h] col))

      (box! [_ [x y w h] col]
        (let [col-str (rgb-str col )]
          (set! (.-fillStyle ctx) col-str)
          (.fillRect ctx x y w h))))))

(defn canvas-component [ {:keys [render-data] :as app} owner]
  (reify
    om/IDidMount
    (did-mount [this]
      (let [[w h] (get-scaled-dims render-data)
            canvas (om/get-node owner canvas-id)
            renderer (the-canvas-renderer canvas w h)]
        (om/set-state! owner [:renderer] renderer)))

    om/IDidUpdate
    (did-update [this _ {:keys [renderer] }]
      (when renderer
        (render! render-data renderer)))

    om/IRender
    (render [_]
      (let [[w h] (get-scaled-dims render-data) ]
        (dom/div
          nil
          (dom/canvas
            #js {:id "main-canvas"
                 :width w :height h
                 :className "canvas"
                 :ref canvas-id})     
          )))))

(defn make-canvas-component []
  (fn [app owner]
    (canvas-component app owner)
    )
  )
