(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!]])
  (:require
    [cljs.pprint :as pp]
    [gaz.tiles :refer [ITileMap
                       mk-tile-map
                       reducer]]
    [gaz.math :refer [cos-01]]
    [gaz.vec2 :refer [v2]]
    [gaz.color :refer [rgb-str]]
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

(def level' (mk-tile-map 10 10 :blank))

(defn traverse-map [f level]
  (reducer
    level
    (fn [memo x y v]
      (cons (f x y v) memo))
    ()))

(defn render-tile [x y v]
  {:x x :y y :tile v})

(defn render-map [level]
  (traverse-map render-tile level))

(defn test-it []
  (let [level (mk-tile-map 10 10 :blank)]
    (render-map level)))


(defn update-game [game dt]
  (let [t (+ dt  (-> game :count :count))]
    (-> game
        (assoc-in [:render-data :boxes]
                  [{:x (* 100  (cos-01 (* 5 t))) :y 10 :w 100 :h 100 :col [0 (cos-01 (* 0.5 t)) 0]}])
        (assoc-in [:render-data :sprs]
                  [ { :id :floor :pos (v2 100 100) } ]
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
                   (let [v (<! update-chan)]
                     (om/transact! app #(update-game % v)))
                   (recur)))

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
      (js/setInterval (fn [] (put! update-chan (/ 1 60))) 16)
      )
    )
  )



