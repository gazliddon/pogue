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

    [om.core                :as om :include-macros true]
    [om.dom                 :as dom :include-macros true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)

;;; }}}

;; {{{ Ignore for now
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
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
      #_(when fps 
        (let [elems (map
                     (fn [[txt id]] (dom/p nil (format "%s: %02f" txt (id fps))))
                     [["avg" :avg-fps]
                      ["min" :min-fps]
                      ["max" :max-fps]])])
        (apply dom/div nil  elems)))))
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Web Audio
(defprotocol IAudio
  (sq [_ ])
  (tri [_ ])
  (saw [_ ]))

(defprotocol ISFX
  (type! [_ t])
  (freq! [_ v])
  (start! [_])
  (stop! [_])
  (vol! [_ v]))

(defn mk-ins
  ([ctx osc-type]
   (let [ins (mk-ins ctx)]
     (do
       (type! ins osc-type))
     ins))

  ([ctx]
   (let [vco (.createOscillator ctx)
         vca (.createGain ctx)
         ret (reify
               ISFX
               (start! [_] (.start vco))
               (stop!  [_] (.stop vco))
               (type!  [_ osc-type] (set! (.-type vco) osc-type))
               (vol!   [_ volume] (set! (.-value (.-gain vca)) volume))
               (freq!  [_ freq] (set! (.-value  (.-frequency vco)) freq))) ]
     (do
       (.connect vco vca)
       (.connect vca (.-destination ctx)))
     ret))
  )

(def audio-html 
  (let [constructor (or js/window.AudioContext
                        js/window.webkitAudioContext)
        ctx (constructor.) ]
    (reify
      IAudio
      (sq [_]
        (mk-ins ctx "square")))))
;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Main

(defonce first-time? (atom true))

(when @first-time?
  (do
    (swap! first-time? not)
    (animate #(put! time-chan %))))

(defn game-component [data owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (om/set-state! owner :dt 0)
      (let [in-chan (:in-chan data)]
        (go-loop []
          (let [dt (<! in-chan)]
            (om/set-state! owner :dt dt))
          (recur)
          )))

    om/IRenderState
    (render-state [_ state]
      (dom/p nil (str (:dt state))))))

(defn chan-component [data owner]
  (let [{:keys [in-chan state-key render]} data]
    (reify
      om/IWillMount
      (will-mount [this]
        (let [in-chan (:in-chan data)]
          (go-loop []
                   (let [dt (<! in-chan)]
                     (om/set-state! owner state-key dt))
                   (recur))))

      om/IRenderState
      (render-state [_ state]
        (if-let [item (state-key state)]
          (render item))))))

(defn game-component-2 [data owner])

(def log-chan (chan))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn main []
  (do
    #_(om/root
      frame-rate-component
      {:in-chan time-chan }
      {:target (by-id "test") })

    (om/root
      game-component
      {:in-chan time-chan}
      {:target (by-id "test") })

    (let [rm (rmhtml/mk-resource-manager "resources")
          _ (clear-resources! rm)
          img-chan (load-img! rm "tiles")
          rend (create-render-target! rm "shit" 300 300) ]
      (go
        (let [img (<! img-chan)]
          (log-js (rman/width img))
          (log-js (rman/height img))))
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
