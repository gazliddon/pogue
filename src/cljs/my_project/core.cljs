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
    [gaz.canvascomp :refer [canvas-component]]
    [om.core :as om :include-macros true]
    [cljs.core.async :refer [put! >! chan <! alts! close!]]
    [om.dom :as dom :include-macros true]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defonce first-time? (atom true))
(defonce update-chan (chan))

(defn traverse-map [f level]
  (tmu/reducer
    level
    (fn [memo x y v]
      (cons (f level (v2/v2 x y) v) memo))
    ()))

(def scaler (v2/v2 11 11))

(def duff-tile
  {:col col/purple})

(def tiles
  {:blank  {:col [0.15 0.15 0.15]}
   :ground {:col [0 1 0]}
   :water {:col [0 0 0.75]}
   :wall   {:col [0.25 0.25 0]} })

(defn render-tile [level pos v]
  (let [tile  (get tiles v duff-tile)
        scaled (v2/mul pos scaler)]
    {:pos scaled :dims scaler :col (:col tile)}))

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
    (mk-tile-map 30 30 :blank)
    (mix-it-up)
    ))

(def rendered-level (render-level level))

(defn update-game [game dt]
  (let [t (+ dt  (-> game :count :count))
        ]
    (-> game
        (assoc-in [:render-data :boxes] rendered-level)
                  ; [{:x (* 100  (cos-01 (* 5 t))) :y 10 :w 100 :h 100 :col [0 (cos-01 (* 0.5 t)) 0]}]
        (assoc-in [:render-data :sprs]
                  [ { :id :floor :pos (v2/v2 100 100) } ]
                  )
        (assoc-in [:render-data :bg-col ] [(cos-01 (* t 1)) 0 t])
        (assoc-in [:count :count] t))))


(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IDidMount
        (did-mount [_ ]
          (go-loop []
                   (let [dt (<! update-chan)
                         new-app (update-game app dt)
                         ]
                     (om/transact! app #(update-game % dt))
                   (recur))))

        om/IRender
        (render [_]
          (dom/div #js {:id "wrapper"}
                   (dom/div nil (dom/h1 nil (-> app :main-app :name)))
                   (om/build canvas-component app )
                   ))))
    app-state
    {:target (. js/document (getElementById "app"))})

  (when @first-time?
    (do
      (swap! first-time? not)
      (js/setInterval (fn [] (put! update-chan (/ 1 60))) 16))))



