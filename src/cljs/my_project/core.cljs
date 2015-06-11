(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ]
                   [gaz.rendermac :as rm]
                   )
  (:require
    [dommy.core :as dommy :refer-macros [sel sel1]]
    [cljs.pprint :as pp]
    [gaz.gameprotocols :as game]
    [gaz.renderprotocols :as rp]
    [gaz.tiles :refer [mk-tile-map]]
    [gaz.tilemaputils :as tmu ]
    [gaz.tilemapprotocol :as tmp ]
    [gaz.math :refer [cos-01]]
    [gaz.vec2 :as v2 ]
    [gaz.color :as col]
    [gaz.appstate :refer [app-state]]
    [gaz.animcomp :refer [animation-view]]
    [gaz.canvascomp :refer [build-canvas-component ]]
    [gaz.canvascomprenderbackend :refer [canvas-immediate-renderer]]
    [om.core :as om :include-macros true]
    [cljs.core.async :refer [put! >! chan <! alts! close!]]
    [om.dom :as dom :include-macros true]))

(enable-console-print!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn resource-img [{:keys [id]} owner]
  (let [src (str "/data/" id ".png")]
    (reify
      om/IRenderState
      (render-state [_ state]
        (dom/img #js {:id id :src src})))))

(defn make-canvas-component[{:keys [id dims]} owner]
  (let [{w :x h :y} dims]
    (reify
      om/IDidMount
      (did-mount [ this ]
        )
      om/IRenderState
      (render-state [this state]
        (dom/canvas #js {:id id :width w :height h})))))

(defprotocol IResourceManager
  (create-render-target! [_ id w h])
  (load-img! [_ source]))


(defrecord OmResManager [om-res res-atom]
  IResourceManager
  (create-render-target! [this id w h]
    (swap! res-atom
           update :targets #(cons {:id id :dims {:x w :y h}} % )))

  (load-img! [this id]
    (swap! res-atom update :imgs #(cons {:id id} % ))))

(defprotocol IResourceManagerInfo
  (find-img [_ id])
  (find-render-target [_ id])
  (list-render-targets [_])
  (list-imgs [_]))


(defn id-ize [v] (str "#" v))

(defprotocol IImage
  (width [_])
  (height [_]))

(defprotocol IHTMLImage
  (img [_]))

(defn mk-html-resource-manager []
  (let [store (atom {:imgs [] :targets []})]
    (reify

      IResourceManagerInfo
      (find-img [_ id]
        (println "not implemented"))

      (find-render-target [_ id]
        (println "not implemented"))

      (list-render-targets [_]
        (println "not implemented"))

      (list-imgs [_]
        (println "not implemented"))

      IResourceManager

      (create-render-target! [this id w h]
        (let [canvas (sel1 (id-ize id))]
          (canvas-immediate-renderer canvas  {:x w :y h})))

      (load-img! [this id]
        (let [img (sel1 (id-ize id))
              w (aget img "width")
              h (aget img "height") ]
          (reify
            IImage
            (width [_] 255)
            (height [_] 255)

            IHTMLImage
            (img [_] img))))
      )))

(def resman (mk-html-resource-manager))
(def rt-gaz (create-render-target! resman "shit-canvas" 100 100))
(def im-gaz (load-img! resman "shit-tiles"))



(defn mk-om-res [div-name res-atom]
  (let [elem (. js/document (getElementById div-name))]
    (om/root
      (fn [{:keys [imgs targets]} owner]
        (reify

          om/IRender
          (render [_]
            (dom/div
              nil 
              (apply dom/div nil (om/build-all resource-img imgs))
              (apply dom/div nil (om/build-all make-canvas-component targets))))))
      res-atom {:target elem})))

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
(defonce update-chan (chan))

(defn traverse-map [f level]
  (tmu/reducer
    level
    (fn [memo x y v]
      (cons (f level (v2/v2 x y) v) memo))
    ()))

(def duff-tile {:col col/purple})

(def tiles
  {:blank  {:col [0.15 0.15 0.15]}
   :ground {:col [0 1 0]}
   :water  {:col [0 0 0.75]}
   :wall   {:col [0.25 0.25 0]} })

(defn render-tile [level pos v]
  (let [tile  (get tiles v duff-tile) ]
    [:box pos {:x 1 :y 1} (:col tile) ]))

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


(def level-dims (v2/v2 10 10))

(def level
  (->
    (mk-tile-map (:x level-dims) (:y level-dims) :blank)
    (mix-it-up)))

(defn make-game-render-data [rd t rendered-level]
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
           rendered-level)
         )
  )

(defn make-level-render-data [rd t]
  (assoc rd 
         :xforms (list
                   [:identity]
                   [:clear 0 0 1]
                   [:box {:x 0 :y 0} {:x 20 :y 20} [0 0 0]] )))

(def rendered-level (vec  (render-level level)))

(defn update-game [{:keys [tick main-render-data level-render-data] :as game} dt]
  (let [new-tick (+ dt  tick) ]
    (do
      (rp/clear! rt-gaz [1 0 1])
      (rp/spr! rt-gaz [{:x (* 100  ( cos-01 new-tick )) :y 20} (img im-gaz)])

      (assoc game
             :level-render-data (make-level-render-data level-render-data new-tick )
             :main-render-data (make-game-render-data level-render-data new-tick rendered-level)
             :tick new-tick))

    ))

(def main-render-canvas  (build-canvas-component "main-render-canvas"))
(def level-render-canvas (build-canvas-component "level-render-canvas"))

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


