;; =============================================================================
; {{{ Requires
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
    [cloj.math.vec2         :as v2 :refer [vec2 vec2-s]]
    [cloj.math.vec3         :as v3 :refer [vec3]]

    [cloj.system            :refer [get-resource-manager
                                    get-render-engine]]

    [cloj.utils             :refer [format]] 
    (cloj.render.protocols  :as rp)
    (cloj.keyboard          :as kb)

    [cloj.web.utils         :refer [by-id log-js]]

    [game.html              :refer [mk-system]]
    [game.game              :as game]
    [game.sprs              :as sprs]
    [game.sprdata           :as sprdata]
    [game.tiledata          :as tiledata]

    [gaz.tiles              :as tiles]

    [gaz.tilemapprotocol    :as tmp]

    [cljs.core.async        :as async
                            :refer [put! >! chan <! close! dropping-buffer mult tap]]

    [ff-om-draggable.core   :refer [draggable-item]]

    [hipo.core              :as hipo  :include-macros true]  
    [dommy.core             :as dommy :include-macros true]    

    [sablono.core :as html :refer-macros [html]]

    [om.core                :as om :include-macros true]
    [om.dom                 :as dom :include-macros true]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(enable-console-print!)

;; }}}

;; =============================================================================
;; {{{ MATHS! :)
(defprotocol IEasing
  (get-v [_ t'])
  (get-p [_ t']))

(defrecord Easer [t p v a]
  IEasing
  (get-v [_ t'] (+ v (* a (- t' t))))
  (get-p [_ t'] (+ p (* t (+ v (* 0.5 a (- t' t)))))))

(def halfv2 (vec2-s 0.5))

(defrecord EaserV2 [t p v a]
  IEasing
  (get-v [_ t'] (v2/add v (v2/mul a (vec2-s (- t' t)) )))
  (get-p [_ t'] (v2/add p (v2/mul t (v2/add v (v2/mul halfv2 a (vec2-s  (- t' t))))))))

(defn mk-easer
  "
  t  is curent time
  t' is when we want to stop moving
  v  current velocity
  p  current positon
  p' target postion"

  [t t' p p' v]

  (let [clamp-t #(max t (min t' %))
        dt (- t' t)
        a (if (== 0 dt)
            (v2/zero)
            (v2/div (v2/neg v) (vec2-s dt)))
        e (->EaserV2 t p v a )]

    (reify
      IEasing
      (get-v [_ this-t]
        (get-v e (clamp-t this-t)))

      (get-p [_ this-t] 
        (get-p e (clamp-t this-t))))))

(defn mk-no-move-easer [t p]
  (->Easer t p 0 0))

;; }}}

;; =============================================================================
;; {{{ tiles
(defn shit-line [tmap block {:keys [x y] :as pos} add len]
  (let [v2-len (vec2 len len) ]
    (loop [tmap (tmp/set-tile tmap x y block)
           i    (dec len)]
      (if (pos? i)
        (let [{x :x y :y} (->
                            (v2/mul add (vec2 i i))
                            (v2/add pos)) ]
          (recur (tmp/set-tile tmap x y block)
                 (dec i)))
        tmap))))

(defn shit-h-line [tmap bl p len] (shit-line tmap bl p v2/right len))
(defn shit-v-line [tmap bl p len] (shit-line tmap bl p v2/down len))

(defn shit-box [tmap bl {:keys [x y] :as pos}  {w :x h :y}]
  (->
    (fn [res y]
      (shit-h-line res bl (v2/add pos (vec2 0 y)) w))
    (reduce tmap (range h))))

(defn shit-room [tmap {:keys [x y] :as pos} {w :x h :y}]
  (let [fl :floor
        wl :wall
        {x1 :x y1 :y} (v2/add pos (vec2 (dec w) (dec h)))
        ]
    (-> tmap
        (shit-box fl pos (vec2 w h))
        (shit-h-line wl pos w)
        (shit-h-line wl (vec2 x y1) w)
        (shit-v-line wl (vec2 x  y) h)
        (shit-v-line wl (vec2 y1 x) h))))

(def tile-offsets
  (let [mul 16
        mul-vec (vec2 mul mul) ]
    (->>
      (for [x [0 1] y [0 1]] (vec2 x y))
      (mapv #(v2/mul mul-vec %))
      (into []))))

(defn mk-tile-printer [rend]
  (reify
    rp/IRenderBackend
    (spr! [this {gfx :gfx} pos]
      (doseq [ [tile offset ] (map vector gfx (map #(v2/add pos %) tile-offsets))]
        (rp/spr! rend tile offset)))))

(defn render-level! [render-target level sprs]
  (let [spr-printer (sprs/mk-spr-printer render-target sprs)
        tile-printer (mk-tile-printer spr-printer)
        [w h] [(tmp/get-width level) (tmp/get-height level)]
        spr! (partial rp/spr! tile-printer)
        to-print (for [x (range w) y (range h)]
                   {:pos  (vec2 x y)
                    :pixel-pos (v2/mul (vec2 32 32) (vec2 x y))
                    :tile (tmp/get-tile level x y)}) ]

    (doseq [{pos :pixel-pos tile :tile} to-print ]
      (rp/spr! tile-printer tile pos)))
  )

(defn mk-level-spr [sprs rman id w-b h-b all-tile-data]
  (let [[w h] [(* 16 w-b) (* 16 h-b)]
        render-target (create-render-target! rman (name id ) w h)
        level (->
                (tiles/mk-tile-map w-b h-b :blank all-tile-data)
                (shit-room (vec2 3 3) (vec2 10 10 ))) ]
    (do
      (rp/clear! render-target [0 1 0])
      (render-level! render-target level sprs)
      render-target)))

;; }}}

;; =============================================================================
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

;; =============================================================================
;; {{{ Timer
(defprotocol ITimer
  (now [_])
  (from-seconds [_ s])
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

      (from-seconds [this s] (+ (* 1000 s) ))

      (now [_] (.now (aget js/window "performance")))

      (tick! [this]
        (do
          (->>
            (assoc @c
                   :previous (:now @c)
                   :now (now this))
            (reset! c)
            (time-passed))
          )))))

;; }}}

;; =============================================================================
;; {{{ Animator with Timer
(def my-timer (html-timer))

(defn animate [callback-fn]
  (do
    (.requestAnimationFrame js/window #(animate callback-fn))
    (let [dt (tick! my-timer)]
      (when (not= dt 0)
        (callback-fn dt)))))

;;; }}}

;; =============================================================================
;; Game Messaging system {{{

(defn increase-by-perc [perc v] (+ v (* perc v)))
(defn descrease-by-perc [perc v] (- v (* perc v)))

(defmulti handle-message! (fn [command packet] command))

(defmethod handle-message! :default [command packet]
  (println (str "got command " command " with packet " packet)))


(defn message-center []
  (let [ret-chan (chan) ]
    (go-loop []
      (let [[command packet] (<! ret-chan)]
        (handle-message! command packet)
        (recur)))
    ret-chan
    ))

(def game-message-chan (message-center))
(defn msg! [k command] (put! game-message-chan [ k command ]))

;; }}}
 
;; =============================================================================
;; {{{ Some stuff for the game view
(defn slider [view owner {:keys [e-range e-key e-label e-func e-steps]}]
  (let [[min max] e-range
        e-label (or e-label "NO LABEL!")
        e-steps (or e-steps 30)
        ]
    (reify
      om/IInitState
      (init-state [_]
        {:value (e-key @view)
         :ch    (chan) })

      om/IWillMount
      (will-mount [_]
        (let [update-chan (om/get-state owner :ch)]
          (go (loop []
                (let [v (<! update-chan)]
                  (e-func v)
                  (om/set-state! owner :value v)
                  (recur))))))

      om/IRenderState
      (render-state [_ {:keys [value ch]}]
        (html
          [:div 
           {:class "slider"}
           [:label  e-label ]
           [:input {:type "range" 
                    :style {:width "100px"}
                    :value value
                    :min min :max max
                    :step (/ (- max min) e-steps )
                    :on-change #(put! ch (js/parseFloat (.. % -target -value)))}]
           [:span value]])))))

(defn bool [view owner {:keys [e-key e-label e-func]}]
  (let [ e-label (or e-label "NO LABEL!")]
    (reify
      om/IInitState
      (init-state [_]
        {:value (e-key @view)
         :ch    (chan) })

      om/IWillMount
      (will-mount [_]
        (let [update-chan (om/get-state owner :ch)]
          (go (loop []
                (let [v (<! update-chan)]
                  (e-func v)
                  (om/set-state! owner :value v)
                  (recur))))))

      om/IRenderState
      (render-state [_ {:keys [value ch]}]
        (html
          [:div 
           {:class "bool"}
           [:label  e-label ]
           [:input {:type "checkbox" 
                    :value value
                    :on-change #(put! ch (.js/Number  (.. % -target -value)))}]
           ])))))

;; }}}

;; =============================================================================
;; Game System Stuff {{{
(defonce game-view (atom { :scale 2
                           :time-speed 1 }))

(defn scale-swap! [f] (swap! game-view assoc :scale (f (:scale @game-view))))

(defmethod handle-message! :zoom-in   [_ perc] (scale-swap! #(increase-by-perc % perc)))
(defmethod handle-message! :zoom-out  [_ perc] (scale-swap! #(descrease-by-perc % perc)) )
(defmethod handle-message! :set-scale [_ scale]  (scale-swap! (constantly scale)))
(defmethod handle-message! :set-time-speed [_ time-speed]  (swap! game-view assoc :time-speed time-speed ))

(defn om-slider [ label data kork range steps func]
  (om/build slider data {:opts {:e-range range
                                :e-key kork
                                :e-label label 
                                :e-func func
                                :e-steps steps
                                }} ))


(defn game-component [data owner]
  (reify
    om/IWillMount
    (will-mount [this]
      (om/set-state! owner :dt 0)
      (let [in-chan (:in-chan data)]
        (go-loop []
                 (let [dt (<! in-chan)]
                   (om/set-state! owner :dt dt))
                 (recur))))

    om/IRenderState
    (render-state [_ {dt :dt}]
      (let [msecs (format "%0.2f" dt)
            fps   (format "%0.2f" (* (/ 3600 1000)  dt )) ]


        (html
          [:div
                (om-slider "scale" game-view :scale [1 10] 1000 #(msg! :set-scale %))
                (om-slider "time" game-view :time-speed [0 5] 100 #(msg! :set-time-speed %))

           [:p (str "ROGUEBOW ISLANDS : " msecs " " fps)] ])))))

;; }}}

;; =============================================================================
;; HTML Keyboard handling {{{
(defn handle-key-event! [kb-handler event new-state]
  (do
    (kb/update-key! kb-handler (.-keyCode event) new-state)
    (.stopPropagation event)))

;; Lets read some keys
(defn kb-attach! [dom-id kb-handler]
  (let [dom-el js/document]
    (do
      (dommy/listen!  dom-el :onblur #(kb/init! kb-handler))
      (dommy/listen!  dom-el :keyup #(handle-key-event! kb-handler % false))
      (dommy/listen!  dom-el :keydown #(handle-key-event! kb-handler % true)))))

(defn kb-update! [kb-handler]
  (if (.hasFocus js/document)
    (kb/update! kb-handler)
    (kb/init! kb-handler)))

;; Game keyboard stuff
(def key-table
  {\A :left  
   \D :right 
   \W :up    
   \S :down  })

(defn decide [kb-handler key-table]
  (->>
    (kb/get-key-states kb-handler)
    (filter (fn [[k {state :state}]]
              (and state (get key-table k)) ))

    (map (fn [[k _]] (get key-table k :none)))
    (into #{})))

;; JS dependent
(defn ->js-key-table 
  "convert game key table to cooky JS keytable"
  [key-table]
  (->
    (fn [r [k v] ] (assoc r (.charCodeAt k 0) v))
    (reduce {} key-table)  ))

(def js-key-table (->js-key-table key-table))

(defn my-decide [kb-handler]
  (decide kb-handler js-key-table))

(def combo-vec
  {#{:left :up}    v2/left-up
   #{:left :down}  v2/left-down
   #{:right :up}   v2/right-up
   #{:right :down} v2/right-down
   #{:up}          v2/up
   #{:down}        v2/down
   #{:left}        v2/left
   #{:right}       v2/right})
;; }}}

;; =============================================================================
;; {{{ Let's do some player state stuff

(defmulti player-update (fn [{state :state}] state))

(defmethod player-update :standing [player] player)
(defmethod player-update :walking  [player] player)

;; }}}

;; =============================================================================
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
;; {{{ Shit Camera
(defn camera [current-pos desired-pos]
  (let [scaler (vec2 12 12)
        diff (v2/sub desired-pos current-pos)
        add (v2/div diff scaler)
        new-pos (v2/add current-pos add) ]
    new-pos
    ))

(defn camera [current-pos desired-pos]
  (->
    (v2/sub desired-pos current-pos)
    (v2/div (vec2 12 12))
    (v2/add current-pos)))

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

(defn anim-func! [dt]
  (do
    (let [dt (* dt (:time-speed @game-view))]
      (swap! g-time #(+ dt %))
      (put! time-chan dt))))

(when @first-time?
  (do
    (swap! first-time? not)
    (animate anim-func!)))

(defn dump->> [s v]
  (println (str s " : " v))
  v)

(defn funny-vec [t-secs uniq]
  (->>
    uniq
    (v2/mul (vec2 10 10))
    (v2/mul (vec2  t-secs t-secs))
    (v2/add (vec2 0 0.5))
    (v2/applyv (vec2 cos-01 cos-01))
    (v2/mul (vec2 20 20))
    ))

(defn anim [t s frms]
  (nth frms 
       (mod (int (/ t s)) (count frms))) )

(defn mk-anim-fn [ speed frames ]
  (fn [t]
    (anim t speed frames)))

(def get-bub-frm
  (mk-anim-fn 0.1 [:bub0 :bub1 :bub2 :bub3] ))


#_({:start-velocity 10
    :target-velocity 0
    :start-time 10
    :target-time 100
    })


(defrecord Player [intention]
  (get-pos [ _ t]
    )
  )

(defn mk-player [t p]
  (->Player (->EaserV2 t p v2/zero v2/zero))
  )


(defn main []
  (let [rm (get-resource-manager system)
        rend (get-render-engine system)
        spr-ch (sprs/load-sprs rm sprdata/spr-data) ]

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
              rand-spr (fn [] [(rand-nth (keys sprs)) (vec2 (rand-int 400) (rand-int 400) ) (vec2 (rand) (rand))] )
              positions (vec (repeatedly 100 rand-spr))
              level-spr  (mk-level-spr sprs rm :level 16 16 tiledata/tile-data)
              kb-handler (kb/default-kb-handler)
              mid-scr (-> (vec2 (rp/width rend) (rp/height rend) ) (v2/mul (vec2 0.5 0.5))) ]

          (kb-attach! "game" kb-handler)

          (println "got here")

          (loop [pos (vec2 20 20)
                 cam-pos (vec2 0 0)
                 ]
            (kb-update! kb-handler)

            (let [dt (<! in-chan)
                  t @g-time 
                  t-secs (/ t 700)
                  c-t (* t 1.5)
                  scale (vec2 (:scale @game-view) (:scale @game-view))
                  mid-scr (v2/div mid-scr scale)
                  desired-pos (v2/sub pos mid-scr)
                  desired-pos (v2/clamp (vec2 0 0) (vec2 1000 1000) desired-pos) ]

              (doto rend
                (rp/clear! [1 0 1])
                (rp/identity! )
                (rp/scale! scale) 
                (rp/translate! (v2/sub (vec2 0 0) cam-pos))
                (rp/spr! level-spr (vec2 0 0)))

              (doseq [[img pos uniq] positions]
                (let [final-pos (v2/add pos (funny-vec t-secs uniq))]
                  (rp/spr! spr-printer img final-pos)))

              (rp/spr! spr-printer (get-bub-frm t-secs) pos)


              (let [actions (my-decide kb-handler)
                    mv (get combo-vec actions (vec2 0 0))
                    dest-pos (v2/mul mv ())

                    start-tm (now my-timer)
                    dest-tm (+ start-tm (from-seconds my-timer 2))
                    new-easer player-intention
                    ]


                (recur (v2/add mv pos)
                       (camera cam-pos desired-pos)
                       ) )
              )

            
            )))))






  ) 

;; }}}

;; =============================================================================
;;ends
