;; {{{ Requires
(ns my-project.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop alt!] ]
                   [gaz.rendermac :as rm])
  (:require

    [clojure.string         :refer [split join]]


    [cloj.resources.manager :as rman
                            :refer [create-render-target!
                                    load-img!
                                    clear-resources!  ]]

    [cloj.resources.html    :as rmhtml]

    [cloj.math.misc         :refer [cos-01 log-base-n ceil floor num-digits]]
    [cloj.math.vec2         :as v2 :refer [vec2]]
    [cloj.math.vec3         :as v3 :refer [vec3]]

    [cloj.system            :refer [get-resource-manager
                                    get-render-engine]]

    [cloj.utils             :refer [format]] 
    (cloj.render.protocols  :as rp)

    [cloj.web.utils         :refer [by-id log-js]]

    [game.html              :refer [mk-system]]
    [game.game              :as game]
    [game.sprs              :as sprs]
    [game.sprdata           :as sprdata]

    [gaz.tiles              :refer [mk-tile-map mix-it-up
                                    render-level]]

    [gaz.appstate           :refer [app-state]]


    [cljs.core.async        :as async
                            :refer [put! >! chan <! alts! close! dropping-buffer mult tap]]

    [ff-om-draggable.core   :refer [draggable-item]]

    [hipo.core              :as hipo  :include-macros true]  
    [dommy.core             :as dommy :include-macros true]    

    [om.core                :as om :include-macros true]
    [om.dom                 :as dom :include-macros true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ FPS Component

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
                     (fn [[txt id]] (dom/p nil (format "%s: %02f" txt (id fps))))
                     [["avg" :avg-fps]
                      ["min" :min-fps]
                      ["max" :max-fps]])]
        (apply dom/div nil  elems))))))
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
  (instrument [_])
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
               (instrument [_] {:vco vco :vca vca})
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

(defonce audio-html 
  (let [constructor (or js/window.AudioContext
                        js/window.webkitAudioContext)
        ctx (constructor.) ]
    (reify
      IAudio
      (sq [_]
        (mk-ins ctx "square")))))

(defonce sq-1 (sq audio-html))
(defonce sq-2 (sq audio-html))

#_(do
  (freq! sq-1 30)
  (freq! sq-2 30.13721)
  (vol! sq-1 5)
  (vol! sq-2 5)
  (start! sq-1 )
  (start! sq-2 )
  
  
  
  )

;; }}}

;; =============================================================================
;; Game System Stuff {{{

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
      (dom/p nil (str "ROGUEBOW ISLANDS : "(:dt state))))))

;; }}}

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; {{{ Game obect

(def system (mk-system "game" "game-canvas"))

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

(defn prn-spr [rend spr t]
  (let [p-x (* 230  (cos-01 (/ t 100) ))
        p-y (* 90  (cos-01 (/ t 199) ))
        s-x (* 100 (cos-01 (/ t 500)))
        s-y (* 100 (cos-01 (/ t 500)))]
    (doto rend
      (rp/spr! spr (vec2 p-x p-y)))))
;; }}}

;; =============================================================================
;; {{{ Main

(def log-chan (chan))
(defonce time-chan (chan (dropping-buffer 10)))
(defonce time-chan-mult (mult time-chan))
(defonce g-time (atom 0))

(defonce first-time? (atom true))

(defn funny-col [tm]
  (->>
    (v3/div
      (vec3 tm tm tm)  
      (vec3 1000 500 100))
    (v3/to-vec)
    (mapv cos-01)))

(when @first-time?
  (do
    (swap! first-time? not)
    (->
      (fn [dt]
        (swap! g-time #(+ dt %))
        (do
          ; (rp/clear! rend (funny-col @g-time))
          (put! time-chan dt)))
      (animate))))

(defn rand-spr []
  )

(defn main []
  (let [rm (get-resource-manager system)
        rend (get-render-engine system)
        spr-ch (sprs/load-sprs rm sprdata/spr-data)
        ]

    (do
      (om/root
        frame-rate-component
        {:in-chan (tap time-chan-mult (chan)) }
        {:target (by-id "app") })

      (om/root
        game-component
        {:in-chan (tap time-chan-mult (chan) )}
        {:target (by-id "app") })

      (clear-resources! rm)

      (go

        (let [sprs (<! spr-ch)
              spr-printer (sprs/mk-spr-printer rend sprs)
              in-chan (tap time-chan-mult (chan))
              rand-spr (fn [] [(rand-nth (keys sprs)) (vec2 (rand-int 200) (rand-int 200) )] )
              positions (vec (repeatedly 400 rand-spr)) ]
          (loop []
            (let [dt (<! in-chan)
                  t @g-time
                  t-secs (/ t 1000)
                  c-t (* t 1.5) ]

              (doto rend
                (rp/clear! [1 0 1])
                (rp/identity! )
                (rp/scale! (vec2 4.5 4.5 )) 
                (rp/translate! (vec2 0 (* 10  (cos-01 t-secs)) )))

              (doseq [[img {:keys [x y] :as pos}] positions]
                (let [add-pos (vec2
                                (* 20  (cos-01 (* (+ y t-secs) 5)))
                                0)]
                  (rp/spr! spr-printer img (v2/add add-pos pos)))))
            (recur))
          ))
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
