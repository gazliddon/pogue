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
    [cloj.math.vec2         :as v2 :refer [vec2]]
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

    [gaz.tiles              :refer [mk-tile-map mix-it-up
                                    render-level!]]
    [gaz.tilemapprotocol    :as tmp]

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
;; {{{ tiles

(defn shit-line [tmap block {:keys [x y] :as pos} add len]
  (let [v2-len (vec2 len len) ]
    (loop [tmap (tmp/set-tile tmap x y block)
           i    (dec len)]
      (if (pos? i)
        (let [{x :x y :y} (->
                            (v2/mul add (vec2 i i))
                            (v2/add pos)) ]
          (println (str x " " y))
          (recur (tmp/set-tile tmap x y block)
                 (dec i)))
        tmap))))

(defn shit-h-line [tmap bl pos len]
  (shit-line tmap bl pos (vec2 1 0) len))

(defn shit-v-line [tmap bl pos len]
  (shit-line tmap bl pos (vec2 0 1) len))


(defn shit-box [tmap bl {:keys [x y] :as pos}  {w :x h :y}]
  (->
    (fn [res y]
      (shit-h-line res bl (v2/add pos (vec2 0 y)) w))
    (reduce tmap (range h))))

(defn shit-room [tmap {:keys [x y] :as pos} {w :x h :y}]
  (let [fl :b-floor
        wl :b-wall ]
    (-> tmap
        (shit-box fl pos (vec2 w h))
        (shit-h-line wl pos w)
        (shit-h-line wl (v2/add (vec2 0 (dec h)) pos) w)
        (shit-v-line wl (vec2 x y) h)
        (shit-v-line wl (v2/add (vec2 (dec w) 0) pos) h)
      )

    )
  )



(defn mk-level-spr [sprs rman id w-b h-b ]
  (let [[w h] [(* 16 w-b) (* 16 h-b)]
        render-target (create-render-target! rman (name id ) w h)
        spr-printer (sprs/mk-spr-printer render-target sprs)
        level (->
                (mk-tile-map w-b h-b :b-blank)
                (shit-room (vec2 3 3) (vec2 10 10 ))
                )
        ]
    (do
      (rp/clear! render-target [0 1 0])
      (render-level! spr-printer level)
      render-target)))

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
            (time-passed))
          )))))

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
  {#{:left :up}    (vec2 -1 -1)
   #{:left :down}  (vec2 -1  1)
   #{:right :up}   (vec2  1 -1)
   #{:right :down} (vec2  1  1)
   #{:up}          (vec2  0 -1)
   #{:down}        (vec2  0  1)
   #{:left}        (vec2 -1  0)
   #{:right}       (vec2  1  0)})

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


(defn camera [current-pos desired-pos]
  (let [scaler (vec2 12 12)
        diff (v2/sub desired-pos current-pos)
        add (v2/div diff scaler)
        new-pos (v2/add current-pos add) ]
    new-pos))

(defn camera [current-pos desired-pos]
  (->
    (v2/sub desired-pos current-pos)
    (v2/div (vec2 12 12))
    (v2/add current-pos)))

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
    (v2/mul (vec2 10 10))
    ))

(defn anim [t s frms]
  (nth frms 
       (mod (int (/ t s)) (count frms))) )

(defn mk-anim-fn [ speed frames ]
  (fn [t]
    (anim t speed frames)))

(def get-bub-frm
  (mk-anim-fn 0.1 [:bub0 :bub1 :bub2 :bub3] ))

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
              rand-spr (fn [] [(rand-nth (keys sprs)) (vec2 (rand-int 200) (rand-int 200) ) (vec2 (rand) (rand))] )
              positions (vec (repeatedly 20 rand-spr))
              level-spr (mk-level-spr sprs rm :level 160 160)
              
              kb-handler (kb/default-kb-handler)

              mid-scr (-> (vec2 (rp/width rend) (rp/height rend) )
                          (v2/mul (vec2 0.5 0.5)))

              scale (vec2 3 3)
              
              ]
          (kb-attach! "game" kb-handler)

          (loop [pos (vec2 20 20)
                 cam-pos (vec2 0 0)]
            (kb-update! kb-handler)

            (let [dt (<! in-chan)
                  t @g-time
                  t-secs (/ t 1000)
                  c-t (* t 1.5)
                  mid-scr (v2/div mid-scr scale)
                  ]

              (doto rend
                (rp/clear! [1 0 1])
                (rp/identity! )
                (rp/scale! scale) 

                (rp/translate! (->
                                   (v2/sub (vec2 0 0) cam-pos) 
                                   (v2/add mid-scr)
                                 ))
                (rp/spr! level-spr (vec2 0 0)))

              (doseq [[img pos uniq] positions]
                (let [final-pos (v2/add pos (funny-vec t-secs uniq))]
                  (rp/spr! spr-printer img final-pos)))
              
              (rp/spr! spr-printer (get-bub-frm t-secs) pos))

            (let [actions (my-decide kb-handler)
                 mv (get combo-vec actions (vec2 0 0))]
              (recur (v2/add mv pos)
                     (camera cam-pos pos)
                     ) )
            )))))




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
