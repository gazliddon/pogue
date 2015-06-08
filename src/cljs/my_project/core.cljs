(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [cljs.pprint :as pp]
    [gaz.tiles :refer [mk-tile-map]]
    [gaz.tilemaputils :as tmu ]
    [gaz.tilemapprotocol :as tmp ]
    [gaz.math :refer [cos-01]]
    [gaz.vec2 :as v2 ]
    [gaz.color :as col]
    [gaz.appstate :refer [app-state]]
    [gaz.animcomp :refer [animation-view]]
    [gaz.canvascomp :refer [build-canvas-component]]
    [om.core :as om :include-macros true]
    [cljs.core.async :refer [put! >! chan <! alts! close!]]
    [om.dom :as dom :include-macros true]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defonce update-chan (chan))

(defn traverse-map [f level]
  (tmu/reducer
    level
    (fn [memo x y v]
      (cons (f level (v2/v2 x y) v) memo))
    ()))


(def level-dims (v2/v2 10 10))

(def duff-tile
  {:col col/purple})

(def tiles
  {:blank  {:col [0.15 0.15 0.15]}
   :ground {:col [0 1 0]}
   :water {:col [0 0 0.75]}
   :wall   {:col [0.25 0.25 0]} })

(defn render-tile [level pos v]
  (let [tile  (get tiles v duff-tile) ]
    {:pos pos :dims {:x 1 :y 1} :col (:col tile)}))

(defn render-level [level]
  (traverse-map render-tile level))


(defn rand-coord [level]
  (let [[w h] (tmu/get-size level)
        [x y] [(rand-int w) (rand-int h)] ]
    [x y]))

(defn rand-tile [] (rand-nth (keys tiles )))

(defn set-rand-tile [l]
  (let [[x y] (rand-coord l)
        tile (rand-tile) ]
    (tmp/set-tile l x y tile)))

(defn mix-it-up [level]
  (reduce
    (fn [memo i] (set-rand-tile memo))
    level
    (range 1000)))

(def level
  (->
    (mk-tile-map (:x level-dims) (:y level-dims) :blank)
    (mix-it-up)))

(def rendered-level (render-level level))

(defn make-game-render-data [rd t]
  (assoc rd 
         :xforms [[:identity]
                  [:scale ( v2/v2 16 16 )]
                  [:translate (v2/mul (v2/v2 (cos-01 t) (cos-01 t)) (v2/v2 10 10))] ]
         :bg-col [(cos-01 (* t 1)) 0 t]
         :boxes rendered-level)
   )

(defn update-game [{:keys [tick main-render-data] :as game} dt]
  (let [new-tick (+ dt  tick) ]
    (assoc game
           :main-render-data (make-game-render-data main-render-data new-tick)
           :tick new-tick)))

(defonce first-time? (atom true))


(def main-render-canvas (build-canvas-component "main-render-canvas"))

(defn main []
  (om/root
    (fn [game-state owner]
      (reify
        om/IDidMount
        (did-mount [_ ]
          (go-loop []
                   (let [dt (<! update-chan) ]
                     (om/transact! game-state #(update-game % dt))
                     (recur))))

        om/IRender
        (render [_]
          (dom/div #js {:id "wrapper"}
                   (dom/div nil (dom/h1 nil (-> game-state :main-app :name)))
                   (dom/p nil (-> game-state :tick))
                   ; (om/build canvas-component (:level-render-data game-state) )
                   (om/build main-render-canvas (:main-render-data game-state) )
                   ))))
    app-state
    {:target (. js/document (getElementById "app"))})

  (when @first-time?
    (do
      (swap! first-time? not)
      (js/setInterval (fn [] (put! update-chan (/ 1 60))) 16))))



