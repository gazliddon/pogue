(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ]
                   [gaz.rendermac :as rm]
                   )
  (:require
    [cljs.pprint :as pp]
    [gaz.gameprotocols :as game]
    [gaz.tiles :refer [mk-tile-map]]
    [gaz.tilemaputils :as tmu ]
    [gaz.tilemapprotocol :as tmp ]
    [gaz.math :refer [cos-01]]
    [gaz.vec2 :as v2 ]
    [gaz.color :as col]
    [gaz.appstate :refer [app-state]]
    [gaz.animcomp :refer [animation-view]]
    [gaz.canvascomp :refer [build-canvas-component ]]
    [om.core :as om :include-macros true]
    [cljs.core.async :refer [put! >! chan <! alts! close!]]
    [om.dom :as dom :include-macros true]))

(enable-console-print!)
(print "got here")

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
   :water  {:col [0 0 0.75]}
   :wall   {:col [0.25 0.25 0]} })
(print "got here 2")

(defn render-tile [level pos v]
  (let [tile  (get tiles v duff-tile) ]
    {:pos pos :dims {:x 1 :y 1} :col (:col tile)}))

(defn render-tile' [level pos v]
  (let [tile  (get tiles v duff-tile) ]
    [:box pos {:x 1 :y 1} (:col tile) ]))

(defn render-level [level]
  (traverse-map render-tile level))

(defn render-level' [level]
  (traverse-map render-tile' level))

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
(def rendered-level' (vec  (render-level' level)))
(def level-start [[:identity]
                  [:clear [1 0 1]]])
(print "got here 3")

(defn make-game-render-data [rd t]
  (assoc rd 
         :xforms
         (concat
           (list
             [:identity]
             [:clear (cos-01 (* t 1)) 0 (cos-01 (* t 20))]
             [:scale {:x 10 :y 10}]
             [:translate (v2/mul (v2/v2 (cos-01 (* t 3)) (cos-01 t)) (v2/v2 20 20))]
             )
           (list
                   [:box {:x 0 :y 0} {:x 20 :y 20} [0 0 0]]  
             )
           rendered-level')
         )
  )

(defn make-level-render-data [rd t]
  (assoc rd 
         :xforms (list
                   [:identity]
                   [:clear 0 0 1]
                   [:box {:x 0 :y 0} {:x 20 :y 20} [0 0 0]] )
         ))


(defn update-game [{:keys [tick main-render-data level-render-data] :as game} dt]
  (let [new-tick (+ dt  tick) ]
    (assoc game
           :level-render-data (make-level-render-data level-render-data new-tick )
           :main-render-data (make-game-render-data level-render-data new-tick)
           :tick new-tick)))


(def main-render-canvas  (build-canvas-component "main-render-canvas"))
(def level-render-canvas (build-canvas-component "level-render-canvas"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn resource-img [{:keys [id] :as state-2} owner]
  (let [src (str "/data/" id ".png")]
    (reify
      om/IRenderState
      (render-state [_ state]
        (println state)
        (println state-2)
        (dom/img #js {:id id :src src})))))

(defn make-canvas-component[{:keys [id dims]} owner]
  (let [{w :x h :y} dims]
    (reify
      om/IDidMount
      (did-mount [ this ]
        )
      om/IRenderState
      (render-state [this state]
        (println "here!")
        (println state)
        (dom/canvas #js {:id id :width w :height h})))))

(defn canvas-component-2 [{id :id w :x h :y} blob]
  (reify
      om/IDidMount
      (did-mount [_ ]
        )
      om/IRenderState
      (render-state [_ blob]
        (println blob)
        (dom/canvas #js {:id id :width w :height h}))))


(defprotocol IResourceManager
  (create-render-target! [_ id w h])
  (load-img! [_ source]))


(defrecord OmResManager [om-res res-atom]

  IResourceManager

  (create-render-target! [this id w h]
    (swap! res-atom
           update :targets #(cons {:id id :dims {:x w :y h}} % ))
    )

  (load-img! [this id]
    (swap! res-atom
           update :imgs #(cons {:id id} % ))
    ))

(defn mk-om-res [div-name res-atom]
  (let [elem (. js/document (getElementById div-name))]
    (om/root
      (fn [{:keys [imgs targets]} owner]
        (reify
          om/IRender
          (render [_]
            (dom/div nil 
                     (apply dom/div nil (om/build-all resource-img imgs {:init-state {:a 1}} ))
                     (apply dom/div nil (om/build-all make-canvas-component targets))
                     ))))
      res-atom
      {:target elem})))


(def resources
  (atom
    {:imgs    []
     :targets []
     }))

(def om-res
  (OmResManager. (mk-om-res "resources" resources ) resources))

(do
  (def tiles (load-img! om-res "tiles"))
  (print "got here 4-3")
  (def rt (create-render-target! om-res "rt0" 100 100))) 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn make-pogue-game [renderer resource-manager]
  (game/make-game
    (reify
      game/IGameInit
      (game-init [_]
        (println "initialised game!"))

      game/IGameUpdate
      (game-update [this dt]
        (println "updated game!")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce first-time? (atom true))

(defn main []
  (om/root
    (fn [game-state owner]
      (reify
        om/IWillMount
        (will-mount [_ ]
          (go-loop []
                   (let [dt (<! update-chan) ]
                     (om/transact! game-state #(update-game % dt))
                     (recur))))

        om/IRender
        (render [_]
          (dom/div #js {:id "wrapper"}
                   (dom/div nil (dom/h1 nil (-> game-state :main-app :name)))
                   (dom/p nil (-> game-state :tick))
                   (om/build level-render-canvas (:level-render-data game-state) )
                   (om/build main-render-canvas (:main-render-data game-state) )
                   ))))
    app-state
    {:target (. js/document (getElementById "app"))})

  (when @first-time?
    (do
      (swap! first-time? not)
      (js/setInterval (fn [] (put! update-chan (/ 1 60))) 16))))


