;; {{{ Requires
(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ]
                   [gaz.rendermac :as rm])
  (:require

    ; [octet.core :as buf]

    [clojure.string         :refer [split join]]

    [cljs-http.client       :as http]


    [cloj.resources.manager :as rman
                            :refer [create-render-target!
                                    load-img!
                                    clear-resources!]]

    [cloj.resources.html    :as rmhtml]

    [cloj.math.misc         :refer [cos-01 log-base-n ceil floor num-digits]]
    [cloj.math.vec2         :as v2 :refer [v2]]

    [cloj.system            :refer [get-resource-manager
                                    get-render-engine]]

    [cloj.web.utils         :refer [by-id log-js]]

    [game.html              :refer [mk-system]]
    [game.game              :as game]

    [gaz.tiles              :refer [mk-tile-map mix-it-up
                                    render-level]]

    [gaz.appstate           :refer [app-state]]
    [gaz.canvascomp         :refer [build-canvas-component ]]


    [cljs.core.async        :refer [put! >! chan <! alts! close!]]
    [ff-om-draggable.core   :refer [draggable-item]]

    [goog.crypt.base64      :as b64]

    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)

;;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Log Window
(def log-chan (chan))
(def log-state (atom {}) )

(defn log-window 
  [{:keys [in-chan class-name] :as data} owner ]
  (let [in-chan    (or in-chan (chan)) 
        class-name (or class-name "pane")]
    (reify
      om/IWillMount
      (will-mount [ this ]
        (go
          (om/set-state! owner :text "")
          (loop []
            (let [txt (<! in-chan)
                  txt-req {:text txt}]
              (om/update-state! owner [:text] #(str % "\n" txt ))
              (recur)))))

      om/IRenderState
      (render-state [_ {:keys [text]}]
        (dom/div
          #js { :className class-name }
          (dom/span nil "Logs")
          (dom/textarea #js {:width "100%" :value text}))))))

(def draggable-log-window (draggable-item log-window [:position]))

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ FPS Component
(defn fps-calc-chan
  ([in-chan window-size]
   (let [ret-chan (chan)]
     (go-loop [last-x ()]
              (let [dt (<! in-chan)
                    new-last-x (take window-size (cons dt last-x))
                    data  {:avg-fps (/ (reduce + 0 new-last-x) (count new-last-x))
                           :low-fps (apply min new-last-x)
                           :max-fps (apply max new-last-x) } ]
                (put! ret-chan data)
                (recur (new-last-x))))
     ret-chan))

  ([in-chan]
   (fps-calc-chan in-chan 10)))

(defn frame-rate-component
  [{:keys [in-chan class-name] :as data} owner ]
  (reify
    om/IWillMount
    (will-mount [ this ]
      (let [fps-chan (fps-calc-chan in-chan) ]
        (go-loop []
                 (om/set-state! owner :fps (<! fps-chan))
                 (recur))))

    om/IRenderState 
    (render-state [_ {:keys [fps]}]
      (when fps 
        (dom/div nil
                 (dom/p nil ( str "avg: " (:avg-fps fps) ))
                 (dom/p nil ( str "min: " (:min-fps fps) ))
                 (dom/p nil ( str "max: " (:max-fps fps) ))
                 )))))
;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Main
(defn main []
  (do
    (om/root
      frame-rate-component
      {:in-chan log-chan :class-name "pane" :position {:left 100 :top 20}}
      {:target (by-id "test") }
      )
    (om/root
      draggable-log-window
      {:in-chan log-chan :class-name "pane" :position {:left 100 :top 20}}
      {:target (by-id "app")})

    (let [rm (rmhtml/mk-resource-manager "resources")
          _ (clear-resources! rm)
          img-chan (load-img! rm "tiles")
          rend (create-render-target! rm "shit" 300 300) ]
      (go
        (let [img (<! img-chan)]
          (log-js (rman/height img))
          (log-js (rman/width img)))))))
;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Game obect
(def pogue-game
  (game/make-game
    (reify 
      game/IGameInit
      (game-init [this]
        this)

      game/IGameUpdate
      (game-update [this dt]
        this)

      game/IGameClose
      (game-close [this]
        this))))
;; }}}
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;ends
