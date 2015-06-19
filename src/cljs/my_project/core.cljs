;; {{{ Requires
(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ]
                   [gaz.rendermac :as rm])
  (:require
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

    [cloj.utils             :as utils]

    [cloj.web.utils         :refer [by-id log-js]]

    [game.html              :refer [mk-system]]
    [game.game              :as game]

    [gaz.tiles              :refer [mk-tile-map mix-it-up
                                    render-level]]

    [gaz.appstate           :refer [app-state]]
    [gaz.canvascomp         :refer [build-canvas-component ]]


    [cljs.core.async       :refer [put! >! chan <! alts! close!]]
    [ ff-om-draggable.core :refer [draggable-item]]

    [goog.crypt.base64 :as b64]

    [om.core :as om :include-macros true]
    [om.dom  :as dom :include-macros true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)

;;; }}}

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
;; {{{ FPS Component
(def time-chan (chan))

(defn fps-calc-chan
  ([in-chan window-size]
   (let [ret-chan (chan)]
     (go-loop [last-x () fr-num 0]
              (let [dt (<! in-chan)
                    new-last-x (take window-size (cons dt last-x))
                    data  {:avg-fps (/ 1000  (/ (reduce + 0 new-last-x) (count new-last-x)))
                           :min-fps (/ 1000  (apply min new-last-x))
                           :max-fps (/ 1000  (apply max new-last-x))
                           :frame fr-num
                           }
                    
                    ]
                (put! ret-chan data)
                (recur new-last-x (inc fr-num))))
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
        (let [elems (map
                      (fn [[txt id]] (dom/p nil (utils/format "%s: %0.2f" txt (id fps))))
                      [["avg" :avg-fps]
                       ["min" :min-fps]
                       ["max" :max-fps]])]
          (apply dom/div nil  elems))))))
;; }}}

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Timer
(defprotocol ITimer
  (tick! [_]))

(defn is-valid? [{:keys [previous now] :as c}]
  (not (or (nil? previous) (nil? now))))

(defn time-passed [{:keys [previous now] :as c}]
  (if (is-valid? c)
    (- now previous)
    0))

(defn html-timer []
  (let [c (atom {:previous nil :now nil}) ]
    (reify ITimer
      (tick! [_]
        (do
          (->>
            (assoc @c
                   :previous (:now @c)
                   :now (.now (aget js/window "performance")))
            (reset! c)
            (time-passed)))))))

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Animator with Timer
(def my-timer (html-timer))

(defn animate [callback-fn]
  (do
    (.requestAnimationFrame js/window #(animate callback-fn))
    (let [dt (tick! my-timer)]
      (when (not= dt 0)
        (callback-fn dt)))))

;;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Main

(defonce first-time? (atom true))
(when @first-time?
  (do
    (swap! first-time? not)
    (animate #(put! time-chan %))))

(defn main []
  (do
    (om/root
      frame-rate-component
      {:in-chan time-chan :class-name "pane" }
      {:target (by-id "test") })

    (let [rm (rmhtml/mk-resource-manager "resources")
          _ (clear-resources! rm)
          img-chan (load-img! rm "tiles")
          rend (create-render-target! rm "shit" 300 300) ]

      (go
        (let [img (<! img-chan)
              i (rman/img img)
              
              ]
          (println "received something")
          (println (rman/id img))
          (println (rman/width img))
          (println (rman/height img))
          )
        )

      )) 

  ; {{{

    ; (om/root
    ;   draggable-log-window
    ;   {:in-chan log-chan :class-name "pane" :position {:left 100 :top 20}}
    ;   {:target (by-id "app")})

  ; (let [rm (rmhtml/mk-resource-manager "resources")
  ;       _ (clear-resources! rm)
  ;       img-chan (load-img! rm "tiles")
  ;       rend (create-render-target! rm "shit" 300 300) ]
  ;   (go
  ;     (let [img (<! img-chan)]
  ;       (log-js (rman/height img))
  ;       (log-js (rman/width img)))))
  ; }}}
  ) 

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;ends
